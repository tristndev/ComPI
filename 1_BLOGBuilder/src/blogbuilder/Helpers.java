package blogbuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

public interface Helpers {

	public static boolean checkForEqualLength(int[][] arrays) {
		int len = arrays[0].length;
		for (int i = 1; i < arrays.length; i++) {
			if (len != arrays[i].length) {
				return false;
			}
		}
		return true;
	}
	
	
	/**
	 * Comments-out a multi line string (java / blog style).
	 * 
	 * @param s String to be commented out
	 * @return commented out String
	 */
	public static String commentOutMultiLine(String s) {
		String[] lines = s.split("\n");
		return "/*\n * " + String.join("\n * ", lines) + "\n */";
	}
	
	
	/**
	 * Creates a PrintStream to a specified path. Handles all possible cases (e.g.
	 * file already exists (-> delete and write to new file), cannot write to file,
	 * ...)
	 * 
	 * @param path to the file
	 * @return PrintStream object.
	 */
	public static PrintStream createFilePrintStream(final String path) {
		try {
			final File file = new File(path);
			if (!file.getParentFile().exists()) {
				System.out.println("   Parent directory does not exist. Creating directories: " + file.getParentFile().getPath());
				file.getParentFile().mkdirs();
			}
			if (!file.createNewFile()) {
				System.out.println("   Cannot create file (already exists): " + file.getPath());
				System.out.println("   Deleting the file and writing to new file.");
				file.delete();
				file.createNewFile();
				// System.exit(1);
			}
			if (!file.canWrite()) {
				System.err.println("   Cannot write to file: " + file.getPath());
				System.exit(1);
			}
			return new PrintStream(new FileOutputStream(file));
		} catch (Exception ex) {
			System.err.println("   Cannot create/open a file for output: " + path);
			System.err.println(ex);
			System.exit(1);
			return null;
		}
	}
}
