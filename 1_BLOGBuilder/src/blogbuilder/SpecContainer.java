package blogbuilder;

public class SpecContainer {
	/**
	 * Size of domains (default = 3).
	 */
	private int[] domainSizes = {3};

	/**
	 * Number of logvars in the world.
	 */
	private int logVarCount = -1;

	/**
	 * Number of randvars in the world.
	 */
	private int randVarCount = -1;

	/**
	 * Max number of randvar arguments (2?)
	 */
	private int maxRandVarArgs = -1;

	/**
	 * Number of factors in the world.
	 */
	private int factorCount = -1;

	/**
	 * Number of arguments per factor.
	 */
	private int factorArgCount = -1;

	/**
	 * Number of maximal occurrences (in (par)factors) for a randvar.
	 */
	private int maxRandVarOccurrences = -1;

	
	/**
	 * With domainSize!!
	 * 
	 * 
	 * @param domainSize
	 * @param logVarCount
	 * @param randVarCount
	 * @param factorCount
	 * @param factorArgCount
	 * @param maxRandVarOccurrences
	 * @param maxRandVarArgs
	 */
	public SpecContainer(int[] domainSizes, int logVarCount, int randVarCount, int factorCount, int factorArgCount,
			int maxRandVarOccurrences, int maxRandVarArgs) {
		this.domainSizes = domainSizes; 
		this.logVarCount = logVarCount;
		this.randVarCount = randVarCount;
		this.factorCount = factorCount;
		this.factorArgCount = factorArgCount;
		this.maxRandVarOccurrences = maxRandVarOccurrences;
		this.maxRandVarArgs = maxRandVarArgs;
	}
	
	/**
	 * Without domainSize!
	 * 
	 * 
	 * 
	 * @param logVarCount
	 * @param randVarCount
	 * @param factorCount
	 * @param factorArgCount
	 * @param maxRandVarOccurrences
	 * @param maxRandVarArgs
	 */
	public SpecContainer(int logVarCount, int randVarCount, int factorCount, int factorArgCount,
			int maxRandVarOccurrences, int maxRandVarArgs) {
		//this.domainSize = domainSize; 
		this.logVarCount = logVarCount;
		this.randVarCount = randVarCount;
		this.factorCount = factorCount;
		this.factorArgCount = factorArgCount;
		this.maxRandVarOccurrences = maxRandVarOccurrences;
		this.maxRandVarArgs = maxRandVarArgs;
	}
	
	
	public int[] getDomainSizes() {
		return this.domainSizes;
	}

	public int getLogVarCount() {
		return logVarCount;
	}

	public int getRandVarCount() {
		return randVarCount;
	}

	public int getFactorCount() {
		return factorCount;
	}

	public int getFactorArgCount() {
		return factorArgCount;
	}

	public int getMaxRandVarOccurrences() {
		return maxRandVarOccurrences;
	}

	public int getMaxRandVarArgs() {
		return maxRandVarArgs;
	}


	public void incFactorCount() {
		this.factorCount++;
	}

	
	
}
