package strategies;

import blogbuilder.ConfigSingle;
import blogbuilder.Main;
import blogbuilder.SpecContainer;
import blogbuilder.World;
import factories.ElementFactory;
import factories.base.RandomSampleFactory;

public class RandomSampleStrategy extends WorldCreationStrategy {

	private boolean allQueries = true;

	// We go through these counts in parallel
	int[] params_logVarCounts = { 2, 3, 4, 5, 7, 10, 20 }; // # randVarCount / 5
	// int[] params_randVarCounts = { 10, 50, 100 };
	int[] params_randVarCounts = new int[params_logVarCounts.length];
	
	int[] params_domainSizes = {10, 100, 1000};
	

	// int[] params_factorCounts = {3, 70, 110};
	int[] params_factorCounts = new int[params_logVarCounts.length];
			

	// We go through these counts as power set (all combinations)
	int[] params_maxRandVarArgs = { 2 };
	int[] params_argsInFactor = { 3, 4, 5, 6 }; // & 10
	int[] params_maxRandVarOcc = { 3, 4, 5, 6 }; // & 10

	// Additional params
	/**
	 * How often do you want each size to be created?
	 */
	int params_rerollCount = 1;

	@Override
	public void start() {
		for (int i = 0; i < params_logVarCounts.length; i++) {
			params_randVarCounts[i] = (int) Math.floor(params_logVarCounts[i] * 3);
		}

		for (int i = 0; i < params_logVarCounts.length; i++) {
			params_factorCounts[i] = params_randVarCounts[i] + (int) Math.floor(params_logVarCounts[i] * 1.5);
		}

		
		 paramHandlerAllRandomSample(params_logVarCounts, params_randVarCounts, params_factorCounts,
		 params_maxRandVarArgs, params_maxRandVarOcc, params_argsInFactor);

	}

	/**
	 * Wrapper function that takes all parameter arrays and handles world & model
	 * file creation.
	 * 
	 * Goes parallel through logVar, randVar, factorCounts (i.e. must be of equal
	 * length). Goes power set wise through the remaining args (all possible
	 * combinations).
	 * 
	 * 
	 * @param logVarCounts
	 * @param randVarCounts
	 * @param factorCounts
	 * @param maxRandVarArgs
	 * @param maxRandVarOccurences
	 * @param factorArgCounts
	 */
	public void paramHandlerAllRandomSample(int[] logVarCounts, int[] randVarCounts, int[] factorCounts,
			int[] maxRandVarArgs, int[] maxRandVarOccurences, int[] factorArgCounts) {
		if (logVarCounts.length != randVarCounts.length || randVarCounts.length != factorCounts.length) {
			System.err.println(String.format(
					"Counts for randVars (= %d), logVars (= %d), factors (= %d) are *NOT* of equal length!",
					logVarCounts.length, randVarCounts.length, factorCounts.length));
			System.exit(1);
		}

		int counter = 1;

		for (int j = 0; j < logVarCounts.length; j++) {
			int a = logVarCounts[j];
			int b = randVarCounts[j];
			int c = factorCounts[j];

			for (int d : maxRandVarArgs) {
				for (int e : maxRandVarOccurences) {
					for (int f : factorArgCounts) {
						ConfigSingle.getInstance().getProgressLogger().logWorldStart(counter);
						SpecContainer sc = new SpecContainer(params_domainSizes, a, b, c, f, e, d);
						World w = new World(this.allQueries, sc);
						ConfigSingle.getInstance().getProgressLogger().logWorldSpecParams(w);
						ElementFactory fac = new RandomSampleFactory();
						w.fillWorld(fac, fac, fac);
						// REAL
						ConfigSingle.getInstance().getProgressLogger().logWorldRealParams(w);
						Main.sfw.addLine(w.constructFilePath(), w.getLogVars().size(), w.getRandVars().size(),
								w.getFactors().size(), d, w.searchRealMaxRVOccurence(), f, w.checkAllRVMentioned());
						Main.odl.addLineForWorld(w.constructFilePath(), w);
						w.createFiles();
						counter++;
					}
				}
			}
		}

	}

}
