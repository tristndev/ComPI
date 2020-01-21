package factories.base;

import java.util.ArrayList;

import blogbuilder.World;
import elements.Factor;
import elements.LogVar;
import elements.RandVar;

/**
 * We use this class for the ParallelFactorArgAugmentation strategy.
 * @author tristan
 *
 */
public class UniformFactorArgsFactory extends RandomSampleFactory{
	
	/**
	 * Insert randvars, like super class, with 1 deviation:
	 * The <i>first</i> RandVar (used to connect all factors) must not have any args.
	 */
	@Override
	public void insertRandVars(World w) {
		// The first RandVar should have no args!
		ArrayList<LogVar> args = new ArrayList<LogVar>();
		w.addRandVar(new RandVar(w.getNextRandVarIndex(), args));
		super.insertRandVars(w);
	}
	
	
	/**
	 * In a world with r RandVars, create 1 Factor for each of the last r-1 RandVars.
	 * The i'th Factor contains two arguments: RandVar i and RandVar 0 (the last one as a connecting randvar)   
	 */
	@Override
	public void insertFactors(World w) {
		int r = w.getSpecContainer().getRandVarCount();
		
		if (r == 1) {
			System.err.println("Just 1 RandVar found in UniformFactorArgsFactory.\\Need >1 RandVars for my strategy.");
			System.exit(1);
		}
		
		for (int i=1; i<r; i++) {
			ArrayList<RandVar> args = new ArrayList<RandVar>();
			
			args.add(w.getRandVars().get(0));
			args.add(w.getRandVars().get(i));
			
			w.addFactor(new Factor(w.getNextFactorIndex(), args));
		}
	}
}
