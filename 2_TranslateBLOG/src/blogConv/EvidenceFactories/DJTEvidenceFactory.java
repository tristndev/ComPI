package blogConv.EvidenceFactories;

import java.util.ArrayList;

import blogConv.ModelWrapper;
import blogSpecs.EvidenceSpec;

public abstract class DJTEvidenceFactory extends AbstrEvidenceFactory{

	public DJTEvidenceFactory(ModelWrapper mw) {
		super(mw);
	}

	public DJTEvidenceFactory(ModelWrapper mw, int maxTimestep) {
		super(mw, maxTimestep);
	}

	public DJTEvidenceFactory(ModelWrapper mw, ArrayList<EvidenceSpec> evSpecList, int maxTimestep) {
		super(mw, evSpecList, maxTimestep);
	}

	public DJTEvidenceFactory(ModelWrapper mw, AbstrEvidenceFactory abstrEF) {
		super(mw, abstrEF);
	}

}
