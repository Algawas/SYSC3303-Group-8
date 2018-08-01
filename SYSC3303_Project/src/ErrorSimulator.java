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

	// error code to simulate
	private int errorSelection;
	// type of error to simulate
	private TFTPPacketType errorOp;
	// packet's block number to modify
	private short errorBlock;
	// error corrupt (mode or opcode)
	private int errorCorrupt;
	// gets delay time input from user
	private int delayTime;

	//flag for losing a packet
	private boolean lose = false;
	//flag for duplicate a packet
	private boolean duplicate = false;

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
		tftpSocket = new TFTPSocket(0, NetworkConfig.PROXY_PORT);
	}

	@Override
	public void run() {
		listen();
	}

	private TFTPPacket establishNewConnection(TFTPPacket tftpPacket) {
		System.out.println(Globals.getVerboseMessage("Error Simulator", "received packet from client."));

		// save client address and port
		this.clientAddress = tftpPacket.getRemoteAddress();
		this.clientPort = tftpPacket.getRemotePort();

		// save server address and port
		try {
			serverThreadAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			System.err.println(Globals.getErrorMessage("Error Simulator", "cannot get localhost address"));
			e.printStackTrace();
			System.exit(-1);
		}
		serverThreadPort = NetworkConfig.SERVER_PORT;

		if (!lose) {
			System.out.println(Globals.getVerboseMessage("Error Simulator", "sending packet to server..."));
			
			TFTPPacket sendTFTPPacket;
			try {
				sendTFTPPacket = new TFTPPacket(tftpPacket.getPacketBytes(), 0, tftpPacket.getPacketBytes().length,
						this.serverThreadAddress, this.serverThreadPort);
				
				if (duplicate) // duplicates packet
					duplicatePacket(tftpSocket, sendTFTPPacket, delayTime);
				else
					tftpSocket.send(sendTFTPPacket);
			} catch (TFTPPacketParsingError e) {
				System.err.println(Globals.getErrorMessage("Error Simulator", "cannot create TFTP packet"));
				e.printStackTrace();
				System.exit(-1);
			}

			System.out.println(Globals.getVerboseMessage("Error Simulator", "waiting for packet from server..."));

			TFTPPacket receiveTFTPPacket = null;
			try {
				receiveTFTPPacket = tftpSocket.receive();
			} catch (SocketTimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			serverThreadAddress = receiveTFTPPacket.getRemoteAddress();
			serverThreadPort = receiveTFTPPacket.getRemotePort();
			return receiveTFTPPacket;
		} else {
			// if packet is lost, it calls this function again to wait from the client
			// TODO Confirm if it's acceptable to call listen from here
			return null;
		}

	}

	private void listen() {
		while (!tftpSocket.isClosed()) {
			// Receive packet from Client
			TFTPPacket receiveTFTPacket = null;
			TFTPPacket sendTFTPPacket = null;

			lose = false;
			duplicate = false;

			try {
				receiveTFTPacket = tftpSocket.receive();
			} catch (SocketTimeoutException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (receiveTFTPacket == null) {
				continue;
			}

			// checks if the incoming packet is a request packet
			// if so then reset the server port back to the main port
			if (receiveTFTPacket.getPacketType() == TFTPPacketType.RRQ
					|| receiveTFTPacket.getPacketType() == TFTPPacketType.WRQ) {

				if (((errorSelection == 2) || (errorSelection == 4) || (errorSelection == 5) || (errorSelection == 6))
						&& (errorOp == TFTPPacketType.RRQ || errorOp == TFTPPacketType.WRQ)) {
					receiveTFTPacket = simulateIllegalOperationError(receiveTFTPacket, errorSelection, errorOp,
							errorBlock);
				}

				receiveTFTPacket = establishNewConnection(receiveTFTPacket);
			}

			// if not a request packet, it checks which error needs to be done, and does
			// them
			else {
				if ((errorSelection == 2) || (errorSelection == 4) || (errorSelection == 5) || (errorSelection == 6)) {
					receiveTFTPacket = simulateIllegalOperationError(receiveTFTPacket, errorSelection, errorOp,
							errorBlock);
				}
			}

			InetAddress sendAddress;
			int sendPort;
			if (receiveTFTPacket.getRemoteAddress().equals(serverThreadAddress) && 
					receiveTFTPacket.getRemotePort() == serverThreadPort) {
				System.out.println(Globals.getVerboseMessage("Error Simulator", "recieved packet from server."));
				System.out.println(Globals.getVerboseMessage("Error Simulator", "sending packet to client..."));
				sendAddress = clientAddress;
				sendPort = clientPort;

			} else {
				System.out.println(Globals.getVerboseMessage("Error Simulator", "recieved packet from client."));
				System.out.println(Globals.getVerboseMessage("Error Simulator", "sending packet to server..."));
				sendAddress = serverThreadAddress;
				sendPort = serverThreadPort;
			}

			if (!lose) {
				try {
					sendTFTPPacket = new TFTPPacket(receiveTFTPacket.getPacketBytes(), 0,
							receiveTFTPacket.getPacketBytes().length, sendAddress, sendPort);
				} catch (TFTPPacketParsingError e) {
					System.err.println(Globals.getErrorMessage("Error Simulator", "cannot create TFTP Packet"));
					e.printStackTrace();
					System.exit(-1);
				}

				if (errorSelection == 3) { // transfer ID error
					TFTPSocket tempTFTPSocket = new TFTPSocket(0);

					if (duplicate) // sends a duplicate packet after delay
						duplicatePacket(tempTFTPSocket, sendTFTPPacket, delayTime);
					else
						tempTFTPSocket.send(sendTFTPPacket);
					tempTFTPSocket.close();
					
				} else {
					if (duplicate) // sends a duplicate packet after delay
						duplicatePacket(tftpSocket, sendTFTPPacket, delayTime);
					else
						tftpSocket.send(sendTFTPPacket);
				}
			}
		}

		tftpSocket.close();
	}

	private void duplicatePacket(TFTPSocket socket, TFTPPacket packet, int time) {
		socket.send(packet);
		System.out.println("The original (non-duplicate) packet has been sent");

		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			System.err.println(Globals.getErrorMessage("Error Simulator", "cannot sleep"));
			e.printStackTrace();
			System.exit(-1);
		}

		socket.send(packet);
		System.out.println("The duplicate packet has been sent");
	}

	// Checks the PacketType, and block, and simulates the appropriate error on it
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

				if (code == 2) {
					if (errorCorrupt == 1)// corrupt opcode
						corruptedPacket = corruptOpCode(rrqwrq);
					else if (errorCorrupt == 2)// corrupt mode
						corruptedPacket = corruptMode(rrqwrq);
				} else if (code == 4)
					activateLosePacket();
				else if (code == 5)
					delayPacket(delayTime);
				else if (code == 6)
					activateDuplicatePacket();
				
				errorSelection = 1;

			}

			else if (op == TFTPPacketType.DATA) {
				// if (code == 2) { //corrupts op code only, there is not mode in DATA
				DATAPacket data = null;

				try {
					data = new DATAPacket(tftpPacket);
				} catch (TFTPPacketParsingError e) {
					System.err.println(Globals.getErrorMessage("Error Simulator", "cannot parse TFTP DATA Packet"));
					e.printStackTrace();
					System.exit(-1);
				}

				if (data.getBlockNumber() == block) {
					if (code == 2)
						corruptedPacket = corruptOpCode(data);
					else if (code == 4)
						activateLosePacket();
					else if (code == 5)
						delayPacket(delayTime);
					else if (code == 6)
						activateDuplicatePacket();
					
					errorSelection = 1;
				}
				// }
			} else {
				// if (code == 2) { // corrupts op code only, there is not mode in ACK
				ACKPacket ack = null;

				try {
					ack = new ACKPacket(tftpPacket);
				} catch (TFTPPacketParsingError e) {
					System.err.println(Globals.getErrorMessage("Error Simulator", "cannot parse TFTP ACK Packet"));
					e.printStackTrace();
					System.exit(-1);
				}

				if (ack.getBlockNumber() == block) {
					if (code == 2)
						corruptedPacket = corruptOpCode(ack);
					else if (code == 4)
						activateLosePacket();
					else if (code == 5)
						delayPacket(delayTime);
					else if (code == 6)
						activateDuplicatePacket();
					
					errorSelection = 1;
				}
				// }
			}
		}

		return corruptedPacket;
	}

	private void activateLosePacket() {
		lose = true;
		System.out.println("The packet has been lost");
	}

	private void activateDuplicatePacket() {

		duplicate = true;
		System.out.println("The packet has been duplicated");
	}

	private void delayPacket(int time) {
		try {
			Thread.sleep(time);
			System.out.println("The packet has been delayed by " + time / 1000 + " seconds");
		} catch (InterruptedException e) {
			System.err.println(Globals.getErrorMessage("ErrorSimulator", "cannot sleep"));
			e.printStackTrace();
			System.exit(-1);
		}
	}

	// Hardcodes a wrong OPcode into the packet and returns the byte
	private TFTPPacket corruptOpCode(TFTPPacket tftpPacket) {

		// Corrupt op code
		byte[] corruptedBytes = tftpPacket.getPacketBytes();
		// hardcoded corruption
		corruptedBytes[0] = 1;
		corruptedBytes[1] = 5;

		TFTPPacket corruptedTFTPPacket = null;
		try {
			corruptedTFTPPacket = new TFTPPacket(corruptedBytes, 0, corruptedBytes.length,
					tftpPacket.getRemoteAddress(), tftpPacket.getRemotePort());
		} catch (TFTPPacketParsingError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return corruptedTFTPPacket;
	}

	// Hardcodes a wrong mode, and returns a byte
	private RRQWRQPacket corruptMode(RRQWRQPacket rrqwrq) {
		String mode = pickRandomMode(rrqwrq.getMode());

		RRQWRQPacket corruptRRQWRQPacket = RRQWRQPacket.buildPacket(rrqwrq.getPacketType(), rrqwrq.getFileName(), mode,
				rrqwrq.getRemoteAddress(), rrqwrq.getRemotePort());

		return corruptRRQWRQPacket;
	}

	// Picks a random mode
	private String pickRandomMode(String x) {
		String[] modes = { "netascii", "octet", "mail" };
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
			// temporary socket is created to send a decoy package to the server so that it
			// can stop listening
			// therefore once it re-evaluates that the boolean online is false it will exit
			try {
				DatagramSocket shutdownClient = new DatagramSocket();
				shutdownClient
						.send(new DatagramPacket(new byte[0], 0, InetAddress.getLocalHost(), NetworkConfig.PROXY_PORT));
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
		System.out.println("1. Normal Start (No error simulation)");
		System.out.println("2. Invalid TFTP");
		System.out.println("3. Invalid Transfer ID");
		System.out.println("4. Lose a packet");
		System.out.println("5. Delay a packet");
		System.out.println("6. Duplicate a packet");
		System.out.println("7. Exit");
		System.out.println("Selection: ");

		int selection = 0;
		Scanner sc = new Scanner(System.in);
		selection = sc.nextInt();

		if (selection != 7) {
			// create server a thread for it listen on
			proxy = new ErrorSimulator();
			proxy.errorSelection = selection; // so the errorSimulator knows what to do

			if ((proxy.errorSelection != 3) && (proxy.errorSelection != 1)) {
				// Invalid TFTP
				System.out.println("Which operation would you like to simulate an error?");
				System.out.println("1. READ");
				System.out.println("2. WRITE");
				System.out.println("3. DATA");
				System.out.println("4. ACK");
				System.out.println("5. Any"); //
				System.out.println("6. Exit");
				System.out.println("Selection: ");

				selection = sc.nextInt();

				// shutsdown
				if (selection == 6) {
					sc.close();
					System.exit(0);
				}
				// picks random packet
				else if (selection == 5) {
					Random rand = new Random();
					TFTPPacketType[] types = TFTPPacketType.values();
					proxy.errorOp = types[rand.nextInt(types.length)];
					while (proxy.errorOp == TFTPPacketType.ERROR) {
						proxy.errorOp = types[rand.nextInt(types.length)];
					}
				} else {
					switch (selection) {
					case 1:
						proxy.errorOp = TFTPPacketType.RRQ;
						break;
					case 2:
						proxy.errorOp = TFTPPacketType.WRQ;
						break;
					case 3:
						proxy.errorOp = TFTPPacketType.DATA;
						break;
					case 4:
						proxy.errorOp = TFTPPacketType.ACK;
						break;
					default:
						break;
					}
				}
				if ((proxy.errorSelection == 2)
						&& ((proxy.errorOp == TFTPPacketType.WRQ) || (proxy.errorOp == TFTPPacketType.RRQ))) {
					System.out.println("What would you like to corrupt?");
					System.out.println("1. OP Code");
					System.out.println("2. Mode");
					proxy.errorCorrupt = selection;
				}
				if ((proxy.errorOp == TFTPPacketType.ACK || proxy.errorOp == TFTPPacketType.DATA)) {
					System.out.println("Which block would you like to corrupt?");
					selection = sc.nextInt();
					proxy.errorBlock = (short) selection;
				}
				if ((proxy.errorSelection == 5) || (proxy.errorSelection == 6)) {
					System.out.println("How long of a delay would you like? (in milliseconds)");
					selection = sc.nextInt();
					proxy.delayTime = selection;
				}
			}

			proxyThread = new Thread(proxy);
			proxyThread.start();

		} else {
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