import java.net.InetAddress;
import java.util.Queue;

/**
 * This class is used to communicate further with a client that made a WQR request
 */
public class RRQServerThread extends Thread {
	private TFTPSocket tftpSocket;
	private TFTPPacket requestPacket;
	
	private FileManager fileManager;
	private ErrorHandler errorHandler;
	private PacketHandler packetHandler;
	
	private InetAddress remoteAddress;
	private int remotePort;
	
	private Queue<DATAPacket> dataPacketStack;
	
	/**
	 * Constructor
	 * 
	 * @param receivedDatagramPacket request datagram packet received from client
	 */
	public RRQServerThread(TFTPPacket requestPacket) {
		this.requestPacket = requestPacket;
		
		tftpSocket = new TFTPSocket(NetworkConfig.TIMEOUT_TIME);
		
		remoteAddress = requestPacket.getRemoteAddress();
		remotePort = requestPacket.getRemotePort();
		packetHandler = new PacketHandler(tftpSocket, errorHandler, remoteAddress, remotePort);
		
		fileManager = new FileManager();
		errorHandler = new ErrorHandler(tftpSocket);
	}
	
	/**
	 * For threading purposes
	 */
	@Override
	public void run() {
		handleRRQConnection();
		cleanUp();
	}
	
	/**
	 * Handles sending DATA datagram packets to client
	 */
	private void handleRRQConnection() {
		RRQWRQPacket rrqPacket = null;
		
		// parse read request packet
		try {
			rrqPacket = new RRQWRQPacket(requestPacket);
		} catch (TFTPPacketParsingError e) {
			/*
			 * If an invalid read request is sent, then and error packet with 
			 * error code 4 will be sent to the client
			 * 
			 * The current read request thread will be terminated
			 */
			System.err.println(Globals.getErrorMessage("RRQServerThread", "cannot parse RRQ TFTP packet"));
			errorHandler.sendIllegalOperationErrorPacket("invalid RRQ TFTP packet", remoteAddress, remotePort);
			return;
		}
		
		packetHandler = new PacketHandler(tftpSocket, errorHandler, remoteAddress, remotePort);
		
		// get the file name requested by the client
		String fileName = rrqPacket.getFileName();
		
		// read the whole file requested by the client
		FileManager.FileManagerResult res = fileManager.readFile(fileName);
		byte[] fileData = null;
		
		if (!res.error) {
			fileData = res.fileBytes;
		}
		else {
			// access violation error will send an error packet with error code 2 and the connection
			if (res.accessViolation) 
				errorHandler.sendAccessViolationErrorPacket(String.format("read access denied to file: %s", fileName), remoteAddress, remotePort);
			// file not found error will send an error packet with error code 1 and the connection
			else if (res.fileNotFound)
				errorHandler.sendFileNotFoundErrorPacket(String.format("file not found: %s", fileName), remoteAddress, remotePort);
				
			return;
		}
		
		// create list of DATA datagram packets that contain up to 512 bytes of file data
		dataPacketStack = TFTPPacketBuilder.getStackOfDATADatagramPackets(fileData, remoteAddress, remotePort);
		
		DATAPacket dataPacket = null;
		ACKPacket ackPacket = null;
		while (!dataPacketStack.isEmpty()) {
			// send each datagram packet in order and wait for acknowledgement packet from the client
			
			dataPacket = dataPacketStack.peek();
		
			packetHandler.sendDATAPacket(dataPacket);
		 
			ackPacket = packetHandler.receiveACKPacket(dataPacket); 
			
			if (ackPacket == null) {
				break;
			}
			
			dataPacketStack.poll();
		}
		
		System.out.println(Globals.getVerboseMessage("RRQServerThread", "connection finished"));
	}
	
	/**
	 * Closes datagram socket once the connection is finished
	 */
	private void cleanUp() {
		System.out.println(Globals.getVerboseMessage("RRQServerThread", "socket closed"));
		tftpSocket.close();
	}
}
