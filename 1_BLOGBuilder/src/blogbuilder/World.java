package blogbuilder;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import elements.Factor;
import elements.LogVar;
import elements.RandVar;
import factories.ElementFactory;

public class World {

	/**
	 * Flag that indicates whether all possible queries hall be created or just 1 query for each randvar.
	 */
	private boolean allQueries;

	/**
	 * Flag that indicates whether  queries shall be created or not.
	 */
	private boolean noQueries;

	/**
	 * Index of current reroll.
	 */
	private int rerollIndex = -1;
	
	
	/**
	 * Current iteration index.
	 */
	private int currentIterationIndex = -1;
	
	/**
	 * Container for spec data.
	 */
	private SpecContainer specci;
	
	/**
	 * List of all available logVars.
	 */
	private ArrayList<LogVar> logVars = new ArrayList<LogVar>();

	/**
	 * List of all available randVars.
	 */
	private ArrayList<RandVar> randVars = new ArrayList<RandVar>();

	/**
	 * List of all available factors.
	 */
	private ArrayList<Factor> factors = new ArrayList<Factor>();

	/**
	 * LogVar factory that was used to fill this world's base world.
	 */
	private ElementFactory baseLogVarFactory;

	/**
	 * RandVar factory that was used to fill this world's base world.
	 */
	private ElementFactory baseRandVarFactory;

	/**
	 * Factor factory that was used to fill this world's base world.
	 */
	private ElementFactory baseFactorFactory;

	/**
	 * Constructor with self explanatory arguments.
	 * World's domainSize will be set to default value(3).
	 *
	 * @param allQueries
	 * @param SpecContainer
	 */
	public World(boolean allQueries, SpecContainer sc) {
		this.noQueries = false;
		this.allQueries = allQueries;
		this.specci = sc;
	}

	/**
	 * Constructor with self explanatory arguments.
	 *
	 * @param rerollIndex
	 * @param currentIterationIndex
	 * @param allQueries
	 * @param specContainer
	 * @param noQueries
	 */
	public World(int rerollIndex, int currentIterationIndex, boolean allQueries,
				 SpecContainer specContainer, boolean noQueries) {
		this.rerollIndex = rerollIndex;
		this.currentIterationIndex = currentIterationIndex;
		this.allQueries = allQueries;
		this.specci = specContainer;
		this.noQueries = noQueries;
	}

	/**
	 * Constructor with self explanatory arguments.
	 *
	 * @param rerollIndex
	 * @param currentIterationIndex
	 * @param allQueries
	 * @param specContainer
	 */
	public World(int rerollIndex, int currentIterationIndex, boolean allQueries,
				 SpecContainer specContainer) {
		this.rerollIndex = rerollIndex;
		this.currentIterationIndex = currentIterationIndex;
		this.allQueries = allQueries;
		this.specci = specContainer;
		this.noQueries = false;
	}


	/**
	 * Fills the world with logVars, randVars & factors based on the given
	 * ElementFactories & saves the used factories as attributes.
	 * 
	 * @param logVarFac  ElementFactory for logvar creation
	 * @param randVarFac ElementFactory for randvar creation
	 * @param factorFac  ElementFactory for factor creation
	 */
	public void fillWorld(ElementFactory logVarFac, ElementFactory randVarFac, ElementFactory factorFac) {
		// 0. Initialize elements
		logVarFac.initLogVars(this);
		randVarFac.initRandVars(this);
		factorFac.initFactors(this);
		
		// 1. create logvars
		logVarFac.insertLogVars(this);
		this.baseLogVarFactory = logVarFac.getBaseFactory();

		// 2. create randvars with random sampled 0-2 logvars
		randVarFac.insertRandVars(this);
		this.baseRandVarFactory = randVarFac.getBaseFactory();

		// 3. create at least 1 (par)factor per randvar, else factorCount factors,
		// with max. factorArgCount arguments.
		factorFac.insertFactors(this);
		this.baseFactorFactory = factorFac.getBaseFactory();
		
		// 4. Check if allRandVars are mentioned (in at least 1 factor)
		// 	  If that's not the case -> notify ProgressLogger
		if(!this.checkAllRVMentioned()) {
			ConfigSingle.getInstance().getProgressLogger().addToAllMentionedFalseFiles(this.constructFilePath());
		}
	}
	
