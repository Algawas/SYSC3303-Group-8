import java.net.InetAddress;

/**
 * This class represents the ACK TFTP packet
 * 
 * @author Group 8
 *
 */
public class ACKPacket extends TFTPPacket {
	private short blockNumber; 
	
	/**
	 * Constructor used in the static buildPacket method
	 */
	private ACKPacket() {
		super();
	}
	
	/**
	 * Main constructor that parses the bytes into attributes
	 *  
	 * @param packetBytes list of bytes that form the packet
	 * @throws TFTPPacketParsingError
	 */
	public ACKPacket(TFTPPacket tftpPacket) throws TFTPPacketParsingError {
		super(tftpPacket);
		
		/* parses list of bytes and initializes packet attributes
		 * 		- block number 
		 */
		parseBlockNumber();
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
	 * Getter function for returning block number
	 * 
	 * @return block number
	 */
	public short getBlockNumber() {
		return blockNumber;
	}
	
	/**
	 * Returns an ACK packet given the attributes
	 * 
	 * @param blockNumber ACK packet attribute
	 * @return ACK packet containing the array of bytes that form the packet and the
	 * 					  initialized attributes
	 */
	public static ACKPacket buildPacket(short blockNumber, InetAddress remoteAddress, int remotePort) {
		ACKPacket ackPacket = new ACKPacket();
		
		// create a properly sized bytes array
		byte[] packetBytes = new byte[4];
		
		// convert opCode to bytes
		short opCode = 4;
		byte[] opCodeBytes = ByteConversions.shortToBytes(opCode);
		
		packetBytes[0] = opCodeBytes[0];
		packetBytes[1] = opCodeBytes[1];
		
		// convert block number to bytes
		byte[] blockNumberBytes = ByteConversions.shortToBytes(blockNumber);
				
		packetBytes[2] = blockNumberBytes[0];
		packetBytes[3] = blockNumberBytes[1];

		// initialize packet attributes
		ackPacket.opCode = opCode;
		ackPacket.blockNumber = blockNumber;
		ackPacket.packetBytes = packetBytes;
		ackPacket.remoteAddress = remoteAddress;
		ackPacket.remotePort = remotePort;
		
		return ackPacket;
	}
}
