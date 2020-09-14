package org.torrent.coredata;

import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;


public class RarestFirst extends PiecePicker {
	
	/**
	 * Contructor
	 * 
	 * @param numPieces The amount of pieces that need gathering
	 * @param logger A logger to output debugging information if required,  Messages in this class are set to CONFIG.
	 */
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
		return endGamepiecesObtained.contains(piece); 
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
			endGamepiecesObtained.add(piece);
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
	
	
	/**
	 * A 2D array of all the pieces and their rarity.  The first column tracks piece numbers, with the second column tracking its frequency among connected peers.
	 */
	private int[][] pieces;
	
	/**
	 * Stored pieces currently being worked on to prevent them being worked on to prevent them being working on by two channels, which would be inefficient of other pieces are waiting 
	 */
	private HashSet<Integer> piecesInProgress = new HashSet<Integer>();
	
	/**
	 * Used to track pieces obtained in endGame
	 */
	private HashSet<Integer> endGamepiecesObtained = new HashSet<Integer>();
	
	/**
	 * Tracks how many pieces have been requested.  When all have been requested, End Game is switched on.
	 */
	private int piecesRequested = 0;
	
	/**
	 * Stores the total amount of pieces needed.
	 */
	private int totalNumPieces;
	
	/**
	 * An optimisation, this marker starts at 0 and moves up the pieces array when possible.  As it moves, everything behind it should be a gathered piece and everything ahead of it should be a 
	 * piece yet to be gathered.  Various operations in this class will use this marker to indicate the start of the pieces Array, meaning they don't need to iterate over large segments of pieces which have
	 * already been obtained and thus reducing time spent iterating over the array.
	 */
	private int piecesMark = 0;
	
	/**
	 * A logger used to print information if the level is set to CONFIG.
	 */
	private Logger logger;
	

}
