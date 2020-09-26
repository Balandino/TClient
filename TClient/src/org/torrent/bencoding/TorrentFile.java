package org.torrent.bencoding;

import java.io.IOException;
import java.net.URL;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.torrent.coredata.ChannelData;
import org.torrent.coredata.FlowControls.TorrentStatus;

/**
 * This class holds information related to the torrent file being downloaded
 * @author mkg
 *
 */
public class TorrentFile {

	/**
	 * Enumerations representing the possible piece selection policies.  At present only 1 has been implemented.
	 * @author mkg
	 *
	 */
	public static enum PieceSelectionPolicy {
		RarestFirst
	}
	
	/**
	 * Constructor which parses a Bencoded .torrent file
	 * @param torrentFile The pathway to the .torrent file
	 * @param targetLocation The intended output location of the file
	 * @param pieceSelectionPolicy The policy to be used in choosing pieces for downloading
	 * @throws IOException If an error occurs in parsing the file or obtaining the tracker URL
	 */
	@SuppressWarnings("unchecked")
	public TorrentFile(Path torrentFile, Path targetLocation, PieceSelectionPolicy pieceSelectionPolicy) throws IOException {
		bencodedFile = torrentFile;
		parsedFile = BencodeParser.parseBencoding(torrentFile);
		outputFileLocation = targetLocation;
		piecePolicy = pieceSelectionPolicy;
		tracker = new URL((String) parsedFile.get("announce"));
		bitfield = new byte[this.getNumPieces() / 8];
		Arrays.fill(bitfield, (byte)0);
		
		
		HashMap<String, Object> infoDict = (HashMap<String, Object>) parsedFile.get("info");
		fileSize = (long) infoDict.get("length");
	}
	
	/**
	 * Obtains the amount of bytes downloaded
	 * @return The amount of bytes downloaded
	 */
	public long getAmountDownloaded() {
		return amountDownloaded;
	}
	
	/**
	 * Update the amount of bytes downloaded
	 * @param num The amount of new bytes that have been obtained
	 */
	public void updateDownloadedBytesCount(int num) {
		amountDownloaded += num;
	}
	
	/**
	 * Obtain the lenght of the file being downloaded
	 * @return The size in bytes of the file being downloaded
	 */
	public long getfileSize() {
		return fileSize;
	}
	
	/**
	 * Confirms whether all bytes for the file have been downloaded
	 * @return True if the amount of bytes downloaded is equal to the size of the targetted file, otherwise false
	 */
	public boolean fileComplete() {
		return amountDownloaded == fileSize;
	}
	
	/**
	 * Obtains the size required for the final piece request.  If the file's size does not divide equally based on piece sizes, then the final piece size will be different
	 * @return The size of the final piece to be requested
	 */
	public long getFinalPieceSize() {
		long difference = (this.getPieceSize() * this.getNumPieces()) - this.getfileSize();
		if(difference == 0) {
			return this.getPieceSize();
		} else {
			return this.getPieceSize() - difference;
		}
	}
	
	/**
	 * Obtain the amount of bytes remaining to be downloaded
	 * @return The amount of bytes remaining to be downloaded
	 */
	@SuppressWarnings("unchecked")
	public long getAmountRemaining() {
		return (Long)((HashMap<String, Object>)parsedFile.get("info")).get("length") - amountDownloaded;
	}
	
	/**
	 * Generates the infohash for the file the client is attempting to download
	 * @return The infohash for the file the client is attempting to download
	 */
	public byte[] getInfoHash() {
		byte[] hash = new byte[20];
		try {
			String bFile = Files.readString(bencodedFile, StandardCharsets.ISO_8859_1);
			bFile = bFile.substring(bFile.lastIndexOf("4:info") + 6, bFile.length() - 1);
			
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			hash = md.digest(bFile.getBytes(StandardCharsets.ISO_8859_1));
			
		} catch (IOException e) {
			System.err.println("IO Exception occured trying to obtain info_hash:");
			e.printStackTrace();
			System.exit(0);
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Algorithm exception occured trying to hash info dictionary:");
			e.printStackTrace();
			System.exit(0);
		}
		return hash;
	}
	
