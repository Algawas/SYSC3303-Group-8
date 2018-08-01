import java.net.InetAddress;
import java.util.Arrays;

/**
 * This class represents the DATA TFTP packet
 * 
 * @author Group 8
 *
 */
public class DATAPacket extends TFTPPacket {
	public static final int MAX_DATA_SIZE_BYTES = 512;
	private short blockNumber;
	private byte[] dataBytes;
	
	/**
	 * Constructor used in the static buildPacket method
	 */
	private DATAPacket() {
		super();
	}
	
	/**
	 * Main constructor that parses the bytes into attributes
	 *  
	 * @param packetBytes list of bytes that form the packet
	 * @throws TFTPPacketParsingError
	 */
	public DATAPacket(TFTPPacket tftpPacket) throws TFTPPacketParsingError {
		super(tftpPacket);
		/* parses list of bytes and initializes packet attributes
		 * 		- block number
		 * 		- data bytes 
		 */
		parseBlockNumber();
		parseDataBytes();
	}
	
	/**
	 * Reads bytes in the position 2 and 3 to parse the block number
	 * 
	 * @throws TFTPPacketParsingError
	 */
	private void parseBlockNumber() throws TFTPPacketParsingError {
		if (super.packetBytes.length < 4)
			// not enough bytes to parse block number 
			throw new TFTPPacketParsingError("error parsing block number");
		
		byte[] blockNumberBytes = {packetBytes[2], packetBytes[3]};
		blockNumber = ByteConversions.bytesToShort(blockNumberBytes);
	}
	
	/**
	 *  Reads packet bytes to parse the data
	 *  
	 * @throws TFTPPacketParsingError
	 */
	private void parseDataBytes() throws TFTPPacketParsingError {
		if (super.packetBytes.length < 4)
			// not enough bytes to parse data
			throw new TFTPPacketParsingError("error parsing data");
		
		dataBytes = Arrays.copyOfRange(packetBytes, 4, packetBytes.length);
	}
	
	/**
	 * Getter function for returning block number
	 * 
	 * @return block number
	 */
	public short getBlockNumber() {
		return blockNumber;
	}
	
	/**
	 * Getter function for returning data bytes
	 * 
	 * @return block number
	 */
	public byte[] getDataBytes() {
		return dataBytes;
	}
	
	/**
	 * Returns an DATA packet given the attributes
	 * 
	 * @param blockNumber DATA packet attribute
	 * @param dataBytes   array of bytes representing the data
	 * 
	 * @return DATA packet containing the array of bytes that form the packet and the
	 * 					  initialized attributes
	 */
	public static DATAPacket buildPacket(short blockNumber, byte[] dataBytes, InetAddress remoteAddress, int remotePort) {
		DATAPacket dataPacket = new DATAPacket();
		
		// create a properly sized bytes array
		byte[] packetBytes = new byte[4 + dataBytes.length];
		
		// convert opCode to bytes
		short opCode = 3;
		byte[] opCodeBytes = ByteConversions.shortToBytes(opCode);
				
		packetBytes[0] = opCodeBytes[0];
		packetBytes[1] = opCodeBytes[1];
		
		// convert block number to bytes
		byte[] blockNumberBytes = ByteConversions.shortToBytes(blockNumber);
				
		packetBytes[2] = blockNumberBytes[0];
		packetBytes[3] = blockNumberBytes[1];
		
		// append the bytes in the data list to the packet's list of bytes
		int c = 4;
		for (int i = 0; i < dataBytes.length; i++) {
			packetBytes[c] = dataBytes[i];
			c++;
		}
		
		// initialize packet attributes
		dataPacket.opCode = opCode;
		dataPacket.blockNumber = blockNumber;
		dataPacket.dataBytes = dataBytes;
		dataPacket.packetBytes = packetBytes;
		dataPacket.remoteAddress = remoteAddress;
		dataPacket.remotePort = remotePort;
		
		return dataPacket;
	}
}
