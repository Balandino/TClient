package org.torrent.coredata;

import java.util.Arrays;
import java.util.HashSet;

@SuppressWarnings("unused")
public class RarestFirst extends PiecePicker {
	
	public RarestFirst(int numPieces, long finalPieceSize) {
		this.finalPieceSize = finalPieceSize;
		pieces = new int[numPieces][numPieces];
		totalNumPieces = numPieces;
		for (int i = 0; i < pieces.length; i++) {
			pieces[i][0] = i + 1;
		}
	}

	@Override
	public int getPiece(byte[] bitfield) {
		int chosenPiece = 0;
		for(int i = 0; i < pieces.length; i++) {
			if(this.checkBit(pieces[i][0], bitfield)) {
				chosenPiece = pieces[i][0];
				piecesInProgress.add(pieces[i][0]);
				break;
			}
		}
		return chosenPiece;
	}
	
	@Override
	public void processBitField(byte[] bitfield) {
		for(int i = 0; i < pieces.length; i++) {
			int pieceNum = pieces[i][0];
			if(this.checkBit(pieceNum, bitfield)) {
				pieces[i][1]++;
			}
		}
		Arrays.parallelSort(pieces, (b, a) -> Integer.compare(a[1], b[1]));
	}

	private boolean checkBit(int piece, byte[] bitfield) {
		int bitToCheck = piece - 1;
		int byteToCheck = bitToCheck / 8;
		int offset = bitToCheck % 8;
		
		return ((bitfield[byteToCheck] & 1 << (7 - offset)) != 0) ? true : false; 
	}
	
	private void setBit(int piece, byte[] bitfield, boolean setOn) {
		int bitToCheck = piece - 1, curByte = bitToCheck / 8, offset = bitToCheck % 8;
		if(setOn) {
			bitfield[curByte] |= 1 << (7 - offset);	
		} else {
			bitfield[curByte] &= ~(1 << (7 - offset));
		} 
	}
	
	@Override
	public boolean pieceAvailable(byte[] bitfield) {
		for(int i = 0; i < pieces.length; i++) {
			if(this.checkBit(pieces[i][0], bitfield)) {
				if(!piecesInProgress.contains(pieces[i][0])) {
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean endGameEnabled() {
		return endGame;
	}

	@Override
	public void processHave(int piece) {
		int index = 0;
		int frequency = 0;
		for(int i = 0; i < pieces.length; i++) {
			if(pieces[i][0] == piece) {
				pieces[i][1]++;
				index = i;
				frequency = pieces[i][1];
				break;
			}
		}
		
		for(int i = index -1; i > -1; i--) {
			if(pieces[i][1] < frequency) {
				int tempPiece = pieces[i][0];
				int tempFreq = pieces[i][1];
				
				pieces[i][0] = pieces[i + 1][0];
				pieces[i][1] = pieces[i + 1][1];
				
				pieces[i + 1][0] = tempPiece;
				pieces[i + 1][1] = tempFreq;
			}
		}
	}
	
	@Override
	public void pieceObtained(int piece) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pieceUnobtained(int piece) {
		// TODO Auto-generated method stub
		
	}
	
		
	private void removeObtainedPieces(int[] pieces) {
		
	}
	
	//FOR DEBUGGING PURPOSES
	public void printArray() {
		System.out.print("Pieces: ");
		for(int i = 0; i < pieces.length; i++) {
			System.out.print(String.format("%-4s", pieces[i][0]) + " ");
		}
		
		System.out.println();
		
		System.out.print("  Freq: ");
		for(int i = 0; i < pieces.length; i++) {
			System.out.print(String.format("%-4s", pieces[i][1]) + " ");
		}
		
		System.out.println();
	}
	
	private int[][] pieces;
	private HashSet<Integer> piecesInProgress = new HashSet<Integer>();
	private int piecesObtainedCount;
	private int totalNumPieces;
	private long finalPieceSize;

}
