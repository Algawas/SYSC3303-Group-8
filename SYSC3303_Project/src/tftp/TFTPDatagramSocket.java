package tftp;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public final class TFTPDatagramSocket implements TFTPSocket {
	
	private DatagramSocket backing;
	
	public TFTPDatagramSocket(int port) throws TFTPSocketException {
		
		try {
			backing = new DatagramSocket(port);
		} catch (SocketException se) {
			throw new TFTPSocketException("Unable to bind socket", se);
		}
	}
	
	
	public TFTPDatagramSocket() throws TFTPSocketException {
		
		try {
			backing = new DatagramSocket();
		} catch (SocketException se) {
			throw new TFTPSocketException("Unable to bind socket", se);
		}
	}
	
	
	/**
	 * Send a TFTP packet over the network
	 * @param packet
	 * @throws TFTPSocketException
	 */
	private void send(TFTPDatagram packet) throws TFTPSocketException {
		
		try {
			backing.send(packet.getBackingDP());
		} catch (IOException se) {
			throw new TFTPSocketException("Failed to send TFTP packet", se);
		}
	}
	
	
	@Override
	public void send(TFTPDatagram msg, InetAddress host, int tid) throws TFTPSocketException {
		
		msg.getBackingDP().setAddress(host);
		msg.getBackingDP().setPort(tid);
		
		send(msg);
	}
	
		
	@Override
	public TFTPPacket receive() throws TFTPSocketException {
		
		TFTPDatagram received = new TFTPDatagram();
		try {
			backing.receive(received.getBackingDP());
			// TODO: basic format validation to go here
			return received;
		} catch (IOException ie) {
			throw new TFTPSocketException("Failed to receive TFTP packet", ie);
		}
	}


	@Override
	public void sendRRQ(String path, InetAddress host, int tid) throws TFTPException {
		
		TFTPDatagram rrq = TFTPDatagram.RRQ(path, host, tid);
		send(rrq);
		
	}


	@Override
	public void sendWRQ(String path, InetAddress host, int tid) throws TFTPException {
		
		TFTPDatagram wrq = TFTPDatagram.WRQ(path, host, tid);
		send(wrq);
		
	}


	@Override
	public void sendDATA(short blockNum, byte[] payload, InetAddress host, int tid) throws TFTPException {
		
		TFTPDatagram data = TFTPDatagram.DATA(blockNum, payload, host, tid);
		send(data);
		
	}


	@Override
	public void sendACK(short blockNum, InetAddress host, int tid) throws TFTPException {
		
		TFTPDatagram ack = TFTPDatagram.ACK(blockNum, host, tid);
		send(ack);
		
	}


	@Override
	public void sendERROR(short errCode, String message, InetAddress host, int tid) throws TFTPException {
		
		TFTPDatagram error = TFTPDatagram.ERROR(errCode, message, host, tid);
		send(error);
		
	}
	
}
