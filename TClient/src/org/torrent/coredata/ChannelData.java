package org.torrent.coredata;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;

import org.torrent.coredata.FlowControls.ChannelStatus;

public class ChannelData {
	
	/**
	 * The constructor.
	 * 
	 * @param nioKey The key to link this channelData to its torrentFile, peerManager and piecePicker objects
	 * @param newPeer The peer we shall connect to 
	 * @param status The status this channel is initialised to
	 * @param socketChannel The channel this object will be associated with
	 */
	public ChannelData(Integer nioKey, Peer newPeer, ChannelStatus status, SocketChannel socketChannel) {
		nioStoresKey = nioKey;
		channelStatus = status;
		peer = newPeer;
		channel = socketChannel;
		channelNum = channelNumCount++;
	}
	
	/**
	 * An alternative constructor not requiring a peer.  This will be use for connecting to a tracker
	 * 
	 * @param nioKey
	 * @param status
	 */
	public ChannelData(Integer nioKey, ChannelStatus status) {
		nioStoresKey = nioKey;
		channelStatus = status;
	}
	
	/**
	 * 
	 * @return The unique number assigned to this channel
	 */
	public int getChannelNum() {
		return channelNum;
	}
	
	/**
	 * 
	 * @param status Sets this channel's status
	 */
	public void setStatus(ChannelStatus status) {
		channelStatus = status;
	}
	
	/**
	 * 
	 * @return This channel's status
	 */
	public ChannelStatus getStatus() {
		return channelStatus;
	}
	
	/**
	 * 
	 * @return the key this channel uses to access HashMaps in NIOThread
	 */
	public Integer getNioKey() {
		return nioStoresKey;
	}
	
	/**
	 * 
	 * @return The peer the channel is assigned to 
	 */
	public Peer getPeer() {
		return peer;
	}
	
	/**
	 * 
	 * @return Whether the channel is interested
	 */
	public boolean isInterested() {
		return connectionStatus[0];
	}
	
	/**
	 * Marks the channel as interested
	 */
	public void setInterested() {
		connectionStatus[0] = true;
	}
	
	/**
	 * Marks the channel as uninterested
	 */
	public void setUninterested() {
		connectionStatus[0] = false;
	}
	
	/**
	 * 
	 * @return Whether the channel is choked or not
	 */
	public boolean isChoked() {
		return connectionStatus[1];
	}
	
	/**
	 * Marks the channel as unchoked
	 */
	public void setUnchoked() {
		connectionStatus[1] = false;
	}
	
	/**
	 * Marks the channel as choked
	 */
	public void setChoked() {
		connectionStatus[1] = true;
	}
		
	/**
	 * 
	 * @return Whether the peer we are connected to has registered interest
	 */
	public boolean peerIsInterested() {
		return connectionStatus[2];
	}
	
	/**
	 * Marks the connected peer as interested
	 */
	public void registerPeerInterest() {
		connectionStatus[2] = true;
	}
	
	/**
	 * Marks the connected peer as uninterested
	 */
	public void setPeerUninterested() {
		connectionStatus[2] = false;
	}

	/**
	 * 
	 * @return Whether the client is choking the connected peer
	 */
	public boolean chokingPeer() {
		return connectionStatus[3];
	}
	
	/**
	 * Marks the connection as not-choking the connected peer 
	 */
	public void unchokePeer() {
		connectionStatus[3] = false;
	}
	
	/**
	 * Marks the connection as choking the connected peer
	 */
	public void chokePeer() {
		connectionStatus[3] = true;
	}
	
	/**
	 * 
	 * @param piece The piece this channel is to work on
	 */
	public void setPiece(int piece) {
		this.piece = piece;
	}
	
	/**
	 * 
	 * @return true if a piece is currently assigned
	 */
	public boolean pieceSet() {
		return piece > -1;
	}
	
	/**
	 * 
	 * @return Returns the piece that is currently assigned to the channel
	 */
	public int getPiece() {
		return piece;
	}
	
	/**
	 * 
	 * @param msg Adds a message to the outboundQueue of this instance
	 */
	public void addMessage(byte[] msg) {
		outboundQueue.add(msg);
		outBoundQueueLength += msg.length;
	}
	
