import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/**
 * This class handles the packet sending and receiving
 * 
 * @author Group 8
 *
 */
public class PacketHandler {
	public class PacketHandlerReturn {
		public ACKPacket ackPacket = null;
		public DATAPacket dataPacket = null;
		public boolean timeout = false;
	}
	
	private TFTPSocket tftpSocket;
	private ErrorHandler errorHandler;
	private InetAddress remoteAddress;
	private int remotePort;
	
	public PacketHandler(TFTPSocket tftpSocket, ErrorHandler errorHandler, InetAddress remoteAddress, int remotePort) {
		this.tftpSocket = tftpSocket;
		this.errorHandler = errorHandler;
		this.remoteAddress = remoteAddress;
		this.remotePort = remotePort;
	}
	
	/**
	 * Sends DATA packet
	 * 
	 * @param dataPacket
	 */
	public void sendDATAPacket(DATAPacket dataPacket) {		
		System.out.println(Globals.getVerboseMessage("PacketHandler", 
				String.format("sending DATA packet %d to client %s:%d", dataPacket.getBlockNumber(), dataPacket.getRemoteAddress(), dataPacket.getRemotePort())));
		
		// send DATA datagram packet
		tftpSocket.send(dataPacket);
	}
	
	/**
	 * Sends ACK packet
	 * 
	 * @param blockNumber
	 */
	public void sendACKPacket(short blockNumber) {
		ACKPacket ackPacket = TFTPPacketBuilder.getACKDatagram(blockNumber, remoteAddress, remotePort);
		
		System.out.println(Globals.getVerboseMessage("PacketHandler", 
				String.format("sending ACK packet %d to client %s:%d", blockNumber, remoteAddress, remotePort)));
		
		// sends acknowledgement to client
		tftpSocket.send(ackPacket);
	}
	
	/**
	 * Receives ACK packet and handles error situations
	 * 
	 * @param expectedBlockNumber
	 * @return ACK packet or null if error occurred
	 */
	private PacketHandlerReturn recACKPacket(short expectedBlockNumber) {	
		PacketHandlerReturn res = new PacketHandlerReturn();
		
		ACKPacket ackPacket = null;
		
		TFTPPacket receivePacket = null;
		while (receivePacket == null) {
			try {
				receivePacket = tftpSocket.receive();
			} catch (SocketTimeoutException e) {
				String errorMessage = "Socket timed out. Cannot receive ACK packet";
				System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));	
				res.timeout = true;
				return res;
			} catch (IOException e) {
				System.err.println(Globals.getErrorMessage("PacketHandler", "oops... the connection broke"));
				e.printStackTrace();
				System.exit(-1);
			}
			
