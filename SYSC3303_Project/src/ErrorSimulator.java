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
	
	// new block number to be changed into
	private short newBlockNumber;
	// error corrupt (mode or opcode)
	private int errorCorrupt;
	// gets delay time input from user
	private int delayTime;
	
	// checks if user wants to corrupt error packet
	private boolean corruptErrPacket;
	// error corrupt option for corrupting error packet
	private int corruptErrPacketCorrupt;

	//flag for losing a packet
	private boolean lose = false;
	//flag for duplicate a packet
	private boolean duplicate = false;

	public ErrorSimulator() {
		while (true) {
			String IPAddress = UIManager.promptForIPAddress();

			// save server address and port
			try {
				serverThreadAddress = InetAddress.getByName(IPAddress);
				System.out.println("Server Address is: " + serverThreadAddress);
				break;
			} catch (UnknownHostException e) {
				UIManager.printErrorMessage("Error Simulator", "cannot get the Server IP address");
				e.printStackTrace();
				//System.exit(-1);
				continue;
			}
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
		String[] messages1 = {
				"received request packet from client.",
				String.format("received request packet from client %s:%d, establisihing new connection.", tftpPacket.getRemoteAddress(), tftpPacket.getRemotePort())
		};
		
		UIManager.printMessage("ErrorSimulator", messages1);

		// save client address and port
		this.clientAddress = tftpPacket.getRemoteAddress();
		this.clientPort = tftpPacket.getRemotePort();

		// save server port
		serverThreadPort = NetworkConfig.SERVER_PORT;

		if (!lose) {
			String[] messages2 = {
					"sending packet to server...",
					String.format("sending %s to server...", tftpPacket.toString())
			};
			
			UIManager.printMessage("ErrorSimulator", messages2);
			
			TFTPPacket sendTFTPPacket;
			try {
				sendTFTPPacket = new TFTPPacket(tftpPacket.getPacketBytes(), 0, tftpPacket.getPacketBytes().length,
						this.serverThreadAddress, this.serverThreadPort);
				
				if (duplicate) // duplicates packet
					duplicatePacket(tftpSocket, sendTFTPPacket, delayTime);
				else
					tftpSocket.send(sendTFTPPacket);
			} catch (TFTPPacketParsingError e) {
				UIManager.printErrorMessage("Error Simulator", "cannot create TFTP packet");
				e.printStackTrace();
				System.exit(-1);
			}

			String[] messages3 = {
					"waiting for packet from server...",
					"waiting for packet from server..."
			};
			
			UIManager.printMessage("ErrorSimulator", messages3);

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
			
			System.out.println("corruptErrPacket is " + corruptErrPacket);
			System.out.println("receiveTFTPacket.getPacketType() is " + receiveTFTPacket.getPacketType());

			// checks if the incoming packet is a request packet
			// if so then reset the server port back to the main port
			if (receiveTFTPacket.getPacketType() == TFTPPacketType.RRQ
					|| receiveTFTPacket.getPacketType() == TFTPPacketType.WRQ) {

				if (((errorSelection == 2) || (errorSelection == 4) || (errorSelection == 5) || (errorSelection == 6))
						&& (errorOp == TFTPPacketType.RRQ || errorOp == TFTPPacketType.WRQ)) {
					System.out.println("The errorOP is " + errorOp);
					receiveTFTPacket = simulateIllegalOperationError(receiveTFTPacket, errorSelection, errorOp,
							errorBlock);
				}

				receiveTFTPacket = establishNewConnection(receiveTFTPacket);
			}

			// if not a request packet, it checks which error needs to be done, and does
			// them
			else if ((errorSelection == 2) || (errorSelection == 4) || (errorSelection == 5) || (errorSelection == 6)) {
					receiveTFTPacket = simulateIllegalOperationError(receiveTFTPacket, errorSelection, errorOp,
							errorBlock);
			}
			

			if ((receiveTFTPacket.getPacketType() == TFTPPacketType.ERROR) && corruptErrPacket) {
				System.out.println("Error is being reached");
				receiveTFTPacket = simulateIllegalOperationError(receiveTFTPacket, errorSelection, TFTPPacketType.ERROR,
						errorBlock);
			}

			if (!lose) {
				InetAddress sendAddress;
				int sendPort;
				
				if (receiveTFTPacket.getRemoteAddress().equals(serverThreadAddress)
						&& receiveTFTPacket.getRemotePort() == serverThreadPort) {
					
					String[] messages1 = {
							"recieved packet from server.",
							String.format("recieved %s from server.", receiveTFTPacket.toString())
					};
					
					UIManager.printMessage("ErrorSimulator", messages1);
					
					
					String[] messages2 = {
							"sending packet to client...",
							"sending packet to client..."
					};
					
					UIManager.printMessage("ErrorSimulator", messages2);
					
					sendAddress = clientAddress;
					sendPort = clientPort;

				} else {
					String[] messages1 = {
							"recieved packet from client.",
							String.format("recieved %s from client.", receiveTFTPacket.toString())
					};
					
					UIManager.printMessage("ErrorSimulator", messages1);
					
					String[] messages2 = {
							"sending packet to server...",
							"sending packet to server..."
					};
					
					UIManager.printMessage("ErrorSimulator", messages2);
					
					sendAddress = serverThreadAddress;
					sendPort = serverThreadPort;
				}

				try {
					sendTFTPPacket = new TFTPPacket(receiveTFTPacket.getPacketBytes(), 0,
							receiveTFTPacket.getPacketBytes().length, sendAddress, sendPort);
				} catch (TFTPPacketParsingError e) {
					UIManager.printErrorMessage("Error Simulator", "cannot create TFTP Packet");
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
			UIManager.printErrorMessage("Error Simulator", "cannot sleep");
			e.printStackTrace();
			System.exit(-1);
		}

		socket.send(packet);
		System.out.println("The duplicate packet has been sent");
	}

	
	// Checks the PacketType, and block, and simulates the appropriate error on it
	private TFTPPacket simulateIllegalOperationError(TFTPPacket tftpPacket, int code, TFTPPacketType op, short block) {
		System.out.println("The tftpPacket is " + tftpPacket.getPacketType());
		TFTPPacket corruptedPacket = tftpPacket;

		if (tftpPacket.getPacketType() == op) {

			if ((op == TFTPPacketType.WRQ) || (op == TFTPPacketType.RRQ)) {

				RRQWRQPacket rrqwrq = null;

				try {
					rrqwrq = new RRQWRQPacket(tftpPacket);
				} catch (TFTPPacketParsingError e) {
					UIManager.printErrorMessage("Error Simulator", "cannot parse TFTP DATA Packet");
					e.printStackTrace();
					System.exit(-1);
				}

				if (code == 2) {
					System.out.println("The tftpPacket is reached");
					if (errorCorrupt == 1)// corrupt opcode
						corruptedPacket = corruptOpCode(rrqwrq);
					else if (errorCorrupt == 2) {// corrupt mode
						System.out.println("Code is being reached");
						corruptedPacket = corruptMode(rrqwrq);
					}
					else if ((errorCorrupt == 3) || (errorCorrupt == 4))
						corruptedPacket = corruptZeroByte(rrqwrq, errorCorrupt);
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
					UIManager.printErrorMessage("Error Simulator", "cannot parse TFTP DATA Packet");
					e.printStackTrace();
					System.exit(-1);
				}

				if (data.getBlockNumber() == block) {
					
					System.out.println("The newBlockNumber is " + newBlockNumber);
					System.out.println("The errorCorruptNumber is " + errorCorrupt);
					System.out.println("The data.getBlockNumber() is " + data.getBlockNumber());
					
					if (code == 2) {
						if (errorCorrupt == 1)
							corruptedPacket = corruptOpCode(data);
						else if (errorCorrupt == 2)
							corruptedPacket = corruptBlockNumber(data, newBlockNumber);
						
						try {
							corruptedPacket = new DATAPacket(corruptedPacket);
						} catch (TFTPPacketParsingError e) {
							UIManager.printErrorMessage("Error Simulator", "cannot parse TFTP DATA Packet");
							e.printStackTrace();
							System.exit(-1);
						}
					}
					else if (code == 4)
						activateLosePacket();
					else if (code == 5)
						delayPacket(delayTime);
					else if (code == 6)
						activateDuplicatePacket();
					
					errorSelection = 1;
				}
				// }
			} else if (op == TFTPPacketType.ACK) {
				// if (code == 2) { // corrupts op code only, there is not mode in ACK
				ACKPacket ack = null;

				try {
					ack = new ACKPacket(tftpPacket);
				} catch (TFTPPacketParsingError e) {
					UIManager.printErrorMessage("Error Simulator", "cannot parse TFTP ACK Packet");
					e.printStackTrace();
					System.exit(-1);
				}

				if (ack.getBlockNumber() == block) {
					if (code == 2) {
						if (errorCorrupt == 1)
							corruptedPacket = corruptOpCode(ack);
						else if (errorCorrupt == 2)
							corruptedPacket = corruptBlockNumber(ack, newBlockNumber);
						
						try {
							corruptedPacket = new ACKPacket(corruptedPacket);
						} catch (TFTPPacketParsingError e) {
							UIManager.printErrorMessage("Error Simulator", "cannot parse TFTP DATA Packet");
							e.printStackTrace();
							System.exit(-1);
						}
					}
					else if (code == 4)
						activateLosePacket();
					else if (code == 5)
						delayPacket(delayTime);
					else if (code == 6)
						activateDuplicatePacket();
					
					errorSelection = 1;
				}
				// }
			} else if (op == TFTPPacketType.ERROR) {
				
				System.out.println("Error is being reached");
				
				ERRORPacket error = null;

				try {
					error = new ERRORPacket(tftpPacket);
				} catch (TFTPPacketParsingError e) {
					UIManager.printErrorMessage("Error Simulator", "cannot parse TFTP ACK Packet");
					e.printStackTrace();
					System.exit(-1);
				}

				if (corruptErrPacketCorrupt == 1) {
					System.out.println("Corrupt error op code is being reached");
					corruptedPacket = corruptOpCode(error);
				}
				else if (corruptErrPacketCorrupt == 2) {
					System.out.println("Corrupt error error code is being reached");
					corruptedPacket = corruptErrorCode(error);
				}
				else if (corruptErrPacketCorrupt == 3) {
					System.out.println("Corrupt error zero byte is being reached");
					corruptedPacket = corruptZeroByte(error, 4);
				}

				try {
					corruptedPacket = new ERRORPacket(corruptedPacket);
				} catch (TFTPPacketParsingError e) {
					UIManager.printErrorMessage("Error Simulator", "cannot parse TFTP DATA Packet");
					e.printStackTrace();
					System.exit(-1);
				}

				errorSelection = 1;
				corruptErrPacket = false;
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
			UIManager.printErrorMessage("ErrorSimulator", "cannot sleep");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private TFTPPacket corruptErrorCode(TFTPPacket tftpPacket) {
		byte[] corruptedBytes = tftpPacket.getPacketBytes();
		// hardcoded corruption
		corruptedBytes[2] = 1;
		corruptedBytes[3] = 5;

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
	
	private TFTPPacket corruptZeroByte(TFTPPacket tftpPacket, int error) {
		
		byte[] corruptedBytes = tftpPacket.getPacketBytes();
		int last = corruptedBytes.length - 1;
		
		System.out.println("Last byte in tftppacket is " + corruptedBytes[last]);
		// hardcoded corruption
		if(error == 4) {
			corruptedBytes[last] = 1;
			System.out.println("Last byte in tftppacket is now " + corruptedBytes[last]);
		} else if (error == 3) {
			for(int i = 2; i <= last ; i++) {
				if (corruptedBytes[i] == 0) {
					System.out.println("The tftppacket at i " + corruptedBytes[i]);
					corruptedBytes[i] = 1;
					System.out.println("The tftppacket at i is now " + corruptedBytes[i]);
					break;
				}
			}
		}

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

	private TFTPPacket corruptBlockNumber(TFTPPacket tftpPacket, short block) {
		byte[] corruptedBytes = tftpPacket.getPacketBytes();
		byte[] blockBytes = ByteConversions.shortToBytes(block);
		
		System.out.println("blockBytes short is " + block);
		System.out.println("blockBytes length is " + blockBytes.length);
		System.out.println("blockBytes[0] is " + blockBytes[0]);
		System.out.println("blockBytes[1] is " + blockBytes[1]);
		
		// hardcoded corruption
		corruptedBytes[2] = blockBytes[0];
		corruptedBytes[3] = blockBytes[1];
		
		byte[] temp = {corruptedBytes[2], corruptedBytes[3]};
		
		if (temp != null)
			System.out.println("blockBytes[1] is " + ByteConversions.bytesToShort(temp));

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
		System.out.println("old mode is " + rrqwrq.getMode());
		String mode = pickRandomMode(rrqwrq.getMode());

		RRQWRQPacket corruptRRQWRQPacket = RRQWRQPacket.buildPacket(rrqwrq.getPacketType(), rrqwrq.getFileName(), mode,
				rrqwrq.getRemoteAddress(), rrqwrq.getRemotePort());

		System.out.println("old mode is " + corruptRRQWRQPacket.getMode());
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
		String[] messages1 = {
				"shutting down...",
				"shutting down..."
		};
		UIManager.printMessage("Error Simulator", messages1);

		// wait for any packets to be replayed
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			UIManager.printErrorMessage("Error Simulator", "cannot make current thread go to sleep.");
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
				String.format("error simulator is shutdown. Port %d released.", tftpSocket.getPort())
		};
		
		UIManager.printMessage("Server", messages2);
	}

	public static void main(String[] args) {
		
		UIManager.promptForUIMode();
		
		ErrorSimulator proxy = null;
		Thread proxyThread = null;

		System.out.println("\nSYSC 3033 TFTP Error Simulator");
		System.out.println("1. Normal Start (No error simulation)");
		System.out.println("2. Invalid TFTP");
		System.out.println("3. Invalid Transfer ID");
		System.out.println("4. Lose a packet");
		System.out.println("5. Delay a packet");
		System.out.println("6. Duplicate a packet");
		System.out.println("7. Corrupt ERROR packet (error codes 1, 2, 3, 6)");
		System.out.println("8. Exit");
		System.out.println("Selection: ");

		int selection = 0;
		Scanner sc = new Scanner(System.in);
		selection = sc.nextInt();

		if (selection != 8) {
			// create server a thread for it listen on
			proxy = new ErrorSimulator();
			proxy.errorSelection = selection; // so the errorSimulator knows what to do

			if ((proxy.errorSelection != 3) && (proxy.errorSelection != 1) && (proxy.errorSelection != 7)) {
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
					System.out.println("Randomly chose packet of type " + proxy.errorOp);
				}
				
				else {
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
				//TODO RECEIVING SIDE DOESN'T CHECK FORMAT OF WRQ OR RRQ PROPERLY (DOESN'T CHECK THE LAST 0 BYTE) 
				if ((proxy.errorSelection == 2)
						&& ((proxy.errorOp == TFTPPacketType.WRQ) || (proxy.errorOp == TFTPPacketType.RRQ))) {
					System.out.println("What would you like to corrupt?");
					System.out.println("1. OP Code");
					System.out.println("2. Mode");
					System.out.println("3. The first 0 byte");
					System.out.println("4. The last 0 byte");
					selection = sc.nextInt();
					
					proxy.errorCorrupt = selection;
					System.out.println("The errorCorrupt is " + proxy.errorCorrupt);
				}
				if ((proxy.errorOp == TFTPPacketType.ACK || proxy.errorOp == TFTPPacketType.DATA)) {
					System.out.println("Which block would you like to corrupt?");
					selection = sc.nextInt();
					proxy.errorBlock = (short) selection;
					if (proxy.errorSelection == 2) {
						System.out.println("What would you like to corrupt?");
						System.out.println("1. OP Code");
						System.out.println("2. Block Number");
						selection = sc.nextInt();
						proxy.errorCorrupt = selection;
						if (proxy.errorCorrupt == 2) {
							System.out.println("What would you like to change the block number to?");
							selection = sc.nextInt();
							proxy.newBlockNumber = (short) selection;
						}
					}
				}
				if ((proxy.errorSelection == 5) || (proxy.errorSelection == 6)) {
					System.out.println("How long of a delay would you like? (in milliseconds)");
					selection = sc.nextInt();
					proxy.delayTime = selection;
				}
				if (proxy.errorSelection < 6) {
					System.out.println("Would you also like to corrupt an ERROR packet?");
					System.out.println("1. Yes");
					System.out.println("2. No");
					selection = sc.nextInt();
					if (selection == 1) {
						proxy.corruptErrPacket = true;
						System.out.println("What would you like to corrupt?");
						System.out.println("1. OP Code");
						System.out.println("2. Error Code");
						System.out.println("3. The Last Zero Byte");
						selection = sc.nextInt();
						proxy.corruptErrPacketCorrupt = selection;
					}
					else if (selection == 0)
						proxy.corruptErrPacket = false;
					
				}
			} else if (proxy.errorSelection == 7) {
				System.out.println("Would you like to corrupt an ERROR packet?");
				System.out.println("(Will only work in testing error codes 1, 2, 3 and 6)");
				System.out.println("1. Yes");
				System.out.println("2. No");
				selection = sc.nextInt();
				if (selection == 1) {
					proxy.corruptErrPacket = true;
					System.out.println("What would you like to corrupt?");
					System.out.println("1. OP Code");
					System.out.println("2. Error Code");
					System.out.println("3. The Last Zero Byte");
					selection = sc.nextInt();
					proxy.corruptErrPacketCorrupt = selection;
				}
				else if (selection == 0)
					proxy.corruptErrPacket = false;
				
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
				UIManager.printErrorMessage("ErrorSimulator", "cannot close server thread");
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
}