	/**
	 * Fills the world with logVars, randVars & factors based on the given 
	 * ElementFactory (needs to be universal for all three elements!).
	 * 
	 * @param augFac
	 */
	public void fillWorld(ElementFactory augFac) {
		// 0. Initialize elements
		augFac.initLogVars(this);
		augFac.initRandVars(this);
		augFac.initFactors(this);
		
		// 1. Create Elements
		augFac.insertElements(this);
		
		
		// 2. Check if allRandVars are mentioned (in at least 1 factor)
		// 	  If that's not the case -> notify ProgressLogger
		if(!this.checkAllRVMentioned()) {
			ConfigSingle.getInstance().getProgressLogger().addToAllMentionedFalseFiles(this.constructFilePath());
		}
	}

	/**
	 * Wrapper function that calls all <code>asLine()</code> methods in the right
	 * order and constructs the content of a blog file.
	 * 
	 * Also replaces the placeholder occurrences with the number given in the domainSize argument.
	 * 
	 * @param domainSize number to be inserted for all domain sizes.
	 * @return String of BLOGFile contents.
	 */
	public String toBLOGString(int domainSize) {
		String ret = "";

		// 1. LogVars
		for (LogVar lv : this.logVars) {
			ret += lv.asLine();
		}
		ret += "\n\n";

		// 2. RandVars
		for (RandVar rv : this.randVars) {
			ret += rv.asLine();
		}
		ret += "\n\n";

		// 3. Parfactors
		for (Factor fac : this.factors) {
			ret += fac.asLine();
		}
		
		if (!this.noQueries) {
			ret += "\n\n";
			// 4. Create query lines (1 per randVar)
			ret += this.createQueryLines();
		}

		// 5. Insert domain size
		ret = ret.replace("[XXX]", String.format("[%d]", domainSize));
		
		return ret;
	}

	
	/**
	 * Creates the query lines (1 per randvar).
	 * 
	 * @return String of query lines
	 */
	private String createQueryLines() {
		String ret = "";
		for (RandVar rv : randVars) {
			if (rv.getArgs().size() > 0) {
				// Parfactor
				ArrayList<String> args = new ArrayList<String>(
						rv.getArgs().stream().map(x -> ("x" + x.getIndex() +"x" + "1")).collect(Collectors.toList()));
				String argString = String.join(",", args);
				ret += String.format("query %s(%s);\n", rv.constructName(), argString);
			} else {
				// Factor
				ret += String.format("query %s;\n", rv.constructName());
			}

			if (!this.allQueries) {
				break;
			}
		}
		return ret;
	}

	/**
	 * Adds a factor or parfactor (i.e. a factor object) to the world's list of
	 * factor objects.
	 * 
	 * Sideeffects for Parfactors: 1. Increase occurences for randvars 2. Add
	 * connected randVars to randVar objects.
	 * 
	 * Sideeffects for Factors: 1. Increase occurence 2. Set hasFactor flag to true
	 * 
	 * @param f
	 */
	public void addFactor(Factor f) {
		if (f.getArgs().size() > 1) {
			// Parfactors
			for (RandVar rv : f.getArgs()) {
				rv.addToFactor(f);
			}
			this.factors.add(f);
		} else if (f.getArgs().size() == 1) {
			// Factors
			f.getArgs().get(0).addToFactor(f);
			f.getArgs().get(0).setHasFactor(true);
			this.factors.add(f);
		} else {
			if (ConfigSingle.getInstance().verbose) {
				System.err.printf(" > Warning: Unknown factor size for factor #%d: %d. Factor not created.\n", f.getIndex(),
						f.getArgs().size());
			}
		}
	}

