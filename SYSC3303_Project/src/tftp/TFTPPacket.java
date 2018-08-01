package tftp;

import java.net.InetAddress;

public interface TFTPPacket {
		
	/**
	 * Set the payload of the TFTP Packet 
	 * The payload is the last <=512 bytes for DATA,ERROR
	 * the last <=514 for RRQ/WRQ,
	 * nothing for ACK
	 * 
	 * @param pld
	 * @throws TFTPPacketException
	 */
	public void setPayload(byte[] pld) throws TFTPPacketException;
	
	/**
	 * Extracts the payload (last 512 bytes) of the TFTP Packet
	 * @return the payload
	 */
	public byte[] getPayload() throws TFTPPacketException; 	
	
	/**
	 * Set the opcode (first 2 bytes) of the TFTP Packet
	 * @param op TFTP opcode
	 */
	public void setOpcode(short op);
	
	
	/**
	 * @return the TFTP opcode of the packet
	 */
	public short getOpcode();
	
	
	/**
	 * Set the parameter (second 2 bytes) of the TFTP packet
	 * Not valid for RRQ or WRQ packets
	 * @param param
	 */
	public void setParameter(short param) throws TFTPPacketException;
	
	
	/**
	 * Not valid for RRQ/WRQ packets
	 * @return the parameter (second 2 bytes) of the TFTP packet
	 */
	public short getParameter() throws TFTPPacketException;
	
	
	/**
	 * 
	 * @return the TFTPPacketType value representing this packet
	 */
	public TFTPPacketType getType();
	
	
	/**
	 * Set the type of the TFTP packet
	 * @param type
	 */
	public void setType(TFTPPacketType type) throws TFTPPacketException;
	
	
	/**
	 * Get the host of this packet
	 * @return
	 */
	public InetAddress getHost();
	
	/**
	 * Get the TID of this packet
	 * @return
	 */
	public int getTID();
	
}
