package org.torrent.coredata;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Peer {
	
	//SHOULD BE Protected, CHANGE BACK WHEN TESTING IS COMPLETE
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
	
	public void setBitfield(byte[] bitfield) {
		this.bitfield = bitfield;
	}
	
	public void setHandShake(byte[] handshakeBytes) {
		handshake = handshakeBytes;
	}
	
	public String getReservedBytes(byte[] handshake) {
		byte[] reservedBytes = Arrays.copyOfRange(handshake, 20, 28);
		StringBuilder sb = new StringBuilder(reservedBytes.length * 8);
		for(byte b : reservedBytes) {
			sb.append(String.format("%8s", (Integer.toBinaryString((byte)b & 0xFF))).replace(' ', '0'));
		}
		return sb.toString();
	}
	
	public static String getProtocol(byte[] handshake) {
		return new String(Arrays.copyOfRange(handshake, 1, Integer.valueOf(handshake[0]) + 1), StandardCharsets.ISO_8859_1);
	}
	
	public static byte[] getinfoHash(byte[] handshake) {
		return Arrays.copyOfRange(handshake, 28, 48);
	}
	
	public static byte[] getPeerID(byte[] handshake) {
		return Arrays.copyOfRange(handshake, 48, 68);
	}
	
	public boolean bitfieldSent() {
		return bitFieldSent;
	}
	
	public void setBitfieldSent() {
		bitFieldSent = true;
	}
	
	public void processHave(int piece) {
		BitfieldOperations.setBit(piece, bitfield, true);
	}
	
	@Override
	public String toString() {
		return String.format("%-15s %s%s", this.getIP(), this.getPort(), System.lineSeparator());
	}
	
	private String ip;
	private int port;
	private byte[] bitfield;
//	private String reservedBytes;
//	private byte[] peerID;
	@SuppressWarnings("unused")
	private byte[] handshake;
	private boolean bitFieldSent = false;
}
