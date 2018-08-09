import java.io.File;
import java.util.InputMismatchException;
import java.util.Scanner;

public class UIManager {
	private static boolean verboseMode = false;
	private static Scanner sc = new Scanner(System.in);
	
	public UIManager() {}
	
	/**
	 * Print verbose or non verbose message
	 * @param className
	 * @param messages
	 */
	public static void printMessage(String className, String[] messages) {
		if (verboseMode)
			System.out.println(String.format("VERBOSE: %s - %s", className, messages[1]));
		else 
		{	
			if (!messages[0].equals(""))
				System.out.println(String.format("NORMAL: %s - %s", className, messages[0]));
		}
		
	}
	
	/**
	 * Print error message
	 * @param className
	 * @param message
	 */
	public static void printErrorMessage(String className, String message) {
		System.err.println(String.format("ERROR: %s - %s", className, message));
	}
	
	/**
	 * Prompt for UI Mode, Quiet or Verbose mode
	 */
	public static void promptForUIMode() {
		System.out.println("\nSelect UI mode");
		String[] options = {
				"Quiet Mode",
				"Verbose Mode"
		};
		
		int selection = UIManager.promptForOperationSelection(options);
		
		if (selection == 1)
			verboseMode = false;
		else
			verboseMode = true;
	}
	
	/**
	 * Promtp  for operation mode, normal or test mode
	 * @return
	 */
	public static int promptForOperationMode() {
		System.out.println("\nSelect operation mode");
		
		String[] options = {
				"Normal mode",
				"Test mode"
		};
		
		return UIManager.promptForOperationSelection(options);
	}
	
	/**
	 * prompt for IP Address
	 * 
	 * @return
	 */
	public static String promptForIPAddress() {
		System.out.print("\nEnter server IP address or press enter for local host: ");
		return sc.nextLine().trim();
	}
	
	public static void showClientTitle() {
		System.out.println("\n***************************");
		System.out.println("*       TFTP Client       *");
		System.out.println("***************************\n");
	}
	
	public static void showServerTitle() {
		System.out.println("\n***************************");
		System.out.println("*       TFTP Server       *");
		System.out.println("***************************\n");
	}
	
	/**
	 * 
	 * @param options
	 * @return
	 */
	public static int promptForOperationSelection(String[] options) {
		for (int i = 0; i < options.length; i++) {
			System.out.println(String.format("%d. %s", (i + 1), options[i]));
		}
		
		int selection = 0;
		int optionsLen = options.length;
		while (selection == 0) {
			System.out.print(String.format("Enter selection (1 - %d): ", optionsLen));
			
			try {
				selection = sc.nextInt();
				sc.nextLine();
			} catch (InputMismatchException e) {
				printErrorMessage("UI_Manager", "Please enter a numerical value.");
				selection = 0;
			}
			
			if (selection < 1 || selection > optionsLen) {
				printErrorMessage("UI_Manager", "Please enter a numerical value in the given range.");
				selection = 0;
			}
		}
		
		return selection;
	}
	
	public static String promptForFileSelection() {
		String fileDirectory = null;
		
		boolean fileExists = false;
		while (!fileExists) {
			System.out.print("Enter file directory: ");
			
			fileDirectory = sc.nextLine().trim();
			
			File file = new File(fileDirectory);
			fileExists = file.exists();
			
			if (!fileExists)
				System.out.println("File does not exist.");
		}
		
		return fileDirectory;
	}
	
	public static String promptForQuit() {
		System.out.print("Enter 'quit' to quit server: ");
		String quitCommand = null;
		
		quitCommand = sc.nextLine().trim();
		
		return quitCommand;
	}
	
	public static void close() {
		sc.close();
	}
	
	
}
