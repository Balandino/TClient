package org.torrent.bencoding;

import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.torrent.coredata.FlowControls.TorrentStatus;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@SuppressWarnings("unused")
public class TorrentFile {

	
	public static enum PieceSelectionPolicy {
		RarestFirst
	}
	
	public TorrentFile(Path torrentFile, Path targetLocation, PieceSelectionPolicy pieceSelectionPolicy) throws IOException {
		bencodedFile = torrentFile;
		parsedFile = BencodeParser.parseBencoding(torrentFile);
		newFileLocation = targetLocation;
		piecePolicy = pieceSelectionPolicy;
		tracker = new URL((String) parsedFile.get("announce"));
	}
	
	public String getTrackerHostSite() {
		return tracker.getHost();
	}
	
	public int getPort() {
		int port = 0;
		return ((port = tracker.getPort()) > -1) ? port : 6969;
	}
	
	public String getResourceAddress() {
		return tracker.getPath();
	}
	
	public void setStatus(TorrentStatus status) {
		this.torrentStatus = status;
	}
	
	public int getAmountDownloaded() {
		return amountDownloaded;
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
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Algorithm exception occured trying to hash info dictionary:");
			e.printStackTrace();
		}
		return hash;
	}
	
	
	
	private HashMap<String, Object> parsedFile;
	private HashMap<String, Object> trackerResponse;
	private HashSet<SocketChannel> currentConns = new HashSet<SocketChannel>();
	private URL tracker;
	private int numSeedingConns = 0;
	private int numDownloadingConns = 0;
	private int amountDownloaded = 0;
	private TorrentStatus torrentStatus;
	private Path newFileLocation;
	private PieceSelectionPolicy piecePolicy;
	private Path bencodedFile;
	
}
