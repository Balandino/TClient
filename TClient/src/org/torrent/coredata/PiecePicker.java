package org.torrent.coredata;

public abstract class PiecePicker {
	
	public abstract int getPiece(byte[] bitfield);
	
	public abstract void processBitField(byte[] bitfield);
	
	public abstract boolean pieceAvailable(byte[] bitfield);
	
	public abstract boolean endGameEnabled();
	
	public abstract boolean pieceAlreadyObtained(int piece);
	
	public abstract void processHave(int piece);
	
	public abstract void pieceObtained(int piece);
	
	public abstract void pieceUnobtained(int piece);
		
	
	
protected boolean endGame = false;
}
