package tftp;

import java.net.InetAddress;

public interface TFTPSocket {
	
	
	/**
	 * Send a TFTPDatagram to another process/machine
	 * @param msg
	 * @param host
	 * @param tid
	 * @throws TFTPException
	 */
	public void send(TFTPDatagram msg, InetAddress host, int tid) throws TFTPException;
	
	
	/**
	 * Attempt to receive a TFTP packet from the network
	 * @return
	 * @throws TFTPException
	 */
	public TFTPPacket receive() throws TFTPException;

	/**
	 * Send a read request to a server
	 * 
	 * @param path of the file to read
	 * @param host to read the file from
	 * @param tid (port #)
	 * @throws TFTPSocketException
	 */
	public void sendRRQ(String path, InetAddress host, int tid) throws TFTPException;
	
	
	/**
	 * Send a write request to a server
	 * @param path of the file to write
	 * @param host to write the file to
	 * @param tid (port #)
	 * @throws TFTPSocketException
	 */
	public void sendWRQ(String path, InetAddress host, int tid) throws TFTPException;
	
	
	/**
	 * Send a DATA packet to another process/machine
	 * @param blockNum
	 * @param payload
	 * @param host
	 * @param tid
	 * @throws TFTPSocketException
	 */
	public void sendDATA(short blockNum, byte[] payload, InetAddress host, int tid) throws TFTPException;
	
	
	/**
	 * Send an acknowledgement for a block of data
	 * @param blockNum
	 * @param host
	 * @param tid
	 * @throws TFTPSocketException
	 */
	public void sendACK(short blockNum, InetAddress host, int tid) throws TFTPException;
	
	
	/**
	 * Indicate an error to another process/machine
	 * @param errCode
	 * @param messsage
	 * @param host
	 * @param tid
	 * @throws TFTPSocketException
	 */
	public void sendERROR(short errCode, String messsage, InetAddress host, int tid) throws TFTPException;
	

}
