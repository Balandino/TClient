package org.torrent.coredata;

import java.util.HashSet;

@SuppressWarnings("unused")
public class RarestFirst extends PieceSelector {
	
	public RarestFirst(int numPieces, long finalPieceSize) {
		this.finalPieceSize = finalPieceSize;
		pieces = new int[numPieces][numPieces];
		totalNumPieces = numPieces;
		for (int i = 0; i < pieces.length; i++) {
			pieces[1][i] = i;
		}
	}

	@Override
	public int getPiece(byte[] bitfield) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void processBitField(byte[] bitfield) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean pieceAvailable() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean endGameStatus() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void processHave(int piece) {
		// TODO Auto-generated method stub
		
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
	
	private int[][] pieces;
	private HashSet<Integer> piecesInProgress;
	private int piecesObtainedCount;
	private int totalNumPieces;
	private long finalPieceSize;

}
