package blogConv.EvidenceFactories;

import java.util.ArrayList;

import blogConv.ModelWrapper;
import blogSpecs.EvidenceSpec;

public class DMLNEvidenceFactory extends AbstrEvidenceFactory {

	public DMLNEvidenceFactory(ModelWrapper mw, ArrayList<EvidenceSpec> evSpecList, int maxTimestep) {
		super(mw, evSpecList, maxTimestep);
	}
	
	public DMLNEvidenceFactory(ModelWrapper mw, int maxTimestep) {
		super(mw, maxTimestep);
	}
	
	public DMLNEvidenceFactory(ModelWrapper mw) {
		super(mw);
	}

	public DMLNEvidenceFactory(ModelWrapper mw, AbstrEvidenceFactory evidenceFactory) {
		super(mw, evidenceFactory);
	}

	/**
	 * Observation format:
	 * <pre>  smokes(2,A)
	 * !friends(0,A,B)</pre>
	 */
	@Override
	protected String createLinesFromSingleEvidenceSpec(boolean[][] tfMat, boolean[][] shMat,
			ArrayList<ArrayList<String>> groups, EvidenceSpec evSpec, int currentMaxTimestep) {
		String ret = "";
		for (int g = 0; g < tfMat.length; g++) {
			//for (int t = 0; t < tfMat[0].length; t++) {
			for (int t = 0; t < currentMaxTimestep; t++) {
				// if evidence is shown...
				if (shMat[g][t]) {
					if (groups.size() > 1 || extractFirstNonTimeArg(evSpec) != null) {
						for (String obj : groups.get(g)) {
							String pref = tfMat[g][t] ? "": "!"; // boolean prefix '!' for false
							ret += String.format("%s%s(%d, %s)\n", pref, evSpec.getRandVar(), t+1, obj.toUpperCase());
						}
					} else {
						String pref = tfMat[g][t] ? "": "!"; // boolean prefix '!' for false
						ret += String.format("%s%s(%d)\n", pref, evSpec.getRandVar(), t+1);
					}
				}
			}
		}
		return ret;
	}
	
	/**
	 * Wrapper function that creates all available evidence lines.
	 * 
	 * @return String containing all evidence lines, separated by line breaks.
	 */
	@Override
	public String createEvidenceLines(int currentMaxTimestep) {
		String res = evSpecList.size() > 0?"// Evidence\n" : "";
		for (int i=0; i < evSpecList.size(); i++) {
			res += this.createLinesFromSingleEvidenceSpec(tfMats.get(i), shMats.get(i), groupsList.get(i), evSpecList.get(i), currentMaxTimestep);
		}
		return res;
	}

}
