package org.torrent.nio;

import java.io.IOException;
import java.nio.file.Path;

import org.torrent.bencoding.TorrentFile;
import org.torrent.bencoding.TorrentFile.PieceSelectionPolicy;

public class TClient {
	
		
	public TClient() {
		
	}
	
	
	public void downloadFile(Path pathToFile, Path newFileLocation, PieceSelectionPolicy piecePolicy) {
		if (nio == null) {
			nio = NIOThread.getInstance();
			nio.start();
		}
		
		try {
			nio.torrentsToProcess.add(new TorrentFile(pathToFile, newFileLocation, piecePolicy));
		} catch (IOException e) {
			System.err.println("Unable to parse torrent file");
			e.printStackTrace();
		}
		
	}
	
	
	
	
	private NIOThread nio = null;
}
