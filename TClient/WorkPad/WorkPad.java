import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;

@SuppressWarnings("unused")
public class WorkPad {

	/*
	request: <len=0013><id=6><index><begin><length>

	The request message is fixed length, and is used to request a block. The payload contains the following information:

	    index: integer specifying the zero-based piece index
	    begin: integer specifying the zero-based byte offset within the piece
	    length: integer specifying the requested length.
	*/
	
	
	public static void main(String[] args) throws IOException {
		
		byte[] message = Files.readAllBytes(Paths.get("/home/mkg/Desktop/ComboTest"));
		System.out.println("Test Bytes Length: " + message.length);
		System.out.println("=====================");
		
		

		ArrayDeque<byte[]> messages = new ArrayDeque<byte[]>();
		
		
		for(int i = 0; i < message.length; i++) {
			messages = WorkPad.extractMessagesV2(new byte[] {message[i]});
			
			
			for(byte[] bytes : messages){
				System.out.print(bytes.length + " - " + "Captured Message: ");
				WorkPad.hexPrint(bytes);
				System.out.println();
			}
			
			
		}
		
	
		//messages = WorkPad.extractMessagesV2(message);
			
			
		
			
			
			
			
			
		
		
		
		
	}
	
	private static ArrayDeque<byte[]> extractMessagesV2(byte[] message){
		ArrayDeque<byte[]> parsedMessages = new ArrayDeque<byte[]>();
		
		byte[] allMsgBytes = message;
		
		if(storedTcpPacketBytes != null) {//Combine bytes from previous message if they are there
			allMsgBytes = new byte[message.length + storedTcpPacketBytes.length];
			System.arraycopy(storedTcpPacketBytes, 0, allMsgBytes, 0, storedTcpPacketBytes.length);
			System.arraycopy(message, 0, allMsgBytes, storedTcpPacketBytes.length, message.length);
		} 
		
		int index = 0;
		while(index < allMsgBytes.length) {
			storedTcpPacketBytes = null;
			if((allMsgBytes.length - index) >= 5) {//Minimum needed for message length & ID
				byte[] handshakePrefix = new byte[] {(byte)0x13, (byte)0x42, (byte)0x69, (byte)0x74, (byte)0x54};
				if(index < 5 && Arrays.compare(Arrays.copyOfRange(allMsgBytes, index, 5), handshakePrefix) == 0) {//Msg starts with handshake
					if(allMsgBytes.length >= 68) {//Rest of handshake is present
						byte[] handshake = new byte[68];
						System.arraycopy(Arrays.copyOfRange(allMsgBytes, 0, 68), 0, handshake, 0, 68);
						parsedMessages.add(handshake);
						index = 68;
						continue;
					} else {
						storedTcpPacketBytes = allMsgBytes;
						index = allMsgBytes.length;
						break;
						
					}
				} 
				
				
				int msgLength = ByteBuffer.wrap(Arrays.copyOfRange(allMsgBytes, index, index + 4)).getInt();
				byte msgID = allMsgBytes[index + 4];
				index += 4;
				if((allMsgBytes.length - index) >= msgLength ) {//Amount remaining is enough for whole message
					byte[] newMessage = new byte[msgLength];
					
					System.arraycopy(Arrays.copyOfRange(allMsgBytes, index, index + msgLength), 0, newMessage, 0, msgLength);
					parsedMessages.add(newMessage);
					index += msgLength;
				} else {
					storedTcpPacketBytes = Arrays.copyOfRange(allMsgBytes, index - 4, allMsgBytes.length);
					break;
				}
				
			} else {
				storedTcpPacketBytes = Arrays.copyOfRange(allMsgBytes, index, allMsgBytes.length);
				break;
			}
		}
		
		
		return parsedMessages;
	}
	
	
	
	private static ArrayDeque<byte[]> extractMessages(byte[] message){
		
		byte[] allMsgBytes;
		
		if(storedTcpPacketBytes != null) {//Combine bytes from previous message if they are there
			allMsgBytes = new byte[message.length + storedTcpPacketBytes.length];
			System.arraycopy(storedTcpPacketBytes, 0, allMsgBytes, 0, storedTcpPacketBytes.length);
			System.arraycopy(message, 0, allMsgBytes, storedTcpPacketBytes.length, message.length);
		} else {
			allMsgBytes = message;
		}
		
		
		
		
		ArrayDeque<byte[]> messages = new ArrayDeque<byte[]>();
		if(allMsgBytes.length < 4) {//Can't get message length, so wait for more info
			storedTcpPacketBytes = message;
			return messages;
		}
		
		int index = 0;
		while(index < allMsgBytes.length) {
			if(allMsgBytes.length >= 68 && index == 0) {
				String messageStart = allMsgBytes[0] + new String(Arrays.copyOfRange(allMsgBytes, 1, 20), StandardCharsets.ISO_8859_1);
				if(messageStart.equals("19BitTorrent protocol")) {
					byte[] handshake = new byte[69];
					handshake[0] = 19;
					System.arraycopy(Arrays.copyOfRange(allMsgBytes, 0, 68), 0, handshake, 1, 68);
					messages.add(handshake);
					index = 68;
				}
			}
			
			ByteBuffer buff = ByteBuffer.wrap(Arrays.copyOfRange(allMsgBytes, index, index + 4));
			int messageLength = buff.getInt();
			
			
			index += 4;
			byte[] idAndPayload = Arrays.copyOfRange(allMsgBytes, index, index + messageLength);
			messages.add(idAndPayload);
			
			index += idAndPayload.length;
		}
		return messages;
	}
	
	
	private int getBlockIndex(byte[] offset, int pieceSize) {
		int offsetIndex = ByteBuffer.wrap(offset).getInt();
		blockReqSize = Math.min(blockReqSize, pieceSize);
		
		return offsetIndex / blockReqSize; 
	}
	
	
	private static void hexPrint(byte[][] bytes) {
		for(int i = 0; i < bytes.length; i++) {
			int count = 0;
			for(byte b : bytes[i]) {
				if(count == 0) {
					System.out.print("Length: ");
				}
				
				if(count == 4) {
					System.out.print("ID: ");
				}

				if(count == 5) {
					System.out.print("Piece: ");
				}
				
				if(count == 9) {
					System.out.print("Offset: ");
				}
				
				if(count == 13) {
					System.out.print("Length: ");
				}

				System.out.print(String.format("%02X", ((byte)b) & 0xFF) + " ");
				count++;
			}
			System.out.println();
		}
	}
	
