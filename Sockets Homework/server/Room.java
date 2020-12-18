package server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Random;

public class Room implements AutoCloseable {

    Random rand = new Random();
    private static SocketServer server;// used to refer to accessible server functions
    private String name;
    private final static Logger log = Logger.getLogger(Room.class.getName());

    // Commands
    private final static String COMMAND_TRIGGER = "/";
    private final static String CREATE_ROOM = "createroom";
    private final static String JOIN_ROOM = "joinroom";
    private final static String ROLL = "roll";
    private final static String FLIP = "flip";
    private final static String MUTE = "mute";
    private final static String UNMUTE = "unmute";
    private final static String PM = "@";

    public Room(String name) {
	this.name = name;
    }

    public static void setServer(SocketServer server) {
	Room.server = server;
    }

    public String getName() {
	return name;
    }

    private List<ServerThread> clients = new ArrayList<ServerThread>();

    protected synchronized void addClient(ServerThread client) {
	client.setCurrentRoom(this);
	if (clients.indexOf(client) > -1) {
	    log.log(Level.INFO, "Attempting to add a client that already exists");
	}
	else {
	    clients.add(client);
	    if (client.getClientName() != null) {
		client.sendClearList();
		sendConnectionStatus(client, true, "joined the room " + getName());
		updateClientList(client);
	    }
	}
    }

    private void updateClientList(ServerThread client) {
	Iterator<ServerThread> iter = clients.iterator();
	while (iter.hasNext()) {
	    ServerThread c = iter.next();
	    if (c != client) {
		boolean messageSent = client.sendConnectionStatus(c.getClientName(), true, null);
	    }
	}
    }

    protected synchronized void removeClient(ServerThread client) {
	clients.remove(client);
	if (clients.size() > 0) {
	    // sendMessage(client, "left the room");
	    sendConnectionStatus(client, false, "left the room " + getName());
	}
	else {
	    cleanupEmptyRoom();
	}
    }

