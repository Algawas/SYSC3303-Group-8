import java.net.InetAddress;
import java.util.Arrays;

/**
 * This class represents the RRQWRQ TFTP packet
 * 
 * @author Group 8
 *
 */
public class RRQWRQPacket extends TFTPPacket {
	
	private String fileName;
	private String mode;
	
	/**
	 * Constructor used in the static buildPacket method
	 */
	private RRQWRQPacket() {
		super();
	}

	/**
	 * Main constructor that parses the bytes into attributes
	 *  
	 * @param packetBytes list of bytes that form the packet
	 * @throws TFTPPacketParsingError
	 */
	public RRQWRQPacket(TFTPPacket tftpPackett) throws TFTPPacketParsingError {
		super(tftpPackett);
		parseFileName();
		parseMode();
	}
	
	/**
	 * Reads bytes to parse the requested file name 
	 * 
	 * @throws TFTPPacketParsingError
	 */
	private void parseFileName() throws TFTPPacketParsingError {
		int fileNameBytesLength =  0;
		for(int i = 2; i < super.packetBytes.length; i++) {
			if (super.packetBytes[i] == 0) {
				break;
			}
			
			fileNameBytesLength++;
		}
		
		if (fileNameBytesLength == 0) {
			throw new TFTPPacketParsingError("invalid file name");
		}
		
		byte[] fileNameBytes = Arrays.copyOfRange(super.packetBytes, 2, 2 + fileNameBytesLength);
		
		fileName =  ByteConversions.bytesToString(fileNameBytes);
	}
	
	/**
	 * Reads bytes to parse the requested mode
	 * 
	 * @throws TFTPPacketParsingError
	 */
	private void parseMode() throws TFTPPacketParsingError {
		int fileNameBytesLength =  0;
		for(int i = 2; i < super.packetBytes.length; i++) {
			if (super.packetBytes[i] == 0)
				break;
			
			fileNameBytesLength++;
		}
		
		int modeBytesLength = 0;
		for(int i = 3 + fileNameBytesLength; i < super.packetBytes.length; i++) {
			if (super.packetBytes[i] == 0) {
				break;
			}
			
			modeBytesLength++;
		}
		
		if (modeBytesLength == 0) {
			throw new TFTPPacketParsingError("invalid mode");
		}
		
		
		byte[] modeBytes = Arrays.copyOfRange(super.packetBytes, 3 + fileNameBytesLength, 3 + fileNameBytesLength + modeBytesLength);
		
		mode =  ByteConversions.bytesToString(modeBytes);
	}
	
	/**
	 * Getter function for returning file name
	 * 
	 * @return block number
	 */
	public String getFileName() {
		return fileName;
	}
	
	/**
	 * Getter function for returning mode
	 * 
	 * @return block number
	 */
	public String getMode() {
		return mode;
	}
	
	/**
	 * Returns an RRQ/WRQ packet given the attributes
	 * 
	 * @param type     packet type - RRQ or WWRQ
	 * @param fileName name of the file that is being requested
	 * @param mode     mode that is being requested
	 * 
	 * @return RRQ/WRQ packet containing the array of bytes that form the packet and the
	 * 		   initialized attributes
	 */
	public static RRQWRQPacket buildPacket(TFTPPacketType type, String fileName, String mode, InetAddress remoteAddress, int remotePort) {
		RRQWRQPacket requestPacket = new RRQWRQPacket();
		
		byte[] fileNameBytes = ByteConversions.stringToBytes(fileName);
		byte[] modeBytes = ByteConversions.stringToBytes(mode);
		
		byte[] packetBytes = new byte[4 + fileNameBytes.length + modeBytes.length];
		
		// assign opCode depending on package type
		// opCode is 1 if RRQ
		// opCode is 2 if WRQ
		short opCode = 0;
		if (type == TFTPPacketType.RRQ)
			opCode = 1;
		else if (type == TFTPPacketType.WRQ)
			opCode = 2;
		else
			opCode = 0;
		
		// convert opCode to bytes
		byte[] opCodeBytes = ByteConversions.shortToBytes(opCode);
		
		packetBytes[0] = opCodeBytes[0];
		packetBytes[1] = opCodeBytes[1];
		
		// append fileName bytes to data bytes
		int c = 2;
		for (int i = 0; i < fileNameBytes.length; i++) {
			packetBytes[c] = fileNameBytes[i];
			c++;
		}
		
		packetBytes[c] = 0;
		c++;
		
		// append mode bytes to data bytes
		for (int i = 0; i < modeBytes.length; i++) {
			packetBytes[c] = modeBytes[i];
			c++;
		}
		
		packetBytes[c] = 0;
		c++;
		
		requestPacket.opCode = opCode;
		requestPacket.fileName = fileName;
		requestPacket.mode = mode;
		requestPacket.packetBytes = packetBytes;
		requestPacket.remoteAddress = remoteAddress;
		requestPacket.remotePort = remotePort;
		
		return requestPacket;
	}
}
