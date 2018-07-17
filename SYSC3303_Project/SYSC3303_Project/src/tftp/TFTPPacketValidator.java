package tftp;

public abstract class TFTPPacketValidator {

	public TFTPPacketValidator() {
		// TODO Auto-generated constructor stub
	}
	
	
	/**
	 * Validates that this TFTP packet is a valid ACK
	 * @param ack
	 * @return
	 * @throws TFTPPacketException
	 */
	protected static TFTPPacket ACK(TFTPPacket ack) throws TFTPPacketException {
		
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
		
		return ack;
	}
	
	
	/**
	 * Validates the format of a DATA packet
	 * @param data
	 * @return
	 * @throws TFTPPacketException
	 */
	protected static TFTPPacket DATA(TFTPPacket data) throws TFTPPacketException {
		
		// check the opcode
		if (data.getType() != TFTPPacketType.DATA) {
			throw new TFTPPacketException("This is not a DATA packet");
		}
		
		// check the block #
		if (data.getParameter() < 0) {
			throw new TFTPPacketException("Invalid negative block numbers");
		}
		
		return data;
	}
	
	
	
	protected static TFTPPacket ERROR(TFTPPacket error) throws TFTPPacketException {
		
		// check the opcode
		if (error.getType() != TFTPPacketType.ERROR) {
			throw new TFTPPacketException("This is not an ERROR packet");
		}
		
		//check the error code
		short ec = error.getParameter();
		if (ec < 0 || ec > 7) {
			throw new TFTPPacketException("Error code out of range");
		}
		
		return error;
	}
	

}
