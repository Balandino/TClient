package org.torrent.bencoding;

import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.torrent.coredata.FlowControls.TorrentStatus;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

@SuppressWarnings("unused")
public class TorrentFile {

	
	public static enum PieceSelectionPolicy {
		RarestFirst
	}
	
	
	
	@SuppressWarnings("unchecked")
	public TorrentFile(Path torrentFile, Path targetLocation, PieceSelectionPolicy piecePolicy) throws IOException {
		this.parsedFile = (HashMap<String, Object>) BencodeParser.parseTorrent(torrentFile);
		this.newFileLocation = targetLocation;
		this.piecePolicy = piecePolicy;
	}
	
	public String getTrackerRequest() {
		return (String) this.parsedFile.get("announce");
	}
	
	public int getPort() {
		Matcher matcher = Pattern.compile("\\:([0-9]{4})").matcher(this.getTrackerRequest());
		if(matcher.find()) {
			return Integer.valueOf(matcher.group(1));
		} else {
			return 6969; //Default tracker port
		}
	}
	
	
	
	
	private HashMap<String, Object> parsedFile;
	private HashMap<String, Object> trackerResponse;
	private HashSet<SocketChannel> currentConns = new HashSet<SocketChannel>();
	private int numSeedingConns = 0;
	private int numDownloadingConns = 0;
	private int amountDownloaded = 0;
	private TorrentStatus torrentStatus;
	private Path newFileLocation;
	private PieceSelectionPolicy piecePolicy;
	
}
