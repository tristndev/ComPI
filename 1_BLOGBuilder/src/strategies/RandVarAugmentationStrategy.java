package strategies;

import blogbuilder.ConfigSingle;
import blogbuilder.Helpers;
import blogbuilder.Main;
import blogbuilder.SpecContainer;
import blogbuilder.World;
import factories.ElementFactory;
import factories.augmentation.RandVarAugmentationFactory;
import factories.base.RandomSampleFactory;

public class RandVarAugmentationStrategy extends WorldCreationStrategy {
	
	// Top level params
	
	/**
	 * Do you want to create all queries or 1 query per randvar?
	 */
	private boolean allQueries = true;
	
	/**
	 * How big should each of the domains be?
	 */
	private int[] domainSizes = {10,100,1000};
	
	/**
	 * How often do you want each size to be created?
	 */
	int rerollCount = 3;
	
	
	/**
	 * Should (old) RandVars from factors be replaced or should the factors be augmented?
	 */
	boolean replaceRandVars = false;
	
	// Param order:
	// 1.) was run with randvar replacement
	int[] params_randVarCounts = 
		//{5,6,7,8,9,10,11,12,13,14,15}; // 1.) 
		{3,4,5,6,7,8,9,10,11,12,14,15,16,17,18,19,20}; // 2.)
	
	
	// We go through these counts in parallel
	int[] params_logVarCounts = 
		//{5}; // 1.)
		{3}; // 2.)
	
	int[] params_factorCounts = 
		//{10}; // 1.)
		{5}; // 2.)
	
	// We go through these counts as power set (all combinations)
	int[] params_maxRandVarArgs = 
		//{2}; // 1.)
		{2}; // 2.)
	
	int[] params_argsInFactor = 
		//{3}; // 1.)
		{5}; // 2.)
	
	int[] params_maxRandVarOcc = 
		//{3}; // 1.)
		{3}; // 2.)
	
	
	@Override
	public void start() {		
		// Additional params
		paramHandlerRandVarAugm(params_logVarCounts, params_randVarCounts, params_factorCounts, params_argsInFactor, params_maxRandVarArgs, params_maxRandVarOcc);
	}

	/**
	 * 
	 * 
	 * The following array groups need to have the same length since we are
	 * iterating over their respective elements in parallel:
	 * 
	 * <pre>
	 * 1. randVarCounts, factorCounts 
	 * 2. factorArgCounts, maxRandVarArgs, maxRandVarOccurrences
	 * </pre>
	 * 
	 * (The respective lengths are checked for equality before start.)
	 * 
	 * @param logVarCounts
	 * @param randVarCounts
	 * @param factorCounts
	 * @param factorArgCounts
	 * @param maxRandVarArgs
	 * @param maxRandVarOccurrences
	 */
	public void paramHandlerRandVarAugm(int[] logVarCounts, int[] randVarCounts, int[] factorCounts,
			int[] factorArgCounts, int[] maxRandVarArgs, int[] maxRandVarOccurrences) {

		// Check for equal lengths
		if (!Helpers.checkForEqualLength(new int[][] { logVarCounts, factorCounts })) {
			System.out.println("Counts for randVars & factors are *NOT* of equal length!");
			System.exit(1);
		}

		if (!Helpers.checkForEqualLength(new int[][] { factorArgCounts, maxRandVarArgs, maxRandVarOccurrences })) {
			System.out.println(
					"Counts for factorArgCounts, maxRandVarArgs and maxRandVarOccurrences are *NOT* of equal length!");
			System.exit(1);
		}
		
		int counter = 1;

		for (int r = 0; r < this.rerollCount; r++) {
			for (int model_i = 0; model_i < logVarCounts.length; model_i++) {
				int n_lv = logVarCounts[model_i];
				int n_fac = factorCounts[model_i];

				for (int adv_i = 0; adv_i < factorArgCounts.length; adv_i++) {
					int facArgs = factorArgCounts[adv_i];
					int maxRVArgs = maxRandVarArgs[adv_i];
					int maxRVOccs = maxRandVarOccurrences[adv_i];

					World temp_w = null;
					for (int rv_i = 0; rv_i < randVarCounts.length; rv_i++) {
						int n_rv = randVarCounts[rv_i];
						ConfigSingle.getInstance().getProgressLogger().logWorldStart(counter);
						SpecContainer sc = new SpecContainer(this.domainSizes, n_lv, n_rv, n_fac, facArgs, maxRVOccs, maxRVArgs);
						if (rv_i == 0) {
							// 1. Create base world (with random elements)
							temp_w = new World(r, model_i, this.allQueries, sc);
							ElementFactory baseFac = new RandomSampleFactory();
							temp_w.fillWorld(baseFac, baseFac, baseFac);
						} else {
							// 2. Augment previous world
							ElementFactory augmentFac = new RandVarAugmentationFactory(temp_w, this.replaceRandVars);
							World new_w = new World(r, model_i, this.allQueries, sc);

							new_w.fillWorld(augmentFac, augmentFac, augmentFac);

							temp_w = new_w;
						}

						temp_w.createFiles();
						
						Main.sfw.addLine(temp_w.constructFilePath(), temp_w.getLogVars().size(),
								temp_w.getRandVars().size(), temp_w.getFactors().size(), maxRVArgs,
								temp_w.searchRealMaxRVOccurence(), facArgs, temp_w.checkAllRVMentioned());
						Main.odl.addLineForWorld(temp_w.constructFilePath(), temp_w);

						ConfigSingle.getInstance().getProgressLogger().logWorldDone(counter, rv_i, temp_w);						
						counter++;
					}
				}
			}
		}
	}

}
