package common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Arrays;

import tftp.TFTPDatagramSocket;
import tftp.TFTPException;
import tftp.TFTPPacket;
import tftp.TFTPPacketException;
import tftp.TFTPPacketType;
import tftp.TFTPSocket;
import tftp.TFTPSocketException;


/**
 * Sends a file over TFTP
 * Assumes the preamble is over and both processes know
 * about the transfer
 * @author Group 8
 *
 */
public class TFTPFileSender {
	
	// CONSTANTS
	private final int MAX_RETRIES = 5;
	
	// TFTP socket to transmit the file
	private TFTPSocket socket;
	
	// Handle on the file we're reading
	private String filePath;
	private InputStream file;
	
	// Where we're sending it
	private InetAddress destination;
	private int tid;
	private int localTid;
	
	// temporary holding place for file data
	private byte[] buffer;
	
	
	/**
	 * Initialize the FileSender
	 * @param path
	 * @param destination
	 * @param tid
	 * @throws FileNotFoundException
	 * @throws TFTPSocketException
	 */
	public TFTPFileSender(String path, InetAddress dest, int tid) {
			
			this.filePath = path;
			this.destination = dest;
			this.tid = tid;
			this.localTid = -1;
			
	}
	
	
	/**
	 * Create a file sender
	 * @param path
	 * @param destination
	 * @param tid
	 */
	public TFTPFileSender(int localTid, String path, InetAddress destination, int tid) {
		
		this(path, destination, tid);
		this.localTid = localTid;
		
	}
	
	
	/**
	 * Reads the next data block from the file
	 * @return
	 * @throws IOException
	 */
	private byte[] readNextData() throws IOException {
		int dataLen = file.read(buffer);
		switch (dataLen) {
		case -1:
			return new byte[0];
		default:
			return Arrays.copyOfRange(buffer, 0, dataLen);
		}
	}
	
	
	/**
	 * Opens the file and socket for the transfer
	 * @throws FileNotFoundException
	 * @throws TFTPSocketException
	 */
	private void openResources() throws FileNotFoundException, TFTPSocketException {
		
		file = new FileInputStream(filePath);
		
		socket = new TFTPDatagramSocket(localTid);
		
	}
	
	
	/**
	 * Closes resources... duh
	 * @throws IOException
	 */
	private void closeResources() throws IOException {
		file.close();
		socket.close();
		
	}
	
	
	/**
	 * transfer the file data
	 * @throws TFTPTransferException
	 * @throws IOException
	 */
	private void sendData() throws TFTPTransferException, IOException {
		boolean isComplete = false; // whether the transfer is over

		TFTPPacket incoming; // latest received packet
		
		//load initial data Block
		byte[] blockData = readNextData();
		short blockNum = 1;
		
		// how many times we retry failed transactions
		int retries = 5;
		
		
		
		while (true) {
						
			try {
							
				socket.sendDATA(blockNum, blockData, destination, tid); // send the block of data
				
				incoming = socket.receive();
				TFTPPacketType inType = incoming.validate();
				
				if (inType == TFTPPacketType.ACK && incoming.getParameter() == blockNum) {
					
					if (blockData.length < 512) {
						isComplete = true;
						break;
					}
					
					// Sent data has been acknowledged
					// increment block #, and load next block
					retries = MAX_RETRIES;
					blockNum++;
					blockData = readNextData();
					
				} else if (inType == TFTPPacketType.ERROR ) {
					
					String message = new String(incoming.getPayload());
					throw new TFTPTransferException("Received ERROR: "+message);
					
				} else {
					
					socket.sendERROR((short)4, "Expected ACK packet", destination, tid);
					throw new TFTPTransferException("Got illegal packet (error 4)");
					
				}
				 	 	
				
			} catch (TFTPPacketException pe) {	// incorrect packet format
				
				continue; // we ignore malformed packets for now
				
			} catch (TFTPSocketException se) {	// timeout or other socket issue
				
				if (retries > 0) {
					retries--;
					continue;
				} else {
					throw new TFTPTransferException("Lost the other host");
				}
				
			} catch (TFTPException te) {		// unrecoverable errors
				
				throw new TFTPTransferException("Unknown error during transmission", te);
				
			}
				
		}
	}
	
	
	/**
	 * Sends a file over TFTP
	 * @throws IOException
	 * @throws TFTPTransferException
	 * @throws TFTPSocketException 
	 */
	public void send() throws IOException, TFTPTransferException, TFTPSocketException {
		
		// ensure that our resources are open for reading and writing
		try {
			openResources();
			sendData();
		} finally {
			closeResources();
		}
		
		
		
	}
	

}
