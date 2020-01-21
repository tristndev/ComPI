package blogConv;

import java.io.PrintStream;

import blog.Evidence;
import blog.Main;
import blog.Model;

public abstract class AbstrFileFactory {
	
	protected String extension;
	protected ModelWrapper mw;
	protected Evidence ev;
	
	public AbstrFileFactory(ModelWrapper mw, String modelExtension) {
		this.mw = mw;
		this.extension = modelExtension;
		this.ev = null;
	}
	
	public AbstrFileFactory(ModelWrapper mw, String modelExtension, Evidence ev) {
		this.mw = mw;
		this.extension = modelExtension;
		
		if (ev.toString() == (new Evidence()).toString()) {
			this.ev = null;
		} else {
			this.ev = ev;
		}
	}
	
	public abstract String createModelFileString();
	
	public abstract String createEvidenceFileString();
	
	/**
	 * Small file creation wrapper. Includes file content generation call.
	 * @param path String of destination file path.
	 */
	public void saveFile(String path) {
		// 1. Model file
		String translateString = this.createModelFileString();
		
		System.out.println("Output file: "+path);
		saveFile(path, translateString);
		
		// 2. Evidence (if present)
		if (ev != null) {
			String evString = this.createEvidenceFileString();
			String evPath = Main.generateOutputPath(path, "db", "_TranslateBLOG");
			System.out.println("Writing evidence information to file " + evPath);
			PrintStream ps2 = Main.filePrintStream(evPath);
			ps2.append(evString);
			ps2.close();
		}
	}
	
	/**
	 * Atomic file creation method.
	 * 
	 * @param path String path of destination file (will be created / overwritten if necessary)
	 * @param fileContent String content of destination file.
	 */
	public void saveFile(String path, String fileContent) {
		if (Main.printToConsole) {
			System.out.println();
			System.out.println("### Console output of model: ");
			System.out.println(fileContent);
		}

		PrintStream ps = Main.filePrintStream(path);
		ps.append(fileContent);
		ps.close();
	}
	
	
	/**
	 * Helper function. Inserts a given query size Integer into a given file path string.<br>
	 * Example: 
	 * <pre> path/to/file.blog -> path/to/file_100.blog</pre>
	 * @param path String of file path.
	 * @param querySize Integer QuerySize to be inserted
	 * @return modified string
	 */
	protected String insertQuerySizeIntoPath(String path, int querySize) {
		return path.substring(0, path.toLowerCase().indexOf("."+this.extension)) + "_"+querySize + "."+this.extension;
	}
	
	
	
	
}
