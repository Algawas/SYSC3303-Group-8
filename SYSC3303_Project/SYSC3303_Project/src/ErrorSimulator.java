import java.io.IOException;
import java.net.*;
import java.util.Random;
import java.util.Scanner;

public class ErrorSimulator implements Runnable {
	/**
	 * This class represents the error simulator
	 */
	
	private TFTPSocket tftpSocket;
	
	// the port of the server that the client is communicating with
	private InetAddress serverThreadAddress;
	private int serverThreadPort;
	
	// the port of the client that the server is communicating with
	private InetAddress clientAddress;
	private int clientPort;
	
	private boolean sendingToServer;
	
	// error code to simulate
	private int errorSelection;
	// type of error to simulate
	private TFTPPacketType errorOp;
	// packet's block number to modify
	private short errorBlock;
	
	
	public ErrorSimulator() {
		try {
			serverThreadAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			System.err.println(Globals.getErrorMessage("Error Simulator", "cannot get localhost address"));
			e.printStackTrace();
			System.exit(-1);
		}
		serverThreadPort = NetworkConfig.SERVER_PORT;
		
		// create a datagram socket to establish a connection with incoming
		tftpSocket = new TFTPSocket(NetworkConfig.PROXY_PORT);
		
		// send packet to server from client
		sendingToServer = true;
	}
	
	public void run() {
		listen();
	}
	
	private TFTPPacket establishNewConnection(TFTPPacket tftpPacket, InetAddress clientAddress, int clientPort) {
		System.out.println(Globals.getVerboseMessage("Error Simulator", "received packet from client."));
		
		// save client address and port
		this.clientAddress = clientAddress;
		this.clientPort = clientPort;
		
		sendingToServer = true;
		
		// save server address and port 
		try {
			serverThreadAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			System.err.println(Globals.getErrorMessage("Error Simulator", "cannot get localhost address"));
			e.printStackTrace();
			System.exit(-1);
		}
		serverThreadPort = NetworkConfig.SERVER_PORT;
		
		System.out.println(Globals.getVerboseMessage("Error Simulator", "sending packet to server..."));
		
		TFTPPacket sendTFTPPacket;
		try {
			sendTFTPPacket = new TFTPPacket(tftpPacket.getPacketBytes(), 0, tftpPacket.getPacketBytes().length, this.serverThreadAddress, this.serverThreadPort);
			tftpSocket.send(sendTFTPPacket);
		} catch (TFTPPacketParsingError e) {
			System.err.println(Globals.getErrorMessage("Error Simulator", "cannot create TFTP packet"));
			e.printStackTrace();
			System.exit(-1);
		}
		
		sendingToServer = false;
		
		System.out.println(Globals.getVerboseMessage("Error Simulator", "waiting for packet from server..."));
		
		TFTPPacket receiveTFTPPacket = tftpSocket.receive();
		serverThreadAddress = receiveTFTPPacket.getRemoteAddress();
		serverThreadPort = receiveTFTPPacket.getRemotePort();
		return receiveTFTPPacket;
	}
	
