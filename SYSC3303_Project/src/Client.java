import java.net.*;
import java.nio.file.Paths;
import java.util.Queue;

/**
 * This class represents the client1
 * @author Group 8
 *
 */
public class Client {
	private TFTPSocket tftpSocket;
   
	private FileManager fileManager;
	private ErrorHandler errorHandler;
	private PacketHandler packetHandler;
   
	private InetAddress serverAddress;
	private int serverPort;

	/**
	 * Constructor
	 */
	public Client(InetAddress serverAddress, int serverPort)
	{
		// create TFTP socket with the specified timeout period in the network configuration
		tftpSocket = new TFTPSocket(NetworkConfig.TIMEOUT_TIME);
		
		// create error handler to different types of errors
		errorHandler =  new  ErrorHandler(tftpSocket);
	   
		// create file manager to handle writing and reading files from the hard drive
		fileManager = new FileManager();
		
		this.serverAddress = serverAddress;
		
		this.serverPort = serverPort;
	}
   
	/**
	 * Makes a read or write request
	 * 
	 * @param packetType   packet type
	 * @param fileName     name of the file that is requested to be read or written (in bytes)
	 * @param mode         mode (in bytes)
	 * @param ipAddress    server IP address
	 * @param port         server port
	 */
	private void makeReadWriteRequest(TFTPPacketType packetType, String fileName, String mode, InetAddress ipAddress, int port) {
		TFTPPacket requestPacket = null;
		
		String verboseMessage = null;
		if (packetType == TFTPPacketType.RRQ) {
			// get read request packet
			requestPacket = TFTPPacketBuilder.getRRQWRQDatagramPacket(TFTPPacketType.RRQ, fileName, mode, ipAddress, port);
			verboseMessage = String.format("sent RRQ packet: %s", requestPacket.toString());
		}
		else {
			// get write request packet
			requestPacket = TFTPPacketBuilder.getRRQWRQDatagramPacket(TFTPPacketType.WRQ, fileName, mode, ipAddress, port);
			verboseMessage = String.format("sent WRQ packet: %s", requestPacket.toString());
		}
		
		// send request
		tftpSocket.send(requestPacket);
		
        String[] messages = {
        		"",
        		verboseMessage
        }; 
        
        UIManager.printMessage("Client", messages);        
	}
	
	public void writeToFile(String fileName, DATAPacket dataPacket) {
    	// creates a file first
    	FileManager.FileManagerResult fmRes;
    	if (dataPacket.getBlockNumber() == 1) {
    		fmRes = fileManager.createFile(fileName);
    		
    		if (fmRes.error) {
    			if (fmRes.accessViolation)
    				// access violation error will send an error packet with error code 2 and the connection
    				errorHandler.sendAccessViolationErrorPacket(String.format("write access denied to file: %s", fileName), serverAddress, serverPort);
    			else if (fmRes.fileAlreadyExist)
    				// file already exists will send an error packet with error code 6 and close the connection
    				errorHandler.sendFileExistsErrorPacket(String.format("file already exists: %s", fileName), serverAddress, serverPort);
    			else if (fmRes.diskFull)
    				// disk full error will send an error packet with error code 3 and close the connection
    				errorHandler.sendDiskFullErrorPacket(String.format("Not enough disk space for file: %s", fileName), serverAddress, serverPort);
    			return;
    		}
    	}
    	
        // gets the data bytes from the DATA packet and converts it into a string
    	byte[] fileData = dataPacket.getDataBytes();
        
        // write file on client side
        fmRes = fileManager.writeFile(fileName, fileData);           
        if (fmRes.error) {
			if (fmRes.accessViolation)
				// access violation error will send an error packet with error code 2 and the connection
				errorHandler.sendAccessViolationErrorPacket(String.format("write access denied to file: %s", fileName), serverAddress, serverPort);
			else if (fmRes.fileAlreadyExist)
				// file already exists will send an error packet with error code 6 and close the connection
				errorHandler.sendFileExistsErrorPacket(String.format("file already exists: %s", fileName), serverAddress, serverPort);
			else if (fmRes.diskFull)
				// disk full error will send an error packet with error code 3 and close the connection
			    errorHandler.sendDiskFullErrorPacket(String.format("Not enough disk space for file: %s", fileName), serverAddress, serverPort);
			return;
		}
	}
   
	/**
	* Handle DATA packets received from server with file data
	* 
	* @param filePath  path of the file that the client requests
	* @param mode      mode of request
	*/
	public void readFile(String filePath, String mode) {
		// get file name from file path
		String fileName = Paths.get(filePath).getFileName().toString();
		
		// make a read request and wait for response
		makeReadWriteRequest(TFTPPacketType.RRQ, fileName, mode, serverAddress, serverPort);
		
		// create a packet handler to handle sending and receiving packets
		packetHandler = new PacketHandler(tftpSocket, errorHandler, serverAddress, serverPort);
				
		// expect to receive DATA with valid block number
	    short expectedBlockNumber = 1;
	 
	   	// receive all data packets from server that wants to transfer a file.
		// once the data length is less than 512 bytes then stop listening for
		// data packets from the server
        int fileDataLen = NetworkConfig.DATAGRAM_PACKET_MAX_LEN;
        while (fileDataLen == NetworkConfig.DATAGRAM_PACKET_MAX_LEN) {
            // receive datagram packet
        	DATAPacket dataPacket = packetHandler.receiveDATAPacket(expectedBlockNumber);
        	
        	// if the returned data packet is null, then an error occurred
        	if (dataPacket == null) {
        		return;
        	}
        	
        	writeToFile(fileName, dataPacket);
        	
	        // save the length of the received packet
	        fileDataLen = dataPacket.getPacketLength();
	        
	        // send ACK packet
	        packetHandler.sendACKPacket(expectedBlockNumber);
	        expectedBlockNumber++;
        }
        
        String[] messages = {
        		"finsihed reading file",
        		String.format("finished reading file %s from the server", filePath)
        }; 
        
        UIManager.printMessage("Client", messages);
    }
    
