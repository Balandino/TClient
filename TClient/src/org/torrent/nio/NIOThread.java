package org.torrent.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
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
import org.torrent.coredata.BitfieldOperations;
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
										tf.addChannelData(channelData);
										newKey.attach(channelData);
									}
								}
							}
						}
					}
				}
				
				selector.select(100);
				Set<SelectionKey> readyKeys = selector.selectedKeys();
				Iterator<SelectionKey> iterator = readyKeys.iterator();
				//System.out.println("FLOW: Looping" + " Key count: " + readyKeys.size());
				while(iterator.hasNext()) {
					SelectionKey key = (SelectionKey) iterator.next();
					iterator.remove();
					
					
					/*===============================================================================================================================================*\
					 * 															Accept Start																		 *
					\*===============================================================================================================================================*/
					
					if(key.isValid() && key.isAcceptable()) {
					//	System.out.println("FLOW: isAcceptable()");
					}
					
					/*===============================================================================================================================================*\
					 * 															Accept End																			 *
					\*===============================================================================================================================================*/
					
					
					
					/*===============================================================================================================================================*\
					 * 															Connect Start																		 *
					\*===============================================================================================================================================*/
					
					if(key.isValid() && key.isConnectable()) {
					//	System.out.println("FLOW: isConnectable()");
						
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
						
					}
					
					/*===============================================================================================================================================*\
					 * 															Connect End																			 *
					\*===============================================================================================================================================*/
					
					
					
					
					/*===============================================================================================================================================*\
					 * 															Read Start																			 *
					\*===============================================================================================================================================*/
					
					if(key.isValid() && key.isReadable()) {
					//	System.out.println("FLOW: isReadable()");
						SocketChannel channel = (SocketChannel)key.channel();
						ChannelData channelData = (ChannelData)key.attachment();
						int channelNioKey = channelData.getNioKey();
						
						
						if(channelData.getStatus() == ChannelStatus.PROCESSING_MESSAGES) {
							
							ByteBuffer buff = ByteBuffer.allocate(10000);
							channel.read(buff);
							
							String result = this.receiveMessage(channelData, Arrays.copyOfRange(buff.array(), 0, buff.position()));
							
							if(result.equals("OK")) {
								int processedDataKey = this.processData(channelData);
								if(processedDataKey == -2) {
									break;
								}
								
								SelectionKey newKey = channel.register(selector, processedDataKey);
								newKey.attach(channelData);
							} else {
								System.out.println("Error reading message!");
								System.exit(0);
							}
								
						} else if(channelData.getStatus() == ChannelStatus.WAITING_TRACKER_RESPONSE) {
							channel = (SocketChannel)key.channel();
							ByteBuffer buff = ByteBuffer.allocate(4000);//Should be sufficient for average dictionary mode if required
							channel.read(buff);
							
							this.parseTrackerResponse(buff.array(), channelNioKey);
							this.setPieceSelector(channelNioKey);
							key.cancel();
							torrentsProcessing.get(channelNioKey).setStatus(TorrentStatus.MESSAGING_PEERS);
							
						} else {
							ByteBuffer buff = ByteBuffer.allocate(17000);
							channel.read(buff);
							
							String result = this.receiveMessage(channelData, Arrays.copyOfRange(buff.array(), 0, buff.position()));
							if(result.equals("OK")) {
								int processedDataKey = this.processData(channelData);
								if(processedDataKey != -1) {
									SelectionKey newKey = channel.register(selector, processedDataKey);
									newKey.attach(channelData);
								} else {
									System.err.println("Error processing data: " + processedDataKey);
									System.exit(0);
								}
							} else {
								System.err.println("Error reading message: " + result);
								System.exit(0);
							}
						 }
					}
					
					/*===============================================================================================================================================*\
					 * 															Read End																			 *
					\*===============================================================================================================================================*/
					
					
					
					/*===============================================================================================================================================*\
					 * 															Write Start																			 *
					\*===============================================================================================================================================*/
					
					if(key.isValid() && key.isWritable()) {
					//	System.out.println("FLOW: isWritable()");
						
						SocketChannel channel = (SocketChannel)key.channel();
						ChannelData channelData = (ChannelData)key.attachment();
						
						if(channelData.getStatus() == ChannelStatus.MESSAGING_TRACKER) {
							TorrentFile tf = torrentsProcessing.get(channelData.getNioKey());
							channelData.setStatus(ChannelStatus.WAITING_TRACKER_RESPONSE);
							this.writeMessage(channel, this.getHTTPRequest(tf).getBytes());
							SelectionKey newKey = channel.register(selector, SelectionKey.OP_READ);
							newKey.attach(channelData);
						} else if(channelData.getStatus() == ChannelStatus.SENDING_HANDSHAKE) {
							this.writeMessage(channel, this.getHandshake(torrentsProcessing.get(channelData.getNioKey())));
							channelData.setStatus(ChannelStatus.WAITING_HANDSHAKE);
							SelectionKey newKey = channel.register(selector, SelectionKey.OP_READ);
							newKey.attach(channelData);
						} else if(channelData.getStatus() == ChannelStatus.PROCESSING_MESSAGES) {
							int processedDataKey = this.processData(channelData);
							this.writeMessage(channel, channelData.getOutboundMessages());
							SelectionKey newKey = channel.register(selector, processedDataKey);
							newKey.attach(channelData);
						}
						
					}
					
					/*===============================================================================================================================================*\
					 * 															Write End																			 *
					\*===============================================================================================================================================*/
					
					
				}
			}
		} catch (IOException io) {
			System.err.println("Error: IO Exception has occured");
			io.printStackTrace();
		}
	}
	
	
	/*===============================================================================================================================================*\
	 * 															METHODS																				 *
	\*===============================================================================================================================================*/
	
	
	/*==================================*\	
	 *	  Return value to Key mapping   *
	 *==================================*
	 * 		     Accept: 16				*
	 * 		       Read: 1				*
	 * 	          Write: 4				*
	 *   		Connect: 8				*
	 *  Write & Connect: 5				*
	\*==================================*/
	
	private int processData(ChannelData channelData) {
		while(true) {
			ChannelStatus status = channelData.getStatus();
			Integer channelNioKey = channelData.getNioKey();
			PiecePicker picker = PiecePickers.get(channelNioKey);
			Peer peer = channelData.getPeer();
			
			if(status == ChannelStatus.CHECKING_FOR_PIECE) {
				picker = PiecePickers.get(channelNioKey);
				if(picker.pieceAvailable(channelData.getPeer().getBitfield())) {
					byte[] interested = new byte[] {0, 0, 0, 1, 2};
					channelData.addMessage(interested);
					
					if(!channelData.isChoked()) {
						channelData.setStatus(ChannelStatus.PROCESSING_MESSAGES);	
						continue;
					} else {
						//TODO Need to add interested message
						channelData.setStatus(ChannelStatus.PROCESSING_MESSAGES);
						return SelectionKey.OP_WRITE;
					}
				} else {
					//TODO  Piece unavailable
				}
				
				
			}
		
			if(status == ChannelStatus.PROCESSING_MESSAGES) {
				if(!channelData.isChoked()) {
					if(!channelData.pieceSet()) {
						if(picker.pieceAvailable(peer.getBitfield())) {
							channelData.setPiece(picker.getPiece(peer.getBitfield()));
							System.out.println("Setting Piece: " + channelData.getPiece());
							
						} else {
							if(torrentsProcessing.get(channelNioKey).fileComplete()) {
								return -2;
							} else {
								System.out.println("Error, no piece available");
								System.exit(0);
							}
						}
					} 
					
					if(!picker.endGameEnabled()) {
						if(!channelData.blocksRequested()) {
							byte[][] requests = channelData.getBlockRequests((int)torrentsProcessing.get(channelNioKey).getPieceSize(), channelData.getPiece(), blockReqSize);
							byte[] blockTracker = channelData.getBlocksRequested();
							for(int i = 0; i < requests.length; i++) {
								if(blockTracker[i] != -1) {
									channelData.addMessage(requests[i]);
									channelData.setBlockRequested(i);
								}
							}
							return SelectionKey.OP_WRITE;									
						} else {
							//TODO Blocks already requested
							return SelectionKey.OP_READ;
						}
					} else {
						//TODO End Game
					}
				} else {
					//TODO Channel is choked
					return SelectionKey.OP_READ;
				}
				
				break;
			}
		}
		return -1;
	}
	
	
	
	/*==================================*\	
	 *	  ID to message mapping   		*
	 *==================================*
	 * 		  Handshake: 19				*
	 * 		 Keep-Alive: ? 				*
	 * 	          Choke: 0				*
	 *   		Unchoke: 1				*
	 *  	 Interested: 2				*
	 *   Not-Interested: 3				*
	 *			   Have: 4				*
	 *		   Bitfield: 5  			*
	 *  		Request: 6				*
	 *  		  Piece: 7				*
	 *			 Cancel: 8  			*
	\*==================================*/
	
	//String will need to contain a relevant error message if a fault is detected
	private String receiveMessage(ChannelData channelData, byte[] msgData) {
		
		String result = "OK";
		Integer channelNioKey = channelData.getNioKey();
		ArrayDeque<byte[]> messages = this.extractMessages(channelData, msgData);
		
		for(byte[] msg : messages) {
			switch (msg[0]) {
				case (byte)19: {
					channelData.getPeer().setHandShake(Arrays.copyOfRange(msg, 1, msg.length));
					channelData.setStatus(ChannelStatus.SENDING_BITFIELD);
					break;
				}
				
				case (byte)5:{
					channelData.getPeer().setBitfield(Arrays.copyOfRange(msg, 1, msg.length));
					PiecePicker picker = PiecePickers.get(channelNioKey);
					picker.processBitField(channelData.getPeer().getBitfield());
					
					TorrentFile tf = torrentsProcessing.get(channelNioKey);
					if(!channelData.getPeer().bitfieldSent()) {
						if(!tf.isBitfieldEmpty()) {
							//TODO Add bitfield to outbound Queue
							channelData.getPeer().setBitfieldSent();
						}
					}
					
					channelData.setStatus(ChannelStatus.CHECKING_FOR_PIECE);
					break;
				}
				
				case (byte)1:{
						channelData.setUnchoked();
					break;
				}
				
				case(byte)7:{
					
					byte[] pieceIndexBytes = Arrays.copyOfRange(msg, 1, 5);
					int pieceIndex = ByteBuffer.wrap(pieceIndexBytes).getInt();
					byte[] offsetBytes = Arrays.copyOfRange(msg, 5, 9);
					byte[] payload = Arrays.copyOfRange(msg, 9, msg.length);
					int piece = channelData.getPiece();
					
					if(pieceIndex == piece) {
						TorrentFile torrentFile = torrentsProcessing.get(channelNioKey);
						channelData.addReceivedBlock(offsetBytes, payload);
						channelData.setBlockObtained(offsetBytes, (int) torrentFile.getPieceSize(), blockReqSize);
						torrentFile.updateDownloadedBytesCount(payload.length);
						
						if(channelData.pieceComplete()) {
							if(torrentFile.validatePiece(channelData.getBlocksCollected().array(), piece) == 0) {
								try (FileChannel fileChannel = (FileChannel) Files.newByteChannel(torrentFile.getOutputFileLocation(), EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ))){
									byte[] pieceBlocks = channelData.getBlocksCollected().array();
									System.out.println("Piece Length: " + pieceBlocks.length + " Offset: " + pieceBlocks.length * piece);
									MappedByteBuffer mapBuff = fileChannel.map(FileChannel.MapMode.READ_WRITE, pieceBlocks.length * piece, pieceBlocks.length);
									mapBuff.put(pieceBlocks);
									 
								} catch (IOException io){
									//TODO File Write Error
									System.err.println("Error writing to file.");
									io.printStackTrace();
									System.exit(0);
								}
								
								PiecePickers.get(channelNioKey).pieceObtained(pieceIndex);
								channelData.setPiece(-1);
								channelData.clearBlockData();
								
								HashSet<ChannelData> allChannelData = torrentFile.getallChannelData();
								for(ChannelData cData : allChannelData) {
									byte[] bitfield = cData.getPeer().getBitfield();
									if(!BitfieldOperations.checkBit(pieceIndex, bitfield)) {
										byte[] haveMsg = new byte[] {0x00, 0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x04, pieceIndexBytes[0], pieceIndexBytes[1], pieceIndexBytes[2], pieceIndexBytes[3]};
										cData.addMessage(msgData);
									}
								}
								
								if(torrentFile.fileComplete()) {
									System.out.println("File Complete!");
									nioShutdown = true;
								}
							} 
						}
					}
					break;
				}
				
				default:
					result = "Unrecognised Message: " + (byte)msg[0];
				}
			
			
		}
		return result;
	}
	
		
	private ArrayDeque<byte[]> extractMessages(ChannelData channelData, byte[] message){
		ArrayDeque<byte[]> parsedMessages = new ArrayDeque<byte[]>();
		
		byte[] storedTcpPacketBytes = channelData.getStoredTcpPacketBytes();
		byte[] allMsgBytes = message;
		
		if(storedTcpPacketBytes != null) {//Combine bytes from previous message if they are there
			allMsgBytes = new byte[message.length + storedTcpPacketBytes.length];
			System.arraycopy(storedTcpPacketBytes, 0, allMsgBytes, 0, storedTcpPacketBytes.length);
			System.arraycopy(message, 0, allMsgBytes, storedTcpPacketBytes.length, message.length);
		} 
		
		int index = 0;
		while(index < allMsgBytes.length) {
			storedTcpPacketBytes = null;
			if((allMsgBytes.length - index) >= 5) {//Minimum needed for message length & ID
				byte[] handshakePrefix = new byte[] {(byte)0x13, (byte)0x42, (byte)0x69, (byte)0x74, (byte)0x54};
				if(index < 5 && Arrays.compare(Arrays.copyOfRange(allMsgBytes, index, 5), handshakePrefix) == 0) {//Msg starts with handshake
					if(allMsgBytes.length >= 68) {//Rest of handshake is present
						byte[] handshake = new byte[68];
						System.arraycopy(Arrays.copyOfRange(allMsgBytes, 0, 68), 0, handshake, 0, 68);
						parsedMessages.add(handshake);
						index = 68;
						continue;
					} else {
						storedTcpPacketBytes = allMsgBytes;
						channelData.setStoredTcpPacketBytes(storedTcpPacketBytes);
						index = allMsgBytes.length;
						break;
						
					}
				} 
				
				
				int msgLength = ByteBuffer.wrap(Arrays.copyOfRange(allMsgBytes, index, index + 4)).getInt();
				byte msgID = allMsgBytes[index + 4];
				index += 4;
				if((allMsgBytes.length - index) >= msgLength ) {//Amount remaining is enough for whole message
					byte[] newMessage = new byte[msgLength];
					
					System.arraycopy(Arrays.copyOfRange(allMsgBytes, index, index + msgLength), 0, newMessage, 0, msgLength);
					parsedMessages.add(newMessage);
					index += msgLength;
				} else {
					storedTcpPacketBytes = Arrays.copyOfRange(allMsgBytes, index - 4, allMsgBytes.length);
					channelData.setStoredTcpPacketBytes(storedTcpPacketBytes);
					break;
				}
				
			} else {
				storedTcpPacketBytes = Arrays.copyOfRange(allMsgBytes, index, allMsgBytes.length);
				channelData.setStoredTcpPacketBytes(storedTcpPacketBytes);
				break;
			}
		}
		return parsedMessages;
	}
	
	
	private void writeMessage(SocketChannel channel, byte[] message) throws IOException {
		channel.write(ByteBuffer.wrap(message));
	}
	
	//DEBUG 
	private void hexPrint(byte[] bytes) {
		for(byte b : bytes) {
			System.out.print(String.format("%02X", ((byte)b) & 0xFF).toUpperCase() + " ");
		}
		System.out.println();
	}
	private byte[] getHandshake(TorrentFile tf) {
		ByteBuffer buff = ByteBuffer.allocate(68);
		buff.put((byte) 19); //pstrlen
		buff.put("BitTorrent protocol".getBytes(StandardCharsets.UTF_8)); //pstr
		buff.put(new byte[] {0, 0, 0, 0, 0, 0, 0, 0}); //extensions
		buff.put(tf.getInfoHash()); //infohash
		buff.put(peerID.toString().getBytes(StandardCharsets.UTF_8));
		buff.flip();
		
		return buff.array();
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
			} else {
				// TODO Need to handle different tracker errors
				System.err.println("Unable to parse tracker response. Exiting.");
				System.exit(0);
			}
			peerManagers.put(channelNioKey, peerManager);
		}
	}
	
		
	private HashMap<Integer, TorrentFile> torrentsProcessing = new HashMap<Integer, TorrentFile>();
	private HashMap<Integer, PeerManager> peerManagers = new HashMap<Integer, PeerManager>();
	private HashMap<Integer, PiecePicker> PiecePickers  = new HashMap<Integer, PiecePicker>();
	private Integer nioKey = -1;
	private static NIOThread nioThread = null;
	private StringBuilder peerID = new StringBuilder("TM470");
	private int myListeningPort = 6888;
	private int blockReqSize = 16000;
	
	protected volatile HashSet<TorrentFile> torrentsToProcess = new HashSet<TorrentFile>();
	
	public volatile HashMap<Path, Boolean> shutdown = new HashMap<Path, Boolean>();
	public volatile int maxNumDownloadingConns = 1;
	public volatile int maxNumSeedingConns = 1;
	public volatile boolean seedAfterwards = true; 
	public volatile boolean nioShutdown = false;
	public int blocksWaiting = 10;
	
	//DEBUG
	private static int msgCount = 0;
	

	
}
