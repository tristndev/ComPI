package blogSpecs;

import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class that contains all information for one EvidenceSpec i.e. all information
 * that is needed to create the evidence statements for one randvar.
 * 
 * Also contains the methods that are used to get the transitions for the evidence automaton.
 * 
 * @author trist
 *
 */
public class EvidenceSpec {

	/**
	 * RandVar name for this evidence spec.
	 */
	private String randVar;

	/**
	 * Number of groups. Covered guaranteed objects will be divided into this many
	 * groups. Within one group, the objects will all be treated equally.
	 */
	private int groupCount;

	/**
	 * Which percentage of all guaranteed objects shall be used (covered) for
	 * evidence generation?
	 */
	private double coverage;

	/**
	 * Percentage of elements that start with evidence on true.
	 */
	private double percStartTrue;

	/**
	 * Transition probability from false to false.
	 */
	private double probF2F;

	/**
	 * Transition probability from true to true.
	 */
	private double probT2T;

	/**
	 * Percentage of evidence that is shown at the beginning. (All evidence is
	 * calculated, but not all is finally presented in the file.)
	 */
	private double percStartShown;

	/**
	 * Transition probability from shown to shown.
	 */
	private double probShown2Shown;

	/**
	 * Transition probability from hidden to hidden.
	 */
	private double probHide2Hide;
	
	/**
	 * Probability for each element of a group to flip to complementary bool state.
	 */
	private double flipProb;
	
	
	private JSONObject jsonObj;
	
	private Random rnd = new Random(1234567);

	/**
	 * Constructor. Gets a JSONObject spec object and extracts all relevant
	 * information.
	 * 
	 * @param jsonSpec
	 */
	public EvidenceSpec(JSONObject jsonSpec) {
		setRandVar(jsonSpec);
		setGroupCount(jsonSpec);
		setCoverage(jsonSpec);
		setPercStartTrue(jsonSpec);
		setProbF2F(jsonSpec);
		setProbT2T(jsonSpec);
		setPercStartShown(jsonSpec);
		setProbShown2Shown(jsonSpec);
		setProbHide2Hide(jsonSpec);
		setFlipProb(jsonSpec);
		
		this.jsonObj = jsonSpec;
	}
	
	
	public void prettyPrint() {
		String txt = String.format("- RandVar: %s \t\t | GroupCount: %d \t | EvidenceCoverage: %.2f\n", this.randVar, this.groupCount, coverage);
		
		txt += String.format("Start true: %.2f \t | probT2T: %.2f \t | probF2F: %.2f \n", percStartTrue, probT2T, probF2F);
		txt += String.format("Start show: %.2f \t | probS2S: %.2f \t | probH2H: %.2f \n", percStartShown, probShown2Shown, probHide2Hide);
		
		System.out.println(txt);
	}

	/**
	 * Determines the true/false-state for the next timestep t+1 depending of the
	 * state lastBool from timestep t. Based on the attribute probabilities in this
	 * class.
	 * 
	 * @param lastBool
	 *            old (timestep t) boolean
	 * @return boolean new (timestep t+1) boolean
	 */
	public boolean nextTimestepTrueFalse(boolean lastBool) {
		if (lastBool) {
			// True -> True
			return getBoolByProb(this.probT2T);
		} else {
			// P(False -> True) = 1 - P(False -> False)
			return getBoolByProb(1 - this.probF2F);
		}
	}

	/**
	 * Determines the show/hidden-state for the next timestep t+1 depending of the
	 * state lastBool from timestep t. Based on the attribute probabilities in this
	 * class.
	 * 
	 * @param lastBool
	 *            old (timestep t) boolean
	 * @return boolean new (timestep t+1) boolean
	 */
	public boolean nextTimestepShowHidden(boolean lastBool) {
		if (lastBool) {
			// Shown -> Shown (T2T)
			return getBoolByProb(this.probShown2Shown);
		} else {
			// P(Hidden -> Shown) = 1 - P(Hidden -> Hidden)
			return getBoolByProb(1 - this.probHide2Hide);
		}
	}

	/**
	 * Overloaded version for doubleProbabilites. If 0 <= trueProb <= 1 ->
	 * multiplicates by 100 and calls the int-version of this method (see below).
	 * 
	 * @param trueProb
	 *            Probability to return true.
	 * @return a boolean.
	 */
	public boolean getBoolByProb(double trueProb) {
		int intProb;
		if (trueProb <= 1 && trueProb >= 0) {
			intProb = (int) (trueProb * 100);
		} else {
			intProb = (int) trueProb;
		}

		return getBoolByProb(intProb);
	}

