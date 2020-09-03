package org.torrent.coredata;

import java.util.Arrays;
import java.util.HashSet;


public class RarestFirst extends PiecePicker {
	
	public RarestFirst(int numPieces) {
		pieces = new int[numPieces][2];
		totalNumPieces = numPieces;
		for (int i = 0; i < pieces.length; i++) {
			pieces[i][0] = i;
		}
	}

	@Override
	public int getPiece(byte[] bitfield) {
		int chosenPiece = 0;
		for(int i = 0; i < pieces.length; i++) {
			if(BitfieldOperations.checkBit(pieces[i][0], bitfield)) {
				chosenPiece = pieces[i][0];
				if(chosenPiece != -1 && !piecesInProgress.contains(chosenPiece)) {
					piecesInProgress.add(pieces[i][0]);
					break;
				}
			}
		}
		return chosenPiece;
	}
	
	@Override
	public void processBitField(byte[] bitfield) {
		for(int i = 0; i < pieces.length; i++) {
			int pieceNum = pieces[i][0];
			if(BitfieldOperations.checkBit(pieceNum, bitfield)) {
				pieces[i][1]++;
			}
		}
		Arrays.parallelSort(pieces, (b, a) -> Integer.compare(a[1], b[1]));
	}

	
	
	@Override
	public boolean pieceAvailable(byte[] bitfield) {
		
		String inProgress = "";
		for(Integer piece : piecesInProgress) {
			inProgress+= piece + " ";
		}
		System.out.println("Pieces In Progress: " + inProgress);
		
		for(int i = 0; i < pieces.length; i++) {
			if(BitfieldOperations.checkBit(pieces[i][0], bitfield)) {
				int chosenPiece = pieces[i][0];
				if(chosenPiece != -1 && !piecesInProgress.contains(chosenPiece)) {
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
		int count = 0;
		while(count < pieces.length) {
			if(pieces[count][0] == piece) {
				pieces[count][0] = -1;
				break;
			}
			count++;
		}
		piecesObtainedCount++;
		piecesInProgress.remove(piece);
		
		if(piecesObtainedCount != totalNumPieces) {
			double percentageComplete = (piecesObtainedCount / pieces.length) * 100;
			if(percentageComplete > 10) {
			//	this.removeObtainedPieces();
			}
		}
	}

	@Override
	public void pieceUnobtained(int piece) {
		piecesInProgress.remove(piece);
	}
	
	private void removeObtainedPieces() {
		int[][] newPiecesArray = new int[piecesObtainedCount][2];
		int count = 0;
		for(int i = 0; i < pieces.length; i++) {
			if(pieces[i][0] != -1) {
						newPiecesArray[count][0] = pieces[i][0];
						newPiecesArray[count][1] = pieces[i][1];
						count++;
				}
			Arrays.parallelSort(newPiecesArray, (b, a) -> Integer.compare(a[1], b[1]));
			}
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
	private int piecesObtainedCount = 0;
	private int piecesRequested = 0;
	private int totalNumPieces;
	

}