	/**
	 * Returns a summary string describing the specified parameters and the model's
	 * achieved parameters.
	 * 
	 * @return Summary string
	 */
	public String writeWorldSummary() {
		String spec = "### BLOGBuilder SUMMARY ###\n>> Spec:\n";

		spec += String.format(
				"\tlogVarCount: %d\n\trandVarCount: %d \n\tfactorCount: %d \n\tmaxFactorArgs: %d"
						+ "\n\tmaxRandVarArgs: %d \n\tmaxRandVarOccurences: %d",
				specci.getLogVarCount(), specci.getRandVarCount(), specci.getFactorCount(), 
				specci.getFactorArgCount(), specci.getMaxRandVarArgs(), specci.getMaxRandVarOccurrences());

		int realRandVarOccurences = searchRealMaxRVOccurence();
		boolean allMentioned = checkAllRVMentioned();

		String real = ">> Model:\n";

		real += String.format(
				"\treal logVarCount: %d \n\treal randVarCount: %d \n\treal factorCount: %d"
						+ "\n\treal maxRandVarOccurences: %d \n\tallMentioned: %b",
				logVars.size(), randVars.size(), factors.size(), realRandVarOccurences, allMentioned);
		
		String coOccurrenceMat =">> RandVar co-occurrence matrix:\n";
		if (this.randVars.size()<=30) {
			coOccurrenceMat += this.createRandVarCoOccurrenceMatrix();
		} else {
			coOccurrenceMat += "   Not created due to count of RandVars > 30.";
		}
		

		return spec + "\n" + real + "\n" + coOccurrenceMat;
	}

	
	/**
	 * Creates the a "RandVar Co-Occurrence Matrix" that looks similar like this:
	 * <pre>
	 *      RV1 RV2 
	 *  RV1   2   1 
	 *  RV2   1   2 
	 * </pre>
	 * 
	 * It gives a short overview which RandVar occurrs how often with the other RandVars. 
	 * The diagonal values of the RandVars correspond to their occurrence count (... "how often they occurr with themselves")
	 * 
	 * @return String representation of rv co-occurrence matrix  
	 */
	private String createRandVarCoOccurrenceMatrix() {
		int[][] mat = new int[this.randVars.size()][this.randVars.size()];
		
		// Calculate values of matrix.
		for (int i=0; i<this.randVars.size(); i++) {
			RandVar currRV = this.randVars.get(i);
			for (Factor fac: currRV.getOccurrenceFactors()) {
				for (RandVar argRV : fac.getArgs()) {
					int argInd = argRV.getIndex() -1;
					mat[i][argInd]++;
				}
			}
		}
		
		// Names of RVs for col / row labels
		int[] labelInd = new int[this.randVars.size()];
		for (int i=0; i<this.randVars.size(); i++) {
			labelInd[i] = this.randVars.get(i).getIndex();
		}
		
		// Create string represenation of matrix.
		int digitNum = 4;
		
		// 1st row (labels)
		String matString = String.join("", Collections.nCopies(digitNum, " "));
		for (int i=0; i<labelInd.length; i++) {
			matString += this.prepRVString(labelInd[i]);
		}
		
		// Remaining values
		matString += "\n";
		for (int i=0; i<mat.length;i++) {
			for (int j = -1; j<mat[0].length; j++) {
				if (j==-1) {
					matString += this.prepRVString(labelInd[i]);
				} else {
					matString += String.format("%"+String.valueOf(digitNum)+"d ", mat[i][j]);
				}
				if (j == mat[0].length-1) {
					matString += "\n";
				}
			}
		}
		
		return matString;
	}
	
	private String prepRVString(int index) {
		int len = String.valueOf(index).length();
		return String.join("", Collections.nCopies(3-len, " ")) + "RV" + index;
	}

	/**
	 * Helper Method for summary writing: Checks if all randvars have been mentioned
	 * at least once in a factor.
	 * 
	 * @return boolean true if all have been mentioned.
	 */
	public boolean checkAllRVMentioned() {
		// TODO: Add `forceAllMentioned` toggle?
		
		boolean b = true;
		for (RandVar rv : this.randVars) {
			if (rv.getOccurrences() == 0) {
				b = false;
				break;
			}
		}
		return b;
	}
	
