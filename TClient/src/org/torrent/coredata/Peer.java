package org.torrent.coredata;

public class Peer {
	
	public Peer(String ipAddress, int portNum) {
		ip = ipAddress;
		port = portNum;
	}
	
	
	
	private String ip;
	private int port;
	private byte[] bitfield;
}
