package blogSpecs;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class BLOGEvidenceSpecParser extends BLOGSpecParser{

	/**
	 * ArrayList containing EvidenceSpec instances. This is the package that will be handed to the EvidenceFactory.
	 */
	ArrayList<EvidenceSpec> evSpecList = new ArrayList<EvidenceSpec>();
	
	/**
	 * Specification in model file of this format:
	 * 
	 * <pre>
	 * [
	 *  {
	 *    "randvar": "DoR",
	 *    "evidenceCoverage": 0.10,
	 *    "groupCount": 10,
	 *    "percStartTrue": 0.30,
	 *    "probF2F": 0.7,
	 *    "probT2T": 0.3,
	 *    "percStartShown": 0.30,
	 *    "probShown2Shown": 0.7,
	 *    "probHide2Hide": 0.8
	 *   }, 
	 *   ... (next randvar evidence spec as above)
	 *  ]
	 *  </pre>
	 * @param filename
	 */
	public BLOGEvidenceSpecParser(String filename) {
		super(filename, "[Evidence Spec]", "[/Evidence Spec]");
		
		addEvidenceSpecObjects();
	}
	
	
	private void addEvidenceSpecObjects() {		
		String specString = this.extractSpecString();
		if (specString != "") {
			JSONArray arr = new JSONArray(specString);
			
			for (Object obj : arr) {
				evSpecList.add(new EvidenceSpec((JSONObject) obj));			
			}
		}
	}
	
	public ArrayList<EvidenceSpec> getEvidenceSpecs() {
		return evSpecList;
	}	

}
