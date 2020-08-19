package org.torrent.coredata;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Iterator;

import org.torrent.coredata.FlowControls.ChannelStatus;

@SuppressWarnings("unused")
public class ChannelData {
	
	public ChannelData(Integer nioKey, Peer peer, ChannelStatus status) {
		nioStoresKey = nioKey;
		channelStatus = status;
		this.peer = peer;
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
			requests[i][0] = (byte)0;
			requests[i][1] = (byte)0;
			requests[i][2] = (byte)0;
			requests[i][3] = (byte)13;
			requests[i][4] = (byte)6;
			System.arraycopy(this.getIntBytes(piece), 0, requests[i], 5, 4);
			System.arraycopy(this.getIntBytes(i * blockReqSize), 0, requests[i], 9, 4);
			System.arraycopy(this.getIntBytes(blockReqSize), 0, requests[i], 13, 4);
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

	public void setObtainedBlock(int index) {
		blocksRequested[index] = 1;
	}
	
	public void clearRequestedBlocks() {
		for(byte b : blocksRequested) {
			if(b == 1) {
				b = 0;
			}
		}
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
	
	
	private ArrayDeque<byte[]> outboundQueue = new ArrayDeque<byte[]>();
	private int outBoundQueueLength = 0;
	private long lastMsgReceived;
	private long interestTimeout;
	private ChannelStatus channelStatus;
	private Boolean[] connectionStatus = new Boolean[]{false, false, false, false};
	private Integer nioStoresKey;
	private boolean seedingApproved = true;
	private int piece = -1;
	private byte[][] blockRequests;
	private ByteBuffer blocks = null;
	private byte[] blocksRequested = null;
	
	private int numBlocksRequested = 0;
	private Peer peer;
	
}