    private void cleanupEmptyRoom() {
	// If name is null it's already been closed. And don't close the Lobby
	if (name == null || name.equalsIgnoreCase(SocketServer.LOBBY)) {
	    return;
	}
	try {
	    log.log(Level.INFO, "Closing empty room: " + name);
	    close();
	}
	catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    protected void joinRoom(String room, ServerThread client) {
	server.joinRoom(room, client);
    }

    protected void joinLobby(ServerThread client) {
	server.joinLobby(client);
    }

    /***
     * Helper function to process messages to trigger different functionality.
     * 
     * @param message The original message being sent
     * @param client  The sender of the message (since they'll be the ones
     *                triggering the actions)
     */
    private boolean processCommands(String message, ServerThread client) {
	boolean wasCommand = false;
   boolean userExists = false;
	try {
      if (message.indexOf(PM) > -1) {
         userExists = false;
         wasCommand = true;
		   String[] comm = message.split(PM);
         message = message.replaceAll(PM, "");
		   log.log(Level.INFO, message);
		   String part1 = comm[1];
		   String[] comm2 = part1.split(" ", 2);
         Iterator<ServerThread> iter = clients.iterator();
	       while (iter.hasNext()) {
	         ServerThread clientSearch = iter.next();
            if (clientSearch.getClientName().equals(comm2[0])){
               sendPrivateMessage(client, "<i>"+client.getClientName()+">You: "+comm2[1]+"</i>", comm2[0]);
               sendPrivateMessage(client, "<i>You>"+comm2[0]+": "+comm2[1]+"</i>", client.getClientName());
               userExists = true;
               break;
               }
               }
           if(!userExists){sendPrivateMessage(client, "<font color='red'><i>User, "+comm2[0]+" does not exist.</i></font>", client.getClientName());}

         }

	    if (message.indexOf(COMMAND_TRIGGER) > -1) {
		String[] comm = message.split(COMMAND_TRIGGER);
		log.log(Level.INFO, message);
		String part1 = comm[1];
		String[] comm2 = part1.split(" ", 2);
		String command = comm2[0];
		if (command != null) {
		    command = command.toLowerCase();
		}
		String roomName;
		switch (command) {
		case CREATE_ROOM:
		    roomName = comm2[1];
		    if (server.createNewRoom(roomName)) {
			joinRoom(roomName, client);
		    }
		    wasCommand = true;
		    break;
		case JOIN_ROOM:
		    roomName = comm2[1];
		    joinRoom(roomName, client);
		    wasCommand = true;
		    break;
      case FLIP:
          int flipAnswer = rand.nextInt(2);
          wasCommand = true;
          if (flipAnswer == 0){
             broadcastMessage(client, "<font color='purple'><b>"+ client.getClientName() + " flipped a coin! It landed on heads.</b></font>");
          }
          if (flipAnswer == 1){
             broadcastMessage(client, "<font color='purple'><b>"+ client.getClientName() + " flipped a coin! It landed on tails.</b></font>");
          }
          break;
      case ROLL:
          int rollAnswer = rand.nextInt(6)+1;
          wasCommand = true;
          broadcastMessage(client, "<font color='purple'><b>"+ client.getClientName() + " used a die! They rolled a "+rollAnswer+".</b></font>");
          break;
      case MUTE:
          wasCommand = true;
          userExists = false;
          Iterator<ServerThread> iter = clients.iterator();
	       while (iter.hasNext()) {
	         ServerThread clientSearch = iter.next();
            if (clientSearch.getClientName().equals(comm2[1])){
               sendPrivateMessage(client, "<font color='red'><b>"+comm2[1]+" has been muted. You will no longer be able to see their messages.</b></font>", client.getClientName());
               sendPrivateMessage(client, "<font color='blue'><b>You have been muted by "+client.getClientName()+". They will no longer be able to see your messages.</b></font>", comm2[1]);
               userExists = true;
               break;
               }
               }
               if(!userExists){sendPrivateMessage(client, "<font color='red'><b>User, "+comm2[1]+" does not exist.</b></font>", client.getClientName());}
            
          break;
      case UNMUTE:
          wasCommand = true;
          sendPrivateMessage(client, "<font color='red'><b>"+comm2[1]+" has been unmuted. You can see their messages again.</b></font>", client.getClientName());
          sendPrivateMessage(client, "<font color='blue'><b>You have been unmuted by "+client.getClientName()+". They can see your messages again.", comm2[1]);
          break;

		}
	    }
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
	return wasCommand;
    }

    // TODO changed from string to ServerThread
    protected void sendConnectionStatus(ServerThread client, boolean isConnect, String message) {
	Iterator<ServerThread> iter = clients.iterator();
	while (iter.hasNext()) {
	    ServerThread c = iter.next();
	    boolean messageSent = c.sendConnectionStatus(client.getClientName(), isConnect, message);
	    if (!messageSent) {
		iter.remove();
		log.log(Level.INFO, "Removed client " + c.getId());
	    }
	}
    }

    /***
     * Takes a sender and a message and broadcasts the message to all clients in
     * this room. Client is mostly passed for command purposes but we can also use
     * it to extract other client info.
     * 
     * @param sender  The client sending the message
     * @param message The message to broadcast inside the room
     */
    protected void sendMessage(ServerThread sender, String message) {
	log.log(Level.INFO, getName() + ": Sending message to " + clients.size() + " clients");
	if (processCommands(message, sender)) {
	    // it was a command, don't broadcast
	    return;
   
	}
   while (message.indexOf("**") > -1){
       try{
          message = message.replaceFirst("\\*\\*","<b>");
          message = message.replaceFirst("\\*\\*","</b>");
          }
       catch(Exception e){
          break;
          }
       }
   while (message.indexOf("*") > -1){
       try{
          message = message.replaceFirst("\\*","<i>");
          message = message.replaceFirst("\\*","</i>");
          }
       catch(Exception e){
          break;
          }
       }
   while (message.indexOf("__") > -1){
       try{
          message = message.replaceFirst("__","<U>");
          message = message.replaceFirst("__","</U>");
          }
       catch(Exception e){
          break;
          }
       }
   while (message.indexOf("~~") > -1){
       try{
          message = message.replaceFirst("~~","<strike>");
          message = message.replaceFirst("~~","</strike>");
          }
       catch(Exception e){
          break;
          }
       }
	Iterator<ServerThread> iter = clients.iterator();
	while (iter.hasNext()) {
	    ServerThread client = iter.next();
	    boolean messageSent = client.send(sender.getClientName(), message);
	    if (!messageSent) {
		iter.remove();
		log.log(Level.INFO, "Removed client " + client.getId());
	    }
	}
    }
    
    protected void sendPrivateMessage(ServerThread sender, String message, String receiver) {
	log.log(Level.INFO, getName() + ": Sending private message to client");
   while (message.indexOf("**") > -1){
       try{
          message = message.replaceFirst("\\*\\*","<b>");
          message = message.replaceFirst("\\*\\*","</b>");
          }
       catch(Exception e){
          break;
          }
       }
   while (message.indexOf("*") > -1){
       try{
          message = message.replaceFirst("\\*","<i>");
          message = message.replaceFirst("\\*","</i>");
          }
       catch(Exception e){
          break;
          }
       }
   while (message.indexOf("__") > -1){
       try{
          message = message.replaceFirst("__","<U>");
          message = message.replaceFirst("__","</U>");
          }
       catch(Exception e){
          break;
          }
       }
   while (message.indexOf("~~") > -1){
       try{
          message = message.replaceFirst("~~","<strike>");
          message = message.replaceFirst("~~","</strike>");
          }
       catch(Exception e){
          break;
          }
       }
	Iterator<ServerThread> iter = clients.iterator();
	while (iter.hasNext()) {
	    ServerThread client = iter.next();
	    if (client.getClientName().equals(receiver)){
         boolean messageSent = client.sendbc(sender.getClientName(), message);
	      //if (!messageSent) {
		     // iter.remove();
		      //log.log(Level.INFO, "Removed client " + client.getId());
	     // }
      }
	}
    }

    
    protected void broadcastMessage(ServerThread sender, String message) {
	log.log(Level.INFO, getName() + ": Sending message to " + clients.size() + " clients");
   	Iterator<ServerThread> iter = clients.iterator();
	while (iter.hasNext()) {
	    ServerThread client = iter.next();
	    boolean messageSent = client.sendbc(sender.getClientName(), message);
	    if (!messageSent) {
		iter.remove();
		log.log(Level.INFO, "Removed client " + client.getId());
	    }
	}
    }

    /***
     * Will attempt to migrate any remaining clients to the Lobby room. Will then
     * set references to null and should be eligible for garbage collection
     */
    @Override
    public void close() throws Exception {
	int clientCount = clients.size();
	if (clientCount > 0) {
	    log.log(Level.INFO, "Migrating " + clients.size() + " to Lobby");
	    Iterator<ServerThread> iter = clients.iterator();
	    Room lobby = server.getLobby();
	    while (iter.hasNext()) {
		ServerThread client = iter.next();
		lobby.addClient(client);
		iter.remove();
	    }
	    log.log(Level.INFO, "Done Migrating " + clients.size() + " to Lobby");
	}
	server.cleanupRoom(this);
	name = null;
	// should be eligible for garbage collection now
    }

}