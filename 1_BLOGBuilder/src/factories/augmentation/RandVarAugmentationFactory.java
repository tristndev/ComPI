package factories.augmentation;

import blogbuilder.ConfigSingle;
import blogbuilder.World;
import elements.Factor;
import elements.RandVar;
import factories.ElementFactory;

public class RandVarAugmentationFactory extends GenericAugmentationFactory {
	
	private boolean replaceRandVars;
	
	public RandVarAugmentationFactory(World baseWorld) {
		super(baseWorld);
	}
	
	/**
	 * Constructor 
	 * 
	 * @param baseWorld Base world to be augmented.
	 * @param replaceRandVars Flag that indicates whether existing RVs should be replaced in factors (=true),
	 * 						or if Factors should be augmented.
	 */
	public RandVarAugmentationFactory(World baseWorld, boolean replaceRandVars) {
		super(baseWorld);
		this.replaceRandVars = replaceRandVars;
	}
	
	@Override
	public void insertRandVars(World w) {
		// 1. Copy existing RandVars without modification in init-method
		int oldRVCount = w.getRandVars().size();
		
		// 2. Add new RandVars (Args: choosing from existing LogVars)
		ElementFactory rvf = super.baseWorld.getRandVarFactory();
		rvf.insertRandVars(w);
		
		// 3. Replace randomly in existing factors (decreasing occurrence counters)
		for (int i = oldRVCount; i < w.getRandVars().size(); i++) {
			RandVar currRV = w.getRandVars().get(i);
		
			
			// Choose how many factors we want to insert the new randvar into.
			int kickOutFactors = ConfigSingle.getInstance().getRandom().nextInt(w.searchRealMaxRVOccurence())+1;
			for (int j = 0; j < kickOutFactors; j++) {
				if (this.replaceRandVars) {
					Factor f = w.selectKickoutFactor();
					f.replaceKickOutRandVar(currRV);
				} else {
					Factor f = w.selectAugmentFactor(currRV);
					f.augmentWithRandVar(currRV);
				}
			}
		}
	}

	@Override
	public ElementFactory getBaseFactory() {
		return super.baseWorld.getRandVarFactory();
	}

}
