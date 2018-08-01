import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class TFTPSocket {
	private DatagramSocket datagramSocket;
	private ErrorHandler errorHandler;
	
	public TFTPSocket(int timeout) {
		try {
			datagramSocket = new DatagramSocket();
			if (timeout > 0) {
				datagramSocket.setSoTimeout(timeout);
			}
		} catch (SocketException e) {
			System.err.println(Globals.getErrorMessage("TFTPSocket", "cannot create datagram socket on unspecified port"));
			e.printStackTrace();
			System.exit(-1);
		}
		
		errorHandler = new ErrorHandler(this);
	}
	
	public TFTPSocket(int timeout, int port) {
		try {
			datagramSocket = new DatagramSocket(port);
			if (timeout > 0) {
				datagramSocket.setSoTimeout(timeout);
			}
		} catch (SocketException e) {
			System.err.println(Globals.getErrorMessage("TFTPSocket", String.format("cannot create datagram socket on port %d", port)));
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public void send(TFTPPacket tftpPacket) {
		byte[] tftpPacketBytes = tftpPacket.getPacketBytes();
		DatagramPacket sendDatagramPacket = new DatagramPacket(tftpPacketBytes, tftpPacketBytes.length, tftpPacket.getRemoteAddress(), tftpPacket.getRemotePort());
		
		try {
			datagramSocket.send(sendDatagramPacket);
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("TFTPSocket", "oops... the connection broke"));
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public TFTPPacket receive() throws SocketTimeoutException, IOException {
		TFTPPacket tftpPacket = null;
		
		byte[] receiveBytes = new byte[NetworkConfig.DATAGRAM_PACKET_MAX_LEN];
		DatagramPacket receiveDatagramPacket = new DatagramPacket(receiveBytes, receiveBytes.length);
		
		if (receiveDatagramPacket.getLength() == 0) {
			close();
			return null;
		}
					
		datagramSocket.receive(receiveDatagramPacket);

		// shutdown signal
		// if data packet is empty that means it should shutdown
		if (receiveDatagramPacket.getLength() == 0) {
			datagramSocket.close();
			return null;
		}
		
		try {
			tftpPacket =  new TFTPPacket(receiveDatagramPacket.getData(), receiveDatagramPacket.getOffset(), 
				receiveDatagramPacket.getLength(), receiveDatagramPacket.getAddress(), receiveDatagramPacket.getPort());
		} catch (TFTPPacketParsingError e) {
			System.err.println(Globals.getErrorMessage("TFTPSocket", "cannot parse TFTP packet"));
			errorHandler.sendIllegalOperationErrorPacket("cannot parse TFTP packet", receiveDatagramPacket.getAddress(), datagramSocket.getPort());
		}
		
		return tftpPacket;
	}
	
	public boolean isClosed() {
		return datagramSocket.isClosed();
	}
	
	public void close() {
		datagramSocket.close();
	}
}
