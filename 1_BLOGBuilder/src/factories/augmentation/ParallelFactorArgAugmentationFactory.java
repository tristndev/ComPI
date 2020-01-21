package factories.augmentation;

import java.util.ArrayList;
import java.util.stream.Collectors;

import blogbuilder.ConfigSingle;
import blogbuilder.World;
import elements.Factor;
import elements.LogVar;
import elements.RandVar;
import factories.ElementFactory;
import factories.base.RandomSampleFactory;

public class ParallelFactorArgAugmentationFactory extends GenericAugmentationFactory {

	public ParallelFactorArgAugmentationFactory(World baseWorld) {
		super(baseWorld);
	}

	/**
	 * First copies all factors from baseWorld into world w.
	 * Then, for each factor, adds a new RandVar and adds that RandVar to the factor.
	 */
	@Override
	public void insertFactors(World w) {
		w.replaceAllFactors(super.baseWorld.getFactors());

		// For each factor, create a new RandVar
		for(Factor f: w.getFactors()) {
			// Chose LogVar args for new RandVar.
			ArrayList<LogVar> rvArgs = ((RandomSampleFactory) super.baseWorld.getLogVarFactory()).selectLogVarArgsForRandVar(w);
			
			// Create new randvar
			RandVar newRV = new RandVar(w.getNextRandVarIndex(),rvArgs);
			
			// Add RandVar to world & Factor
			w.addRandVar(newRV);
			f.augmentWithRandVar(newRV);
		}
	}

	@Override
	public ElementFactory getBaseFactory() {
		return super.baseWorld.getFactorFactory();
	}
}
