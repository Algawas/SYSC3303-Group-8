import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * This class represents a server.
 * It is used to accept incoming WRQ or RRQ requests 
 */
public class Server implements Runnable {
	private TFTPSocket tftpSocket;
	private ErrorHandler errorHandler;
	
	public Server() {
		tftpSocket = new TFTPSocket(0, NetworkConfig.SERVER_PORT);
		errorHandler = new ErrorHandler(tftpSocket);
	}
	
	@Override
	public void run() {
		listen();
	}
	
	private void listen() {
		while (!tftpSocket.isClosed()) {
			String[] messages = {
					"waiting for packet...",
					String.format("waiting for packet on port %d", tftpSocket.getPort())
			};
			
			UIManager.printMessage("Server", messages);
			
			TFTPPacket requestPacket = null;
			try {
				requestPacket = tftpSocket.receive();
			} catch (SocketTimeoutException e) {
				String errorMessage = "Socket timed out. Cannot receive TFTP packet";
				UIManager.printErrorMessage("Server", errorMessage);	
				continue;
			} catch (IOException e) {
				UIManager.printErrorMessage("Server", "oops... the connection broke");
				e.printStackTrace();
				System.exit(-1);
			}
			
			/*
			 * If the received TFTP packet cannot be parsed TFTP socket returns a null
			 * If the received TFTP packet is null then send an ERROR packet with error code 4
			 */
			if (requestPacket == null) {
				// continue listening for new connections
				continue;
			}
			
			TFTPPacketType packetType = requestPacket.getPacketType();
			
			if (packetType == TFTPPacketType.RRQ) {
				String[] messages1 = {
						"RRQ request recevied.",
						String.format("RRQ request recevied from client %s:%d", requestPacket.getRemoteAddress(), requestPacket.getRemotePort())
				};
				
				UIManager.printMessage("Server", messages1);
				
				// create a server thread for handling read requests
				RRQServerThread rrqServerThread = new RRQServerThread(requestPacket);
				rrqServerThread.start();
			}
			else if (packetType == TFTPPacketType.WRQ) {
				String[] messages1 = {
						"WRQ request recevied.",
						String.format("RRQ request recevied from client %s:%d", requestPacket.getRemoteAddress(), requestPacket.getRemotePort())
				};
				
				UIManager.printMessage("Server", messages1);
				
				// create a server thread for handling write requests
				WRQServerThread wrqServerThread = new WRQServerThread(requestPacket);
				wrqServerThread.start();
			}
			else {
				UIManager.printErrorMessage("Server", "invalid request packet");
				errorHandler.sendIllegalOperationErrorPacket("cannot parse TFTP packet", requestPacket.getRemoteAddress(), requestPacket.getRemotePort());
			}
		}
		
		tftpSocket.close();
	}
	
	public void shutdown() {
		String[] messages1 = {
				"shutting down...",
				"shutting down..."
		};
		
		UIManager.printMessage("Server", messages1);
				
		// wait for any connections that are to be classified
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			UIManager.printErrorMessage("Server", "cannot make current thread go to sleep.");
			e.printStackTrace();
			System.exit(-1);
		}
		
		if (!tftpSocket.isClosed()) {
			// temporary socket is created to send a decoy package to the server so that it can stop listening
			// therefore once it re-evaluates that the boolean online is false it will exit
			try {
				DatagramSocket shutdownClient = new DatagramSocket();
				shutdownClient.send(new DatagramPacket(new byte[0], 0, InetAddress.getLocalHost(), NetworkConfig.SERVER_PORT));
				shutdownClient.close();
			} catch (UnknownHostException e) {
				UIManager.printErrorMessage("Server", "cannot find localhost address.");
				e.printStackTrace();
				System.exit(-1);
			} catch (IOException e) {
				UIManager.printErrorMessage("Server", "cannot send packet to server.");
				e.printStackTrace();
				System.exit(-1);
			}
		}

		String[] messages2 = {
				"goodbye",
				String.format("server is shutdown. Port %d released.", tftpSocket.getPort())
		};
		
		UIManager.printMessage("Server", messages2);
	}
	
	public static void main(String[] args) {
		UIManager.promptForUIMode();
		
		UIManager.showServerTitle();
		
		Thread serverThread = null;
		
		String[] options1 = {
    			"Start server",
    			"Close server",
    	};
		
		int selection = UIManager.promptForOperationSelection(options1);
		
		Server server = null;
		if (selection == 1) {
			// create server a thread for it listen on
			server = new Server();
			serverThread = new Thread(server);
			serverThread.start();
		}
		else {
			return;
		}
		
		String[] options2 = {
    			"Close server",
    	};
		
		selection = UIManager.promptForOperationSelection(options2);
		
		if (selection == 1) {
			server.shutdown();
			try {
				serverThread.join(1000);
			} catch (InterruptedException e) {
				UIManager.printErrorMessage("Server", "cannot close server thread");
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		UIManager.close();
	}
}
