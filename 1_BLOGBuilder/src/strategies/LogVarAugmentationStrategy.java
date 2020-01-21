package strategies;

import blogbuilder.ConfigSingle;
import blogbuilder.Helpers;
import blogbuilder.Main;
import blogbuilder.SpecContainer;
import blogbuilder.World;
import factories.ElementFactory;
import factories.augmentation.LogVarAugmentationFactory;
import factories.base.RandomSampleFactory;

public class LogVarAugmentationStrategy extends WorldCreationStrategy {
	
	private boolean allQueries = true;
	
	int[] params_logVarCounts = { 2, 3, 5}; // # randVarCount / 5
	
	// Going through randVarCounts & factorCounts in parallel
	int[] params_randVarCounts = { 4, 4, 5 };
	int[] params_factorCounts = {3, 4, 5};
	

	// We go through these counts as power set (all combinations)
	int[] params_maxRandVarArgs = { 2};
	int[] params_argsInFactor = { 3}; // & 10
	int[] params_maxRandVarOcc = { 3}; // & 10

	int[] params_domainSizes = { 5, 10 }; // % 100?

	// Additional params

	/**
	 * How often do you want each size to be created?
	 */
	int params_rerollCount = 1;


	@Override
	public void start() {		
		paramHandlerLogVarAugm(params_logVarCounts, params_randVarCounts, params_factorCounts, params_argsInFactor, params_maxRandVarArgs, params_maxRandVarOcc, params_rerollCount);
	}

	/**
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
	 * @param rerollCount
	 */
	public void paramHandlerLogVarAugm(int[] logVarCounts, int[] randVarCounts, int[] factorCounts,
			int[] factorArgCounts, int[] maxRandVarArgs, int[] maxRandVarOccurrences, int rerollCount) {

		// Check for equal lengths
		if (!Helpers.checkForEqualLength(new int[][] { randVarCounts, factorCounts })) {
			System.out.println("Counts for randVars & factors are *NOT* of equal length!");
			System.exit(1);
		}

		if (!Helpers.checkForEqualLength(new int[][] { factorArgCounts, maxRandVarArgs, maxRandVarOccurrences })) {
			System.out.println(
					"Counts for factorArgCounts, maxRandVarArgs and maxRandVarOccurrences are *NOT* of equal length!");
			System.exit(1);
		}

		int counter = 1;

		for (int r = 0; r < rerollCount; r++) {
			for (int model_i = 0; model_i < randVarCounts.length; model_i++) {
				int n_rv = randVarCounts[model_i];
				int n_fac = factorCounts[model_i];

				for (int adv_i = 0; adv_i < factorArgCounts.length; adv_i++) {
					int facArgs = factorArgCounts[adv_i];
					int maxRVArgs = maxRandVarArgs[adv_i];
					int maxRVOccs = maxRandVarOccurrences[adv_i];

					World temp_w = null;
					for (int lv_i = 0; lv_i < logVarCounts.length; lv_i++) {
						ConfigSingle.getInstance().getProgressLogger().logWorldStart(counter);
						int n_lv = logVarCounts[lv_i];
						
						SpecContainer sc = new SpecContainer(params_domainSizes, n_lv, n_rv, n_fac, facArgs, maxRVOccs, maxRVArgs);
						if (lv_i == 0) {
							// 1. Create base world (with random elements)
							temp_w = new World(r, model_i, this.allQueries, sc);
							ElementFactory baseFac = new RandomSampleFactory();

							temp_w.fillWorld(baseFac, baseFac, baseFac);
						} else {
							// 2. Augment previous world
							ElementFactory augmentFac = new LogVarAugmentationFactory(temp_w);
							World new_w = new World(r, model_i, this.allQueries,sc);

							new_w.fillWorld(augmentFac, augmentFac, augmentFac);

							temp_w = new_w;
						}

						temp_w.createFiles();
						
						Main.sfw.addLine(temp_w.constructFilePath(), temp_w.getLogVars().size(),
								temp_w.getRandVars().size(), temp_w.getFactors().size(), maxRVArgs,
								temp_w.searchRealMaxRVOccurence(), facArgs, temp_w.checkAllRVMentioned());
						Main.odl.addLineForWorld(temp_w.constructFilePath(), temp_w);

						ConfigSingle.getInstance().getProgressLogger().logWorldDone(counter, lv_i, temp_w);						
						counter++;
					}
				}
			}
		}
	}

}
