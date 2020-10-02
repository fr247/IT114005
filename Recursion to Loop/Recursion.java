public class Recursion {
  
	public static int sum(int num) {
      int sumValue = num;
		while (num > 0) {
         num--;
			sumValue = sumValue + num;
		}
		if (num == sumValue){sumValue = 0;}
      return sumValue;
	}
   

	public static void main(String[] args) {
		System.out.println(sum(100));
	}
}