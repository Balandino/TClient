import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.stream.IntStream;

@SuppressWarnings("unused")
public class WorkPad {

		
	public static void main(String[] args) throws Exception {
		
		
		
		
	
	
	
	
	}
	
	
//	public byte[] getOutboundMessages() {
//		byte[] messages = new byte[outBoundQueueLength];
//		Iterator<byte[]> iterator = outboundQueue.iterator();
//		int count = 0;
//		while(iterator.hasNext()) {
//			byte[] nextMsg = iterator.next();
//			System.arraycopy(nextMsg, 0, messages, count, nextMsg.length);
//			count += nextMsg.length; 
//		}
//		outBoundQueueLength = 0;
//		return messages;
//	}
//	
	
	public static ArrayDeque<byte[]> outBoundQueue = new ArrayDeque<byte[]>();
}
