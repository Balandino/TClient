package org.torrent.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.torrent.bencoding.BencodeParser;
import org.torrent.bencoding.TorrentFile;
import org.torrent.coredata.ChannelData;
import org.torrent.coredata.FlowControls.ChannelStatus;
import org.torrent.coredata.FlowControls.TorrentStatus;
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
				if(torrentsToProcess.size() > 0) {
					for(TorrentFile tf : this.torrentsToProcess) {
						SocketChannel channel = SocketChannel.open();
						channel.configureBlocking(false);
						System.out.println(tf.getTrackerHostSite());
						//channel.connect(new InetSocketAddress("http://bttracker.debian.org:6969/announce", 6969));
						channel.connect(new InetSocketAddress("bttracker.debian.org", 6969));
						
						tf.setStatus(TorrentStatus.CONTACTING_TRACKER);
						
						SelectionKey newKey = channel.register(selector, SelectionKey.OP_CONNECT);
						ChannelData channelData = new ChannelData(++nioKey, ChannelStatus.CONTACTING_TRACKER);
						newKey.attach(channelData);
						torrentsProcessing.put(nioKey, tf);
						torrentsToProcess.remove(tf);
					}
				}
				
				selector.select(1000);
				Set<SelectionKey> readyKeys = selector.selectedKeys();
				Iterator<SelectionKey> iterator = readyKeys.iterator();
				System.out.println("FLOW: Looping");
				while(iterator.hasNext()) {
					SelectionKey key = (SelectionKey) iterator.next();
					iterator.remove();
					
					if(key.isAcceptable()) {//Acceptable Start
						System.out.println("FLOW: isAcceptable()");
					}//Acceptable End
					
					if(key.isConnectable()) {//Connect Start
						System.out.println("FLOW: isConnectable()");
						try {
							SocketChannel channel = (SocketChannel)key.channel();
							channel.finishConnect();
							
							ChannelData channelData = (ChannelData)key.attachment();
							if(channelData.getStatus() == ChannelStatus.CONTACTING_TRACKER) {
								channelData.setStatus(ChannelStatus.MESSAGING_TRACKER);
								SelectionKey newKey = channel.register(selector, SelectionKey.OP_WRITE);
								newKey.attach(channelData);
							}
							
						} catch (IOException io) {
							System.err.println("Error connecting to tracker:");
							io.printStackTrace();
						}
						
					}//Connect End
					
					if(key.isReadable()) {//Read Start
						//Matcher matcher = Pattern.compile("(?m)^d8.*e{3}$").matcher(response);//Match line in with bencoded dictionary
						System.out.println("FLOW: isReadable()");
						
						SocketChannel channel = (SocketChannel)key.channel();
						ChannelData channelData = (ChannelData)key.attachment();
						
						if(channelData.getStatus() == ChannelStatus.WAITING_TRACKER_RESPONSE) {
							
							channel = (SocketChannel)key.channel();
							ByteBuffer buff = ByteBuffer.allocate(4000);//Should be sufficient for average dictionary mode if required
							channel.read(buff);
							
							String response = new String(buff.array(), StandardCharsets.ISO_8859_1).trim();
							String[] lines = response.split("\\r\\n");
							response = (lines[lines.length -1]);
							Matcher matcher = Pattern.compile("ip[0-9]{2}:[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}4:porti[0-9]{4}")
									.matcher(response);
							
							if(matcher.find()) {//Dictionary Model
								String ip = matcher.group();
								System.out.format("%s -> %s %s%s", ip, ip.subSequence(5, ip.lastIndexOf(":") - 1), ip.substring(ip.lastIndexOf("i") + 1), System.lineSeparator());
								while(matcher.find()) {
									ip = matcher.group();
									System.out.format("%s -> %s %s%s", ip, ip.subSequence(5, ip.lastIndexOf(":") - 1), ip.substring(ip.lastIndexOf("i") + 1), System.lineSeparator());
								}
							} else {//Binary Model
								matcher = Pattern.compile("(?<=peers)([0-9]+)").matcher(response);
								if(matcher.find()) {
									int peersNumLength = matcher.group().length();
									int numPeerBytes = Integer.valueOf(matcher.group());
									byte[] peerBytes = response.substring(response.indexOf("peers") + 5 + peersNumLength + 1).getBytes();
									
									for(int i = 0; i < numPeerBytes;){
										int stopper = i + 4;
										for(; i <  stopper; i++) {
											System.out.printf("%s", (peerBytes[i] & 0xff));
											if((stopper - i) > 1) {
												 System.out.print(".");
											}
										}
										System.out.print(" ");
										
										int val = ((peerBytes[i] & 0xff) << 8) | (peerBytes[i + 1] & 0xff);
										System.out.println(val);
										
										i += 2;
									}
								} else {
									System.err.println("Unable to parse tracker response. Exiting.");
									System.exit(0);
								}
							}
						}
						
						
						System.out.println("Exiting.");
						System.exit(0);
					}//Read end
					
					if(key.isWritable()) {//Write Start
						System.out.println("FLOW: isWritable()");
						
						SocketChannel channel = (SocketChannel)key.channel();
						ChannelData channelData = (ChannelData)key.attachment();
						
						if(channelData.getStatus() == ChannelStatus.MESSAGING_TRACKER) {
							TorrentFile tFile = torrentsProcessing.get(channelData.getNioKey());
														
							String endLine = "\r\n";
							String message = "GET " + 
							tFile.getResourceAddress() +
							"?info_hash=" + this.urlEncode(tFile.getInfoHash()) +
							"&peer_id=" + this.urlEncode(this.getPeerID()) +
							"&port=" + this.getPort() +
							"&downloaded=" + tFile.getAmountDownloaded() +
							"&left=" + tFile.getAmountRemaining() +
							"&compact=1" +
							" HTTP/1.1" + endLine +
							"User-Agent: " + System.getProperty("java.vm.name") + endLine +
							"Accept: text/*" + endLine +
							"Connection: close" + endLine +
							"Host: " + tFile.getTrackerHostSite() + endLine + endLine;
							
							channelData.setStatus(ChannelStatus.WAITING_TRACKER_RESPONSE);
							channel.write(ByteBuffer.wrap(message.getBytes()));
							 
							SelectionKey newKey = channel.register(selector, SelectionKey.OP_READ);
							newKey.attach(channelData);
						}
						
					}//Write end
					
					
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
	
	private byte[] getPeerID() {
		if(peerID.length() < 20) {
			for(int i = 0; i < 15; i++) {
			peerID.append(new Random().nextInt(10));	
			}
		}
		return peerID.toString().getBytes();
	}
	
	private int getPort() {
		return myListeningPort;
	}
			
	private String urlEncode(byte[] array) {
		StringBuilder encodedString = new StringBuilder(20);
		
		//Pulled from: https://stackoverflow.com/questions/11894945/convert-torrent-info-hash-from-bencoded-to-urlencoded-data
		for(byte entry : array) {
			if(entry == ' ') {
				encodedString.append("+");
			} else if(entry == '.' || entry == '-' || entry == '_' || entry == '~' || (entry >= 'a' && entry <= 'z') || (entry >= 'A' && entry <= 'Z') || (entry >= '0' && entry <= '9')) { 
				encodedString.append(String.format("%c", entry));
			} else {
				encodedString.append(String.format("%%%02X", entry));
			}
		}
		return encodedString.toString();
	}
	
		
	private HashMap<Integer, TorrentFile> torrentsProcessing = new HashMap<Integer, TorrentFile>();
	private HashMap<Integer, PeerManager> peerManagers = new HashMap<Integer, PeerManager>();
	private HashMap<Integer, PieceSelector> pieceSelectors  = new HashMap<Integer, PieceSelector>();
	private Integer nioKey = 0;
	private byte[] handshake;
	private static NIOThread nioThread = null;
	private StringBuilder peerID = new StringBuilder("TM470");
	private int myListeningPort = 6888;
			
	protected volatile HashSet<TorrentFile> torrentsToProcess = new HashSet<TorrentFile>();
	
	public volatile HashMap<Path, Boolean> shutdown = new HashMap<Path, Boolean>();
	public volatile int maxNumDownloadingConns = 10;
	public volatile int maxNumSeedingConns = 10;
	public volatile boolean seedAfterwards = true; 
	public volatile boolean nioShutdown = false;
	public int blocksWaiting = 10;
	
	
}
