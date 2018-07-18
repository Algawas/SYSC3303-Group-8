package tftp;

import java.nio.ByteBuffer;

public abstract class TFTPPacketValidator {

	public TFTPPacketValidator() {
		// TODO Auto-generated constructor stub
	}
	
	
	private static void request(TFTPPacket rq, TFTPPacketType type) throws TFTPPacketException {
		if (rq.getType() != type) {
			throw new TFTPPacketException("This is not a request packet");
		}
		
		byte[] payload = rq.getPayload();
		
		String payloadString = new String(payload);
		
		String[] requestElements = payloadString.split("\0");
		
		if (requestElements.length != 2 || requestElements[1] != "octet") {
			throw new TFTPPacketException("Corrupted request payload");
		}
		
		return;
		
	}
	
	
	/**
	 * Validates the format of a read request packet
	 * @param rrq
	 * @throws TFTPPacketException
	 */
	protected static void RRQ(TFTPPacket rrq) throws TFTPPacketException {
		request(rrq, TFTPPacketType.RRQ);
	}
	
	
	/**
	 * Validates the format of a write request packet
	 * @param wrq
	 * @throws TFTPPacketException
	 */
	protected static void WRQ(TFTPPacket wrq) throws TFTPPacketException {
		request(wrq, TFTPPacketType.WRQ);
	}
	
	/**
	 * Validates that this TFTP packet is a valid ACK
	 * @param ack
	 * @return
	 * @throws TFTPPacketException
	 */
	protected static void ACK(TFTPPacket ack) throws TFTPPacketException {
		
		// check that the opcode is correct
		if (ack.getType() != TFTPPacketType.ACK) {
			throw new TFTPPacketException("This is not an ACK packet");
		}
		
		// check that the packet is of the correct length
		if (ack.getLength() != 4) {
			throw new TFTPPacketException("This is not a valid ACK packet");
		}
		
		// check the block #
		if (ack.getParameter() < 0) {
			throw new TFTPPacketException("Invalid negative block number");
		}
		
		return;
	}
	
	
	/**
	 * Validates the format of a DATA packet
	 * @param data
	 * @return
	 * @throws TFTPPacketException
	 */
	protected static void DATA(TFTPPacket data) throws TFTPPacketException {
		
		// check the opcode
		if (data.getType() != TFTPPacketType.DATA) {
			throw new TFTPPacketException("This is not a DATA packet");
		}
		
		// check the block #
		if (data.getParameter() < 0) {
			throw new TFTPPacketException("Invalid negative block numbers");
		}
		
		return;
	}
	
	
	
	protected static void ERROR(TFTPPacket error) throws TFTPPacketException {
		
		// check the opcode
		if (error.getType() != TFTPPacketType.ERROR) {
			throw new TFTPPacketException("This is not an ERROR packet");
		}
		
		//check the error code
		short ec = error.getParameter();
		if (ec < 0 || ec > 7) {
			throw new TFTPPacketException("Error code out of range");
		}
		
		return;
	}
	
	
	/**
	 * 
	 * @param packet
	 * @return
	 * @throws TFTPPacketException
	 */
	protected static TFTPPacketType validate(TFTPPacket packet) throws TFTPPacketException {
		
		TFTPPacketType type = packet.getType();
		switch (type) {
		case RRQ:
			RRQ(packet);
			return TFTPPacketType.RRQ;
		case WRQ:
			WRQ(packet);
			return TFTPPacketType.WRQ;
		case ACK:
			ACK(packet);
			return TFTPPacketType.ACK;
		case DATA:
			DATA(packet);
			return TFTPPacketType.DATA;
		case ERROR:
			ERROR(packet);
			return TFTPPacketType.ERROR;
		default:
			throw new TFTPPacketException("This is not a valid TFTP packet");
		}
	}
	

}
