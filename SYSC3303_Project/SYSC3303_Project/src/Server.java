import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * This class represents a server.
 * It is used to accept incoming WRQ or RRQ requests 
 */
public class Server implements Runnable {
	private TFTPSocket tftpSocket;
	private ErrorHandler errorHandler;
	
	public Server() {
		tftpSocket = new TFTPSocket(NetworkConfig.SERVER_PORT);
		errorHandler = new ErrorHandler(tftpSocket);
	}
	
	@Override
	public void run() {
		listen();
	}
	
	private void listen() {
		while (!tftpSocket.isClosed()) {
			System.out.println(Globals.getVerboseMessage("Server", "waiting for packet..."));
			
			TFTPPacket requestPacket = tftpSocket.receive();
			
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
				System.out.println(Globals.getVerboseMessage("Server", "RRQ request recevied."));
				
				// create a server thread for handling read requests
				RRQServerThread rrqServerThread = new RRQServerThread(requestPacket);
				rrqServerThread.start();
			}
			else if (packetType == TFTPPacketType.WRQ) {
				System.out.println(Globals.getVerboseMessage("Server", "WRQ request received."));
				
				// create a server thread for handling write requests
				WRQServerThread wrqServerThread = new WRQServerThread(requestPacket);
				wrqServerThread.start();
			}
			else {
				System.err.println(Globals.getErrorMessage("Server", "invalid request packet"));
				errorHandler.sendIllegalOperationErrorPacket("cannot parse TFTP packet", requestPacket.getRemoteAddress(), requestPacket.getRemotePort());
			}
		}
		
		tftpSocket.close();
	}
	
	public void shutdown() {
		System.out.println(Globals.getVerboseMessage("Server", "shutting down..."));
				
		// wait for any connections that are to be classified
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			System.err.println(Globals.getErrorMessage("Server", "cannot make current thread go to sleep."));
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
				System.err.println(Globals.getErrorMessage("Server", "cannot find localhost address."));
				e.printStackTrace();
				System.exit(-1);
			} catch (IOException e) {
				System.err.println(Globals.getErrorMessage("Server", "cannot send packet to server."));
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		System.out.println(Globals.getVerboseMessage("Server", "goodbye!"));
	}
	
	public static void main(String[] args) {
		Server server = null;
		Thread serverThread = null;
		
		System.out.println("\nSYSC 3033 TFTP Server");
		System.out.println("1. Start");
		System.out.println("2. Exit");
		System.out.println("Selection: ");
		
		int selection = 0;
		Scanner sc = new Scanner(System.in);
		selection = sc.nextInt();
		
		if (selection == 1) {
			// create server a thread for it listen on
			server = new Server();
			serverThread = new Thread(server);
			serverThread.start();
		}
		else {
			sc.close();
			System.exit(0);
		}
		
		// shutdown option
		String shutdownCommand = "";
		while (!shutdownCommand.equals("quit")) {
			System.out.println("\nSYSC 3033 TFTP Server");
			System.out.println("Type quit to shutdown");
			System.out.println("Selection: ");
			
			shutdownCommand = sc.nextLine();
		}
		
		if (shutdownCommand.equals("quit")) {
			server.shutdown();
			sc.close();
			try {
				serverThread.join(1000);
			} catch (InterruptedException e) {
				System.err.println(Globals.getErrorMessage("Server", "cannot close server thread"));
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
}
