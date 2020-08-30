package org.torrent.coredata;

public class BitfieldOperations {
	
	public static boolean checkBit(int piece, byte[] bitfield) {
		int bitToCheck = piece;
		int byteToCheck = bitToCheck / 8;
		int offset = bitToCheck % 8;
		
		return ((bitfield[byteToCheck] & 1 << (7 - offset)) != 0) ? true : false; 
	}
	
	public static void setBit(int piece, byte[] bitfield, boolean setOn) {
		int bitToCheck = piece - 1, curByte = bitToCheck / 8, offset = bitToCheck % 8;
		if(setOn) {
			bitfield[curByte] |= 1 << (7 - offset);	
		} else {
			bitfield[curByte] &= ~(1 << (7 - offset));
		}
	}

}
