import java.net.InetAddress;

/**
 * This class represents the TFTP packet
 * 
 * @author Group 8
 *
 */
public class TFTPPacket {
	protected byte[] packetBytes;
	protected short opCode;
	
	protected int packetLength;
	
	protected InetAddress remoteAddress;
	protected int remotePort;
	
	/**
	 * Constructor used in the static buildPacket method
	 */
	protected TFTPPacket() {}
	
	/**
	 * Main constructor that parses the bytes into attributes
	 *  
	 * @param packetBytes list of bytes that form the packet
	 * @throws TFTPPacketParsingError
	 */
	public TFTPPacket(byte[] packetBytes, int offset, int packetLength, InetAddress remoteAddress, int remotePort) throws TFTPPacketParsingError {		
		byte[] data = new byte[packetLength];
		System.arraycopy(packetBytes, offset, data, 0, packetLength);
		
		this.packetBytes = data;
		this.packetLength = packetLength;
	
		this.remoteAddress = remoteAddress;
		this.remotePort = remotePort;
		
		parseOPCode();
	}
	
	protected TFTPPacket(TFTPPacket tftpPacket) {
		this.packetBytes = tftpPacket.packetBytes;
		this.packetLength = tftpPacket.packetLength;
		this.opCode = tftpPacket.opCode;
		this.remoteAddress = tftpPacket.remoteAddress;
		this.remotePort = tftpPacket.remotePort;
	}
	
	/**
	 * Reads bytes to parse the OP Code
	 * 
	 * @throws TFTPPacketParsingError
	 */
	private void parseOPCode() throws TFTPPacketParsingError {
		if (packetBytes.length < 2)
			throw new TFTPPacketParsingError("error parsing OPCode");
		
		byte[] opCodeBytes = {packetBytes[0], packetBytes[1]};
		opCode = ByteConversions.bytesToShort(opCodeBytes);
	}
	
	/**
	 * Getter function for returning packet bytes array
	 * 
	 * @return block number
	 */
	public byte[] getPacketBytes() {
		return packetBytes;
	}
	
	public int getPacketLength() {
		return packetLength;
	}
	
	/**
	 * Getter function for returning OP code
	 * 
	 * @return block number
	 */
	public short getOPCode() {
		return opCode;
	}
	
	public InetAddress getRemoteAddress() {
		return remoteAddress;
	}
	
	public int getRemotePort() {
		return remotePort;
	}
	
	/**
	 * Returns TFTP packet type depending the OP code
	 * 
	 * @return block number
	 */
	public TFTPPacketType getPacketType() {
		if (opCode == 1)
			return TFTPPacketType.RRQ;
		else if (opCode == 2)
			return TFTPPacketType.WRQ;
		else if (opCode == 3)
			return TFTPPacketType.DATA;
		else if (opCode == 4)
			return TFTPPacketType.ACK;
		else if (opCode == 4)
			return TFTPPacketType.ERROR;
		else
			return TFTPPacketType.INVALID;
	}
}
