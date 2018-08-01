import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * This class contains methods required to convert
 * 		- bytes to short
 * 		- short to bytes
 * 		- bytes to string
 * 		- string to bytes
 * 
 * @author Group 8
 *
 */
public class ByteConversions {
	/**
	 * Converts short data type value to array of bytes of length 2
	 * 
	 * @param num value to convert
	 * @return bytes array of length 2
	 */
	public static byte[] shortToBytes(short num) {
		ByteBuffer buffer = ByteBuffer.allocate(2);
		buffer.putShort(num);
		return buffer.array();
	}
	
	/**
	 * Converts array of bytes to short data value
	 * 
	 * @param numBytes array of bytes
	 * @return short value 
	 */
	public static short bytesToShort(byte[] numBytes) {
		ByteBuffer wrapped = ByteBuffer.wrap(numBytes);
		return wrapped.getShort();
	}
	
	/**
	 * Converts String data type value to array of bytes
	 * 
	 * @param str string to convert
	 * @return array of bytes
	 */
	public static byte[] stringToBytes(String str) {
		return str.getBytes();
	}
	
	/**
	 * Converts array of bytes to String
	 * 
	 * @param strBytes array of bytes
	 * @return string representation of the array of bytes
	 */
	public static String bytesToString(byte[] strBytes) {
		return new String(strBytes, StandardCharsets.UTF_8);
	}
}
