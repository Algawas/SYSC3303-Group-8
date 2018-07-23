

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import common.TFTPFileSender;
import common.TFTPTransferException;
import tftp.TFTPDatagramSocket;
import tftp.TFTPException;
import tftp.TFTPSocket;
import tftp.TFTPSocketException;

public class TestClient {

	public TestClient() {
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String[] args) {
		TFTPSocket sock = null;
		TFTPFileSender fs = null;
		try {
			sock = new TFTPDatagramSocket();
		} catch (TFTPSocketException e1) {
			System.exit(1);
		}
		
		
		try {
			sock.sendWRQ("test.txt", InetAddress.getLocalHost(), NetworkConfig.PROXY_PORT);
			tftp.TFTPPacket recvPack = sock.receive();
			if (recvPack.validate() == tftp.TFTPPacketType.ACK) {
				fs = new TFTPFileSender(sock, "test.txt", InetAddress.getLocalHost(), recvPack.getTID());
				fs.send();
			}
		} catch (TFTPTransferException te) {
			te.printStackTrace();
			System.exit(1);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (TFTPException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		System.out.println("Success");
				
		
	}

}
