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
		String[] messages = {
				"",
				String.format("sending %s to %s:%d", dataPacket.toString(), dataPacket.getRemoteAddress(), dataPacket.getRemotePort())
		};
		UIManager.printMessage("PacketHandler", messages);
		
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
		
		String[] messages = {
				"",
				String.format("sending %s to %s:%d", ackPacket.toString(), remoteAddress, remotePort)
		};
		UIManager.printMessage("PacketHandler", messages);
		
		// sends acknowledgement to client
		tftpSocket.send(ackPacket);
	}
	
	/**
	 * Receives ACK packet and handles error situations
	 * 
	 * @param expectedBlockNumber
	 * @return return object with ACK packet 
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
				UIManager.printErrorMessage("PacketHandler", errorMessage);	
				res.timeout = true;
				return res;
			} catch (IOException e) {
				UIManager.printErrorMessage("PacketHandler", "oops... the connection broke");
				e.printStackTrace();
				System.exit(-1);
			}
			
			if (expectedBlockNumber == 0) {
				remoteAddress = receivePacket.getRemoteAddress();
				remotePort = receivePacket.getRemotePort();
			}
			else {
				// if the packet was received from another source
				// then send error packet with error code 5
				// then keep on listening for a packet from the correct source
				if (!receivePacket.getRemoteAddress().equals(remoteAddress) ||
						receivePacket.getRemotePort() != remotePort) {
					String errorMessage = String.format("Received packet from unknown source. Expected: %s:%d, Received: %s:%d", 
							remoteAddress, remotePort, receivePacket.getRemoteAddress(), receivePacket.getRemotePort());
					
					UIManager.printErrorMessage("PacketHandler", errorMessage);	
					
					// send error packet to the wrong source
					errorHandler.sendUnknownTrasnferIDErrorPacket(errorMessage, receivePacket.getRemoteAddress(), receivePacket.getRemotePort());
					receivePacket = null;
					continue;
				}
			}
			
			
			if (receivePacket.getPacketType() == TFTPPacketType.ACK) {
				// parse ACK packet
				try {
					ackPacket = new ACKPacket(receivePacket);
					
					// if different block number is received then send error packet with error code 4
					// reset the received tftp packet to null and listen for new packets again
					if (ackPacket.getBlockNumber() < expectedBlockNumber) {
						String errorMessage = String.format("duplicate ACK packet block number received. Expected: %d, Received: %d", expectedBlockNumber, ackPacket.getBlockNumber());
						UIManager.printErrorMessage("PacketHandler", errorMessage);
						receivePacket = null;
						continue;
					}
					else if (ackPacket.getBlockNumber() > expectedBlockNumber) {
						String errorMessage = String.format("incorrect ACK packet block number received. Expected: %d, Received: %d", expectedBlockNumber, ackPacket.getBlockNumber());
						UIManager.printErrorMessage("PacketHandler", errorMessage);
						errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
					}
					
				} catch(TFTPPacketParsingError e) {
					// send error packet with error code 4
					String errorMessage = String.format("cannot parse ACK packet %d", expectedBlockNumber);
					UIManager.printErrorMessage("PacketHandler", errorMessage);
					errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
				}
				
				String[] messages = {
						"",
						String.format("received %s from %s:%d", ackPacket.toString(), remoteAddress, remotePort)
				};
				
				UIManager.printMessage("PacketHandler", messages);
				
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
					UIManager.printErrorMessage("PacketHandler", errorMessage);
					errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
				}
				
				String errorMessage = String.format("received %s from %s:%d", errorPacket.toString(), remoteAddress, remotePort);
				UIManager.printErrorMessage("PacketHandler", errorMessage);
			}
			else {
				// send error packet with error code 4
				String errorMessage = "invalid TFTP packet";
				UIManager.printErrorMessage("PacketHandler", errorMessage);
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
	 * @return return object with DATA packet 
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
				UIManager.printErrorMessage("PacketHandler", errorMessage);	
				res.timeout = true;
				return res;
			} catch (IOException e) {
				UIManager.printErrorMessage("PacketHandler", "oops... the connection broke");
				e.printStackTrace();
				System.exit(-1);			
			}
			
			if (expectedBlockNumber == 1) {
				remoteAddress = receivePacket.getRemoteAddress();
				remotePort = receivePacket.getRemotePort();
			}
			else {
				// if the packet was received from another source
				// then send error packet with error code 5
				// then keep on listening for a packet from the correct source
				if (!receivePacket.getRemoteAddress().equals(remoteAddress) ||
						receivePacket.getRemotePort() != remotePort) {
					String errorMessage = String.format("Received packet from unknown source. Expected: %s:%d, Received: %s:%d", 
							remoteAddress, remotePort, receivePacket.getRemoteAddress(), receivePacket.getRemotePort());
					
					// send error packet to the wrong source
					UIManager.printErrorMessage("PacketHandler", errorMessage);	
					errorHandler.sendUnknownTrasnferIDErrorPacket(errorMessage, receivePacket.getRemoteAddress(), receivePacket.getRemotePort());
					receivePacket = null;
					continue;
				}
			}
			
			if (receivePacket.getPacketType() == TFTPPacketType.DATA) {
				// parse DATA packet
				
				try {
					dataPacket = new DATAPacket(receivePacket);
				} catch(TFTPPacketParsingError e) {
					String errorMessage = String.format("cannot parse DATA packet %d", expectedBlockNumber);
					UIManager.printErrorMessage("PacketHandler", errorMessage);
					errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
				}
				
				// if different block number is received then send error packet with error code 4
				if (dataPacket.getBlockNumber() < expectedBlockNumber) {
					String errorMessage = String.format("duplicate DATA packet block number received. Expected: %d, Received: %d", expectedBlockNumber, dataPacket.getBlockNumber());
					UIManager.printErrorMessage("PacketHandler", errorMessage);
					receivePacket = null;
					continue;
				}
				else if (dataPacket.getBlockNumber() > expectedBlockNumber) {
					String errorMessage = String.format("incorrect DATA packet block number received. Expected: %d, Received: %d", expectedBlockNumber, dataPacket.getBlockNumber());
					UIManager.printErrorMessage("PacketHandler", errorMessage);
					errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
				}
				
				String[] messages = {
						"",
						String.format("received %s from %s:%d", dataPacket.toString(), remoteAddress, remotePort)
				};
				
				UIManager.printMessage("PacketHandler", messages);
							
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
					UIManager.printErrorMessage("PacketHandler", errorMessage);
					errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
				}
				
				String errorMessage = String.format("received %s from %s:%d", errorPacket.toString(), remoteAddress, remotePort);
				UIManager.printErrorMessage("PacketHandler", errorMessage);
			}
			else {
				System.out.println(receivePacket.getOPCode());
				
				// send error packet with error code 4
				String errorMessage = "invalid DATA sent";
				UIManager.printErrorMessage("PacketHandler", errorMessage);
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
    		UIManager.printErrorMessage("PacketHandler", "max tries reached. Exitting connection");
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
        	UIManager.printErrorMessage("PacketHandler", "max tries reached. Exitting connection");
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
        	UIManager.printErrorMessage("PacketHandler", "max tries reached. Exitting connection");
        }
		
		return phRes.ackPacket;
	}
}