	/**
	 * Confirms if a piece validates in accordance with the info in the .torrent file
	 * @param pieceBytes The byte representation of the piece to be checked
	 * @param piece The piece being checked 
	 * @return True if the piece validates, otherwise false
	 */
	@SuppressWarnings("unchecked")
	public boolean validatePiece(byte[] pieceBytes, int piece) {
		HashMap<String, Object> infoDict = (HashMap<String, Object>) parsedFile.get("info");
		byte[] pieceHashes = (byte[]) infoDict.get("pieces");
		byte[] pieceHash = Arrays.copyOfRange(pieceHashes, piece * 20, (piece * 20) + 20);
		
			
		MessageDigest md;
		byte[] newPieceHash = new byte[20];
		try {
		
			md = MessageDigest.getInstance("SHA-1");
			newPieceHash = md.digest(pieceBytes);
			
			
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Algorithm exception occured trying to hash info dictionary:");
			e.printStackTrace();
			System.exit(0);
		}
		
		return Arrays.equals(newPieceHash, pieceHash);
	}
	
	/**
	 * Obtain the number of pieces in the file being downloaded
	 * @return The number of pieces in the file being downloaded
	 */
	@SuppressWarnings("unchecked")
	public int getNumPieces() {
		HashMap<String, Object> infoDict = (HashMap<String, Object>) parsedFile.get("info");
		byte[] pieceHashes = (byte[]) infoDict.get("pieces");
		return pieceHashes.length / 20;
	}
	
	/**
	 * Obtain the length of the pieces making up the target file.  Note that the final piece is potentially of a different size
	 * @return the length of the pieces making up the target file
	 */
	@SuppressWarnings("unchecked")
	public long getPieceSize() {
		return (long) ((HashMap<String, Object>) parsedFile.get("info")).get("piece length");
	}
	
	/**
	 * Obtains the port number the tracker is listening on
	 * @return the tracker's port number, or 6969 as default if port number not found
	 */
	public int getPort() {
		int port = 0;
		return ((port = tracker.getPort()) > -1) ? port : 6969;
	}
	
	/**
	 * Obtain the URL address of the tracker
	 * @return Tracker's URL address
	 */
	public String getResourceAddress() {
		return tracker.getPath();
	}
	
	/**
	 * Obtain the tracker's host name
	 * @return The tracker's host name
	 */
	public String getTrackerHostSite() {
		return tracker.getHost();
	}
	
	/**
	 * Set's the status of the torrentFile.  This status is used to control which actions are taken in NIOThread 
	 * @param status the current status of the torrent file.
	 */
	public void setStatus(TorrentStatus status) {
		this.torrentStatus = status;
	}
	
	/**
	 * Obtain the status of the torrentFile
	 * @return The torrentfile's status
	 */
	public TorrentStatus getStatus() {
		return torrentStatus;
	}
	
	/**
	 * Obtain's the client's bitfield for this torrentFile
	 * @return The bitfield for this client related to this torrentFile
	 */
	public byte[] getBitfield() {
		return this.bitfield;
	}
	
