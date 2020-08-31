package org.torrent.coredata;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Iterator;

import org.torrent.coredata.FlowControls.ChannelStatus;

@SuppressWarnings("unused")
public class ChannelData {
	
	public ChannelData(Integer nioKey, Peer newPeer, ChannelStatus status, SocketChannel socketChannel) {
		nioStoresKey = nioKey;
		channelStatus = status;
		peer = newPeer;
		channel = socketChannel;
	}
	
	public ChannelData(Integer nioKey, ChannelStatus status) {
		nioStoresKey = nioKey;
		channelStatus = status;
	}
	
	public void setStatus(ChannelStatus status) {
		channelStatus = status;
	}
	
	public ChannelStatus getStatus() {
		return channelStatus;
	}
	
	public Integer getNioKey() {
		return nioStoresKey;
	}
	
	public Peer getPeer() {
		return peer;
	}
	
	public boolean isInterested() {
		return connectionStatus[0];
	}
	
	public void setInterested() {
		connectionStatus[0] = true;
	}
	
	public void setUninterested() {
		connectionStatus[0] = false;
	}
	
	public boolean isChoked() {
		return connectionStatus[1];
	}
	
	public void setUnchoked() {
		connectionStatus[1] = false;
	}
	
	public void setChoked() {
		connectionStatus[1] = true;
	}
		
	public boolean peerIsInterested() {
		return connectionStatus[2];
	}
	
	public void registerPeerInterest() {
		connectionStatus[2] = true;
	}
	
	public void setPeerUninterested() {
		connectionStatus[2] = false;
	}

	public boolean chokingPeer() {
		return connectionStatus[3];
	}
	
	public void unchokePeer() {
		connectionStatus[3] = false;
	}
	
	public void chokePeer() {
		connectionStatus[3] = true;
	}
	
	public void setPiece(int piece) {
		this.piece = piece;
	}
	
	public boolean pieceSet() {
		return piece > -1;
	}
	
	public int getPiece() {
		return piece;
	}
	
	public void addMessage(byte[] msg) {
		outboundQueue.add(msg);
		outBoundQueueLength += msg.length;
	}
	
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
	
	public byte[] getOutboundMessage() {
		return outboundQueue.pop();
	}
	
	public boolean hasMessages() {
		return outboundQueue.size() > 0;
	}
	
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
	
	private int[] numRequestsReq(ByteBuffer buff, int blockReqSize) {
		int[] results = new int[2];
		results[0] = (buff.capacity() - buff.position()) / blockReqSize;
		results[1] = (buff.capacity() - buff.position()) % blockReqSize;
		
		return results;
	}
	
	
	public void setBlockRequested(int index) {
		blocksRequested[index] = 1;
	}
	
	public byte[] getBlocksRequested() {
		return blocksRequested;
	}
	
	public void setBlockObtained(byte[] offset, int pieceSize, int blockReqSize) {
		int offsetIndex = ByteBuffer.wrap(offset).getInt();
		blockReqSize = Math.min(blockReqSize, pieceSize);
		blocksRequested[offsetIndex / blockReqSize] = -1;
	}
	
	public void clearRequestedBlocks() {
		for(byte b : blocksRequested) {
			if(b == 1) {
				b = 0;
			}
		}
		
	}
	
	public boolean pieceComplete() {
		return blocksCollected.position() == blocksCollected.limit();
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
	
	public void addReceivedBlock(byte[] offset, byte[] block) {
		int offsetIndex = ByteBuffer.wrap(offset).getInt();
		blocksCollected.put(offsetIndex, block, 0, block.length);
		blocksCollected.position(blocksCollected.position() + block.length);
	}
	
	public byte[] getStoredTcpPacketBytes() {
		return storedTcpPacketBytes;
	}
	
	public void setStoredTcpPacketBytes(byte[] bytesToStore) {
		storedTcpPacketBytes = bytesToStore;
	}
	
	public void clearBlockData() {
		blocksRequested = null;
		blocksCollected = null;
		storedTcpPacketBytes = null;
	}
	
	public ByteBuffer getBlocksCollected() {
		return blocksCollected;
	}
	
	public void setTimeout(int time) {
		lastActionTime = System.nanoTime();
		timeout = time;
	}
	
	public boolean checkForTimeout() {
		return (((System.nanoTime() - lastActionTime) / 1000000000) > timeout) ? true : false;
	}
	
	public SocketChannel getChannel() {
		return channel;
	}

	
	
	private ArrayDeque<byte[]> outboundQueue = new ArrayDeque<byte[]>();
	private int outBoundQueueLength = 0;
	private long interestTimeout;
	private long lastActionTime;
	private int timeout;
	private ChannelStatus channelStatus;
	private Boolean[] connectionStatus = new Boolean[]{false, false, false, false};
	private Integer nioStoresKey;
	private boolean seedingApproved = true;
	private int piece = -1;
	private byte[][] blockRequests;
	private byte[] blocksRequested = null;
	private ByteBuffer blocksCollected = null;
	private byte[] storedTcpPacketBytes = null;
	private SocketChannel channel;
	private int numBlocksRequested = 0;
	private Peer peer;
	
}
