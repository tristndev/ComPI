package factories.augmentation;

import java.util.ArrayList;
import java.util.Arrays;

import com.rits.cloning.Cloner;

import blogbuilder.ConfigSingle;
import blogbuilder.World;
import elements.Factor;
import elements.LogVar;
import elements.RandVar;
import factories.ElementFactory;

public class IncByWorldFactory extends GenericAugmentationFactory  {

	World incWorld;
	
	public IncByWorldFactory(World baseWorld, World incWorld) {
		super(baseWorld);
		
		Cloner cloner = new Cloner();
		
		this.incWorld = cloner.deepClone(incWorld);
	}
	
	@Override
	public ElementFactory getBaseFactory() {
		// TODO Fill getBaseFactory.
		throw new UnsupportedOperationException("IncByWorldFactory.getBaseFactory() not yet implemented.");
		//return null;
	}
	
	
	/**
	 * Handles elements (lv, rv, factors) insertion of world w after element init (i.e. copy from base world) was already done!
	 * => We do the incWorld - increment here!
	 */
	@Override
	public void insertElements(World w) {
		mergeWorlds(w, this.incWorld);
	}
	
	private World mergeWorlds(World w1, World w2) { 
		// Merge LogVars:
		for (int i = 0; i<w2.getLogVars().size(); i++) {
			LogVar currLV = w2.getLogVars().get(i);
			
			currLV.updateIndex(w1.getNextLogVarIndex());
			w1.addLogVar(currLV);
		}
		
		int rvCountW1 = w1.getRandVars().size();
		// Merge RandVars:
		for (int i = 0; i <w2.getRandVars().size(); i++) {
			RandVar currRV = w2.getRandVars().get(i);
			currRV.updateIndex(w1.getNextRandVarIndex());
			w1.addRandVar(currRV);
		}
		
		// Merge Factors:
		for (int i=0; i<w2.getFactors().size(); i++) {
			Factor currFac = w2.getFactors().get(i);
			currFac.updateIndex(w1.getNextFactorIndex());
			w1.addFactor(currFac);
		}
		
		// Create connection factor between worlds
		RandVar rndRandVarW1 = w1.getRandVars().get(ConfigSingle.getInstance().getRandom().nextInt(rvCountW1));
		RandVar rndRandVarW2 = w1.getRandVars().get(ConfigSingle.getInstance().getRandom().nextInt(w2.getRandVars().size()) + rvCountW1);
		
		Factor connectionFactor = new Factor(w1.getNextFactorIndex(), new ArrayList<RandVar>(Arrays.asList(rndRandVarW1, rndRandVarW2)));
		w1.addFactor(connectionFactor);
		
		return w1;
	}
	
}
