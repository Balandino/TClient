import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class WorkPad {
	public static void main(String[] args) throws Exception {
		
		byte[] haveMsg = Files.readAllBytes(Paths.get("/home/mkg/Desktop/Have Message"));
		
		haveMsg = Arrays.copyOfRange(haveMsg, 4, 9);
		System.out.println(Arrays.toString(haveMsg));
		
		System.out.println(Arrays.toString(Arrays.copyOfRange(haveMsg, 1, 4)));
		int piece = ByteBuffer.wrap(Arrays.copyOfRange(haveMsg, 1, 5)).getInt();
		System.out.println(piece);
		
		
		
		
		
	}
}