	/**
	 * Goes through all RandVars and collects the ones with occurrence == 0
	 * @return ArrayList of non mentioned RandVars
	 */
	public ArrayList<RandVar> searchNonMentionedRV() {
		ArrayList<RandVar> rvs = new ArrayList<RandVar>();
		for (RandVar rv: this.randVars) {
			if (rv.getOccurrences() == 0) {
				rvs.add(rv);
			}
		}
		return rvs;
	}
	
	
	/**
	 * Compares the parameters given in this world's specification with the worlds real values. 
	 * Side effect: If any spec deviations are found, adds this world to the ProgressLoggers list of specDeviation files (including the parameters which show a deviation)
	 * 
	 * @return A spec deviation string to be printed to the console.
	 */
	public String checkForSpecDeviations() {
		String out = "> SpecDeviations:\n";
		String deviations = "";
		boolean noDeviation = true;
		// LogVars
		if (specci.getLogVarCount() != this.logVars.size()) {
			out += String.format("  LogVarCount - Spec: %d, World: %d\n", specci.getLogVarCount(), this.logVars.size());
			noDeviation = false;
			deviations += String.format("LogVarCount (delta: %+d), ", this.logVars.size() - specci.getLogVarCount());
		}
		
		// RandVars
		if (specci.getRandVarCount() != this.randVars.size()) {
			out += String.format("  RandVarCount - Spec: %d, World: %d\n", specci.getRandVarCount(), this.randVars.size());
			noDeviation = false;
			deviations += String.format("RandVarCount (delta: %+d), ", this.randVars.size() - specci.getRandVarCount());
		}
		
		// Factors
		if (specci.getFactorCount() != this.factors.size()) {
			out += String.format("  FactorCount - Spec: %d, World: %d\n", specci.getFactorCount(), this.factors.size());
			noDeviation = false;
			deviations += String.format("FactorCount (delta: %+d), ", this.factors.size() - specci.getFactorCount());
		}
		
		// FactorArgCount
		if (specci.getFactorArgCount() != this.searchRealMaxFactorArgCount()) {
			out += String.format("  MaxFactorArgCount - Spec: %d, World: %d\n", specci.getFactorArgCount(), this.searchRealMaxFactorArgCount());
			noDeviation = false;
			deviations += String.format("FactorArgCount (delta: %+d), ", this.searchRealMaxFactorArgCount() - specci.getFactorArgCount());
		}
		
		// MaxRandVarOccurrences
		if (specci.getMaxRandVarOccurrences() != this.searchRealMaxRVOccurence()) {
			out += String.format("  MaxRandVarOccurrences - Spec: %d, World: %d\n", specci.getMaxRandVarOccurrences(), this.searchRealMaxRVOccurence());
			noDeviation = false;
			deviations += String.format("MaxRandVarOccurrences (delta: %+d), ", this.searchRealMaxRVOccurence() - specci.getMaxRandVarOccurrences());
		}
		
		// MaxRandVarArgs
		if (specci.getMaxRandVarArgs() != this.searchRealMaxRVArgs()) {
			out += String.format("  MaxRandVarArgs - Spec: %d, World: %d\n", specci.getMaxRandVarArgs(), this.searchRealMaxRVArgs());
			noDeviation = false;
			deviations += String.format("MaxRandVarArgs (delta: %+d), ", this.searchRealMaxRVArgs() - specci.getMaxRandVarArgs());
		}
		
		if (noDeviation) {
			return "";
		} else {
			ConfigSingle.getInstance().getProgressLogger().addToSpecDeviationFiles(this.constructFilePath() + ": "+ deviations.substring(0, deviations.length()-2));
			return out;
		}
	}

	/**
	 * Helper Method for summary writing: searches the real maximal randvar
	 * occurrence in (par)factors.
	 * 
	 * @return int: number of maximum randvar occurrences
	 */
	public int searchRealMaxRVOccurence() {
		int max = 0;
		for (RandVar rv : this.randVars) {
			if (rv.getOccurrences() > max) {
				max = rv.getOccurrences();
			}
		}
		return max;
	}
	
	/**
	 * Helper method: Counts LogVar Occurrences and returns the array. 
	 * 
	 * @return int: number of maximum logvar occurrences.
	 */
	public int searchRealMaxLVOccurence() {
		int lvCount = this.logVars.size();
		int[] counts = new int[lvCount]; // default = 0
		
		for (RandVar rv : this.randVars) {
			for (LogVar lv : rv.getArgs()) {
				int countIndex = lv.getIndex()-1;
				counts[countIndex]++;
			}
		}
		
		return Arrays.stream(counts).max().getAsInt();
	}
	
	/**
	 * Helper method: Iterates through randvars and searches max. rand var arg count.
	 * 
	 * @return int: max rand var arg count.
	 */
	public int searchRealMaxRVArgs() {
		int max = 0;
		for (int i = 0; i<this.randVars.size(); i++) {
			int currSize = this.randVars.get(i).getArgs().size(); 
			if (currSize > max) {
				max = currSize;
			}
		}
		return max;
	}
	
	/**
	 * Helper method: Iterates through factors and searches max. factor arg count.
	 * 
	 * @return int: max factor arg count.
	 */
	public int searchRealMaxFactorArgCount() {
		int max = 0;
		for (int i = 0; i<this.factors.size(); i++) {
			int currSize = this.factors.get(i).getArgs().size(); 
			if (currSize > max) {
				max = currSize;
			}
		}
		return max;
	}

