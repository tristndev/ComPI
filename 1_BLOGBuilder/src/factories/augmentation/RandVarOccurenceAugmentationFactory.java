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

public class RandVarOccurenceAugmentationFactory extends GenericAugmentationFactory {

	public RandVarOccurenceAugmentationFactory(World baseWorld) {
		super(baseWorld);
	}

	/**
	 * First copies all factors from baseWorld into world w. Then, based on a
	 * certain probability adds a randomly chosen randVar to those factors.
	 */
	@Override
	public void insertFactors(World w) {
		w.replaceAllFactors(super.baseWorld.getFactors());

		// For each factor, decide randomly if it should be augmented.
		int prob = 50;
		for (Factor fac : w.getFactors()) {
			int rndInt = ConfigSingle.getInstance().getRandom().nextInt(101);

			if (rndInt > prob) {
				// Augmentation: add randomly sampled randvar.
				ArrayList<RandVar> candidates = new ArrayList<RandVar>(
						w.getRandVars().stream().filter(x -> !fac.getArgs().contains(x)).collect(Collectors.toList()));
				int rndFacInd = 0;
				if (candidates.size() > 0 ) {
					rndFacInd = ConfigSingle.getInstance().getRandom().nextInt(candidates.size());
					fac.augmentWithRandVar(candidates.get(rndFacInd));
				} else {
					// No candidates? -> Pick a random one.
					fac.augmentWithRandVar(w.getRandVars().get(ConfigSingle.getInstance().getRandom().nextInt(w.getRandVars().size())));
				}

				for (RandVar rv : fac.getArgs()) {
					rv.addToFactor(fac);
				}
			}
		}
	}

	@Override
	public ElementFactory getBaseFactory() {
		return super.baseWorld.getFactorFactory();
	}
}
