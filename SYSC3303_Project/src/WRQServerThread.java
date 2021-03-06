import java.net.InetAddress;

public class WRQServerThread extends Thread {
	/**
	 * This class is used to communicate further with a client that made a WQR request
	 */
	
	private Server server;
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
	public WRQServerThread(Server server,TFTPPacket tftpPacket) {
		this.server = server;
		this.requestPacket = tftpPacket;
		
		//tftpSocket = new TFTPSocket(0);
		tftpSocket = new TFTPSocket(NetworkConfig.TIMEOUT_TIME);
		
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
			UIManager.printErrorMessage("WRQServerThread", "cannot parse WRQ TFTP packet");
			errorHandler.sendIllegalOperationErrorPacket("invalid WRQ TFTP packet", remoteAddress, remotePort);
			return;
		}
		
		packetHandler = new PacketHandler(tftpSocket, errorHandler, remoteAddress, remotePort);
		
		// creates file if it does not exist
		String fileName = wrqPacket.getFileName();
		FileManager.FileManagerResult res = fileManager.createFile(fileName);
		
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
		
		// send ACK packet to client in response to the write request
		packetHandler.sendACKPacket((short) 0);
		
		// receive all data packets from client that wants to transfer a file
		// once the data length is less than 512 bytes then stop listening for
		// data packets from the client
		int dataLenReceived = NetworkConfig.DATAGRAM_PACKET_MAX_LEN;
		
		short blockNumber = 1;
		DATAPacket dataPacket = null;
		while (dataLenReceived == NetworkConfig.DATAGRAM_PACKET_MAX_LEN) {
			String[] messages1 = {
					"",
					String.format("waiting for DATA packet from client %s:%d", remoteAddress, remotePort)
			};
			
			UIManager.printMessage("WRQServerThread", messages1);
			
			// receives data packet from client
			dataPacket = packetHandler.receiveDATAPacket(blockNumber);
        	
			if (dataPacket == null) {
				break;
			}
			
			if (dataPacket.getBlockNumber() == blockNumber) {
				byte[] fileData = dataPacket.getDataBytes();
	
				// write file data from DATA packet to hard drive
				res = fileManager.writeFile(fileName, fileData);
				
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
				
				String[] messages2 = {
						"",
						String.format("finsihed writing data from DATA packet %d to file %s", dataPacket.getBlockNumber(), fileName)
				};
				
				UIManager.printMessage("WRQServerThread", messages2);
				
				// save the length of file data that was just saved
				dataLenReceived = dataPacket.getPacketLength();
				
				blockNumber++;
			}
		
			packetHandler.sendACKPacket(dataPacket.getBlockNumber());
		}
		
		String[] messages3 = {
				String.format("finsihed writing file %s", wrqPacket.getFileName()),
				String.format("finsihed writing file %s", wrqPacket.getFileName())
		};
		
		UIManager.printMessage("WRQServerThread", messages3);
	}
	
	/**
	 * Closes datagram socket once the connection is finished
	 */
	private void cleanUp() {
		
		String[] messages = {
				"",
				"closing socket..."
		};
		
		UIManager.printMessage("WRQServerThread", messages);
		tftpSocket.close();
		
		server.removeConnection(remoteAddress + ":" + remotePort);
	}
}