	/**
	 * Gives the number of real factors (i.e. factors that have been initialized
	 * with at least 1 randvar as argument)
	 * 
	 * @return Int Number of real factors.
	 */
	public int searchRealFactors() {
		int count = 0;
		for (Factor fac : this.factors) {
			if (fac.getArgs().size() > 0) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Constructs a filename suffix containing all relevant parameters:
	 * <code>-logVarCount_randVarCount_factorCount-maxRandVarArgs_maxRandVarOccurences_factorArgs</code>
	 * 
	 * @return filename suffix as String
	 */
	public String constructFileNameSuffix() {
		return String.format("-%03d_%03d_%03d-%d_%d_%d", 
				specci.getLogVarCount(), specci.getRandVarCount(), specci.getFactorCount(), 
				specci.getMaxRandVarArgs(), specci.getMaxRandVarOccurrences(), specci.getFactorArgCount());
	}

	// GENERIC GETTERS
	public SpecContainer getSpecContainer() {
		return this.specci;
	}

	public ArrayList<LogVar> getLogVars() {
		return this.logVars;
	}

	public ArrayList<RandVar> getRandVars() {
		return this.randVars;
	}

	public ArrayList<Factor> getFactors() {
		return this.factors;
	}

	public ElementFactory getLogVarFactory() {
		if (this.baseLogVarFactory == null) {
			System.err.println("LogVarFactory of world accessed before world was filled.");
			System.exit(1);
		}
		return this.baseLogVarFactory;
	}

	public ElementFactory getRandVarFactory() {
		if (this.baseRandVarFactory == null) {
			System.err.println("RandVarFactory of world accessed before world was filled.");
			System.exit(1);
		}
		return this.baseRandVarFactory;
	}

	public ElementFactory getFactorFactory() {
		if (this.baseFactorFactory == null) {
			System.err.println("LogVarFactory of world accessed before world was filled.");
			System.exit(1);
		}
		return this.baseFactorFactory;
	}

	// FILTERED GETTERS

	public ArrayList<RandVar> getNonMaxedRandVars() {
		return new ArrayList<RandVar>(randVars.stream().filter(x -> x.getOccurrences() < specci.getMaxRandVarOccurrences())
				.collect(Collectors.toList()));
	}

	public ArrayList<RandVar> getNonMentionedRandVars() {
		return new ArrayList<RandVar>(
				randVars.stream().filter(x -> x.getOccurrences() == 0).collect(Collectors.toList()));
	}

	public ArrayList<RandVar> getMaxedRandVars() {
		return new ArrayList<RandVar>(
				randVars.stream().filter(x -> x.getOccurrences() == specci.getMaxRandVarArgs()).collect(Collectors.toList()));
	}
	
	
	// ADDERS for LISTS
	public void addLogVar(LogVar lv) {
		this.logVars.add(lv);
	}

	public void addAllLogVars(ArrayList<LogVar> lvs) {
		this.logVars.addAll(lvs);
	}

	public void addRandVar(RandVar rv) {
		this.randVars.add(rv);
	}

	public void addAllRandVars(ArrayList<RandVar> rvs) {
		this.randVars.addAll(rvs);
	}

	/**
	 * Replace (!= ADD) factors.
	 * 
	 * @param facs new Factor-ArrayList to replace the old one (which might be
	 *             empty).
	 */
	public void replaceAllFactors(ArrayList<Factor> facs) {
		this.factors = facs;
	}

	// NAME CREATION
	public int getNextLogVarIndex() {
		return this.logVars.size() + 1;
	}

	public int getNextRandVarIndex() {
		return this.randVars.size() + 1;
	}

	public int getNextFactorIndex() {
		return this.factors.size() + 1;
	}

	// WORLD SPEC CHANGE
	public void incFactorCount() {
		this.specci.incFactorCount();
	}

	/**
	 * Selects and returns a factor where a randVar shall be replaced by an in
	 * retrospect added randVar.
	 * 
	 * @return Factor where kickout shall happen.
	 */
	public Factor selectKickoutFactor() {		
		// Can only kickout randvars out of *par*factors (i.e. at least 1 arg).
		ArrayList<Factor> candidateParfactors = new ArrayList<Factor>();
		for (Factor f : this.factors) {
			if (f.getArgs().size() > 0) {
				candidateParfactors.add(f);
			}
		}
		
		// Prefer factors with at least 1 RandVar that has > 1 occurrence.
		ArrayList<Factor> candidatesOccWise = new ArrayList<Factor>();
		for (Factor f : candidateParfactors) {
			if (f.hasMultipleOccurringArg()) {
				candidatesOccWise.add(f);
			}
		}
		
		ArrayList<Factor> choseFromList = candidatesOccWise.size() > 0 ? candidatesOccWise : candidateParfactors;
		
		int rnd = ConfigSingle.getInstance().getRandom().nextInt(choseFromList.size());
		return choseFromList.get(rnd);
	}
	
	/**
	 * Randomly selects and returns a factor that shall be augmented with 1 (or more) additional randVars. 
	 * 
	 * @return Factor to be augmented.
	 */
	public Factor selectAugmentFactor(RandVar rv) {
		// Randomly select a factor out of candidates that fulfill these requirements:
		// 	1. randVar does not occurr in their args yet:
		ArrayList<Factor> candidates = this.searchNewFactors(rv);
		
		// 2. Factor is not the biggest factor of the model (globally)
		int maxArgs = this.searchRealMaxFactorArgCount();
		ArrayList<Factor> cand_copy = (ArrayList<Factor>) candidates.clone();
		cand_copy = (ArrayList<Factor>) cand_copy.stream().filter(x-> x.getArgs().size() != maxArgs).collect(Collectors.toList());
		
		if (cand_copy.size()==0) {
			System.err.println("No candidate factor found in selectAugmentFactor(), i.e. no new non-max factor.");
			System.err.println(" ... chosing from newFactors instead (maxArg-factors allowed).");
			cand_copy = candidates;
		}
		
		if (cand_copy.size()==0) {
			System.err.println(" ... also no 'new' factor found. Aborting.");
			System.exit(1);
		}
		
		return cand_copy.get(ConfigSingle.getInstance().getRandom().nextInt(cand_copy.size()));
	}
	
	
	/**
	 * Searches and returns candidate factors for a given randvar, i.e. factors where the randvar does not occurr yet.
	 * 
	 * @param rv that should not occurr
	 * @return ArrayList of candidate Factors
	 */
	public ArrayList<Factor> searchNewFactors(RandVar rv) {
		ArrayList<Factor> out = new ArrayList<Factor>();
		for (Factor world_f: this.factors) {
			boolean elegible = true;
			for (Factor rv_f : rv.getOccurrenceFactors()) {
				if (world_f.getIndex() == rv_f.getIndex()) {
					elegible = false;
				}
			}
			
			if (elegible) {
				out.add(world_f);
			}
		}
		return out;
	}
	
	
	/**
	 * Selects and returns a randvar where a LogVar shall be replaced by an in retrospect
	 * added LogVar.
	 * 
	 * @return RandVar where kickout shall happen.
	 */
	public RandVar selectKickOutRandVar() {		
		// We can only kick logvars out of randvars with at least 1 arg.	
		ArrayList <RandVar> candidateRVs = new ArrayList<RandVar>();
		for (RandVar rv: this.randVars) {
			if (rv.getArgs().size() > 0) {
				candidateRVs.add(rv);
			}
		}
		
		int rnd = ConfigSingle.getInstance().getRandom().nextInt(candidateRVs.size());		
		return candidateRVs.get(rnd);
	}
	
	
	// ####################################
	//	      	FILE HANDLING
	// ####################################
	
	/**
	 *
	 * Set rerollIndex to -1 if no rerolling is done. 
	 */
	public void createFiles () {
		for (int ds : this.specci.getDomainSizes()) {
			String path = this.constructFilePath(ds);
			PrintStream ps = Helpers.createFilePrintStream(path);
			ps.append(this.toBLOGString(ds));
			ps.append(Helpers.commentOutMultiLine(this.writeWorldSummary()));
			ps.close();		
		}
	}

	
	/**
	 * Constructs a file path based on output path, export directory and the
	 * constructed file suffix by the world.
	 *  
	 * @return file path.
	 */
	public String constructFilePath(String curr_domainSize) {
		if (this.rerollIndex == -1) {
			return Main.outputPath + "/export" + this.constructFileNameSuffix() + ".blog";
		} else {
			char rerollChar = (char) ('a' + this.rerollIndex);

			String currIterIndexStr = (this.currentIterationIndex == -1) ? ""
					: "-"+ String.format("%02d", this.currentIterationIndex); //String.valueOf(this.currentIterationIndex);
			
			return Main.outputPath + "/" + rerollChar  + currIterIndexStr + "-" + curr_domainSize + "#export"
					+ this.constructFileNameSuffix() + ".blog";
		}
	}
	
	public String constructFilePath() {
		String strOfInts = Arrays.toString(this.specci.getDomainSizes()).replaceAll("\\[|\\]|\\s", "");
		return this.constructFilePath(strOfInts);
	}
	
	public String constructFilePath(int currDomainSize) {
		return this.constructFilePath(String.valueOf(currDomainSize));
	}
	

	
}
