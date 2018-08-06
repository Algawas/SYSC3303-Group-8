import java.util.Scanner;

/**
* This class contains the network configuration settings
* 
* @author Group 8
*/

public class NetworkConfig {
	// should be changed to 69 when submitting the project
	public static final int SERVER_PORT = 69;
	public static final int DATAGRAM_PACKET_MAX_LEN = 516;
	public static final int PROXY_PORT = 23;
	public static final int TIMEOUT_TIME = 5000;
	public static final int MAX_TRIES = 5;
	
	//This function selects the IP Address of the server or error simulator 

	public static String IPSelector() {

		System.out.println("Enter in the IP Address of the server (Type 'local' for local host): ");
		Scanner sc = new Scanner(System.in);
		String scan = sc.nextLine();
		if (scan.equals("local"))
			scan = "localhost";
		// sc.close();
		return scan;
	}
}
/*
public static InetAddress IPSelector() {
	InetAddress address;
	System.out.println("Enter in the IP Address of the server (Type 'local' for local host): ");
	Scanner sc = new Scanner(System.in);
	String scan = sc.nextLine();
	if (scan.equals("local"))
		address = InetAddress.getLocalHost();
	// sc.close();
	address = scan;
	return scan;
}
*/
