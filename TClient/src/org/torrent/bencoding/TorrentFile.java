package org.torrent.bencoding;

import java.util.HashMap;
import java.util.HashSet;

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
