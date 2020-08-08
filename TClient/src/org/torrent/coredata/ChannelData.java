package org.torrent.coredata;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;

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
	
	private ArrayDeque<String> outboundQueue = new ArrayDeque<String>();
	private long lastMsgReceived;
	private long interestTimeout;
	private ChannelStatus channelStatus;
	private Boolean[] connectionStatus = new Boolean[]{false, false, false, false};
	private Integer nioStoresKey;
	private boolean seedingApproved = true;
	private int piece;
	private int[] blocksRequested;
	private ByteBuffer blocksObtained;
	private Peer peer;
	
}
