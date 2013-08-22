package edu.ucsb.nceas.metacat.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Extension of FileInputSteam that deletes the sourcefile when the
 * InputStream is closed. Should typically be used with temporary files
 * when a more immediate deletion should be performed than is offered
 * by the File.deleteOnExit() method.
 * @author leinfelder
 *
 */
public class DeleteOnCloseFileInputStream extends FileInputStream {
	private File file;

	public DeleteOnCloseFileInputStream(String name) throws FileNotFoundException {
		this(new File(name));
	}

	public DeleteOnCloseFileInputStream(File file) throws FileNotFoundException {
		super(file);
		this.file = file;
	}
	
	/**
	 * Allow access to the underlying file - careful!
	 * @return
	 */
	public File getFile() {
		return file;
	}
	
	
	/**
	 * Delete the file when the InputStream is closed
	 */
	public void close() throws IOException {
		try {
			super.close();
		} finally {
			if (file != null) {
				file.delete();
				file = null;
			}
		}
	}

}