	/**
	 * 
	 * @return Confirms whether any block requests are currently pending
	 */
	public boolean blocksRequested() {
		if(blocksRequested == null) {
			return false;
		}
		
		for(byte b : blocksRequested) {
			if(b == 1) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @return Returns an array consisting of all the messages ready for sending
	 */
	public byte[] getOutboundMessages() {
		byte[] messages = new byte[outBoundQueueLength];
		
		int count = 0;
		while(outboundQueue.size() > 0) {
			byte[] nextMsg = outboundQueue.pop();
			System.arraycopy(nextMsg, 0, messages, count, nextMsg.length);
			count += nextMsg.length; 
		}
		outBoundQueueLength = 0;
		return messages;
	}
	
	/**
	 * 
	 * @return Whether any messages are stored for sending
	 */
	public boolean hasMessages() {
		return outboundQueue.size() > 0;
	}
	
	/**
	 * If blocksRequested is already set, then it gets returned.  If not, then a 2D array of all the request messages needed for the the assigned 
	 * piece is generated along with a 1D array to keep track of which requests have been received.  The 1D is assigned to blocksRequested, whereas
	 * the 2D is assigned to blockRequests
	 * 
	 * @param pieceSize The size of each piece of the torrent file
	 * @param piece The assigned piece
	 * @param blockReqSize The request size being used in th epiece messages
	 * @return A 2D array of all the piece messages needed for a piece, with each row consisting of an individual piece message
	 */
	public byte[][] getBlockRequests(int pieceSize, int piece, int blockReqSize) {
		if(blocksCollected != null) {
			return blockRequests;
		}
		blocksCollected = ByteBuffer.allocate(pieceSize);
				
		blockReqSize = Math.min(blockReqSize, pieceSize);
		int[] numReqs = this.numRequestsReq(blocksCollected, blockReqSize);
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
	
	/**
	 * Returns a 2D array of the number of requests needed to download a piece.  Index 0 in the array specifies the number of requests needed
	 * based on the blockReqSize.  Array position 1 only contains a value if the last request needs to be a different size than the others.  If it's 
	 * empty, then the number of requests required resides solely in index position 0, otherwise index 1 contains the size the final request needs to be.
	 * 
	 * @param buff 
	 * @param blockReqSize
	 * @return
	 */
	private int[] numRequestsReq(ByteBuffer buff, int blockReqSize) {
		int[] results = new int[2];
		results[0] = (buff.capacity() - buff.position()) / blockReqSize;
		results[1] = (buff.capacity() - buff.position()) % blockReqSize;
		
		return results;
	}
	
	/**
	 * Marks a block message as having been requested 
	 *
	 * @param index The index in blocksRequested which corresponds to the message in blockRequests 
	 */
	public void setBlockRequested(int index) {
		blocksRequested[index] = 1;
	}
	
	/**
	 * 
	 * 
	 * @return The array which tracks which piece messages have been requested
	 */
	public byte[] getBlocksRequested() {
		return blocksRequested;
	}
	
	/**
	 * Marks the index position in blocksRequested as received.  The index position corresponds to the index position in blockRequests
	 * representing a piece request message
	 * 
	 * @param offset
	 * @param pieceSize
	 * @param blockReqSize
	 */
	public void setBlockObtained(byte[] offset, int pieceSize, int blockReqSize) {
		int offsetIndex = ByteBuffer.wrap(offset).getInt();
		blockReqSize = Math.min(blockReqSize, pieceSize);
		blocksRequested[offsetIndex / blockReqSize] = -1;
	}
	
	/**
	 * Iterates over blocksRequested, marking any entries that haven't been marked as obtained as unrequested.
	 */
	public void clearRequestedBlocks() {
		for(byte b : blocksRequested) {
			if(b == 1) {
				b = 0;
			}
		}
	}
	
	/**
	 * 
	 * @return Whether all the blocks of the piece have been obtained
	 */
	public boolean pieceComplete() {
		return blocksCollected.position() == blocksCollected.limit();
	}
	
	/**
	 * 
	 * @param num The number to convert into bytes
	 * @return The bytes that form the argument
	 */
	private byte[] getIntBytes(int num) {
		byte[] pieceBytes = new byte[4];
		int count = 0;
			for(int i = 24; i > -8;) {
				pieceBytes[count++] = (byte)(num >>> i);
				i -= 8;
		}
		return pieceBytes;
	}
	
	/**
	 * Stored the new data block into blocksCollected
	 * 
	 * @param offset The offset where the piece should be stored
	 * @param block The data to be stored
	 */
	public void addReceivedBlock(byte[] offset, byte[] block) {
		int offsetIndex = ByteBuffer.wrap(offset).getInt();
		blocksCollected.put(offsetIndex, block, 0, block.length);
		blocksCollected.position(blocksCollected.position() + block.length);
	}
	
	/**
	 * 
	 * @return how much space is left to write in blocksCollected
	 */
	public int getRemainingBlockSpace() {
		return blocksCollected.position();
	}
	
	/**
	 * 
	 * @return Stored data from previous TCP packets
	 */
	public byte[] getStoredTcpPacketBytes() {
		return storedTcpPacketBytes;
	}
	
	/**
	 * Holds bytes until enough packets have come in to parse a complete message
	 * 
	 * @param bytesToStore The bytes to store until the next message
	 */
	public void setStoredTcpPacketBytes(byte[] bytesToStore) {
		storedTcpPacketBytes = bytesToStore;
	}
	
	/**
	 * Clears all data related to storing blocks
	 */
	public void clearBlockData() {
		blocksRequested = null;
		blocksCollected = null;
		storedTcpPacketBytes = null;
	}
	
	/**
	 * 
	 * @return the blocks this channel has collected
	 */
	public ByteBuffer getBlocksCollected() {
		return blocksCollected;
	}
	
	/**
	 * 
	 * @param time A number of seconds to wait
	 */
	public void setTimeout(int time) {
		lastActionTime = System.nanoTime();
		timeout = time;
	}
	
	/**
	 * Checks whether this channel has timed out
	 * 
	 * @return true if the channel has been waiting longer than the timeout period specified
	 */
	public boolean checkForTimeout() {
		return (((System.nanoTime() - lastActionTime) / 1000000000) > timeout) ? true : false;
	}
	
	/**
	 * 
	 * @return The channel associated with this instance of channelData
	 */
	public SocketChannel getChannel() {
		return channel;
	}
	
	/**
	 * 
	 * @return The key this channel used to access HashMaps in NIOThread
	 */
	public SelectionKey getCurrentKey() {
		return currentKey;
	}
	
	/**
	 * 
	 * @param key Sets the key this channelData instance will use
	 */
	public void setCurrentKey(SelectionKey key) {
		currentKey = key;
	}
	
	/**
	 * Contains any messages to be sent when the channel is next configured to write.  This could be empty if no messages need sending
	 */
	private ArrayDeque<byte[]> outboundQueue = new ArrayDeque<byte[]>();
	
	/**
	 * The cumulative length of the messages in the outboundQueue, used to create a single large byte array for sending
	 */
	private int outBoundQueueLength = 0;
	
	/**
	 * Stores a time, used to help calculate if a timeout has occured
	 */
	private long lastActionTime;
	
	/**
	 * Stored a number of seconds to wait for a timeout, used with lastActionTime to calculate if a timeout has occurred
	 */
	private int timeout;
	
	/**
	 * A status used to control the channel's flow through the code
	 */
	private ChannelStatus channelStatus;
	
	/**
	 * This array holds values for choking and interest for both the client and a remote peer
	 */
	private Boolean[] connectionStatus = new Boolean[]{false, true, false, true};
	
	/**
	 * The key used to access HashMaps stored in NIOthread containing objects such as a PeerManager
	 */
	private Integer nioStoresKey;
	
	/**
	 * Stored the piece the channel is currently working on, or -1 if no piece is set
	 */
	private int piece = -1;
	
	/**
	 * This and blocksRequested work together to track block requests.  This variable contains the piece messages that need to be sent.
	 * The amount of messages stored here will be equal to the amount of messages needed to get the whole of the target piece.
	 */
	private byte[][] blockRequests;
	
	/**
	 * This and blockRequests work together to track block requests.  This variable keeps track of which piece messages have been sent and received and the size
	 * of this array directly corresponds with the size of blockRequests 1st column.  As a result the index values in this array match up with piece request messages
	 * in blockRequests. For blocksRequested, a value of 1 means that the message has been sent and a value
	 * of -1 means that the response has been successfully received.
	 */
	private byte[] blocksRequested = null;
	
	/**
	 * This holds the actual block data that is received
	 */
	private ByteBuffer blocksCollected = null;
	
	/**
	 * Used to temporarily hold received tcp packet data until a full message can be constructed
	 */
	private byte[] storedTcpPacketBytes = null;
	
	/**
	 * A reference to the channel this instance is tracking data for
	 */
	private SocketChannel channel;
	
	/**
	 * A reference to the peer this channel is communicating with
	 */
	private Peer peer;
	
	/**
	 * The current key assigned to the channel and registered with the selector
	 */
	private SelectionKey currentKey = null;
	
	/**
	 * A unique number for the channel
	 */
	private int channelNum;
	
	/**
	 * Used to generate unique numbers for the channel
	 */
	private static int channelNumCount = 0;
	
}
