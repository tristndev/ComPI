package blogbuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class SpecParser {
	private String filePath;
	private JSONObject jsonObject;
	
	public SpecParser(String filePath) {
		this.filePath = filePath;
		this.jsonObject = this.convert2json(this.readFile(filePath));
	}
	
	private String readFile(String filename) {
		List<String> lines = new ArrayList<String>();
		try {
			lines = Files.readAllLines(Paths.get(filename));
		} catch (IOException e) {
			System.err.print(">> Error importing spec: \n" + e);
			System.exit(1);
		}
		
		return String.join("\n", lines);
	}
	
	
	/**
	 * Function that converts a JSON object string to JSONObject Exits the program
	 * if an error occurs during parsing.
	 * 
	 * @param str Cleansed string of the object from the file.
	 * @return JSONObject
	 */
	private JSONObject convert2json(String str) {
		JSONObject obj = null;
		try {
			obj = new JSONObject(str);
		} catch (Exception e) {
			System.err.println("Error parsing the JSON Query Specification: \n" + e);
			System.exit(1);
		}
		return obj;
	}
	
	
	public int getRandVarCount() {
		// TODO: implement with defaults
		return 0;
	}
	
	// TODO: (SpecParser) Implement remaining getters (with defaults)
	
	
	

}
