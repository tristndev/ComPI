package blogbuilder;

import logging.RVOccDistLogger;
import logging.SummaryFileWriter;
import strategies.*;

public class Main {

	public static String outputPath = "out";
	public static SummaryFileWriter sfw = new SummaryFileWriter(outputPath);
	public static RVOccDistLogger odl = new RVOccDistLogger(outputPath);

	public static void main(String[] args) {
		ConfigSingle.getInstance().verbose = true;

		//WorldCreationStrategy strat = new RandomSampleStrategy();
		//WorldCreationStrategy strat = new RandVarOccAugmStrategy();
		//WorldCreationStrategy strat = new LogVarAugmentationStrategy();
		//WorldCreationStrategy strat = new RandVarAugmentationStrategy();
		//WorldCreationStrategy strat = new FactorAugmentationStrategy();
		// WorldCreationStrategy strat = new IncByWorldStrategy();
		//WorldCreationStrategy strat = new ParallelFactorArgsAugmentationStrategy();
		WorldCreationStrategy strat = new IncDegreeStrategy();

		strat.start();
		strat.createWorldCreationSummary();
		
		ConfigSingle.getInstance().getProgressLogger().logFinalReport();
		
		sfw.close();
		odl.close();
	}
}
