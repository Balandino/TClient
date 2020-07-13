package org.torrent.nio;

import java.io.IOException;
import java.math.MathContext;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
			while(!nioShutdown) {
				if(this.torrentsToProcess.size() > 0) {
					for(TorrentFile tf : this.torrentsToProcess) {
						SocketChannel channel = SocketChannel.open();
						channel.configureBlocking(false);
						channel.socket().setSoTimeout(3);
						channel.connect(new InetSocketAddress(tf.getTrackerRequest(), tf.getPort()));
						
						
						
						
					}
				
				
				
				
					System.exit(0);
				}
			
			}
		
		
		} catch (IOException io) {
			System.err.println("Error: IO Exception has occured");
			io.printStackTrace();
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
