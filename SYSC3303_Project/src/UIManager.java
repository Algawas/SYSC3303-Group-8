import java.util.InputMismatchException;
import java.util.Scanner;

public class UIManager {
	private static boolean verboseMode = false;
	private static Scanner sc = new Scanner(System.in);
	
	public UIManager() {}
	
	public static void printMessage(String className, String[] messages) {
		if (!verboseMode)
			System.out.println(String.format("VERBOSE: %s - %s", className, messages[1]));
		else
			System.out.println(String.format("NORMAL: %s - %s", className, messages[0]));
		
	}
	
	public static void printErrorMessage(String className, String message) {
		System.err.println(String.format("ERROR: %s - %s", className, message));
	}
	
	public static void promptForUIMode() {
		System.out.println("Select UI mode");
		String[] options = {
				"Normal Mode",
				"Verbose Mode"
		};
		
		int selection = UIManager.promptForOperationSelection(options);
		
		if (selection == 1)
			verboseMode = true;
		else
			verboseMode = false;
	}
	
	public static String promptForIPAddress() {
		System.out.print("Enter Server IP Address or press enter for local host: ");
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
		
		System.out.print("Enter file directory: ");
		
		fileDirectory = sc.nextLine().trim();
		
		return fileDirectory;
	}
	
	public static void close() {
		sc.close();
	}
	
	
}
