// The client sends a character string to the echo server, then waits 
// for the server to send it back to the client.

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Client {

   DatagramPacket sendPacket, receivePacket;
   static DatagramSocket sendReceiveSocket;

   public Client()
   {
      try {
         // Construct a datagram socket and bind it to any available 
         // port on the local host machine. This socket will be used to
         // send and receive UDP Datagram packets.
         sendReceiveSocket = new DatagramSocket();
      } catch (SocketException se) {   // Can't create the socket.
    	 System.err.println(Globals.getErrorMessage("Client", "cannot create datagram socket"));
         se.printStackTrace();
         System.exit(-1);
      } 
   }

   public void send(byte[] msg)
   {
	      // Construct a datagram packet that is to be sent to a specified port 
	      // on a specified host.
	      // The arguments are:
	      //  msg - the message contained in the packet (the byte array)
	      //  msg.length - the length of the byte array
	      //  InetAddress.getLocalHost() - the Internet address of the 
	      //     destination host.
	      //     In this example, we want the destination to be the same as
	      //     the source (i.e., we want to run the client and server on the
	      //     same computer). InetAddress.getLocalHost() returns the Internet
	      //     address of the local host.
	      //  5000 - the destination port number on the destination host.
	      try {
	         sendPacket = new DatagramPacket(msg, msg.length,
	                                         InetAddress.getLocalHost(), NetworkConfig.SERVER_PORT);
	      } catch (UnknownHostException e) {
	    	  System.err.println(Globals.getErrorMessage("Client", "cannot send datagram socket"));
	    	  e.printStackTrace();
	    	  System.exit(-1);
	      }

	      System.out.println("Client: Sending packet:");
	      System.out.println("To host: " + sendPacket.getAddress());
	      System.out.println("Destination host port: " + sendPacket.getPort());
	      int len = sendPacket.getLength();
	      System.out.println("Length: " + len);
	      System.out.print("Containing: ");
	      System.out.println(new String(sendPacket.getData(),0,len)); // or could print "s"

	      // Send the datagram packet to the server via the send/receive socket. 

	      try {
	         sendReceiveSocket.send(sendPacket);
	      } catch (IOException e) {
	         e.printStackTrace();
	         System.exit(1);
	      }

	      System.out.println("Client: Packet sent.\n");

   }
   
   public String receive()
   {
	  // Construct a DatagramPacket for receiving packets up 
      // to 100 bytes long (the length of the byte array).

      byte data[] = new byte[NetworkConfig.DATAGRAM_PACKET_MAX_LEN];
      receivePacket = new DatagramPacket(data, data.length);

      try {
         // Block until a datagram is received via sendReceiveSocket.  
         sendReceiveSocket.receive(receivePacket);
      } catch(IOException e) {
         e.printStackTrace();
         System.exit(1);
      }

      // Process the received datagram.
      System.out.println("Client: Packet received:");
      System.out.println("From host: " + receivePacket.getAddress());
      System.out.println("Host port: " + receivePacket.getPort());
      int len = receivePacket.getLength();
      System.out.println("Length: " + len);
      System.out.print("Containing: ");

      // Form a String from the byte array.
      String received = new String(data,0,len);   
      
      return received;
   }

   public static void main(String args[])
   {
      Client c = new Client();
      
      Scanner sc = new Scanner(System.in);
      int userInput = 0;
	  do
	  {
		  System.out.println("\nSYSC 3033 Client");
		  System.out.println("1. Write file to Server");
		  System.out.println("2. Read file from Server");
		  System.out.println("3. Close Client");
		  System.out.print("Enter choice (1-3): ");
		  userInput = sc.nextInt();
		  sc.nextLine(); //just to prevent error
		  // send (write)
		  if (userInput == 1)
		  {
			  System.out.print("Enter path to file to send to Server: ");
		      String s = sc.nextLine();
		      Path path = Paths.get(s);
		      byte[] msg = null;
		      try {
				msg = Files.readAllBytes(path);
		      } catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
		      }
		      // send message
		      c.send(msg);
		  }
		  else if (userInput == 2)
		  {
			  String s = c.receive();
			  System.out.print("Received from server: " + s);
		  }
		  else if (userInput == 3)
		  {
			// We're finished, so close the socket.
		      sendReceiveSocket.close();
		  }
		  else
		  {
			  System.out.println("Wrong input number!\nEnter integer number 1-3!");
		  }
	  }
	  while (userInput != 3);
	  
	  sc.close();
   }
}
