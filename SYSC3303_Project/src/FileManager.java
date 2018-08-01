import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

/**
 * This class provides an interface for objects to write or read files from hard drive
 * 
 * @author Group 8
 */

public class FileManager {
	// directory where the file that are transferred will be saved
	private static String destinationDirectoryStr = "transfered_files";
	
	/**
	 * This class has the necessary variables to indicate the result
	 * of read or write to the hard drive
	 * 
	 * @author Group 8
	 */
	public class FileManagerResult {
		public byte[] fileBytes = null;
		boolean accessViolation = false;
		boolean fileNotFound = false;
		boolean fileAlreadyExist = false;
		boolean diskFull = false;
		boolean error = false;
	}

	public FileManager() {}
	
	/**
	 * Read file from hard dive and return data in list of bytes
	 * 
	 * @param fileName: fileName
	 * 
	 * Return FileManagerResult containing the list of bytes read or error flagged
	 */
	public FileManagerResult readFile(String fileName) {		
		FileManagerResult res = new FileManagerResult();
		
		File file = new File(fileName);
		
		byte[] fileBytes = new byte[(int)file.length()];
		
		try {
			FileInputStream fileInputStream = new FileInputStream(file); 
			fileInputStream.read(fileBytes);
			fileInputStream.close();
			res.fileBytes = fileBytes;
		} catch (FileNotFoundException e) {	
			
			
			// if the error message contains "Permission denied"
			// then set the accessViolation flag to true
			if (e.getMessage().contains("Permission denied"))
				res.accessViolation = true;
			else
				// if the error message contains "cannot find the file"
				// then set fileNotFound to true
				res.fileNotFound = true;
			
			// set error flag
			res.error = true;
			
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("FileManager", "cannot read file."));
			e.printStackTrace();
			
			// if the error message contains "Permission denied"
			// then set the accessViolation flag to true
			if (e.getMessage().contains("Permission denied"))
				res.accessViolation = true;

			// set error flag
			res.error = true;
		}
		
		return res;
	}
	
	/**
	 * Method used to create a file if it is not already there
	 * 
	 * @param  fileName
	 * @return FileManagerResult containing the list of bytes read or error flagged
	 */
	public FileManagerResult createFile(String fileName) {
		FileManagerResult res = new FileManagerResult();
		
		String fileNameFull = System.getProperty("user.dir") + File.separator + destinationDirectoryStr + File.separator + fileName;
		File file = new File(fileNameFull);
		
		// create a new file if it does not exist
		try {
			if (!file.getParentFile().exists())
				file.getParentFile().mkdirs();
			
			if (file.exists()) {
				res.fileAlreadyExist = true;
				res.error = true;
			}
			else {
				file.createNewFile();
			}
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("FileManager", "cannot read file."));
			e.printStackTrace();
			
			// if the error message contains "Permission denied"
			// then set the accessViolation flag to true
			if (e.getMessage().contains("Permission denied"))
				res.accessViolation = true;
			
			// Check error message to see if the error is about disk full
			if (e.getMessage().contains("not enough space") || e.getMessage().contains("Not enough space")
			        || e.getMessage().contains("No space left"))
			    res.diskFull = true;
			
			// set error flag
			res.error = true;
		}
		
		return res;
	}
	
	/**
	 * Writes file data in bytes to a hard drive with the given file name
	 * 
	 * @param fileName: file name
	 * @param data: file data in byte form
	 * 
	 * Return FileManagerResult containing the errors flags 
	 */
	public FileManagerResult writeFile(String fileName, byte[] data) {
		FileManagerResult res = new FileManagerResult();
		
		String fileNameFull = System.getProperty("user.dir") + File.separator + destinationDirectoryStr + File.separator + fileName;
		File file = new File(fileNameFull);
		
		// write to file
		try {
			// append to previous data on file
			FileOutputStream fileOutputStream = new FileOutputStream(file, true);
			fileOutputStream.write(data);
			fileOutputStream.close();
		} catch (FileNotFoundException e) {	
			
			// if the error message contains "cannot find the file"
			// then set fileNotFound to true
			res.fileNotFound = true;
			
			// set error flag
			res.error = true;
			
		} catch (IOException e) {
			System.err.println(Globals.getErrorMessage("FileManager", "cannot read file."));
			e.printStackTrace();
			
			// if the error message contains "Permission denied"
			// then set the accessViolation flag to true
			if (e.getMessage().contains("Permission denied"))
				res.accessViolation = true;
			
			// set error flag
			res.error = true;
		}
		
		return res;
	}
}
