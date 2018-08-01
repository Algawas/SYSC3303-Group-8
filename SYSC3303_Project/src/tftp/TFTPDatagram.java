package tftp;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public final class TFTPDatagram implements TFTPPacket {
	
	private ByteBuffer body;
	private DatagramPacket backing;
	
	
	/**
	 * Constructs a new TFTPDatagram backed by a DatagramPacket
	 */
	public TFTPDatagram() {
		backing = new DatagramPacket(new byte[516], 516);
		body = ByteBuffer.wrap(backing.getData());
	}
	
	
	/**
	 * Constructs a new TFTPDatagram targeted for the specified host and port
	 * @param host
	 * @param port
	 */
	public TFTPDatagram(InetAddress host, int port) {
		this();
		backing.setAddress(host);
		backing.setPort(port);
	}
	
			
	/**
	 * Set the payload field for multiple packet types.
	 * also sets the backing DatagramPacket length to the end of the payload,
	 * as payload is always the last field in a TFTP packet
	 * @param offset
	 * @param pld
	 */
	private void setOffsetPayload(int offset, byte[] pld) throws TFTPPacketException {
		
		try {
			body.put(pld, offset, pld.length);		// place the payload into the packet 
			backing.setLength(body.position());		// set the packet length to the end of the payload
		} catch (BufferOverflowException be) {
			throw new TFTPPacketException("Payload too big");
		}
		
	}

	
	@Override
	public void setPayload(byte[] pld) throws TFTPPacketException {
			
		switch (getType()) {
		case RRQ:
			setOffsetPayload(2, pld);
			break;
		case WRQ:
			setOffsetPayload(2, pld);
			break;
		case DATA:
			setOffsetPayload(4, pld);
			break;
		case ERROR:
			setOffsetPayload(4, pld);
			break;
		default:
			throw new TFTPPacketException("Cannot set payload for this packet type");
		}
	}
	
	
	/**
	 * Extract the payload field for multiple packet types.
	 * may be extended later to process RRQ/WRQ payloads into file paths
	 * @param offset
	 * @return
	 */
	private byte[] getOffsetPayload(int offset) {
		
		int plen = backing.getLength() - offset; // length of the payload
		byte[] pl = new byte[plen];
		body.get(pl, offset, plen); // fill array with payload data or error message
		return pl;
	}
	
	
	@Override
	public byte[] getPayload() throws TFTPPacketException {
		
		switch(getType()) {
		case DATA:
			return getOffsetPayload(4);
		case ERROR:
			return getOffsetPayload(4);
		case ACK:
			return new byte[0];
		case RRQ:
			return getOffsetPayload(2);
		case WRQ:
			return getOffsetPayload(2);
		default:
			throw new TFTPPacketException("Payload field not defined for this packet type");
		}
	}

	
	@Override
	public void setOpcode(short op) {
		body.putShort(0, op);
	}

	
	@Override
	public short getOpcode() {
		return body.getShort(0);
	}

	
	@Override
	public void setParameter(short param) throws TFTPPacketException {
		switch(getType()) {
		case RRQ:
			throw new TFTPPacketException("Parameter not defined for RRQ packets");
		case WRQ:
			throw new TFTPPacketException("Parameter not defined for WRQ packets");
		case NONE:
			throw new TFTPPacketException("Parameter not defined for this packet type");
		default:
			body.putShort(2, param);
			break;
		}

	}

	
	@Override
	public short getParameter() throws TFTPPacketException {
		
		switch(getType()) {
		case RRQ:
			throw new TFTPPacketException("Parameter not defined for RRQ packets");
		case WRQ:
			throw new TFTPPacketException("Parameter not defined for WRQ packets");
		case NONE:
			throw new TFTPPacketException("Parameter not defined for this packet type");
		default:
			return body.getShort(2);	
		}
	}

	
	@Override
	public TFTPPacketType getType() {
		switch (getOpcode()) {
		case 01:
			return TFTPPacketType.RRQ;
		case 02:
			return TFTPPacketType.WRQ;
		case 03:
			return TFTPPacketType.DATA;
		case 04:
			return TFTPPacketType.ACK;
		case 05:
			return TFTPPacketType.ERROR;
		default:
			return TFTPPacketType.NONE;
		}
	}
	
	
	@Override
	public void setType(TFTPPacketType type) throws TFTPPacketException {
		
		switch(type) {
		case RRQ:
			setOpcode((short) 1);
			break;
		case WRQ:
			setOpcode((short) 2);
			break;
		case DATA:
			setOpcode((short) 3);
			break;
		case ACK:
			setOpcode((short) 4);
			break;
		case ERROR:
			setOpcode((short) 5);
			break;
		default:
			throw new TFTPPacketException("Cannot create an invalid TFTP packet");
		}
	}
	
	
	@Override
	public InetAddress getHost() {
		return backing.getAddress();
	}


	@Override
	public int getTID() {
		return backing.getPort();
	}
	
	/*
	 * PACKAGE METHODS 
	 */
	
	/**
	 * Returns the DatagramPacket object backing this TFTPDatagram
	 * @return
	 */
	protected DatagramPacket getBackingDP() {
		return backing;
	}
	
	private static TFTPDatagram request(TFTPPacketType type, String path, InetAddress host, int tid) throws TFTPPacketException {
		
		TFTPDatagram req = new TFTPDatagram(host, tid);
		ByteBuffer buf = ByteBuffer.allocate(514);
		
		buf.put(path.getBytes()).put((byte) 0).put("octet".getBytes()).put((byte) 0);
		
		byte[] tmp = new byte[buf.position()];
		buf.position(0);
		buf.get(tmp);
		req.setType(type);
		req.setPayload(tmp);
		
		return req;
	}
	
	
	/**
	 * Constructs a read request packet
	 * @param path
	 * @param host
	 * @param tid
	 * @return
	 * @throws TFTPPacketException
	 */
	protected static TFTPDatagram RRQ(String path, InetAddress host, int tid) throws TFTPPacketException {
		
		return request(TFTPPacketType.RRQ, path, host, tid);
	}
	
	
	/**
	 * Constructs a write request packet
	 * @param path
	 * @param host
	 * @param tid
	 * @return
	 * @throws TFTPPacketException
	 */
	protected static TFTPDatagram WRQ(String path, InetAddress host, int tid) throws TFTPPacketException {
		return request(TFTPPacketType.WRQ, path, host, tid);
	}
	
	
	/**
	 * Constructs a data block packet
	 * @param blockNum
	 * @param payload
	 * @param host
	 * @param tid
	 * @return
	 * @throws TFTPPacketException
	 */
	protected static TFTPDatagram DATA(short blockNum, byte[] payload, InetAddress host, int tid) throws TFTPPacketException {
		
		TFTPDatagram data = new TFTPDatagram(host, tid);
		
		data.setType(TFTPPacketType.DATA);
		data.setParameter(blockNum);
		data.setPayload(payload);
		
		return data;
	}
	
	
	/**
	 * Constructs an acknowledgement packet
	 * @param blockNum
	 * @param host
	 * @param tid
	 * @return
	 * @throws TFTPPacketException
	 */
	protected static TFTPDatagram ACK(short blockNum, InetAddress host, int tid) throws TFTPPacketException {
		
		TFTPDatagram ack = new TFTPDatagram(host, tid);
		
		ack.setType(TFTPPacketType.ACK);
		ack.setParameter(blockNum);
		
		return ack;
	}
	
	/**
	 * Constructs an error packet
	 * @param errCode
	 * @param message
	 * @param host
	 * @param tid
	 * @return
	 * @throws TFTPPacketException
	 */
	protected static TFTPDatagram ERROR(short errCode, String message, InetAddress host, int tid) throws TFTPPacketException {
		
		TFTPDatagram error = new TFTPDatagram(host, tid);
		
		error.setType(TFTPPacketType.ERROR);
		error.setParameter(errCode);
		error.setPayload(message.getBytes());
		
		return error;
	}


	
	
}
