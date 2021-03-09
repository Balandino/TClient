package org.torrent.nio;

import java.io.IOException;
import java.nio.file.Path;

import org.torrent.bencoding.TorrentFile;
import org.torrent.bencoding.TorrentFile.PieceSelectionPolicy;
/**
 * The façade class used to initiate a download for the client
 * @author mkg
 *
 */
public class TClient {
	
		
	public TClient() {};
	
	/**
	 * The method used to initiate a download
	 * @param pathToFile The Path to the .torrent file
	 * @param newFileLocation The Path to where the target file should be downloaded to
	 * @param piecePolicy The policy to use when downloading pieces
	 */
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
	
	
	
	/**
	 * Used to implement singleton pattern on NIOThread
	 */
	private NIOThread nio = null;
}
