package org.torrent.coredata;

public abstract class PieceSelector {
	
	public abstract int getPiece(byte[] bitfield);
	
	public abstract void processBitField(byte[] bitfield);
	
	public abstract boolean pieceAvailable();
	
	public abstract boolean endGameStatus();
	
	public abstract void processHave(int piece);
	
	public abstract void pieceObtained(int piece);
	
	public abstract void pieceUnobtained(int piece);
		
	
	
protected boolean endGame = false;
}
