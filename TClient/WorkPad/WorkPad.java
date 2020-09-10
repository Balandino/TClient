import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.torrent.coredata.PiecePicker;

@SuppressWarnings("unused")
public class WorkPad {
	public static void main(String[] args) throws Exception {
		

//		byte[] data = new byte[16000];
//		Arrays.fill(data, (byte)1);
//		
//		
//		
//		Path pathway = Paths.get("/home/mkg/Desktop/WorkpadTest.txt");
//		Files.deleteIfExists(pathway);
//		int offset = 0;
//		
//		PieceWriter writer = new PieceWriter();
//		writer.start();
//		
//		
//		long start = System.nanoTime();
//		
//		writer.addPiece(data, offset, pathway);
//		
//		long end = System.nanoTime();
//		
//		System.out.println("WorkPad: " + (end - start) / 1000000 + " Milliseconds");
//		
//		
//		
//		writer.keepRunning = false;
		
		
		long start = System.nanoTime();
		
		Thread.sleep(4000);
		
		long end = System.nanoTime();
		
		System.out.println((end - start) / 1_000_000_000);
		
		
		
	}
}
	
	

	
	
	
	
	
	

	
	
	
