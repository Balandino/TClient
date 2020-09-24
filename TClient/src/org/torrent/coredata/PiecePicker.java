package org.torrent.coredata;

/**
 * An abstract class for subclassing.  This design targets flexibility, so that future subclasses can vary in their criteria for selecting pieces whilst
 * the main code can work against this class.
 * 
 * @author mkg
 *
 */
public abstract class PiecePicker {
	
	/**
	 * Returns a piece that the bitfield indicates it has
	 * 
	 * @param bitfield The bitfield of the peer the client is connected to
	 * @return A piece to request
	 */
	public abstract int getPiece(byte[] bitfield);
	
	/**
	 * Takes a peer's bitfield and updates internal information, such as piece availability
	 * 
	 * @param bitfield The bitfield of the peer the client is connected to
	 */
	public abstract void processBitField(byte[] bitfield);
	
	/**
	 * Checks if a bitfield has a piece that the client wants
	 * 
	 * @param bitfield The bitfield of the peer the client is connected to
	 * @return true if a piece is available, otherwise false
	 */
	public abstract boolean pieceAvailable(byte[] bitfield);
	
	/**
	 * Checks if endGame has been set to true
	 * 
	 * @return true if endGame is set to true, otherwise false
	 */
	public abstract boolean endGameEnabled();
	
	/**
	 * Checks if a piece has already been obtained.
	 * 
	 * @param piece The piece the channel wants to check for
	 * @return true if the piece has been obtained, otherwise false
	 */
	public abstract boolean pieceAlreadyObtained(int piece);
	
	/**
	 * Processes a peer's have message
	 * 
	 * @param piece The piece that the peer has indicated it now has
	 */
	public abstract void processHave(int piece);
	
	/**
	 * Marks a piece as obtained
	 * 
	 * @param piece The piece that the channel has obtained
	 */
	public abstract void pieceObtained(int piece);
	
	/**
	 * Marks a piece as not obtained.  This occurs if a channel is working on a piece and then loses its connection.  
	 * The piece that it was working on is no longer reachable and thus needs to be returned to the picker for another channel to work on.
	 * 
	 * @param piece The piece that the channel can no longer work on
	 */
	public abstract void pieceUnobtained(int piece);
		
	
/**
 * Set to true when every piece has been requested, triggering End Game mode.	
 */
protected boolean endGame = false;
}