	/**
	 * Confirms if the client's bitfield has obtained any parts yet
	 * @return False if at least one piece has been obtained, otherwise true
	 */
	public boolean isBitfieldEmpty() {
		for(byte b : bitfield) {
			if(b != (byte)0) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * confirms the PieceSelectionPolicy for this torrent file
	 * @return The policy selected for this torrentFile
	 */
	public PieceSelectionPolicy getPiecePickerPolicy() {
		return piecePolicy;
	}
	
	/**
	 * Sets the time when the tracker should be contacted again
	 * @param interval The time indicated in the .torrent file that the client should wait before contacting the client again
	 */
	public void setTrackerRefreshTime(long interval) {
		trackerRefreshTime = System.currentTimeMillis() + interval;
	}
	
	/**
	 * Obtain the time whereby the tracker should be contacted again
	 * @return The time whereby the tracker should be contacted again
	 */
	public long getTrackerRefreshTime() {
		return trackerRefreshTime;
	}
	
	/**
	 * Obtain the amount of connections currently working on this file
	 * @return The amount of connections currently working on this file
	 */
	public int getNumDownloadingConns() {
		return numDownloadingConns;
	}
	
	/**
	 * Add another connection to the pool of connections working on the file
	 * @param channel The connection to be added
	 */
	public void addNewDownloadingConn(SocketChannel channel) {
		currentConns.add(channel);
		numDownloadingConns++;
	}
	
	/**
	 * Removes a connection from the pool of connections working on the file
	 * @param channel The connection to be removed
	 */
	public void removeDownloadingConn(SocketChannel channel) {
		currentConns.remove(channel);
		numDownloadingConns--;
	}
	
	/**
	 * Returns the pool of connections currently working on this file
	 * @return A HashSet of all the connections currently working on the file
	 */
	public HashSet<SocketChannel> getCurrentConns() {
		return currentConns;
	}
	
	/**
	 * Adds channelData to the pool of channelDatas
	 * @param channelData The channelData object to be added
	 */
	public void addChannelData(ChannelData channelData) {
		channelDataStore.add(channelData);
	}
	
	/**
	 * Removes a channelData object from the pool
	 * @param channelData The channelData object to be removed
	 */
	public void removeChannelData(ChannelData channelData) {
		channelDataStore.remove(channelData);
	}
	
	/**
	 * Obtain the HashSet of all the channelDatas
	 * @return A HashSet of all the channelDatas currently being held
	 */
	public HashSet<ChannelData> getallChannelData() {
		return channelDataStore;
	}
	
	/**
	 * Obtain the location the target file is being downloaded to
	 * @return The Path to the new file
	 */
	public Path getOutputFileLocation() {
		return outputFileLocation;
	}
	
	/**
	 * A HashMap containing all the information stored in the Bencoded .torrent file 
	 */
	private HashMap<String, Object> parsedFile;
	
	/**
	 * The time whereby the tracker should be contacted again
	 */
	private long trackerRefreshTime;
	
	/**
	 * A HashSet of all the connections currently working on downloading the target file 
	 */
	private HashSet<SocketChannel> currentConns = new HashSet<SocketChannel>();
	
	/**
	 * A HashSet of all the channel data objects for each of the connections working on the target file.  This is used to perform operations on every channel, such as adding messages to all of their queues.  
	 */
	private HashSet<ChannelData> channelDataStore = new HashSet<ChannelData>();
	
	/**
	 * The URL address of the tracker to connect to
	 */
	private URL tracker;
	
	/**
	 * A count of the amount of connections currently working to download data
	 */
	private int numDownloadingConns = 0;
	
	/**
	 * A count of the amount of bytes downloaded 
	 */
	private long amountDownloaded = 0;
	
	/**
	 * The number of bytes contained within the target file
	 */
	private long fileSize;
	
	/**
	 * The client's bitfield, representing which pieces of the target file have been obtained 
	 */
	private byte[] bitfield;
	
	/**
	 * The status of this object.  This is used by NIOThread to decide in which actions to take
	 */
	private TorrentStatus torrentStatus;
	
	/**
	 * The output location of the file to be downloaded
	 */
	private Path outputFileLocation;
	
	/**
	 * The policy to be used when downloading pieces.  At present, only one policy has been implemented and it prioritises pieces with low availability
	 */
	private PieceSelectionPolicy piecePolicy;
	/**
	 * The Path to the .torrent file for parsing
	 */
	private Path bencodedFile;
	

}
