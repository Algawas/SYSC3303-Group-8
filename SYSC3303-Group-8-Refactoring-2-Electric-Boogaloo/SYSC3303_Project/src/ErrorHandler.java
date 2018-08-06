import java.net.InetAddress;

/**
 * This class handles error packet sending
 * 
 * @author Group 8
 *
 */
public class ErrorHandler {
	TFTPSocket tftpSocket;
	
	/**
	 * Constructor
	 * @param datagramSocket tftpSocket
	 */
	public ErrorHandler(TFTPSocket datagramSocket) {
		this.tftpSocket = datagramSocket;
	}
	
	/**
	 * Sends ERROR packet and prints error message
	 * @param errorCode
	 * @param errorMessage
	 * @param remoteAddress
	 * @param remotePort
	 */
    private void sendErrorPacket(short errorCode, String errorMessage, InetAddress remoteAddress, int remotePort) {
        ERRORPacket errorPacket = TFTPPacketBuilder.getERRORDatagram(errorCode, errorMessage, remoteAddress, remotePort);
       
        System.err.println(Globals.getErrorMessage("ErrorHandler", String.format("sending error packet, errorCode: %d, errorMessage: %s", errorCode, errorMessage)));
        tftpSocket.send(errorPacket);
    }
    
    /**
     * Sends ERROR packet with error code 1
     * @param errorMessage
     * @param remoteAddress
     * @param remotePort
     */
	public void sendFileNotFoundErrorPacket(String errorMessage, InetAddress remoteAddress, int remotePort) {
		sendErrorPacket(ERRORPacket.FILE_NOT_FOUND, errorMessage, remoteAddress, remotePort);
	}
	
	/**
     * Sends ERROR packet with error code 2
     * @param errorMessage
     * @param remoteAddress
     * @param remotePort
     */
	public void sendAccessViolationErrorPacket(String errorMessage, InetAddress remoteAddress, int remotePort) {
		sendErrorPacket(ERRORPacket.ACCESS_VIOLATION, errorMessage, remoteAddress, remotePort);
	}
	
	/**
     * Sends ERROR packet with error code 3
     * @param errorMessage
     * @param remoteAddress
     * @param remotePort
     */
	public void sendDiskFullErrorPacket(String errorMessage, InetAddress remoteAddress, int remotePort) {
		sendErrorPacket(ERRORPacket.DISK_FULL, errorMessage, remoteAddress, remotePort);
	}
	
	/**
     * Sends ERROR packet with error code 4
     * @param errorMessage
     * @param remoteAddress
     * @param remotePort
     */
	public void sendIllegalOperationErrorPacket(String errorMessage, InetAddress remoteAddress, int remotePort) {
		sendErrorPacket(ERRORPacket.ILLEGAL_TFTP_OPERATION, errorMessage, remoteAddress, remotePort);
	}
	
	/**
     * Sends ERROR packet with error code 5
     * @param errorMessage
     * @param remoteAddress
     * @param remotePort
     */
	public void sendUnknownTrasnferIDErrorPacket(String errorMessage, InetAddress remoteAddress, int remotePort) {
		sendErrorPacket(ERRORPacket.UNKNOWN_TID, errorMessage, remoteAddress, remotePort);
	}
	
	/**
     * Sends ERROR packet with error code 6
     * @param errorMessage
     * @param remoteAddress
     * @param remotePort
     */
	public void sendFileExistsErrorPacket(String errorMessage, InetAddress remoteAddress, int remotePort) {
		sendErrorPacket(ERRORPacket.FILE_EXISTS, errorMessage, remoteAddress, remotePort);
	}
}
