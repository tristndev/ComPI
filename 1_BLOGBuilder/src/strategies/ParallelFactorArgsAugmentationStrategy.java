package strategies;

import blogbuilder.ConfigSingle;
import blogbuilder.Main;
import blogbuilder.SpecContainer;
import blogbuilder.World;
import factories.ElementFactory;
import factories.augmentation.ParallelFactorArgAugmentationFactory;
import factories.base.UniformFactorArgsFactory;

public class ParallelFactorArgsAugmentationStrategy extends WorldCreationStrategy {

	private boolean allQueries = true;
	
	
	private boolean bigOne = true;
	
	int[] params_domainSizes = {10, 100, 1000};
	
	/**
	 * We go through these counts in parallel 
	 */
	//int[] params_logVarCounts =  {1,1,2,2,2};
	//int[] params_randVarCounts = {2,3,3,4,5};
	int[] params_logVarCounts = (bigOne) ? new int[]{4} : new int[]{2,2,2};
	int[] params_randVarCounts = bigOne? new int[]{11} : new int[]{3,4,5};
	/*
	// Automatic RandVar counts
	int[] params_randVarCounts = new int[params_logVarCounts.length];
	for (int i = 0; i < params_logVarCounts.length; i++) {
		params_randVarCounts[i] = (int) Math.floor(params_logVarCounts[i] * 5);
	}
	*/
	
	// >> Attention:
	// The counts for Factors do not need to be specified, since they are implicitly
	// derived from the randVar counts. 
	
	/**
	 * How often should the base world be augmented? In a baseworld with r
	 * RandVars,each augmentation step leads to an increase by (r-1) RandVars (which
	 * are distributed on the (r-1) factors - 1 per factor).
	 */
	int augmSteps = bigOne? 15 : 16;
	
	/**
	 * How often do you want each size to be created?
	 */
	int params_rerollCount = 3;
	
	int maxRandVarArgs = 2;
	
	@Override
	public void start() {
		paramHandlerParaFacArgAugm(params_logVarCounts, params_randVarCounts, augmSteps, maxRandVarArgs, params_rerollCount);
	}

	public void paramHandlerParaFacArgAugm(int[] logVarCounts, int[] randVarCounts, int augmSteps, int maxRandVarArgs, int rerollCount) {
		int counter = 1;
		
		if (logVarCounts.length != randVarCounts.length) {
			System.err.println("Specified arrays logVarCounts and randVarCounts need to be of equal lenght which they are not!");
			System.exit(1);
		}
		
		for (int r = 0; r < rerollCount; r++) {
			
				
				for (int i = 0; i < logVarCounts.length; i++) {
					int n_lv = logVarCounts[i];
					int n_rv = randVarCounts[i];
					int n_fac = n_rv - 1;
					// RV1 will always be in all factors.
					int maxRVOccs = n_fac;
					
					World temp_w = null;
					
					for (int augmStep=0; augmStep < augmSteps; augmStep++) {
						ConfigSingle.getInstance().getProgressLogger().logWorldStart(counter);
						
						int factorArgCount = augmStep + 2;
						
						if (augmStep == 0) {
							// 1. Create base world (with random elements)
							
							SpecContainer sc = new SpecContainer(params_domainSizes, n_lv, n_rv, n_fac, factorArgCount, maxRVOccs, maxRandVarArgs);
							temp_w = new World(r, i, allQueries, sc);
							ElementFactory baseFac = new UniformFactorArgsFactory();

							temp_w.fillWorld(baseFac, baseFac, baseFac);
						} else {
							// 2. Augment previous world
							ElementFactory augmentFac = new ParallelFactorArgAugmentationFactory(temp_w);
							
							n_rv = randVarCounts[i] + (augmStep * n_fac); 
							SpecContainer sc = new SpecContainer(params_domainSizes, n_lv, n_rv, n_fac, factorArgCount, maxRVOccs, maxRandVarArgs);
							
							World new_w = new World(r, i, this.allQueries, sc);

							new_w.fillWorld(augmentFac, augmentFac, augmentFac);

							temp_w = new_w;
						}
						
						temp_w.createFiles();
						
						Main.sfw.addLine(temp_w.constructFilePath(), temp_w.getLogVars().size(), temp_w.getRandVars().size(),
								temp_w.getFactors().size(), maxRandVarArgs, temp_w.searchRealMaxRVOccurence(),
								factorArgCount, temp_w.checkAllRVMentioned());
						Main.odl.addLineForWorld(temp_w.constructFilePath(), temp_w);

						ConfigSingle.getInstance().getProgressLogger().logWorldDone(counter, augmStep, temp_w);						
						counter++;
					}
				}
			
		}
	}

}