	/**
	 * Returns true based on the probability given by the integer trueProbability.
	 * Uses a fixed seed (i.e. 12345).
	 * 
	 * @param trueProb
	 *            integer representing the probability (in %) to return true.
	 * @return a boolean
	 */
	public boolean getBoolByProb(int trueProb) {
		int val = rnd.nextInt(101);
		return val < trueProb;
	}

	// ============================
	// SETTERS
	// ============================

	private void setRandVar(JSONObject jsonSpec) {
		try {
			this.randVar = jsonSpec.getString("randvar");
		} catch (JSONException e) {
			System.err.println("Evidence spec: 'randvar' tag not found or not containing a String. Aborting...");
			System.exit(1);
		}
	}

	private void setGroupCount(JSONObject jsonSpec) {
		try {
			this.groupCount = jsonSpec.getInt("groupCount");
		} catch (JSONException e) {
			System.err.println("Evidence spec: 'groupCount' tag not found or not containing an Integer. Aborting...");
			System.exit(1);
		}
	}

	private void setCoverage(JSONObject jsonSpec) {
		try {
			this.coverage = jsonSpec.getDouble("evidenceCoverage");
		} catch (JSONException e) {
			System.err.println(
					"Evidence spec: 'evidenceCoverage' tag not found or not containing a Decimal (double). Aborting...");
			System.exit(1);
		}
	}

	private void setPercStartTrue(JSONObject jsonSpec) {
		try {
			this.percStartTrue = jsonSpec.getDouble("percStartTrue");
		} catch (JSONException e) {
			System.err.println(
					"Evidence spec: 'percStartTrue' tag not found or not containing a Decimal (double). Aborting...");
			System.exit(1);
		}
	}

	private void setProbF2F(JSONObject jsonSpec) {
		try {
			this.probF2F = jsonSpec.getDouble("probF2F");
		} catch (JSONException e) {
			System.err.println(
					"Evidence spec: 'probF2F' tag not found or not containing a Decimal (double). Aborting...");
			System.exit(1);
		}
	}

	private void setProbT2T(JSONObject jsonSpec) {
		try {
			this.probT2T = jsonSpec.getDouble("probT2T");
		} catch (JSONException e) {
			System.err.println(
					"Evidence spec: 'probT2T' tag not found or not containing a Decimal (double). Aborting...");
			System.exit(1);
		}
	}

	private void setPercStartShown(JSONObject jsonSpec) {
		try {
			this.percStartShown = jsonSpec.getDouble("percStartShown");
		} catch (JSONException e) {
			System.err.println(
					"Evidence spec: 'percStartShown' tag not found or not containing a Decimal (double). Aborting...");
			System.exit(1);
		}
	}

	private void setProbShown2Shown(JSONObject jsonSpec) {
		try {
			this.probShown2Shown = jsonSpec.getDouble("probShown2Shown");
		} catch (JSONException e) {
			System.err.println(
					"Evidence spec: 'probShown2Shown' tag not found or not containing a Decimal (double). Aborting...");
			System.exit(1);
		}
	}

	private void setProbHide2Hide(JSONObject jsonSpec) {
		try {
			this.probHide2Hide = jsonSpec.getDouble("probHide2Hide");
		} catch (JSONException e) {
			System.err.println(
					"Evidence spec: 'probHide2Hide' tag not found or not containing a Decimal (double). Aborting...");
			System.exit(1);
		}
	}
	
	
	private void setFlipProb(JSONObject jsonSpec) {
		try {
			this.flipProb = jsonSpec.getDouble("flipProb");
		} catch (JSONException e) {
			System.err.println(
					"Evidence spec: 'flipProb' tag not found or not containing a Decimal (double). Aborting...");
			System.exit(1);
		}
	}
	
	public JSONObject getJSONObj() {
		return jsonObj;
	}

	public String getRandVar() {
		return randVar;
	}

	public int getGroupCount() {
		return groupCount;
	}

	public double getCoverage() {
		return coverage;
	}

	public double getPercStartTrue() {
		return percStartTrue;
	}

	public double getProbF2F() {
		return probF2F;
	}

	public double getProbT2T() {
		return probT2T;
	}

	public double getPercStartShown() {
		return percStartShown;
	}

	public double getProbShown2Shown() {
		return probShown2Shown;
	}

	public double getProbHide2Hide() {
		return probHide2Hide;
	}
	
	public double getFlipProb() {
		return flipProb;
	}

}
