package blogConv.fileFactories;

import blogConv.ModelWrapper;
import blogConv.EvidenceFactories.AbstrEvidenceFactory;
import blogConv.EvidenceFactories.DJTEvidenceFactory;

public abstract class AbstrDynamicFileFactory extends AbstrFileFactory {

	public AbstrDynamicFileFactory(ModelWrapper mw, String modelExtension) {
		super(mw, modelExtension);
	}

	/**
	 * Evidence factory. 
	 */
	protected AbstrEvidenceFactory evidenceFactory;
	
	public AbstrEvidenceFactory getEvidenceFactory() {
		return this.evidenceFactory;
	}
}
