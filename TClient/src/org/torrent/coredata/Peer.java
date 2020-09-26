package org.torrent.coredata;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 
 * @author mkg
 * This class represents information about Peers that the client may connect to 
 */
public class Peer {
	
	/**
	 * Constructor
	 * 
	 * @param ipAddress The ip address of the peer
	 * @param portNum The number of the port to connect to
	 */
	protected Peer(String ipAddress, int portNum) {
		ip = ipAddress;
		port = portNum;
	}
	
	/**
	 * Obtain the ip address of the peer
	 * @return This peer's ip address
	 */
	public String getIP() {
		return ip;
	}
	
	/**
	 * Obtain the port number this peer is listening on
	 * @return The port number of this peer
	 */
	public int getPort() {
		return port;
	}
	
	/**
	 *  Get the bitfield that this peer has sent us
	 * @return This peer's bitfield
	 */
	public byte[] getBitfield() {
		return bitfield;
	}
	
	/**
	 * Store the bitfield received by this peer
	 * @param bitfield The bitfield received from the peer
	 */
	public void setBitfield(byte[] bitfield) {
		this.bitfield = bitfield;
	}
	
	/**
	 * Stores the handshake received by this peer
	 * @param handshakeBytes The bytes representing this peer's handshake
	 */
	public void setHandShake(byte[] handshakeBytes) {
		handshake = handshakeBytes;
	}
	
	/**
	 * Retrieves the handshake stored for this peer
	 * @return The handshake this peer has sent to the client
	 */
	public byte[] getHandShake() {
		return handshake;
	}

	/**
	 * 
	 * @param handshake Extracts the reserved bytes section of the handshake
	 * @return A String where each digit represents a bit in the reserved bytes portion of the handshake
	 */
	public String getReservedBytes(byte[] handshake) {
		byte[] reservedBytes = Arrays.copyOfRange(handshake, 20, 28);
		StringBuilder sb = new StringBuilder(reservedBytes.length * 8);
		for(byte b : reservedBytes) {
			sb.append(String.format("%8s", (Integer.toBinaryString((byte)b & 0xFF))).replace(' ', '0'));
		}
		return sb.toString();
	}
	
	/**
	 * 
	 * @param handshake Returns the protocol described in the received handshake
	 * @return A string representing the peer's desired protocol
	 */
	public static String getProtocol(byte[] handshake) {
		return new String(Arrays.copyOfRange(handshake, 1, Integer.valueOf(handshake[0]) + 1), StandardCharsets.ISO_8859_1);
	}
	
	/**
	 * obtain the infohash contained within the peer's handshake
	 * @param handshake The bytes representing the peer's handshake
	 * @return The infohash for the file the client desires
	 */
	public static byte[] getinfoHash(byte[] handshake) {
		return Arrays.copyOfRange(handshake, 28, 48);
	}
	
	/**
	 * Returns the ID that the peer is using
	 * @param handshake The bytes representing the peer's handshake
	 * @return The peer's ID
	 */
	public static byte[] getPeerID(byte[] handshake) {
		return Arrays.copyOfRange(handshake, 48, 68);
	}
	
	/**
	 * Checks whether the client has sent its bitfield to this peer
	 * @return True if the client has sent a bitfield to the peer, otherwise false
	 */
	public boolean bitfieldSent() {
		return bitFieldSent;
	}
	
	/**
	 * Marks the client as having sent its bitfield to this peer
	 */
	public void setBitfieldSent() {
		bitFieldSent = true;
	}
	
	/**
	 * Updates this client's bitfield to represent the new piece it has obtained
	 * @param piece The piece this client now states it has
	 */
	public void processHave(int piece) {
			BitfieldOperations.setBit(piece, bitfield, true);
	}

	@Override
	public String toString() {
		return String.format("%-15s %s%s", this.getIP(), this.getPort(), System.lineSeparator());
	}
	
	/**
	 * The ip address of this peer
	 */
	private String ip;
	
	/**
	 * the port number being used by this peer
	 */
	private int port;
	
	/**
	 * The bitfield received by this peer
	 */
	private byte[] bitfield;
	
	/**
	 * The handshake bytes received by this peer
	 */
	private byte[] handshake;
	
	/**
	 * Confirmation of whether the client has sent its bitfield to this peer or not
	 */
	private boolean bitFieldSent = false;
}
