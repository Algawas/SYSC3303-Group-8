
/**
 * This class represents the exception for an error that occurs when parsing 
 * one of the TFTP packet bytes
 * @author Group 8
 *
 */
public class TFTPPacketParsingError extends Exception {
	private static final long serialVersionUID = 2190636435833303710L;

	public TFTPPacketParsingError() {
		super();
	}
	
	public TFTPPacketParsingError(String message) {
		super(message);
	}
}
