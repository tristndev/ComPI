package factories.augmentation;

import blogbuilder.World;
import elements.Factor;
import factories.ElementFactory;

/**
 * This abstract class contains the generic methods for copying elements
 * (LogVars, RandVars, Factors) from a given baseWorld into a new world.
 * 
 * A subclass (i.e. concrete augmentation factory) can then just implement 
 * those methods where things shall be handled differently.
 */
public abstract class GenericAugmentationFactory implements ElementFactory {

	protected World baseWorld;

	public GenericAugmentationFactory(World baseWorld) {
		this.baseWorld = baseWorld;
	}

	/**
	 * Default insertLogVars behaviour: Do nothing.
	 */
	public void insertLogVars(World w) {
	}

	/**
	 * Default insertRandVars behaviour: Do nothing.
	 */
	public void insertRandVars(World w) {
		
	}

	/**
	 * Default insertFactors behaviour: Do nothing.
	 */
	public void insertFactors(World w) {
		
	}
	
	public void initLogVars(World w) {
		w.addAllLogVars(baseWorld.getLogVars());
	}
	
	/**
	 * Copies all randVars from given baseWorld into world w.
	 */
	public void initRandVars(World w) {
		w.addAllRandVars(baseWorld.getRandVars());
	}
	
	/**
	 * Copies all factors from given baseWorld into world w.
	 */
	public void initFactors(World w) {
		for (Factor fac : baseWorld.getFactors()) {
			w.addFactor(fac);
		}
	}
}