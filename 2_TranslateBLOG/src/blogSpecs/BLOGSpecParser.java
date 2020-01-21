package blogSpecs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

/**
 * Abstract class that wraps useful methods for Specification extraction from model files.
 * @author trist
 *
 */
public abstract class BLOGSpecParser {
	
	// Class attributes
	protected String filePath;

	protected JSONObject specJson;

	protected String specTag;
	protected String specEndTag;


	public BLOGSpecParser (String filename, String specTag, String specEnd) {
		this.filePath = filename;
		
		this.specTag = specTag;
		this.specEndTag = specEnd; 
		
		if (specTag == "[Query Spec]") {
			String specString = extractSpecString();
			this.specJson = specString.length() > 0? convert2json(specString): new JSONObject();
		}
	}
	
	/**
	 * Extracts the query specification part from the BLOG input file. Start and end
	 * of that part is marked with the according tags (slashes instead of
	 * backslashes):
	 * 
	 * <pre>
	 * \*
	 * [Query Spec]
	 * ...
	 * [/Query Spec]
	 * *\
	 * </pre>
	 * 
	 * @return Cleansed String of the part between the tags
	 */
	protected String extractSpecString() {
		List<String> lines = new ArrayList<String>();
		try {
			lines = Files.readAllLines(Paths.get(filePath));
		} catch (IOException e) {
			System.err.print(">> Error importing spec: " + e);
			System.exit(1);
		}

		if (!String.join(" ", lines).contains(specTag) &&
				!String.join(" ", lines).contains(specEndTag)) {
			System.out.printf("No Specification Tag ('%s' and '%s') found in BLOG file. Continuing with default settings.\n",
					specTag, specEndTag);
			return "";
		} else {
			// Find Start and Finish of Query Spec (indicated by the corresponding tags)
			int start = 0;
			int end = lines.size() - 1;
			for (int i = 0; i < lines.size(); i++) {
				if (lines.get(i).contains(specTag)) {
					start = i;
				} else if (lines.get(i).contains(specEndTag)) {
					end = i;
				}
			}

			List<String> queryLines = lines.subList(start + 1, end);
			for (int j = 0; j < queryLines.size(); j++) {
				if (queryLines.get(j).trim().startsWith("//")) {
					queryLines.set(j, queryLines.get(j).trim().substring(2).trim());
				} else {
					queryLines.set(j, queryLines.get(j).trim());
				}
			}
			return String.join("\n", queryLines);
		}
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
	
	
	/**
	 * Getter for parsed JSON Object.
	 * 
	 * @return parsed JSONObject
	 */
	public JSONObject getSpecJson() {
		return this.specJson;
	}

}
