
/**
 * This class contains any methods that need to be globally accessed by all classes
 * 
 * @author Group 8
 */
public class Globals {
	/**
	* Returns a formatted verbose message
	* 
	* @param className: name of the class where the message is needed
	* @param message: message
	*
	* @return message
	*/
	public static String getVerboseMessage(String className, String message) {
		return new String(String.format("VERBOSE: %s - %s", className, message));
	}
	
	/**
	* Returns a formatted error message
	* 
	* @param className: name of the class where the error occurred in 
	* @param message: error message
	*
	* @return error message
	*/
	public static String getErrorMessage(String className, String message) {
		return new String(String.format("ERROR: %s - %s", className, message));
	}
}
