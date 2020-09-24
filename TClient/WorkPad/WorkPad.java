import java.util.Arrays;

public class WorkPad {
	public static void main(String[] args) throws Exception {
		

		int[][]pieces = new int[5][2];
		for (int i = 0; i < pieces.length; i++) {
			pieces[i][0] = i;
		}
		
		WorkPad.printArray(pieces);
		
		
		
		pieces[3][1] = -1;
		
		WorkPad.printArray(pieces);
		
		Arrays.parallelSort(pieces, (b, a) -> Integer.compare(b[1], a[1]));
		
		
		
		System.out.println();
		WorkPad.printArray(pieces);
		
		
		System.out.println();
		System.out.println((double)7/10 * 100 > 10);
		
	}
	
	
	private static void printArray(int[][] pieces) {
		System.out.print("Pieces: ");
		for(int i = 0; i < pieces.length; i++) {
			System.out.print(String.format("%-4s", pieces[i][0]) + " ");
		}
		System.out.println();
		
		System.out.print("  Freq: ");
		for(int i = 0; i < pieces.length; i++) {
			System.out.print(String.format("%-4s", pieces[i][1]) + " ");
		}
		System.out.println();
	}
	
}
	
	

	
	
	
	
	
	

	
	
	
