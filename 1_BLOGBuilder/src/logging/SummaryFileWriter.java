package logging;

import java.io.PrintStream;

import blogbuilder.Helpers;

public class SummaryFileWriter {

	private String filename;
	
	private PrintStream ps;
	
	public SummaryFileWriter(String pathToDir) {
		this.filename = pathToDir + "/" + "ModelSummary.csv";
		ps = Helpers.createFilePrintStream(filename);
		this.writeHeaders();
	}
	
	private void writeHeaders() {
		String[] headers = {"filename", "realLV", "realRV", "realFac", "maxRVArgs", "maxRVocc", "facArgs", "allRVMentioned"};
		
		this.append(String.join(";", headers));
	}
	
	public void addLine(String filename, int realLV, int realRV, int realFac, int maxRVArgs, int maxRVocc, int facArgs, boolean allRVMentioned) {
		this.append(filename +";"+ realLV +";"+ realRV +";"+ realFac +";"+ maxRVArgs +";"+ maxRVocc +";"+ facArgs +";"+ allRVMentioned);
	}
	
	
	public void append(String str) {
		this.ps.append(str + "\n");
		this.ps.flush();
	}
	
	public void close() {
		this.ps.close();
	}
}
