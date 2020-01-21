package factories.augmentation;

import blogbuilder.ConfigSingle;
import blogbuilder.World;
import elements.LogVar;
import elements.RandVar;
import factories.ElementFactory;

public class LogVarAugmentationFactory extends GenericAugmentationFactory {
	
	public LogVarAugmentationFactory(World baseWorld) {
		super(baseWorld);
	}

	public void insertLogVars(World w) {
		// 1. Clone existing logVars in init.
		int oldLVCount = w.getLogVars().size();
		
		// 2. Add new LogVars.
		ElementFactory lvf = super.baseWorld.getLogVarFactory();
		lvf.insertLogVars(w);
		
		// 3. Replace randomly in existing RandVars.
		for (int i=oldLVCount; i < w.getLogVars().size(); i++) {
			LogVar currLV = w.getLogVars().get(i);
			
			int kickOutRandVars = ConfigSingle.getInstance().getRandom().nextInt(w.searchRealMaxLVOccurence()) + 1 ;
			for (int j = 0; j < kickOutRandVars; j++) {
				RandVar rv = w.selectKickOutRandVar();
				rv.replaceKickOutLogVar(currLV);
			}
		}
	}
	
	@Override
	public ElementFactory getBaseFactory() {
		return super.baseWorld.getLogVarFactory();
	}
}