	private void listen() {
		while (!tftpSocket.isClosed()) {
			// Receive packet from Client
			TFTPPacket receiveTFTPacket = null;
			TFTPPacket sendTFTPPacket = null;			
					
			receiveTFTPacket = tftpSocket.receive();
			
			if (receiveTFTPacket == null) {
				continue;
			}

			// checks if the incoming packet is a request packet
			// if so then reset the server port back to the main port
			if (receiveTFTPacket.getPacketType() == TFTPPacketType.RRQ || 
					receiveTFTPacket.getPacketType() == TFTPPacketType.WRQ) {

				if (errorSelection == 2 && (errorOp == TFTPPacketType.RRQ || errorOp == TFTPPacketType.WRQ)) {
					receiveTFTPacket = simulateIllegalOperationError(receiveTFTPacket, errorSelection, errorOp, errorBlock);
				}

				receiveTFTPacket = establishNewConnection(receiveTFTPacket, receiveTFTPacket.getRemoteAddress(), receiveTFTPacket.getRemotePort());
			}
			else {
				if ((errorSelection == 2) || (errorSelection == 3)) {
					receiveTFTPacket = simulateIllegalOperationError(receiveTFTPacket,  errorSelection, errorOp, errorBlock);
				}
			}
		
			InetAddress sendAddress;
			int sendPort;
			if (!sendingToServer) {
				System.out.println(Globals.getVerboseMessage("Error Simulator", "recieved packet from server."));
				System.out.println(Globals.getVerboseMessage("Error Simulator", "sending packet to client..."));
				sendAddress = clientAddress;
				sendPort = clientPort;
				
			}
			else {
				System.out.println(Globals.getVerboseMessage("Error Simulator", "recieved packet from client."));		
				System.out.println(Globals.getVerboseMessage("Error Simulator", "sending packet to server..."));
				sendAddress = serverThreadAddress;
				sendPort = serverThreadPort;
			}
			
			try {
				sendTFTPPacket = new TFTPPacket(receiveTFTPacket.getPacketBytes(), 0, receiveTFTPacket.getPacketBytes().length, 
						sendAddress, sendPort);
			} catch (TFTPPacketParsingError e) {
				System.err.println(Globals.getErrorMessage("Error Simulator", "cannot create TFTP Packet"));
				e.printStackTrace();
				System.exit(-1);
			}
			
			if (errorSelection == 3) {
				TFTPSocket tempTFTPSocket = new TFTPSocket();

				
				tempTFTPSocket.send(sendTFTPPacket);
				tempTFTPSocket.close();
			}
			else {
				tftpSocket.send(sendTFTPPacket);
			}
			
			sendingToServer = !sendingToServer;
		}
		
		tftpSocket.close();
	}

	//Checks the PacketType, and block, and simulates the appropriate error on it
	private TFTPPacket simulateIllegalOperationError(TFTPPacket tftpPacket, int code, TFTPPacketType op, short block) {
		TFTPPacket corruptedPacket = tftpPacket;
		
		if (tftpPacket.getPacketType() == op) {
			
			if ((op == TFTPPacketType.WRQ) || (op == TFTPPacketType.RRQ)) {
				
				RRQWRQPacket rrqwrq = null;
				
				try {
					rrqwrq = new RRQWRQPacket(tftpPacket);
				} catch (TFTPPacketParsingError e) {
					System.err.println(Globals.getErrorMessage("Error Simulator", "cannot parse TFTP DATA Packet"));
					e.printStackTrace();
					System.exit(-1);
				}
				
				
				if (code == 2) //corrupt opcode
					corruptedPacket =  corruptOpCode(rrqwrq);
				else if (code == 3) //corrupt mode
					corruptedPacket =  corruptMode(rrqwrq);
				
			}
			
			else if (op == TFTPPacketType.DATA) {
				if (code == 2) { //corrupts op code only, there is not mode in DATA
					DATAPacket data = null;

					try {
						data = new DATAPacket(tftpPacket);
					} catch (TFTPPacketParsingError e) {
						System.err.println(Globals.getErrorMessage("Error Simulator", "cannot parse TFTP DATA Packet"));
						e.printStackTrace();
						System.exit(-1);
					}

					if (data.getBlockNumber() == block)
						corruptedPacket = corruptOpCode(data);
				}
			} 
			else {
				if (code == 2) { //corrupts op code only, there is not mode in ACK
					ACKPacket ack = null;

					try {
						ack = new ACKPacket(tftpPacket);
					} catch (TFTPPacketParsingError e) {
						System.err.println(Globals.getErrorMessage("Error Simulator", "cannot parse TFTP ACK Packet"));
						e.printStackTrace();
						System.exit(-1);
					}

					if (ack.getBlockNumber() == block) {
						corruptedPacket = corruptOpCode(ack);
					}
				}
			}
		}
		
		return corruptedPacket;
	}
	
