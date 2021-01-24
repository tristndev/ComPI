package blogConv.EvidenceFactories;

import java.util.ArrayList;

import blog.Type;
import blogConv.ModelWrapper;
import blogSpecs.BLOGEvidenceSpecParser;
import blogSpecs.BLOGQuerySpecParser;
import blogSpecs.EvidenceSpec;

public class DJTStaticEvidenceFactory extends DJTEvidenceFactory{
	
	public DJTStaticEvidenceFactory(ModelWrapper mw, ArrayList<EvidenceSpec> evSpecList, int maxTimestep) {
		super(mw, evSpecList, maxTimestep);
	}
	
	public DJTStaticEvidenceFactory(ModelWrapper mw, int maxTimestep) {
		super(mw, maxTimestep);
	}
	
	public DJTStaticEvidenceFactory(ModelWrapper mw) {
		super(mw);
	}

	public DJTStaticEvidenceFactory(ModelWrapper mw, AbstrEvidenceFactory abstrEF) {
		super(mw, abstrEF);
	}

	/**
	 * Creates the evidence lines for a single given EvidenceSpec object and the according created matrices,
	 * and returns them as a String (each line separated with a newline \n) 
	 * <br>
	 * Format of observations: 
	 * <pre> obs DoR1(@1) = true;
	 * obs DoR2(@3) = false; </pre>
	 * 
	 * @param tfMat
	 * @param shMat
	 * @param groups
	 * @param evSpec EvidenceSpec object to create the lines for.
	 * @param dynamic
	 * @return lines as String.
	 */
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
							ret += String.format("obs %s%d(%s) = %b;\n", evSpec.getRandVar(), t + 1, obj,
											tfMat[g][t]);
						}
					} else {
						// no bool flipping if group size = 1
						ret += String.format("obs %s%d(@%d) = %b;\n", evSpec.getRandVar(), t == 0 ? 1 : 2, t + 1,
								tfMat[g][t]);
					}
				}
			}
		}
		return ret;
	}
}
