public class JavaLoopsHomework {
	public static void main(String[] args) {
      int[] collection = {0,1,1,2,3,4,5,5,6,6,7,8,8,8,9,10,12,13,14,15,69,1717,1984,2020,69420};        // Initializes the list
      int i=0;                                                                                          // Uses int i to indicate which number in the list to refer to.
      while(i < collection.length){                                                                     // Loops only while i is less than the amount of numbers in the list.
         if (collection[i]%2==0){                                                                       // Tests to see if the remainder is 0 when dividing by two to test for even numbers.
            System.out.println(collection[i]);                                                          // Prints the number if it succeeded in the prior even number test.
         }
      i++;                                                                                              // Increases i by one in order to move onto the next number in the list.
      }
   }
}