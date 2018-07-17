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
	
	// TFTP socket to transmit the file
	private TFTPSocket socket;
	
	// Handle on the file we're reading
	private InputStream file;
	
	// Where we're sending it
	private InetAddress destination;
	private int tid;
	
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
	public TFTPFileSender(String path, InetAddress destination, int tid) 
			throws FileNotFoundException, TFTPSocketException {
			
			file = new FileInputStream(path);	// Open the file for reading
			
	}
	
	
	/**
	 * Create a file sender
	 * @param path
	 * @param destination
	 * @param tid
	 */
	public TFTPFileSender(String path, TFTPSocket sock, InetAddress destination, int tid)
			throws FileNotFoundException, TFTPSocketException {
		
		this(path, destination, tid);
		socket = new TFTPDatagramSocket();
		
	}
	
	
	private byte[] readNextData() throws IOException {
		int dataLen = file.read(buffer);
		switch (dataLen) {
		case -1:
			return new byte[0];
		default:
			return Arrays.copyOfRange(buffer, 0, dataLen);
		}
	}
	
	
	private void send() throws IOException {
		
		boolean isComplete = false; // whether the transfer is over
		short blockNum = 1; // the # of the current block
		byte[] blockData; // the data in the current block
				
		while (!isComplete) {
			
			blockData = readNextData();
			
			try {
							
				socket.sendDATA(blockNum, blockData, destination, tid); // send the block of data
				
				TFTPPacket ack = socket.receive();
				
				
			} catch (TFTPPacketException pe) {
				// packet format error, or error packet
			} catch (TFTPSocketException se) {
				// tx/rx problem
			} catch (TFTPException te) {
				// something's fucky
			}
				
		}
		
	}
	

}