    /**
    * Handle sending DATA packets to server 
    * 
    * @param filePath path of the file client wants to write to
    * @param mode     mode of the request
    */
    public void writeFile(String filePath, String mode) {
        // get file name from file path
        String fileName = Paths.get(filePath).getFileName().toString();
        
        // make a write request and wait for response
        makeReadWriteRequest(TFTPPacketType.WRQ, fileName, mode, serverAddress, serverPort);
        
        // create a packet handler to handle sending and receiving packets
     	packetHandler = new PacketHandler(tftpSocket, errorHandler, serverAddress, serverPort);
     		
        ACKPacket ackPacket = packetHandler.receiveACKPacket((short) 0);
        
        // if the returned ACK packet is null, then an error occurred
        if (ackPacket == null) {
        	return;
        }
        
        if (ackPacket.getBlockNumber() == 0) {
	        // reads a file on client side to create on the server side
        	FileManager.FileManagerResult res = fileManager.readFile(filePath);
        	
    		byte[] fileData = null;
    		
    		if (!res.error) {
    			fileData = res.fileBytes;
    		}
    		else {
    			if (res.accessViolation)
    				// access violation error will send an error packet with error code 2 and the connection
    				errorHandler.sendAccessViolationErrorPacket(String.format("read access denied to file: %s", fileName), serverAddress, serverPort);
    			else if (res.fileNotFound)
    				// file not found error will send an error packet with error code 1 and the connection
    				errorHandler.sendFileNotFoundErrorPacket(String.format("file not found: %s", fileName), serverAddress, serverPort);
    			return;
    		}
	        
	        // create list of DATA datagram packets that contain up to 512 bytes of file data
	        Queue<DATAPacket> dataPacketStack = TFTPPacketBuilder.getStackOfDATADatagramPackets(fileData, ackPacket.getRemoteAddress(), ackPacket.getRemotePort());
	        
	        DATAPacket dataPacket = null;
	        while (!dataPacketStack.isEmpty()) {
				// send each datagram packet in order and wait for acknowledgement packet from the client
				dataPacket = dataPacketStack.peek();
				
				packetHandler.sendDATAPacket(dataPacket);
				
				ackPacket = packetHandler.receiveACKPacket(dataPacket);
				
				// if the returned ACK packet is null, then an error occurred
				if (ackPacket == null) {
					return;
				}
				
				// remove DATA packet from queue
				dataPacketStack.poll();
	        }
        }
        
        String[] messages = {
        		"finished writing file",
        		String.format("finished writing file %s to the server", filePath)
        }; 
        UIManager.printMessage("Client", messages);
    }
    
    /**
     * Closes the datagram socket when the connection is finished
     * */
    public void shutdown() {
        tftpSocket.close();
    }
    
    public String getIPAddress() {
    	return tftpSocket.getIPAddress().getHostAddress();
    }
    
    public int getPort() {
    	return tftpSocket.getPort();
    }

    public static void main(String args[])
    {
    	UIManager.promptForUIMode();
    	
    	InetAddress serverAddress = null;
		try {
			serverAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	int serverPort = NetworkConfig.SERVER_PORT;
    	
    	String[] options1 = {
    		"Normal Mode",
    		"Test Mode"
    	};
    	
    	int selection = UIManager.promptForOperationSelection(options1);
    	
    	if (selection == 2) {
    		serverPort = NetworkConfig.PROXY_PORT;
    	}
    	
    	String ipAddress = UIManager.promptForIPAddress();
    	if (!ipAddress.equals("")) {
    		try {
				serverAddress = InetAddress.getByName(ipAddress);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
    	Client c = new Client(serverAddress, serverPort);
    	
    	UIManager.showClientTitle();
    	
    	String[] options2 = {
    			"Write file to server",
    			"Read file to server",
    			"Close client"
    	};
    	
    	while (selection != 3) {
	    	selection = UIManager.promptForOperationSelection(options2);
	    	
	    	if (selection == 1) {
	    		String filePath = UIManager.promptForFileSelection();
	    		c.writeFile(filePath, "asciinet");
	    	}
	    	else if (selection == 2) {
	    		String filePath = UIManager.promptForFileSelection();
	    		c.readFile(filePath, "asciinet");
	    	}
    	}
    	
    	String[] messagesBeforeShutdown = {
				"shutting down client...",
				"shutting down client..."
		};
		
		UIManager.printMessage("Client", messagesBeforeShutdown);
		
		String[] messagesAfterShutdown = {
				"client closed",
				String.format("client closed. Released port %d.", c.getPort())
		};
		
		c.shutdown();
		
		UIManager.printMessage("Client", messagesAfterShutdown);
    	
    	UIManager.close();
    }
}
