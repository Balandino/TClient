package org.torrent.coredata;

public class Peer {
	
	protected Peer(String ipAddress, int portNum) {
		ip = ipAddress;
		port = portNum;
	}
	
	public String getIP() {
		return ip;
	}
	
	public int getPort() {
		return port;
	}
	
	public byte[] getBitfield() {
		return bitfield;
	}
	
	public void setBitfield(byte[] newBitfield) {
		bitfield = newBitfield;
	}
	
	@Override
	public String toString() {
		return String.format("%-15s %s%s", this.getIP(), this.getPort(), System.lineSeparator());
	}
	
	private String ip;
	private int port;
	private byte[] bitfield;
}