	//Hardcodes a wrong OPcode into the packet and returns the byte
	private TFTPPacket corruptOpCode(TFTPPacket tftpPacket) {

		// Corrupt op code
		byte[] corruptedBytes = tftpPacket.getPacketBytes();
		// hardcoded corruption
		corruptedBytes[0] = 1;
		corruptedBytes[1] = 5;
		
		TFTPPacket corruptedTFTPPacket = null;
		try {
			corruptedTFTPPacket = new TFTPPacket(corruptedBytes, 0, corruptedBytes.length, tftpPacket.getRemoteAddress(), tftpPacket.getRemotePort());
		} catch (TFTPPacketParsingError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return corruptedTFTPPacket;
	}
	
	//Hardcodes a wrong mode, and returns a byte
	private RRQWRQPacket corruptMode(RRQWRQPacket rrqwrq) {
		String mode = pickRandomMode(rrqwrq.getMode());
		
		RRQWRQPacket corruptRRQWRQPacket = RRQWRQPacket.buildPacket(rrqwrq.getPacketType(), rrqwrq.getFileName(), mode, 
				rrqwrq.getRemoteAddress(), rrqwrq.getRemotePort());
		
		return corruptRRQWRQPacket;
	}
	
	//Picks a random mode
	private String pickRandomMode(String x) {
		String[] modes = {"netascii", "octet", "mail"};
		int random = 0;
		while (modes[random] == x) {
			random = new Random().nextInt(modes.length);
		}
		return modes[random];
	}
	
	public void shutdown() {
		System.out.println(Globals.getVerboseMessage("Error Simulator", "shutting down..."));
		
		// wait for any packets to be replayed
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			System.err.println(Globals.getErrorMessage("Error Simulator", "cannot make current thread go to sleep."));
			e.printStackTrace();
			System.exit(-1);
		}
		
		if (!tftpSocket.isClosed()) {
			// temporary socket is created to send a decoy package to the server so that it can stop listening
			// therefore once it re-evaluates that the boolean online is false it will exit
			try {
				DatagramSocket shutdownClient = new DatagramSocket();
				shutdownClient.send(new DatagramPacket(new byte[0], 0, InetAddress.getLocalHost(), NetworkConfig.PROXY_PORT));
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
		
		System.out.println(Globals.getVerboseMessage("Error Simulator", "goodbye!"));
	}
	
	public static void main(String[] args) {
		ErrorSimulator proxy = null;
		Thread proxyThread = null;
		
		System.out.println("\nSYSC 3033 TFTP Error Simulator");
		System.out.println("1. Start");
		System.out.println("2. Invalid TFTP");
		System.out.println("3. Invalid Transfer ID");
		System.out.println("4. Exit");
		System.out.println("Selection: ");
		
		int selection = 0;
		Scanner sc = new Scanner(System.in);
		selection = sc.nextInt();
		
		if (selection != 4) {
			// create server a thread for it listen on
			proxy = new ErrorSimulator();
			proxy.errorSelection = selection; //so the errorSimulator knows what to do
			
			if (proxy.errorSelection == 2) {
				//Invalid TFTP
				System.out.println("Which operation would you like to corrupt?");
				System.out.println("1. READ");
				System.out.println("2. WRITE");
				System.out.println("3. DATA");
				System.out.println("4. ACK");
				System.out.println("5. Any"); //
				System.out.println("6. Exit");
				System.out.println("Selection: ");

				selection = sc.nextInt();
				
				//shutsdown
				if (selection == 6) {
					sc.close();
					System.exit(0);
				} 
				//picks random packet
				else if (selection == 5) {
					Random rand = new Random();
					TFTPPacketType[] types = TFTPPacketType.values();
					proxy.errorOp = types[rand.nextInt(types.length)];
					while (proxy.errorOp == TFTPPacketType.ERROR) {
						proxy.errorOp = types[rand.nextInt(types.length)];
					}
				}
				else {
					switch(selection) {
					case 1: proxy.errorOp = TFTPPacketType.RRQ;
						break;
					case 2: proxy.errorOp = TFTPPacketType.WRQ;
						break;
					case 3: proxy.errorOp = TFTPPacketType.DATA;
						break;
					case 4: proxy.errorOp = TFTPPacketType.ACK;
						break;
					default:
						break;
					}
				}
				if (proxy.errorOp == TFTPPacketType.ACK || proxy.errorOp == TFTPPacketType.DATA) {
					System.out.println("Which block would you like to corrupt?");
					selection = sc.nextInt();
					proxy.errorBlock = (short)selection;
				}
			}
			
			proxyThread = new Thread(proxy);
			proxyThread.start();
			
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
			proxy.shutdown();
			sc.close();
			try {
				proxyThread.join(1000);
			} catch (InterruptedException e) {
				System.err.println(Globals.getErrorMessage("ErrorSimulator", "cannot close server thread"));
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
}