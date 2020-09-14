package org.torrent.coredata;

/**
 * In order to control what actions a channel or torrent file needs to take it will always be given a status.  That status will be checked on operations such as reading and writing within NIOThread in order
 * to judge what action needs to be taken next.  These enumerations are the statuses that will be assigned.
 * 
 * @author mkg
 *
 */
public class FlowControls {

	/**
	 * A series of statuses used in code control statements within NIOThread. These ones control torrent files.
	 * 
	 * @author mkg
	 *
	 */
	public static enum TorrentStatus{
		CONTACTING_TRACKER,
		MESSAGING_PEERS
	}
	
	/**
	 * A series of statuses used in code control statements within NIOThread.  Decisions on what actions a channel needs to take are heavily
	 * based on the current state it is in, which will be signified by these enumerations.  
	 * 
	 * @author mkg
	 *
	 */
	public static enum ChannelStatus {
		CONTACTING_TRACKER,
		MESSAGING_TRACKER,
		CONTACTING_PEER,
		SENDING_HANDSHAKE,
		WAITING_HANDSHAKE,
		WAITING_TRACKER_RESPONSE,
		SENDING_BITFIELD,
		WAITING_BITFIELD,
		CHECKING_FOR_PIECE,
		PROCESSING_MESSAGES,
		LISTENING_TO_PEER,
		LISTENING_FOR_BITFIELD,
		SENDING_HANDSHAKE_BITFIELD,
	}
	
}
