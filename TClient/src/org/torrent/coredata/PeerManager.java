package org.torrent.coredata;

import java.util.LinkedList;
import java.util.List;

public class PeerManager {
	
	public PeerManager() {
		
	}
	
	public void addPeer(String ipAddress, int portNum) {
		peers.add(new Peer(ipAddress, portNum));
	}
	
	public Peer getPeer() {
		return peers.remove(0);
	}
	
	public boolean hasPeers() {
		return (peers.size() > 0) ? true : false;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Peers" + System.lineSeparator() + "====================" + System.lineSeparator());
		for(Peer p : peers) {
			sb.append(p.toString());
		}
		return sb.toString();
	}
	
	private List<Peer> peers = new LinkedList<Peer>();

}
