package org.torrent.coredata;

/**
 * A small class containing methods to operate on bits contained within bytes
 * @author mkg
 *
 */
public class BitfieldOperations {
	
	/**
	 * Checks if a bit is set within a bitfield
	 * 
	 * @param piece The piece the client needs to check the bitfield for 
	 * @param bitfield The bitfield of the peer the client is connected to 
	 * @return true if the peer has the piece we desire, otherwise false 
	 */
	public static boolean checkBit(int piece, byte[] bitfield) {
		int bitToCheck = piece;
		int byteToCheck = bitToCheck / 8;
		int offset = bitToCheck % 8;
		
		return ((bitfield[byteToCheck] & 1 << (7 - offset)) != 0) ? true : false; 
	}
	
	/**
	 * Sets a bit to on or off
	 * 
	 * @param piece The piece in the bitfield 
	 * @param bitfield The bitfield to modify
	 * @param setOn if set to true, the bit will be set to on, otherwise it will be set to off
	 */
	public static void setBit(int piece, byte[] bitfield, boolean setOn) {
		int bitToCheck = piece - 1, curByte = bitToCheck / 8, offset = bitToCheck % 8;
		if(setOn) {
			bitfield[curByte] |= 1 << (7 - offset);	
		} else {
			bitfield[curByte] &= ~(1 << (7 - offset));
		}
	}

}
