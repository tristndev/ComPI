package logging;

import java.io.PrintStream;

import blogbuilder.Helpers;
import blogbuilder.World;
import elements.RandVar;

public class RVOccDistLogger {
	
	private String filename;
	
	private PrintStream ps;
	
	public RVOccDistLogger(String pathToDir) {
		this.filename = pathToDir + "/" + "RVOccDistLog.csv";
		ps = Helpers.createFilePrintStream(filename);
		this.writeHeaders();
	}
	
	private void writeHeaders() {
		String[] headers = {"file", "rvOccNumber", "count"};
		this.append(String.join(";", headers));
	}
	
	public void addLineForWorld(String filename, World w) {
		int[] counts = getRVOccCountsForWorld(w);
		
		//System.out.println(Arrays.toString(counts));
		
		for (int i = 0; i < counts.length; i++) {
			this.append(String.join(";", new String[] {filename, String.valueOf(i), String.valueOf(counts[i])}));
		}
	}
	
	public int[] getRVOccCountsForWorld(World w) {
		int maxOcc = w.searchRealMaxRVOccurence();
		int[] counts = new int[maxOcc+1];
		
		for (RandVar rv: w.getRandVars()) {
			counts[rv.getOccurrences()]++; 
		}
		return counts;
	}
	
	public void append(String str) {
		this.ps.append(str + "\n");
		this.ps.flush();
	}
	
	public void close() {
		this.ps.close();
	}
}
