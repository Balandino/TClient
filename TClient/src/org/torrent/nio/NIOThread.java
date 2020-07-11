package org.torrent.nio;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;

import org.torrent.bencoding.TorrentFile;
import org.torrent.coredata.PeerManager;
import org.torrent.coredata.PieceSelector;

@SuppressWarnings("unused")
public class NIOThread extends Thread {
	
	
	@Override
	public void run() {
		
		//Generate Selector
		try {
			Selector selector = Selector.open();
		} catch (IOException io) {
			System.out.println("Error: Unable to generate selector.");
			io.printStackTrace();
		}
	
		
		while(!nioShutdown) {
			if(this.torrentsToProcess.size() > 0) {
				System.out.println("Size: " + this.torrentsToProcess.size());
				System.exit(0);
			}
			
		}
			
			
		
	}
	
	protected static NIOThread getInstance() {
		if (nioThread == null) {
			nioThread = new NIOThread();
		}
		return nioThread;
	}
	
			
	
		
	private HashMap<Integer, TorrentFile> torrentsProcessing = new HashMap<Integer, TorrentFile>();
	private HashMap<Integer, PeerManager> peerManagers = new HashMap<Integer, PeerManager>();
	private HashMap<Integer, PieceSelector> pieceSelectors  = new HashMap<Integer, PieceSelector>();
	private Integer nioKeys = 0;
	private byte[] handshake;
	private static NIOThread nioThread = null;
	
	protected volatile HashSet<TorrentFile> torrentsToProcess = new HashSet<TorrentFile>();
	
	public volatile HashMap<Path, Boolean> shutdown = new HashMap<Path, Boolean>();
	public volatile int maxNumDownloadingConns = 10;
	public volatile int maxNumSeedingConns = 10;
	public volatile boolean seedAfterwards = true; 
	public volatile boolean nioShutdown = false;
	public int blocksWaiting = 10;
	
	
}
