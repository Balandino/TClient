import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Arrays;

import org.torrent.coredata.ChannelData;
import org.torrent.coredata.FlowControls.ChannelStatus;

public class WorkPad {
	public static void main(String[] args) throws Exception {
		
		byte[] messageData = Files.readAllBytes(Paths.get("/home/mkg/Desktop/debugMsgs"));
		System.out.println("Data Length: " + messageData.length);
		ChannelData cd = new ChannelData(0, ChannelStatus.PROCESSING_MESSAGES);
		
		
		
		ArrayDeque<byte[]> messages = WorkPad.extractMessages(cd, messageData);
		System.out.println("ArrayDeque Length: " + messages.size());
		for(byte[] msg : messages) {
			System.out.print("msg Length: " + msg.length + " ID: ");
			System.out.println(msg[0]);
		}
	}
	
	
	private static ArrayDeque<byte[]> extractMessages(ChannelData channelData, byte[] message){
		ArrayDeque<byte[]> parsedMessages = new ArrayDeque<byte[]>();
		
		byte[] storedTcpPacketBytes = channelData.getStoredTcpPacketBytes();
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
						channelData.setStoredTcpPacketBytes(storedTcpPacketBytes);
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
					channelData.setStoredTcpPacketBytes(storedTcpPacketBytes);
					break;
				}
				
			} else {
				storedTcpPacketBytes = Arrays.copyOfRange(allMsgBytes, index, allMsgBytes.length);
				channelData.setStoredTcpPacketBytes(storedTcpPacketBytes);
				break;
			}
		}
		return parsedMessages;
	}
	
}