	private static void hexPrint(byte[] bytes) {
		for(byte b : bytes) {
			//System.out.print(String.format("%-2s", Integer.toHexString(((byte)b) & 0xFF).toUpperCase()) + " ");
			System.out.print(String.format("%02X", ((byte)b) & 0xFF).toUpperCase() + " ");
		}
		System.out.println();
	}
	
	
	public byte[] getOutboundMessages() {
		byte[] messages = new byte[outBoundQueueLength];
		Iterator<byte[]> iterator = outboundQueue.iterator();
		int count = 0;
		while(iterator.hasNext()) {
			byte[] nextMsg = iterator.next();
			System.arraycopy(nextMsg, 0, messages, count, nextMsg.length);
			count += nextMsg.length; 
		}
		outBoundQueueLength = 0;
		return messages;
	}
	
	public byte[] getOutboundMessage() {
		return outboundQueue.pop();
	}
	
	public boolean messageReady() {
		return outboundQueue.size() > 0;
	}
	
	public byte[][] getBlockRequests(int pieceSize, int piece, int blockReqSize) {
		if(blocks != null) {
			return blockRequests;
		}
		blocks = ByteBuffer.allocate(pieceSize);
				
		
		//TODO blocks == null
		blockReqSize = Math.min(blockReqSize, pieceSize);
		int[] numReqs = this.numRequestsReq(blocks, blockReqSize);
		int totalReqs = numReqs[0] + (numReqs[1] == 0 ? 0 : 1);
		
		byte[][] requests = new byte[totalReqs][17];
		for(int i = 0; i < totalReqs; i++) {
			System.arraycopy(this.getIntBytes(13), 0, requests[i], 0, 4);
			requests[i][4] = (byte)6;
			System.arraycopy(this.getIntBytes(piece), 0, requests[i], 5, 4);
			System.arraycopy(this.getIntBytes(i * blockReqSize), 0, requests[i], 9, 4);
			
			if(i == (totalReqs - 1) && (pieceSize % blockReqSize) != 0) {
				System.arraycopy(this.getIntBytes(pieceSize % blockReqSize), 0, requests[i], 13, 4);
			} else {
				System.arraycopy(this.getIntBytes(blockReqSize), 0, requests[i], 13, 4);
			}
		}
		
		blocksRequested = new byte[totalReqs];
		blockRequests = requests;
		
		return requests;
	}
	
	private int[]  numRequestsReq(ByteBuffer buff, int blockReqSize) {
		int[] results = new int[2];
		results[0] = (buff.capacity() - buff.position()) / blockReqSize;
		results[1] = (buff.capacity() - buff.position()) % blockReqSize;
		
		return results;
	}
	
	
	private static void setBit(int piece, byte[] bitfield, boolean setOn) {
		int bitToCheck = piece - 1, curByte = bitToCheck / 8, offset = bitToCheck % 8;
		if(setOn) {
			bitfield[curByte] |= 1 << (7 - offset);	
		} else {
			bitfield[curByte] &= ~(1 << (7 - offset));
		} 
	}
		

	
	
	private static boolean checkBit(int piece, byte[] bitfield) {
		int bitToCheck = piece - 1;
		int byteToCheck = bitToCheck / 8;
		int offset = bitToCheck % 8;
		
		return ((bitfield[byteToCheck] & 1 << (7 - offset)) != 0) ? true : false; 
	}
	
	private byte[] getIntBytes(int num) {
		byte[] pieceBytes = new byte[4];
		int count = 0;
			for(int i = 24; i > -8;) {
				pieceBytes[count++] = (byte)(num >>> i);
				i -= 8;
		}
		return pieceBytes;
	}
	
	 
	private static byte[] storedTcpPacketBytes = null;
	

	private ArrayDeque<byte[]> outboundQueue = new ArrayDeque<byte[]>();
	private int piece = 1;
	private byte[][] blockRequests;
	private ByteBuffer blocks = null;
	private byte[] blocksRequested = null;
	private int outBoundQueueLength = 0;
	private int numBlocksRequested = 0;
	private int blockReqSize = 16000;
	
	
	
}
