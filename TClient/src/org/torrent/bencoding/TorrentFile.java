package org.torrent.bencoding;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.torrent.coredata.ChannelData;
import org.torrent.coredata.FlowControls.TorrentStatus;

@SuppressWarnings("unused")
public class TorrentFile {

	
	public static enum PieceSelectionPolicy {
		RarestFirst
	}
	
	
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
		//newFileLocation = Paths.get(String.format("%s%s%s%s%s", System.getProperty("user.home"), File.separator, "Downloads", File.separator, (String) infoDict.get("name")));
		fileSize = (long) infoDict.get("length");
	}
	
	public long getAmountDownloaded() {
		return amountDownloaded;
	}
	
	public void updateDownloadedBytesCount(int num) {
		amountDownloaded += num;
		System.out.println("Amount Downloaded: " + amountDownloaded + "  File Size: " + fileSize);
	}
	
	public long getfileSize() {
		return fileSize;
	}
	
	public boolean fileComplete() {
		return amountDownloaded == fileSize;
	}
	
	public long getFinalPieceSize() {
		long difference = (this.getPieceSize() * this.getNumPieces()) - this.getfileSize();
		if(difference == 0) {
			return this.getPieceSize();
		} else {
			return this.getPieceSize() - difference;
		}
	}
	
	@SuppressWarnings("unchecked")
	public long getAmountRemaining() {
		return (Long)((HashMap<String, Object>)parsedFile.get("info")).get("length") - amountDownloaded;
	}
	
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
	
	@SuppressWarnings("unchecked")
	public int validatePiece(byte[] pieceBytes, int piece) {
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
		
		return Arrays.compare(newPieceHash, pieceHash);
	}
	
	@SuppressWarnings("unchecked")
	public int getNumPieces() {
		HashMap<String, Object> infoDict = (HashMap<String, Object>) parsedFile.get("info");
		byte[] pieceHashes = (byte[]) infoDict.get("pieces");
		return pieceHashes.length / 20;
	}
	
	@SuppressWarnings("unchecked")
	public long getPieceSize() {
		return (long) ((HashMap<String, Object>) parsedFile.get("info")).get("piece length");
	}
	
	public PieceSelectionPolicy getPieceSelectionPolicy() {
		return piecePolicy;
	}
	
	public int getPort() {
		int port = 0;
		return ((port = tracker.getPort()) > -1) ? port : 6969;
	}
	
	public String getResourceAddress() {
		return tracker.getPath();
	}
	
	public String getTrackerHostSite() {
		return tracker.getHost();
	}
	
	public void setStatus(TorrentStatus status) {
		this.torrentStatus = status;
	}
	
	public TorrentStatus getStatus() {
		return torrentStatus;
	}
	
	public byte[] getBitfield() {
		return this.bitfield;
	}
	
	public boolean isBitfieldEmpty() {
		for(byte b : bitfield) {
			if(b != (byte)0) {
				return false;
			}
		}
		return true;
	}
	
	public void setTrackerRefreshTime(long interval) {
		trackerRefreshTime = System.currentTimeMillis() + interval;
	}
	
	public int getNumDownloadingConns() {
		return numDownloadingConns;
	}
	
	public void addNewDownloadingConn(SocketChannel channel) {
		currentConns.add(channel);
		numDownloadingConns++;
	}
	
	public void removeDownloadingConn(SocketChannel channel) {
		currentConns.remove(channel);
		numDownloadingConns--;
	}
	
	public HashSet<SocketChannel> getCurrentConns() {
		return currentConns;
	}
	
	public void addChannelData(ChannelData channelData) {
		channelDataStore.add(channelData);
	}
	
	public void removeChannelData(ChannelData channelData) {
		channelDataStore.remove(channelData);
	}
	
	public HashSet<ChannelData> getallChannelData() {
		return channelDataStore;
	}
	
	
	public Path getOutputFileLocation() {
		return outputFileLocation;
	}
	
	
	private HashMap<String, Object> parsedFile;
	private long trackerRefreshTime;
	private HashSet<SocketChannel> currentConns = new HashSet<SocketChannel>();
	private HashSet<ChannelData> channelDataStore = new HashSet<ChannelData>();
	private URL tracker;
	
	
	private int numSeedingConns = 0;
	private int numDownloadingConns = 0;
	private long amountDownloaded = 0;
	private long fileSize;
	private byte[] bitfield;
	private TorrentStatus torrentStatus;
	private Path outputFileLocation;
	private PieceSelectionPolicy piecePolicy;
	private Path bencodedFile;

}