			// if the packet was received from another source
			// then send error packet with error code 5
			// then keep on listening for a packet from the correct source
			if (!receivePacket.getRemoteAddress().equals(remoteAddress) ||
					receivePacket.getRemotePort() != remotePort) {
				String errorMessage = String.format("Received packet from unknown source. Expected: %s:%d, Received: %s:%d", 
						remoteAddress, remotePort, receivePacket.getRemoteAddress(), receivePacket.getRemotePort());
				
				System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));	
				
				// send error packet to the wrong source
				errorHandler.sendUnknownTrasnferIDErrorPacket(errorMessage, receivePacket.getRemoteAddress(), receivePacket.getRemotePort());
				receivePacket = null;
				continue;
			}
			
			
			if (receivePacket.getPacketType() == TFTPPacketType.ACK) {
				// parse ACK packet
				try {
					ackPacket = new ACKPacket(receivePacket);
					
					// if different block number is received then send error packet with error code 4
					// reset the received tftp packet to null and listen for new packets again
					if (ackPacket.getBlockNumber() < expectedBlockNumber) {
						String errorMessage = String.format("duplicate ACK packet block number received. Expected: %d, Received: %d", expectedBlockNumber, ackPacket.getBlockNumber());
						System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));
						//errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
						receivePacket = null;
						continue;
					}
					else if (ackPacket.getBlockNumber() > expectedBlockNumber) {
						String errorMessage = String.format("incorrect ACK packet block number received. Expected: %d, Received: %d", expectedBlockNumber, ackPacket.getBlockNumber());
						System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));
						errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
					}
					
				} catch(TFTPPacketParsingError e) {
					// send error packet with error code 4
					String errorMessage = String.format("cannot parse ACK packet %d", expectedBlockNumber);
					System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));
					errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
				}
				
				System.out.println(Globals.getVerboseMessage("PacketHandler", 
						String.format("received ACK packet %d from client %s%d", ackPacket.getBlockNumber(), remoteAddress, remotePort)));
				
				res.ackPacket = ackPacket;
			}
			else if (receivePacket.getPacketType() == TFTPPacketType.ERROR) {
				// parse ERROR packet
				ERRORPacket errorPacket = null;
				
				try {
					errorPacket = new ERRORPacket(receivePacket);
				} catch (TFTPPacketParsingError e) {
					// send error packet with error code 4
					String errorMessage = "cannot parse ERROR packet";
					System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));
					errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
				}
				
				System.out.println(Globals.getVerboseMessage("PacketHandler", 
						String.format("received ERROR packet from client %s%d, errorCode: %d, errorMessage: %s", remoteAddress, 
								remotePort, errorPacket.getErrorCode(), errorPacket.getErrorMessage())));
			}
			else {
				System.out.println(receivePacket.getOPCode());
				
				// ssend error packet with error code 4
				String errorMessage = "invalid TFTP packet";
				System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));
				errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
			}
		}
		
		res.ackPacket = ackPacket;

		return res;
	}
	
	/**
	 * Receives DATA packet and handles error situations
	 * 
	 * @param expectedBlockNumber
	 * @return DATA packet or null if error occurred
	 */
	private PacketHandlerReturn recDATAPacket(short expectedBlockNumber) {
		PacketHandlerReturn res = new PacketHandlerReturn();
		DATAPacket dataPacket = null;
		
		TFTPPacket receivePacket = null;
		while (receivePacket == null) {
			try {
				receivePacket = tftpSocket.receive();
			} catch (SocketTimeoutException e) {
				String errorMessage = "Socket timed out. Cannot receive DATA packet";
				System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));	
				res.timeout = true;
				return res;
			} catch (IOException e) {
				System.err.println(Globals.getErrorMessage("PacketHandler", "oops... the connection broke"));
				e.printStackTrace();
				System.exit(-1);			
			}
			
			// if the packet was received from another source
			// then send error packet with error code 5
			// then keep on listening for a packet from the correct source
			if (!receivePacket.getRemoteAddress().equals(remoteAddress) ||
					receivePacket.getRemotePort() != remotePort) {
				String errorMessage = String.format("Received packet from unknown source. Expected: %s:%d, Received: %s:%d", 
						remoteAddress, remotePort, receivePacket.getRemoteAddress(), receivePacket.getRemotePort());
				
				// send error packet to the wrong source
				System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));	
				errorHandler.sendUnknownTrasnferIDErrorPacket(errorMessage, receivePacket.getRemoteAddress(), receivePacket.getRemotePort());
				receivePacket = null;
				continue;
			}
			
			if (receivePacket.getPacketType() == TFTPPacketType.DATA) {
				// parse DATA packet
				
				try {
					dataPacket = new DATAPacket(receivePacket);
				} catch(TFTPPacketParsingError e) {
					String errorMessage = String.format("cannot parse DATA packet %d", expectedBlockNumber);
					System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));
					errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
				}
				
				// if different block number is received then send error packet with error code 4
				if (dataPacket.getBlockNumber() < expectedBlockNumber) {
					String errorMessage = String.format("duplicate DATA packet block number received. Expected: %d, Received: %d", expectedBlockNumber, dataPacket.getBlockNumber());
					System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));
					//errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
					receivePacket = null;
					continue;
				}
				else if (dataPacket.getBlockNumber() > expectedBlockNumber) {
					String errorMessage = String.format("incorrect ACK packet block number received. Expected: %d, Received: %d", expectedBlockNumber, dataPacket.getBlockNumber());
					System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));
					errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
				}
				
				System.out.println(Globals.getVerboseMessage("PacketHandler", 
						String.format("received DATA packet %d from client %s%d", dataPacket.getBlockNumber(), remoteAddress, remotePort)));
				
				res.dataPacket = dataPacket;
			}
			else if (receivePacket.getPacketType() == TFTPPacketType.ERROR) {
				// parse ERROR packet
				ERRORPacket errorPacket = null;
				
				try {
					errorPacket = new ERRORPacket(receivePacket);
				} catch (TFTPPacketParsingError e) {
					// send error packet with error code 4
					String errorMessage = "cannot parse ERROR packet";
					System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));
					errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
				}
				
				System.out.println(Globals.getVerboseMessage("PacketHandler", 
						String.format("received ERROR packet from client %s%d, errorCode: %d, errorMessage: %s", remoteAddress, 
								remotePort, errorPacket.getErrorCode(), errorPacket.getErrorMessage())));
			}
			else {
				System.out.println(receivePacket.getOPCode());
				
				// send error packet with error code 4
				String errorMessage = "invalid DATA sent";
				System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));
				errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
			}
		}
		
		return res	;
	}
	
	public DATAPacket receiveDATAPacket(short blockNumber) {
		PacketHandlerReturn phRes = null;
		
		int numberOfTries = 1; 
    	while (numberOfTries < NetworkConfig.MAX_TRIES) {
    		phRes = recDATAPacket(blockNumber);
    		numberOfTries++;
    		
    		if (!phRes.timeout)
    			break;
    	}
    	
    	if (numberOfTries == NetworkConfig.MAX_TRIES) {
    		System.err.println(Globals.getErrorMessage("Client", "max tries reached. Exitting connection"));
    	}
    	
    	return phRes.dataPacket;
	}
	
	public ACKPacket receiveACKPacket(short expectedBlockNumber) {
		PacketHandlerReturn phRes = null;
		
		int numberOfTries = 1;
		while (numberOfTries < NetworkConfig.MAX_TRIES) {
			phRes = recACKPacket(expectedBlockNumber);
			numberOfTries++;
			
			if (!phRes.timeout)
				break;
		}
		
		if (numberOfTries == NetworkConfig.MAX_TRIES) {
        	System.err.println(Globals.getErrorMessage("Client", "max tries reached. Exitting connection"));
        }
		
		return phRes.ackPacket;
	}
	
	public ACKPacket receiveACKPacket(DATAPacket sentDataPacket) {
		PacketHandlerReturn phRes = null;
		
		int numberOfTries = 1;
		while (numberOfTries < NetworkConfig.MAX_TRIES) {
			phRes = recACKPacket(sentDataPacket.getBlockNumber());
			numberOfTries++;
			
			if (!phRes.timeout)
				break;
			else
				sendDATAPacket(sentDataPacket);
		}
		
		if (numberOfTries == NetworkConfig.MAX_TRIES) {
        	System.err.println(Globals.getErrorMessage("Client", "max tries reached. Exitting connection"));
        }
		
		return phRes.ackPacket;
	}
}
