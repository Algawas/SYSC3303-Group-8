import java.net.InetAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * This class contains all the methods to create a RRQ, WQR, DATA, ACK and ERROR datagram packets
 * Other datagram packets creation methods will be found here.
 * 
 * @author Group 8
 *
 */
public class TFTPPacketBuilder {	
	/**
	* Returns a datagram packet RRQ/WRQ format
	* 
	* @param packetType  packet type
	* @param fileName    name of the file to request from the server
	* @param mode        mode used to write or read file on the server
	* @param ipAddress   IP address where the datagram packet will be sent to
	* @param port        port where the datagram packet will bent to 
	*
	* @return datagram packet in DATA format
	*/
	public static RRQWRQPacket getRRQWRQDatagramPacket(TFTPPacketType packetType, String fileName, String mode, InetAddress ipAddress, int port) {
		return RRQWRQPacket.buildPacket(packetType, fileName, mode, ipAddress, port);	
	}
	
	/**
	* Returns a datagram packet in DATA format
	* 
	* @param blockNumber   the blockNumber
	* @param data          a list of bytes containing the data
	* @param returnAddress return socket address where the datagram packet will be sent to
	*
	* @return datagram packet in DATA format
	*/
	public static DATAPacket getDATADatagram(short blockNumber, byte[] data, InetAddress ipAddress, int port) {		
		return DATAPacket.buildPacket(blockNumber, data, ipAddress, port);
	}
	
	/**
	* Returns a datagram packet in ACK format
	* 
	* @param blockNumber    the blockNumber
	* @param returnAddress  return socket address where the datagram packet will be sent to
	*
	* @return datagram packet in ACK format
	*/
	
	public static ACKPacket getACKDatagram(short blockNumber, InetAddress ipAddress, int port) {
		return ACKPacket.buildPacket(blockNumber, ipAddress, port);
	}
	
	/**
	 * Returns a datagram packet in ERROR format
	 * 
	 * @param errorCode     error code
	 * @param errorMessage  additional information about the error
	 * @param returnAddress return socket address where the datagram packet will be sent to
	 * 
	 * @return datagram packet in ERROR format
	 */
	public static ERRORPacket getERRORDatagram(short errorCode, String errorMessage, InetAddress ipAddress, int port) {
		return ERRORPacket.buildPacket(errorCode, errorMessage, ipAddress, port);
	}
	
	/**
	 * Return a stack of DATA datagram packets each containing maximum of 512 bytes of the file
	 * @param fileData       bytes from the whole file
	 * @param returnAddress  address where the datagram packets will be sent to 
	 * 
	 * @return stack of datagram packets
	 */
	public static Queue<DATAPacket> getStackOfDATADatagramPackets(byte[] fileData, InetAddress ipAddress, int port) {
		Queue<DATAPacket> dataPacketStack = new LinkedList<DATAPacket>();
		
		// calculate the number of packets will be required to transfer the whole file
		int numOfPackets = (fileData.length / DATAPacket.MAX_DATA_SIZE_BYTES) + 1;
		for (int i = 0; i < numOfPackets; i++) {
			short blockNumber = (short) (i + 1);
			
			int start = i * DATAPacket.MAX_DATA_SIZE_BYTES;
			int end = (i + 1) * DATAPacket.MAX_DATA_SIZE_BYTES;
			
			if (end > fileData.length)
				end = fileData.length;
			
			byte[] fileDataPart = Arrays.copyOfRange(fileData, start, end);
			dataPacketStack.add(TFTPPacketBuilder.getDATADatagram(blockNumber, fileDataPart, ipAddress, port));
		}
		
		return dataPacketStack;
	}
}
