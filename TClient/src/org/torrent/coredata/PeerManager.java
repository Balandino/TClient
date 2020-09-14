package org.torrent.coredata;

import java.util.LinkedList;
import java.util.List;

public class PeerManager {
	
	/**
	 * Constructor
	 */
	public PeerManager() {
		
	}
	
	/**
	 * Adds a new peer to the manager's internal linked list.
	 * 
	 * @param ipAddress IP Address of the peer
	 * @param portNum Port number the peer is listening on
	 */
	public void addPeer(String ipAddress, int portNum) {
		peers.add(new Peer(ipAddress, portNum));
	}
	
	/**
	 * Removes and returns a peer from the manager's internal linked list
	 * 
	 * @return  A Peer object
	 */
	public Peer getPeer() {
		return peers.remove(0);
	}
	
	/**
	 * Confirms if any peers are left to give out
	 * 
	 * @return true if at least one peer is left in the manager's internal linked list
	 */
	public boolean hasPeers() {
		return (peers.size() > 0) ? true : false;
	}
	
	/**
	 * 
	 * @return The amount of available peers
	 */
	public int numAvailablePeers() {
		return peers.size();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Peers" + System.lineSeparator() + "====================" + System.lineSeparator());
		for(Peer p : peers) {
			sb.append(p.toString());
		}
		return sb.toString();
	}
	
	/**
	 * The internal linked list used to store peers.  As they are given out they are removed from this list and not re-added.
	 */
	private List<Peer> peers = new LinkedList<Peer>();

}
