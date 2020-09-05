package org.torrent.coredata;

import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;


public class RarestFirst extends PiecePicker {
	
	public RarestFirst(int numPieces, Logger logger) {
		pieces = new int[numPieces][2];
		totalNumPieces = numPieces;
		for (int i = 0; i < pieces.length; i++) {
			pieces[i][0] = i;
		}
		
		this.logger = logger;
	}
	
	@Override
	public int getPiece(byte[] bitfield) {
		int chosenPiece = 0;
		boolean pieceFound = false;
		for(int i = piecesMark; i < pieces.length; i++) {
			if(BitfieldOperations.checkBit(pieces[i][0], bitfield)) {
				chosenPiece = pieces[i][0];
				if(chosenPiece != -1) {
					if(!endGame) {
						if(!piecesInProgress.contains(chosenPiece)) {
							piecesInProgress.add(pieces[i][0]);
							pieceFound = true;
							break;
						}
					} else {
						pieceFound = true;
						break;
					}
				}
			}
		}
		
		if(endGame) {
			if(!pieceFound) {
				if(piecesInProgress.size() > 0) {
					for(int piece: piecesInProgress) {
						if(BitfieldOperations.checkBit(piece, bitfield)) {
							chosenPiece = piece;
						}
					}
				}
			}
		} else {
			++piecesRequested;
			if(piecesRequested == totalNumPieces) {
				endGame = true;
			}
		}
		
		return chosenPiece;
	}
	
	@Override
	public boolean pieceAlreadyObtained(int piece) {
		return endGamepiecesInProgress.contains(piece); 
	}
	
	@Override
	public boolean pieceAvailable(byte[] bitfield) {
		
		int[] progressPieces = new int[piecesInProgress.size()];
		int count = 0;
		for(int piece: piecesInProgress) {
			progressPieces[count++] = piece; 
		}
		Arrays.sort(progressPieces);
		String arrayString = Arrays.toString(progressPieces);
		arrayString = arrayString.substring(1, arrayString.length() - 1);
		
		logger.log(Level.CONFIG, "Pieces in progress: " + arrayString + " (" + "Size: " + piecesInProgress.size() + ")");
		
		
		int chosenPiece = 0;
		for(int i = piecesMark; i < pieces.length; i++) {
			if(BitfieldOperations.checkBit(pieces[i][0], bitfield)) {
				chosenPiece = pieces[i][0];
				if(chosenPiece != -1) {
					if(!endGame) {
						if(!piecesInProgress.contains(chosenPiece)) {
							return true;
						}
					} else {
						return true;
					}
				}
			}
		}
		
		if(endGame && piecesInProgress.size() > 0) {
			for(int piece: piecesInProgress) {
				if(BitfieldOperations.checkBit(piece, bitfield)) {
					return true;
				}
			}
		}
			return false;
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
		this.updatePieceMark();
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
				
				if(pieces[i][0] == -1) {//Piece already obtained
					return;
				}
				
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
				
				index = i;
			} else {
				break;
			}
		}
		if(index < piecesMark) {
			piecesMark = index;
		}
	}
	
	@Override
	public void pieceObtained(int piece) {
		int count = piecesMark;
		while(count < pieces.length) {
			if(pieces[count][0] == piece) {
				pieces[count][0] = -1;
				break;
			}
			count++;
		}
		
		piecesInProgress.remove(piece);
		this.updatePieceMark();
		
		if(endGame){
			endGamepiecesInProgress.add(piece);
		}
		
		
		logger.log(Level.CONFIG, "Pieces Mark: " + piecesMark);
		
		StringBuilder sb = new StringBuilder(pieces.length);
		sb.append("Pieces: ");
		for(int i = 0; i < pieces.length; i++) {
			sb.append(pieces[i][0] + " ");
		}
		logger.log(Level.CONFIG, sb.toString());
	}
	
	private void updatePieceMark() {
		while(piecesMark < pieces.length) {
			if(pieces[piecesMark][0] == -1) {
				piecesMark++;
			} else {
				break;
			}
		}
	}

	@Override
	public void pieceUnobtained(int piece) {
		piecesInProgress.remove(piece);
	}
	
	@SuppressWarnings("unused")
	private void printArray() {
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
	private HashSet<Integer> endGamepiecesInProgress = new HashSet<Integer>();
	private int piecesRequested = 0;
	private int totalNumPieces;
	private int piecesMark = 0;
	
	private Logger logger;
	

}
