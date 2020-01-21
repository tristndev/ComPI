package strategies;

import blogbuilder.ConfigSingle;
import blogbuilder.Helpers;
import blogbuilder.Main;
import blogbuilder.SpecContainer;
import blogbuilder.World;
import factories.ElementFactory;
import factories.augmentation.RandVarOccurenceAugmentationFactory;
import factories.base.RandomSampleFactory;

public class RandVarOccAugmStrategy extends WorldCreationStrategy {

	private boolean allQueries = true;

	// We go through these counts in parallel
	int[] params_logVarCounts = { 1, 2, 3, 4 }; // # randVarCount / 5
	// int[] params_randVarCounts = { 10, 50, 100 };
	int[] params_randVarCounts = new int[params_logVarCounts.length];

	// int[] params_factorCounts = {3, 70, 110};
	int[] params_factorCounts = new int[params_logVarCounts.length];

	// We go through these counts as set (all combinations)
	//int[] params_maxRandVarArgs = { 2 };
	int[] params_argsInFactor = { 3, 4, 5, 6, 7 }; // & 10
	//int[] params_maxRandVarOcc = { 3, 4, 5, 6 }; // & 10
	//
	int[] params_domainSizes = {10,100,1000}; // % 100?

	// Additional params

	/**
	 * How often do you want each size to be created?
	 */
	int params_rerollCount = 1;
	
	
	@Override
	public void start() {
		
		for (int i = 0; i < params_logVarCounts.length; i++) {
			params_randVarCounts[i] = (int) Math.floor(params_logVarCounts[i] * 2);
		}
		// 2, 4, 6, 8
		
		for (int i = 0; i < params_logVarCounts.length; i++) {
			params_factorCounts[i] = params_randVarCounts[i] + (int) Math.floor(params_logVarCounts[i] * 1.5);
		}
		
		// 3, 7, 10, 14  

		
		
		paramHandlerRandVarOccAugm(params_logVarCounts, params_randVarCounts, params_factorCounts, params_argsInFactor,
				params_rerollCount);

	}

	public void paramHandlerRandVarOccAugm(int[] logVarCounts, int[] randVarCounts, int[] factorCounts,
			int[] factorArgCounts, int rerollCount) {
		// TODO: Check if factorArgCounts are an increasing sequence of numbers (e.g.
		// [3,4,5,6]) without jumps.

		int maxRandVarArgs = 2;
		int maxRVOccs = 10;

		int counter = 1;

		for (int r = 0; r < rerollCount; r++) {
			for (int i = 0; i < logVarCounts.length; i++) {
				int n_lv = logVarCounts[i];
				int n_rv = randVarCounts[i];
				int n_fac = factorCounts[i];

				World temp_w = null;
				for (int j = 0; j < factorArgCounts.length; j++) {
					ConfigSingle.getInstance().getProgressLogger().logWorldStart(counter);
					SpecContainer sc = new SpecContainer(params_domainSizes, n_lv, n_rv, n_fac, factorArgCounts[j], maxRVOccs, maxRandVarArgs);
					if (j == 0) {
						// 1. Create base world (with random elements)
						temp_w = new World(r, i, this.allQueries, sc);
						ElementFactory baseFac = new RandomSampleFactory();

						temp_w.fillWorld(baseFac, baseFac, baseFac);
					} else {
						// 2. Augment previous world
						ElementFactory augmentFac = new RandVarOccurenceAugmentationFactory(temp_w);
						World new_w = new World(r, i, this.allQueries, sc);

						new_w.fillWorld(augmentFac, augmentFac, augmentFac);

						temp_w = new_w;
					}

					temp_w.createFiles();

					Main.sfw.addLine(temp_w.constructFilePath(), temp_w.getLogVars().size(), temp_w.getRandVars().size(),
							temp_w.getFactors().size(), maxRandVarArgs, temp_w.searchRealMaxRVOccurence(),
							factorArgCounts[j], temp_w.checkAllRVMentioned());
					Main.odl.addLineForWorld(temp_w.constructFilePath(), temp_w);

					ConfigSingle.getInstance().getProgressLogger().logWorldDone(counter, j, temp_w);						
					counter++;
				}
			}
		}
	}

}
