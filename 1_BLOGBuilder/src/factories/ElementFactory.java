package factories;

import blogbuilder.World;

public interface ElementFactory {

	/**
	 * Creates the logvars.
	 * 
	 * @param w world object to create the objects for.
	 */
	public void insertLogVars(World w);

	/**
	 * Creates the randvars.
	 * 
	 * @param w world object to create the objects for.
	 */
	public void insertRandVars(World w);

	/**
	 * Creates the (par)factors.
	 * 
	 * @param w world object to create the objects for.
	 */
	public void insertFactors(World w);

	/**
	 * Returns this factory's base factory, i.e. the factory that was used to initially fill a base world that was later augmented.
	 * @return base factory that was used for initial filling.
	 */
	public ElementFactory getBaseFactory();
	
	/**
	 * Default behavior: Throw exception:
	 * @param w
	 */
	public default void insertElements(World w) {
		throw (new UnsupportedOperationException("insertElements(world) not implemented for the chosen kind of augmentation factory."));
	}
	
	/**
	 * One of the three init*() methods that is called before the insert*() methods.
	 * Needed for e.g. copying all LogVars, RandVars and Factors from a baseWorld before we augment.
	 * 
	 * @param w World to be initialized.
	 */
	public void initLogVars(World w);
	
	/**
	 * One of the three init*() methods that is called before the insert*() methods.
	 * Needed for e.g. copying all LogVars, RandVars and Factors from a baseWorld before we augment.
	 * 
	 * @param w World to be initialized.
	 */
	public void initRandVars(World w);
	
	/**
	 * One of the three init*() methods that is called before the insert*() methods.
	 * Needed for e.g. copying all LogVars, RandVars and Factors from a baseWorld before we augment.
	 * 
	 * @param w World to be initialized.
	 */
	public void initFactors(World w);
}
