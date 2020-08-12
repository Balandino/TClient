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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.torrent.bencoding.BencodeParser;
import org.torrent.bencoding.TorrentFile;
import org.torrent.bencoding.TorrentFile.PieceSelectionPolicy;
import org.torrent.coredata.ChannelData;
import org.torrent.coredata.FlowControls;
import org.torrent.coredata.FlowControls.ChannelStatus;
import org.torrent.coredata.FlowControls.TorrentStatus;
import org.torrent.coredata.Peer;
import org.torrent.coredata.PeerManager;
import org.torrent.coredata.PiecePicker;
import org.torrent.coredata.RarestFirst;

@SuppressWarnings("unused")
public class NIOThread extends Thread {
	
	
	@Override
	public void run() {
		//Create an id for our client
		peerID = this.getSessionPeerID();
		
		//Generate Selector
		try {
			Selector selector = Selector.open();
			while(!nioShutdown) {
				if(torrentsToProcess.size() > 0) {
					for(TorrentFile tf : torrentsToProcess) {
						SocketChannel channel = SocketChannel.open();
						channel.configureBlocking(false);
						channel.connect(new InetSocketAddress(tf.getTrackerHostSite(), tf.getPort()));
						tf.setStatus(TorrentStatus.CONTACTING_TRACKER);
						SelectionKey newKey = channel.register(selector, SelectionKey.OP_CONNECT);
						ChannelData channelData = new ChannelData(++nioKey, ChannelStatus.CONTACTING_TRACKER);
						newKey.attach(channelData);
						torrentsProcessing.put(nioKey, tf);
						torrentsToProcess.remove(tf);
					}
				}
				
				if(torrentsProcessing.size() > 0) {
					for(int storesKey : torrentsProcessing.keySet()) {
						TorrentFile tf = torrentsProcessing.get(storesKey);
						if(!(tf.getStatus() == TorrentStatus.CONTACTING_TRACKER)) {
							if(tf.getAmountRemaining() > 0) {
								if(tf.getNumDownloadingConns() < maxNumDownloadingConns) {
									PeerManager pm = peerManagers.get(storesKey);
									if(pm.hasPeers()) {
										Peer peer = pm.getPeer();
										SocketChannel channel = SocketChannel.open();
										channel.configureBlocking(false);
										//System.out.println("IP: " + peer.getIP() + " Port: " + peer.getPort());
										
										//FOR TESTING ONLY
										peer = new Peer("5.9.144.2", 54321);
										
										channel.connect(new InetSocketAddress(peer.getIP(), peer.getPort()));
										SelectionKey newKey = channel.register(selector, SelectionKey.OP_CONNECT);
										System.out.println("Registered with selector!");
										ChannelData channelData = new ChannelData(storesKey, peer, ChannelStatus.CONTACTING_PEER);
										tf.addNewDownloadingConn(channel);
										newKey.attach(channelData);
									}
								}
							}
						}
					}
				}
				
				selector.select(1000);
				Set<SelectionKey> readyKeys = selector.selectedKeys();
				Iterator<SelectionKey> iterator = readyKeys.iterator();
				//System.out.println("FLOW: Looping" + " Key count: " + readyKeys.size());
				while(iterator.hasNext()) {
					SelectionKey key = (SelectionKey) iterator.next();
					iterator.remove();
					
					if(key.isValid() && key.isAcceptable()) {//Acceptable Start
						System.out.println("FLOW: isAcceptable()");
					}//Acceptable End
					
					if(key.isValid() && key.isConnectable()) {//Connect Start
						System.out.println("FLOW: isConnectable()");
						
						try {
							SocketChannel channel = (SocketChannel)key.channel();
							channel.finishConnect();
							ChannelData channelData = (ChannelData)key.attachment();
							
							if(channelData.getStatus() == ChannelStatus.CONTACTING_TRACKER) {
								channelData.setStatus(ChannelStatus.MESSAGING_TRACKER);
								SelectionKey newKey = channel.register(selector, SelectionKey.OP_WRITE);
								newKey.attach(channelData);
							} else if(channelData.getStatus() == ChannelStatus.CONTACTING_PEER) {
								channelData.setStatus(ChannelStatus.SENDING_HANDSHAKE);
								SelectionKey newKey = channel.register(selector, SelectionKey.OP_WRITE);
								newKey.attach(channelData);
							} else {
								System.err.println("Unable to parse channel state.");
								System.exit(0);
							}
							
						} catch (IOException io) {
							System.err.println("Error connecting: " + io.getMessage() + System.lineSeparator() + "Exiting");
							System.exit(0);
						}
						
					}//Connect End
					
					if(key.isValid() && key.isReadable()) {//Read Start
						System.out.println("FLOW: isReadable()");
						
						SocketChannel channel = (SocketChannel)key.channel();
						ChannelData channelData = (ChannelData)key.attachment();
						int channelNioKey = channelData.getNioKey();
						
						if(channelData.getStatus() == ChannelStatus.WAITING_TRACKER_RESPONSE) {
							channel = (SocketChannel)key.channel();
							ByteBuffer buff = ByteBuffer.allocate(4000);//Should be sufficient for average dictionary mode if required
							channel.read(buff);
							
							this.parseTrackerResponse(buff.array(), channelNioKey);
							this.setPieceSelector(channelNioKey);
							key.cancel();
							torrentsProcessing.get(channelNioKey).setStatus(TorrentStatus.MESSAGING_PEERS);
							
						} else {
							ByteBuffer buff = ByteBuffer.allocate(500000);
							channel.read(buff);
							
							//DEBUG
							Files.write(Paths.get("/home/mkg/Desktop/Handshake"), Arrays.copyOfRange(buff.array(), 0, buff.position()));
							
							String result = this.receiveMessage(channelData, Arrays.copyOfRange(buff.array(), 0, buff.position()));
							if(result.equals("OK")) {
								//this.processData(channelData);
								
								
								

								//TODO End of code
								System.out.println("Code end reached!");
								System.exit(0);
								
							} else {
								System.err.println("Error reading message: " + result);
								System.exit(0);
							}
						}
							
							
							
						
					}//Read end
					
					if(key.isValid() && key.isWritable()) {//Write Start
						System.out.println("FLOW: isWritable()");
						
						SocketChannel channel = (SocketChannel)key.channel();
						ChannelData channelData = (ChannelData)key.attachment();
						
						if(channelData.getStatus() == ChannelStatus.MESSAGING_TRACKER) {
							TorrentFile tf = torrentsProcessing.get(channelData.getNioKey());
							channelData.setStatus(ChannelStatus.WAITING_TRACKER_RESPONSE);
							channel.write(ByteBuffer.wrap(this.getHTTPRequest(tf).getBytes()));
							SelectionKey newKey = channel.register(selector, SelectionKey.OP_READ);
							newKey.attach(channelData);
						} else if(channelData.getStatus() == ChannelStatus.SENDING_HANDSHAKE) {
							channel.write(this.getHandshake(torrentsProcessing.get(channelData.getNioKey())));
							channelData.setStatus(ChannelStatus.WAITING_HANDSHAKE);
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
	
	private ChannelStatus processData(ChannelData channelData) {
		ChannelStatus status = channelData.getStatus();
		Integer channelNioKey = channelData.getNioKey();
		
		
		
		if(status == ChannelStatus.CHECKING_FOR_PIECE) {
			PiecePicker picker = PiecePickers.get(channelNioKey);
			if(picker.pieceAvailable(channelData.getPeer().getBitfield())) {
				
			}
			
			
		}
		
		
		
		
		return null;
	}
	
	
	
	//String will need to contain an error message if we go wrong,
	//or something else if all is successful
	private String receiveMessage(ChannelData channelData, byte[] msgData) {
		String result = "OK";
		Integer channelNioKey = channelData.getNioKey();
		LinkedHashMap<Byte, byte[]> messages = this.extractMessages(msgData);
		
		for(byte msgID : messages.keySet()) {
			
			switch (msgID) {
				case (byte)19: {
					channelData.getPeer().setHandShake(messages.get(msgID));
					channelData.setStatus(ChannelStatus.SENDING_BITFIELD);
					break;
				}
				
				case (byte)5:{
					channelData.getPeer().setBitfield(messages.get(msgID));
					PiecePicker picker = PiecePickers.get(channelNioKey);
					picker.processBitField(channelData.getPeer().getBitfield());
					
					TorrentFile tf = torrentsProcessing.get(channelNioKey);
					if(!channelData.getPeer().bitfieldSent()) {
						if(!tf.isBitfieldEmpty()) {
							//Add bitfield to outbound Queue
							
							channelData.getPeer().setBitfieldSent();
						}
					}
						
					channelData.setStatus(ChannelStatus.CHECKING_FOR_PIECE);
					break;
				}
				
				case (byte)1:{
					//Process unchoked message
					
					
					break;
				}
				
				
				
				default:
					System.err.println("Error: Unable to parse message, exiting");
					System.exit(0);
				}
				
				
			
			
			
		}
		return result;
	}
	
	private LinkedHashMap<Byte, byte[]> extractMessages(byte[] message){
		LinkedHashMap<Byte, byte[]> messages = new LinkedHashMap<Byte, byte[]>();
		
		int index = 0;
		while(index < message.length) {
		
			if(message.length >= 68 && index == 0) {
				String messageStart = message[0] + new String(Arrays.copyOfRange(message, 1, 20), StandardCharsets.ISO_8859_1);
				if(messageStart.equals("19BitTorrent protocol")) {
					messages.put((byte) 19, Arrays.copyOfRange(message, 0, 68));
					index = 68;
				} 
			}
			
			ByteBuffer buff = ByteBuffer.wrap(Arrays.copyOfRange(message, index, index + 4));
			int messageLength = buff.getInt();
			index += 4;
			
			messages.put(message[index], Arrays.copyOfRange(message, index + 1, (index + messageLength)));
			index += messageLength;
		}
		return messages;
	}
	
	
	private ByteBuffer getHandshake(TorrentFile tf) {
		ByteBuffer buff = ByteBuffer.allocate(68);
		buff.put((byte) 19); //pstrlen
		buff.put("BitTorrent protocol".getBytes(StandardCharsets.UTF_8)); //pstr
		buff.put(new byte[] {0, 0, 0, 0, 0, 0, 0, 0}); //extensions
		buff.put(tf.getInfoHash()); //infohash
		buff.put(peerID.toString().getBytes(StandardCharsets.UTF_8));
		buff.flip();
		
		return buff;
	}
	
	
	protected static NIOThread getInstance() {
		if (nioThread == null) {
			nioThread = new NIOThread();
		}
		return nioThread;
	}
	
	private StringBuilder getSessionPeerID() {
		peerID.append("-");	
		for(int i = 0; i < 14; i++) {
			peerID.append(new Random().nextInt(10));	
		}
		return peerID;
	}
	
	private int getPort() {
		return myListeningPort;
	}
	
	private String getHTTPRequest(TorrentFile tf) {
		String endLine = "\r\n";
		String message = "GET " + 
		tf.getResourceAddress() +
		"?info_hash=" + this.urlEncode(tf.getInfoHash()) +
		"&peer_id=" + this.urlEncode(peerID.toString().getBytes()) +
		"&port=" + this.getPort() +
		"&downloaded=" + tf.getAmountDownloaded() +
		"&left=" + tf.getAmountRemaining() +
		"&compact=1" +
		" HTTP/1.1" + endLine +
		"User-Agent: " + System.getProperty("java.vm.name") + endLine +
		"Accept: text/*" + endLine +
		"Connection: close" + endLine +
		"Host: " + tf.getTrackerHostSite() + endLine + endLine;
		
		return message;
	}
	
	private void setPieceSelector(Integer channelNioKey){
		//If different policies are added in the future, then this can be expanded
		PieceSelectionPolicy policy = torrentsProcessing.get(channelNioKey).getPieceSelectionPolicy();
		TorrentFile tf = torrentsProcessing.get(channelNioKey);
		if(policy == PieceSelectionPolicy.RarestFirst) {
			PiecePickers.put(channelNioKey, new RarestFirst(tf.getNumPieces(), tf.getFinalPieceSize()));
		} else {
			System.err.println("Error: No piece selection policy set");
			System.exit(0);
		}
	}
			
	private String urlEncode(byte[] array) {
		//Pulled from: https://stackoverflow.com/questions/11894945/convert-torrent-info-hash-from-bencoded-to-urlencoded-data
		StringBuilder encodedString = new StringBuilder(20);
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
	
	private void parseTrackerResponse(byte[] buffer, Integer channelNioKey) {
		String response = new String(buffer, StandardCharsets.ISO_8859_1).trim();
		Matcher matcher = Pattern.compile("d8:intervali([0-9]+)e").matcher(response);
		if(matcher.find()) {
			torrentsProcessing.get(channelNioKey).setTrackerRefreshTime(Integer.valueOf(matcher.group(1)));
		} else {
			System.err.println("Error: Unable to parse tracker interval time");
			System.exit(0);
		}
		
		String[] lines = response.split("\\r\\n");
		response = (lines[lines.length -1]);
		matcher = Pattern.compile("ip[0-9]{2}:[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}4:porti[0-9]{4}")
				.matcher(response);
		
		PeerManager peerManager = new PeerManager();
		if(matcher.find()) {//Dictionary Model
			String ip = matcher.group();
			peerManager.addPeer(ip.substring(5, ip.lastIndexOf(":") - 1), Integer.valueOf(ip.substring(ip.lastIndexOf("i") + 1)));
			
			while(matcher.find()) {
				ip = matcher.group();
				peerManager.addPeer(ip.substring(5, ip.lastIndexOf(":") - 1), Integer.valueOf(ip.substring(ip.lastIndexOf("i") + 1)));
			}
		} else {//Binary Model
			matcher = Pattern.compile("(?<=peers)([0-9]+)").matcher(response);
			if(matcher.find()) {
				StringBuilder ipAddress = new StringBuilder();
				int peersNumLength = matcher.group().length();
				int numPeerBytes = Integer.valueOf(matcher.group());
				byte[] peerBytes = response.substring(response.indexOf("peers") + 5 + peersNumLength + 1).getBytes(StandardCharsets.ISO_8859_1);
				
				for(int i = 0; i < numPeerBytes;){
					int stopper = i + 4;
					for(; i <  stopper; i++) {
						ipAddress.append((peerBytes[i] & 0xff));
						if((stopper - i) > 1) {
							ipAddress.append(".");
						}
					}
					int port = ((peerBytes[i] & 0xff) << 8) | (peerBytes[i + 1] & 0xff);
					peerManager.addPeer(ipAddress.toString(), port);
					ipAddress =  new StringBuilder();
					i += 2;
				}
			} else {// TODO Need to handle different tracker errors
				System.err.println("Unable to parse tracker response. Exiting.");
				System.exit(0);
			}
			peerManagers.put(channelNioKey, peerManager);
		}		
	}
	
		
	private HashMap<Integer, TorrentFile> torrentsProcessing = new HashMap<Integer, TorrentFile>();
	private HashMap<Integer, PeerManager> peerManagers = new HashMap<Integer, PeerManager>();
	private HashMap<Integer, PiecePicker> PiecePickers  = new HashMap<Integer, PiecePicker>();
	private Integer nioKey = 0;
	private static NIOThread nioThread = null;
	private StringBuilder peerID = new StringBuilder("TM470");
	private int myListeningPort = 6888;
			
	protected volatile HashSet<TorrentFile> torrentsToProcess = new HashSet<TorrentFile>();
	
	public volatile HashMap<Path, Boolean> shutdown = new HashMap<Path, Boolean>();
	public volatile int maxNumDownloadingConns = 1;
	public volatile int maxNumSeedingConns = 1;
	public volatile boolean seedAfterwards = true; 
	public volatile boolean nioShutdown = false;
	public int blocksWaiting = 10;
	
	
}
