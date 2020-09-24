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
		
		int[] progressPieces = new int[piecesInProgress.size()];
		int count = 0;
		for(int piece: piecesInProgress) {
			progressPieces[count++] = piece; 
		}
		Arrays.sort(progressPieces);
		String arrayString = Arrays.toString(progressPieces);
		arrayString = arrayString.substring(1, arrayString.length() - 1);
		
		
		int chosenPiece = 0;
		boolean pieceFound = false;
		for(int i = piecesMark; i < pieces.length; i++) {
			if(BitfieldOperations.checkBit(pieces[i][0], bitfield)) {
				chosenPiece = pieces[i][0];
				if(pieces[i][1] != Integer.MIN_VALUE) {
					if(!endGame) {
						if(!piecesInProgress.contains(chosenPiece)) {
							logger.log(Level.CONFIG, "Giving out piece: " + chosenPiece + ", Number of pieces obtained: " + numPiecesObtained + ", Pieces in Progress: " + arrayString + " Size: " + piecesInProgress.size());
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
							logger.log(Level.CONFIG, "Giving out piece in End Game: " + chosenPiece + ", Number of pieces obtained: " + numPiecesObtained + ", Pieces in Progress: " + arrayString + " Size: " + piecesInProgress.size());
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
		
		
		if(logger.getLevel() == Level.CONFIG) {
			int[] progressPieces = new int[piecesInProgress.size()];
			int count = 0;
			for(int piece: piecesInProgress) {
				progressPieces[count++] = piece; 
			}
			Arrays.sort(progressPieces);
			String arrayString = Arrays.toString(progressPieces);
			arrayString = arrayString.substring(1, arrayString.length() - 1);
			
			logger.log(Level.CONFIG, "Checking if piece available, Pieces in progress: " + arrayString + " (" + "Size: " + piecesInProgress.size() + ")");
		}
		
		int chosenPiece = 0;
		for(int i = piecesMark; i < pieces.length; i++) {
			if(BitfieldOperations.checkBit(pieces[i][0], bitfield)) {
				chosenPiece = pieces[i][0];
				if(pieces[i][1] != Integer.MIN_VALUE) {
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
		for(int i = piecesMark; i < pieces.length; i++) {
			int pieceNum = pieces[i][0];
			if(BitfieldOperations.checkBit(pieceNum, bitfield)) {
				if(pieces[i][1] != Integer.MIN_VALUE) {
					pieces[i][1]++;
				}
			}
		}
		Arrays.parallelSort(pieces, (b, a) -> Integer.compare(b[1], a[1]));
		this.updatePieceMark();
		logger.log(Level.CONFIG, "Sorted Pieces Array!");
	}
	
	@Override
	public boolean endGameEnabled() {
		return endGame;
	}

	@Override
	public void processHave(int piece) {
		int index = 0;
		int frequency = 0;
		for(int i = piecesMark; i < pieces.length; i++) {
			if(pieces[i][0] == piece) {
				
				if(pieces[i][1] == Integer.MIN_VALUE) {//Piece already obtained
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
				pieces[count][1] = Integer.MIN_VALUE;
				break;
			}
			count++;
		}
		
		piecesInProgress.remove(piece);
		
		
		if(endGame){
			endGamepiecesObtained.add(piece);
		}
		
		numPiecesObtained++;
		double percentageComplete = (double)numPiecesObtained / totalNumPieces * 100;
		if(percentageComplete > sortThreshold && sortThreshold < 100) {
			sortThreshold += 10;
			Arrays.parallelSort(pieces, (b, a) -> Integer.compare(b[1], a[1]));
			logger.log(Level.CONFIG, "Sorted Pieces Array!, new threshold: " + sortThreshold + ", Num Pieces Obtained: " + numPiecesObtained);
		}
		
		this.updatePieceMark();
		
		if(logger.getLevel() == Level.CONFIG) {
			logger.log(Level.CONFIG, "Percentage Complete: " + percentageComplete + ", Pieces Mark: " + piecesMark);
			
			StringBuilder sb = new StringBuilder(pieces.length);
			sb.append("Pieces: ");
			for(int i = 0; i < pieces.length; i++) {
				sb.append(pieces[i][0] + "(" + pieces[i][1] + ") ");
			}
			logger.log(Level.CONFIG, sb.toString());
		}
	}
	
	private void updatePieceMark() {
		while(piecesMark < pieces.length) {
			if(pieces[piecesMark][1] == Integer.MIN_VALUE) {
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
	 * Tracks the number of pieces obtained
	 */
	private int numPiecesObtained = 0;
	
	/**
	 * Excluding 100%, when this percentage of the file has been reached the pieces array is sorted based on piece frequency from least to highest for optimisation purposes.  Namely, moving all the collected pieces to the front of
	 * the array and allowing piecesMark to move past the collected pieces, meaning operations can then work only on pieces still in play 
	 */
	private int sortThreshold = 10;
	
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
