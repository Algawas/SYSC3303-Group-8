import java.net.InetAddress;

public class PacketHandler {
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
	
	public void sendDATAPacket(DATAPacket dataPacket) {		
		System.out.println(Globals.getVerboseMessage("PacketHandler", 
				String.format("sending DATA packet %d to client %s:%d", dataPacket.getBlockNumber(), dataPacket.getRemoteAddress(), dataPacket.getRemotePort())));
		
		// send DATA datagram packet
		tftpSocket.send(dataPacket);
	}
		
	public void sendACKPacket(short blockNumber) {
		ACKPacket ackPacket = TFTPPacketBuilder.getACKDatagram(blockNumber, remoteAddress, remotePort);
		
		System.out.println(Globals.getVerboseMessage("PacketHandler", 
				String.format("sending ACK packet %d to client %s:%d", blockNumber, remoteAddress, remotePort)));
		
		// sends acknowledgement to client
		tftpSocket.send(ackPacket);
	}
	
	
	public ACKPacket receiveACKPacket(short expectedBlockNumber) {	
		ACKPacket ackPacket = null;
		
		TFTPPacket receivePacket = null;
		while (receivePacket == null) {
			receivePacket = tftpSocket.receive();
			
			if (!receivePacket.getRemoteAddress().equals(remoteAddress) ||
					receivePacket.getRemotePort() != remotePort) {
				String errorMessage = String.format("Received packet from unknown source. Expected: %s:%d, Received: %s:%d", 
						remoteAddress, remotePort, receivePacket.getRemoteAddress(), receivePacket.getRemotePort());
				
				System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));	
				errorHandler.sendUnknownTrasnferIDErrorPacket(errorMessage, receivePacket.getRemoteAddress(), receivePacket.getRemotePort());
				receivePacket = null;
				continue;
			}
		}
		
		if (receivePacket.getPacketType() == TFTPPacketType.ACK) {
			try {
				ackPacket = new ACKPacket(receivePacket);
				
				if (ackPacket.getBlockNumber() != expectedBlockNumber) {
					String errorMessage = String.format("unexpected ACK packet block number received. Expected: %d, Received: %d", expectedBlockNumber, ackPacket.getBlockNumber());
					System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));
					errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
				}
				
			} catch(TFTPPacketParsingError e) {
				String errorMessage = String.format("cannot parse ACK packet %d", expectedBlockNumber);
				System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));
				errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
			}
			
			System.out.println(Globals.getVerboseMessage("PacketHandler", 
					String.format("received ACK packet %d from client %s%d", ackPacket.getBlockNumber(), remoteAddress, remotePort)));
		}
		else if (receivePacket.getPacketType() == TFTPPacketType.ERROR) {
			ERRORPacket errorPacket = null;
			
			try {
				errorPacket = new ERRORPacket(receivePacket);
			} catch (TFTPPacketParsingError e) {
				String errorMessage = "cannot parse ERROR packet";
				System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));
				errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
			}
			
			System.out.println(Globals.getVerboseMessage("PacketHandler", 
					String.format("received ERROR packet from client %s%d, errorCode: %d, errorMessage: %s", remoteAddress, 
							remotePort, errorPacket.getErrorCode(), errorPacket.getErrorMessage())));
		}
		else {
			String errorMessage = "invalid TFTP packet";
			System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));
			errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
		}
		
		return ackPacket;
	}
	
	public DATAPacket receiveDATAPacket(short expectedBlockNumber) {
		DATAPacket dataPacket = null;
		
		TFTPPacket receivePacket = null;
		while (receivePacket == null) {
			receivePacket = tftpSocket.receive();
				
			if (!receivePacket.getRemoteAddress().equals(remoteAddress) ||
					receivePacket.getRemotePort() != remotePort) {
				String errorMessage = String.format("Received packet from unknown source. Expected: %s:%d, Received: %s:%d", 
						remoteAddress, remotePort, receivePacket.getRemoteAddress(), receivePacket.getRemotePort());
				
				System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));	
				errorHandler.sendUnknownTrasnferIDErrorPacket(errorMessage, receivePacket.getRemoteAddress(), receivePacket.getRemotePort());
				return null;
			}
		}
			
		if (receivePacket.getPacketType() == TFTPPacketType.DATA) {
			try {
				dataPacket = new DATAPacket(receivePacket);
			} catch(TFTPPacketParsingError e) {
				String errorMessage = String.format("cannot parse DATA packet %d", expectedBlockNumber);
				System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));
				errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
			}
			
			if (dataPacket.getBlockNumber() != expectedBlockNumber) {
				String errorMessage = String.format("unexpected DATA packet block number received. Expected: %d, Received: %d", expectedBlockNumber, dataPacket.getBlockNumber());
				System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));
				errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
			}
			
			System.out.println(Globals.getVerboseMessage("PacketHandler", 
					String.format("received DATA packet %d from client %s%d", dataPacket.getBlockNumber(), remoteAddress, remotePort)));
		}
		else if (receivePacket.getPacketType() == TFTPPacketType.ERROR) {
			ERRORPacket errorPacket = null;
			
			try {
				errorPacket = new ERRORPacket(receivePacket);
			} catch (TFTPPacketParsingError e) {
				String errorMessage = "cannot parse ERROR packet";
				System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));
				errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
			}
			
			System.out.println(Globals.getVerboseMessage("PacketHandler", 
					String.format("received ERROR packet from client %s%d, errorCode: %d, errorMessage: %s", remoteAddress, 
							remotePort, errorPacket.getErrorCode(), errorPacket.getErrorMessage())));
		}
		else {
			String errorMessage = "invalid DATA sent";
			System.err.println(Globals.getErrorMessage("PacketHandler", errorMessage));
			errorHandler.sendIllegalOperationErrorPacket(errorMessage, remoteAddress, remotePort);
		}
		
		return dataPacket;
	}
}
