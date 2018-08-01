import java.net.InetAddress;
import java.util.Arrays;

/**
 * This class represents the ERROR TFTP packet
 * 
 * @author Group 8
 *
 */
public class ERRORPacket extends TFTPPacket {
	public static final short OP_CODE = 5;
	
	public static final short FILE_NOT_FOUND = 1;
	public static final short ACCESS_VIOLATION = 2;
	public static final short DISK_FULL = 3;
	public static final short ILLEGAL_TFTP_OPERATION = 4;
	public static final short UNKNOWN_TID = 5;
	public static final short FILE_EXISTS = 6;
	
	private short errorCode; 
	private String errorMessage;
	
	/**
	 * Constructor used in the static buildPacket method
	 */
	private ERRORPacket() {
		super();
	}
	
	/**
	 * Main constructor that parses the bytes into attributes
	 *  
	 * @param packetBytes list of bytes that form the packet
	 * @throws TFTPPacketParsingError
	 */
	public ERRORPacket(TFTPPacket tftpPacket) throws TFTPPacketParsingError {
		super(tftpPacket);
		parseErrorCode();
		parseErrorMessage();
	}
	
	/**
	 * Reads bytes in the position 2 and 3 to parse the block number
	 * 
	 * @throws TFTPPacketParsingError
	 */
	private void parseErrorCode() throws TFTPPacketParsingError {
		if (super.packetBytes.length < 4)
			// not enough byte to parse error code
			throw new TFTPPacketParsingError("error parsing error code");
		
		byte[] errorCodeBytes = {packetBytes[2], packetBytes[3]};
		errorCode = ByteConversions.bytesToShort(errorCodeBytes);
	}
	
	/**
	 *  Reads packet bytes to parse the error message
	 *  
	 * @throws TFTPPacketParsingError
	 */
	private void parseErrorMessage() throws TFTPPacketParsingError {
		if (super.packetBytes.length < 5) {
			// not enough byte to parse error message
			throw new TFTPPacketParsingError("error parsing error message");
		}
		
		// count the length  of the error message
		int errorMessageLength = 0;
		for (int i = 4; i < super.packetBytes.length; i++) {
			if (packetBytes[i] == 0)
				break;
			
			errorMessageLength++;
		}
		
		// parse the sub array which contains the bytes for the error message
		byte[] errorMessageBytes = Arrays.copyOfRange(packetBytes, 4, errorMessageLength);
		errorMessage = ByteConversions.bytesToString(errorMessageBytes);
	}
	
	/**
	 * Getter function for returning error code
	 * 
	 * @return block number
	 */
	public short getErrorCode() {
		return errorCode;
	}
	
	/**
	 * Getter function for returning error message
	 * 
	 * @return block number
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
	
	@Override
	public String toString() {
		return "errorCode: " + errorCode + ", message: " + errorMessage; 
	}
	
	/**
	 * Returns an ERROR packet given the attributes
	 * 
	 * @param blockNumber ERROR packet attribute
	 * @return ERROR packet containing the array of bytes that form the packet and the
	 * 					  initialized attributes
	 */
	public static ERRORPacket buildPacket(short errorCode, String errorMessage, InetAddress remoteAddress, int remotePort) {
		ERRORPacket errorPacket = new ERRORPacket();
		
		// convert error code and error message into bytes
		byte[] errorCodeBytes = ByteConversions.shortToBytes(errorCode);
		byte[] errorMessageBytes = ByteConversions.stringToBytes(errorMessage);
		
		// create an appropriately sized array to contain all bytes
		byte[] packetBytes = new byte[3 + errorCodeBytes.length + errorMessageBytes.length];
		
		// error packet OP code is 5
		short opCode = 5;
		
		// convert opCode to bytes
		byte[] opCodeBytes = ByteConversions.shortToBytes(opCode);
		
		packetBytes[0] = opCodeBytes[0];
		packetBytes[1] = opCodeBytes[1];
				
		packetBytes[2] = errorCodeBytes[0];
		packetBytes[3] = errorCodeBytes[1];

		// append error message bytes to data bytes
		int c = 4;
		for (int i = 0; i < errorMessageBytes.length; i++) {
			packetBytes[c] = errorMessageBytes[i];
			c++;
		}
		
		packetBytes[c] = 0;
		
		// Initialize error packet attributes
		errorPacket.opCode = opCode;
		errorPacket.errorCode = errorCode;
		errorPacket.errorMessage = errorMessage;
		errorPacket.packetBytes = packetBytes;
		errorPacket.remoteAddress = remoteAddress;
		errorPacket.remotePort = remotePort;
		
		return errorPacket;
	}
}
