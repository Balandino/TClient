import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.EnumSet;

public class PieceWriter extends Thread {
	
		
		public void addPiece(byte[] pieceBlocks, int offset, Path pathToFile) {
			pieceData.addLast(pieceBlocks);
			offsets.addLast(offset);
			pathways.addLast(pathToFile);
		}
		
		
		@Override
		public void run() {
			while(keepRunning || pieceData.size() > 0) {
				if(pieceData.size() > 0 && offsets.size() > 0 && pathways.size() > 0) {
					try (FileChannel fileChannel = (FileChannel) Files.newByteChannel(pathways.removeFirst(), EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ))){
						MappedByteBuffer mapBuff = fileChannel.map(FileChannel.MapMode.READ_WRITE, offsets.removeFirst(), pieceData.getFirst().length);
						mapBuff.put(pieceData.removeFirst());
					} catch (IOException io){
						io.printStackTrace();
						System.exit(0);
					}
				}
			}
		}
		
		
	public boolean keepRunning = true;
	private ArrayDeque<byte[]> pieceData = new ArrayDeque<byte[]>();
	private ArrayDeque<Integer> offsets = new ArrayDeque<Integer>();
	private ArrayDeque<Path> pathways = new ArrayDeque<Path>();
}
