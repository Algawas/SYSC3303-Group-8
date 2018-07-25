import java.net.InetAddress;

public class WRQServerThread extends Thread {
	/**
	 * This class is used to communicate further with a client that made a WQR request
	 */
	
	private TFTPSocket tftpSocket;
	private TFTPPacket requestPacket;
	
	private FileManager fileManager;
	private ErrorHandler errorHandler;
	private PacketHandler packetHandler;
	
	private InetAddress remoteAddress;
	private int remotePort;
	
	/**
	 * Constructor
	 * 
	 * @param receivedDatagramPacket request datagram packet received from client
	 */
	public WRQServerThread(TFTPPacket tftpPacket) {
		this.requestPacket = tftpPacket;
		tftpSocket = new TFTPSocket();
		
		fileManager = new FileManager();
		errorHandler = new ErrorHandler(tftpSocket);
		
		remoteAddress = tftpPacket.getRemoteAddress();
		remotePort = tftpPacket.getRemotePort();
	}

	/**
	 * For threading purposes only
	 */
	@Override
	public void run() {
		handleWRQConnection();
		cleanUp();
	}
	
	/**
	 * Handles DATA datagram packets received from client
	 */
	private void handleWRQConnection() {
		RRQWRQPacket wrqPacket = null;
		
		// parse read request packet
		try {
			wrqPacket = new RRQWRQPacket(requestPacket);
		} catch (TFTPPacketParsingError e) {
			/*
			 * If an invalid read request is sent, then and error packet with 
			 * error code 4 will be sent to the client
			 * 
			 * The current write request thread will be terminated
			 */
			System.err.println(Globals.getErrorMessage("WRQServerThread", "cannot parse WRQ TFTP packet"));
			errorHandler.sendIllegalOperationErrorPacket("invalid WRQ TFTP packet", remoteAddress, remotePort);
			return;
		}
		
		packetHandler = new PacketHandler(tftpSocket, errorHandler, remoteAddress, remotePort);
		
		// send ACK packet to client in response to the write request
		packetHandler.sendACKPacket((short) 0);
		
		// receive all data packets from client that wants to transfer a file
		// once the data length is less than 512 bytes then stop listening for
		// data packets from the client
		int dataLenReceived = NetworkConfig.DATAGRAM_PACKET_MAX_LEN;
		short blockNumber = 0;
		while (dataLenReceived == NetworkConfig.DATAGRAM_PACKET_MAX_LEN) { 
			blockNumber++;
			System.out.println(Globals.getVerboseMessage("WRQServerThread", 
					String.format("waiting for DATA packet from client %s:%d", remoteAddress, remotePort)));
			
			// receives data packet from client
			DATAPacket dataPacket = packetHandler.receiveDATAPacket(blockNumber);
			if (dataPacket == null) {
				break;
			}
			
			String fileName = wrqPacket.getFileName();
			byte[] fileData = dataPacket.getDataBytes();

			// write file data from DATA packet to hard drive
			FileManager.FileManagerResult res = fileManager.writeFile(fileName, fileData);
			
			// if error occurred end connection
			if (res.error) {
				// access violation error will send an error packet with error code 2 and the connection
				if (res.accessViolation)
					errorHandler.sendAccessViolationErrorPacket(String.format("write access denied to file: %s", fileName), remoteAddress, remotePort);
				// disk full error will send an error packet with error code 3 and close the connection
				else if (res.fileAlreadyExist)
					errorHandler.sendFileExistsErrorPacket(String.format("file already exists: %s", fileName), remoteAddress, remotePort);
				else if (res.diskFull)
				    errorHandler.sendDiskFullErrorPacket(String.format("Not enough disk space for file: %s", fileName), remoteAddress, remotePort);
				return;
			}
			
			System.out.println(Globals.getVerboseMessage("WRQServerThread", String.format("finsihed writing data to file %s", fileName)));
			
			// save the length of file data that was just saved
			dataLenReceived = dataPacket.getPacketLength();
		
			packetHandler.sendACKPacket(blockNumber);
		}
		
		System.out.println(Globals.getVerboseMessage("WRQServerThread", "connection is finsihed"));
	}
	
	/**
	 * Closes datagram socket once the connection is finished
	 */
	private void cleanUp() {
		System.out.println(Globals.getVerboseMessage("WRQServerThread", "closing socket"));
		tftpSocket.close();
	}
}
