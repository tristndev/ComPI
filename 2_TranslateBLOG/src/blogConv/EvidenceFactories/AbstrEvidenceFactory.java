package blogConv.EvidenceFactories;

import java.util.ArrayList;

import blog.Type;
import blogConv.ModelWrapper;
import blogSpecs.BLOGEvidenceSpecParser;
import blogSpecs.BLOGQuerySpecParser;
import blogSpecs.EvidenceSpec;

public abstract class AbstrEvidenceFactory {
	protected ModelWrapper mw;
	protected ArrayList<EvidenceSpec> evSpecList;
	protected int maxTimestep;
	
	protected ArrayList<boolean[][]> tfMats = new ArrayList<boolean[][]>();
	protected ArrayList<boolean[][]> shMats = new ArrayList<boolean[][]>();
	protected ArrayList<ArrayList<ArrayList<String>>> groupsList = new ArrayList<ArrayList<ArrayList<String>>>();
	
	
	public AbstrEvidenceFactory(ModelWrapper mw, ArrayList<EvidenceSpec> evSpecList, int maxTimestep) {
		this.mw = mw;
		this.evSpecList = evSpecList;
		this.maxTimestep = maxTimestep;
		evSpecsValid();
		createObjectsFromSpecs();
	}
	
	public AbstrEvidenceFactory(ModelWrapper mw, int maxTimestep) {
		String filePath = mw.getFilePath();
		BLOGEvidenceSpecParser sp = new BLOGEvidenceSpecParser(filePath);
		
		this.maxTimestep = maxTimestep;
		this.evSpecList = sp.getEvidenceSpecs();
		this.mw = mw;
		createObjectsFromSpecs();
	}
	
	/**
	 * Creates an evidence factory. 
	 * Includes loading a evidence spec for mw's path and deriving the overall absolute max time from the query spec.
	 * 
	 * @param mw ModelWrapper.
	 */
	public AbstrEvidenceFactory(ModelWrapper mw) {
		String filePath = mw.getFilePath();
		BLOGEvidenceSpecParser sp = new BLOGEvidenceSpecParser(filePath);
		
		// QueryFactory just needed for maxTime extraction.
		BLOGQuerySpecParser qsp = new BLOGQuerySpecParser(filePath);
		
		this.maxTimestep = qsp.getQueryAbsMaxTime();
		this.evSpecList = sp.getEvidenceSpecs();
		this.mw = mw;
		
		evSpecsValid();
		createObjectsFromSpecs();
	}
	
	public AbstrEvidenceFactory(ModelWrapper mw, AbstrEvidenceFactory absEF) {
		this.mw = mw;
		createObjectsFromOtherFactory(absEF);
	}
	
	/**
	 * Wrapper function that creates all available evidence lines.
	 * 
	 * @return String containing all evidence lines, separated by line breaks.
	 */
	public String createEvidenceLines(int currentMaxTimestep) {
		String res = evSpecList.size() > 0?"// Evidence\n" : "// No Evidence specified\n";
		for (int i=0; i < evSpecList.size(); i++) {
			res += this.createLinesFromSingleEvidenceSpec(tfMats.get(i), shMats.get(i), groupsList.get(i), evSpecList.get(i), currentMaxTimestep);
		}
		return res;
	}
	
	protected abstract String createLinesFromSingleEvidenceSpec(boolean[][] tfMat, boolean[][] shMat,
			ArrayList<ArrayList<String>> groups, EvidenceSpec evSpec, int currentMaxTimestep);
	
	
	/**
	 * Checks if the randVars specified in the evidenceSpecs are also present in the
	 * model.
	 * 
	 * @return true if valid, false otherwise
	 */
	private boolean evSpecsValid() {
		for (EvidenceSpec evSpec : evSpecList) {
			if (mw.getRandVarByName(evSpec.getRandVar()) == null) {
				System.err.println(String.format(
						"Evidence specified for randvar '%s' which is not present in the model. Aborting...",
						evSpec.getRandVar()));
				System.exit(1);
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Read and convert the specs into internal objects, so they are stored in a DJTEvidenceFactory instance.
	 */
	private void createObjectsFromSpecs() {
		for (EvidenceSpec es: evSpecList) {
			tfMats.add(createTFMatrix(es));
			shMats.add(createSHMatrix(es));
			groupsList.add(distributeGuarObjsToGroups(es));
		}
		doTFMatrixFlips();
	}
	
	/**
	 * Copy objects from another already created evidence factory. 
	 * Needed so that created evidence is identical.
	 * 
	 * @param fac already created evidence factory 
	 */
	private void createObjectsFromOtherFactory(AbstrEvidenceFactory fac) {
		this.tfMats = fac.getTFMats();
		this.shMats = fac.getSHMats();
		this.groupsList = fac.getGroupList();
		this.evSpecList = fac.getEVSpecList();
	}

	public ArrayList<boolean[][]> getTFMats() {
		return this.tfMats;
	}
	
	public ArrayList<boolean[][]> getSHMats() {
		return this.shMats;
	}
	
	public ArrayList<ArrayList<ArrayList<String>>> getGroupList() {
		return this.groupsList;
	}
	
	public ArrayList<EvidenceSpec> getEVSpecList() {
		return this.evSpecList;
	}
	
	/**
	 * Goes through all the tf matrices and does the random flips. 
	 */
	private void doTFMatrixFlips() {
		for (int i=0; i<tfMats.size(); i++) {
			boolean[][] tfMat = tfMats.get(i);
			boolean[][] shMat = shMats.get(i);
			ArrayList<ArrayList<String>> groups = groupsList.get(i);
			// Only do flipping if more than 1 group.
			if (groups.size() > 1) {
				for (int g=0; g<tfMat.length; g++) {
					for (int t=0; t<tfMat[0].length; t++) {
						// if evidence is shown...
						if (shMat[g][t]) {
							tfMat[g][t] = boolFlipByChance(tfMat[g][t], evSpecList.get(i));
						}
					}
				}
			}
		}
	}
	
	/**
	 * Flips a given inputBool by chance (probability is defined in the evSpec as flipProb). 
	 * Needed for symmetry breaking in Groups.
	 * 
	 * @param inputBool boolean value that shall be flipped.
	 * @param evSpec EvidenceSpec object with the flipProb defined.
	 * @return the flipped or non-flipped inputBool
	 */
	public boolean boolFlipByChance(boolean inputBool, EvidenceSpec evSpec) {
		boolean flip = evSpec.getBoolByProb(evSpec.getFlipProb());
		return flip? !inputBool : inputBool;
	}

	/**
	 * Distributes an evidenceSpecs guaranteed objects and distributes them evenly
	 * into X groups. Return object is an ArrayList that contains a String Array for
	 * each group (containing the object names).
	 * 
	 * @param evSpec
	 * @return
	 */
	public ArrayList<ArrayList<String>> distributeGuarObjsToGroups(EvidenceSpec evSpec) {
		ArrayList<ArrayList<String>> groups = new ArrayList<ArrayList<String>>(); 
		
		Type arg = extractFirstNonTimeArg(evSpec);
		
		if (arg != null) {
			// Case A: RandVar has a non-timestep argument
			ArrayList<Object> objects = new ArrayList<Object>(arg.getGuaranteedObjects());
			int nObjects = this.calcNumberOfCoveredObjects(evSpec);
			int nGroups = calcRealGroupCount(evSpec);
			for (int i = 0; i < nGroups; i++) {
				// Idea: Given N elements and M Groups.
				// 1. Put floor(N/M)+1 elements in N mod M of the groups.
				// 2. Put floor(N/M) elements in the rest.
				int currGroupSize = (int) Math.floor((double) nObjects / nGroups);
				
				if (i < (nObjects % nGroups)) {
					currGroupSize++;
				}
				
				ArrayList<String> currGroup = new ArrayList<String>();
				for (int j = 0; j<currGroupSize; j++) {
					currGroup.add(objects.remove(0).toString());
				}
				
				groups.add(currGroup);
			}
			
		} else {
			// Case B: RandVar doesn't have a non-timestep argument
			
		}
		return groups;		
	}

	/**
	 * Calculates the *real* groupCount (the specified group count might be
	 * restricted by the number of covered objects).
	 * 
	 * @param evSpec
	 * @return -1 if RandVar without non-timestep args
	 */
	private int calcRealGroupCount(EvidenceSpec evSpec) {
		Type arg1 = extractFirstNonTimeArg(evSpec);
		if (arg1 == null) {
			// Handle RandVars with just timesteps as argument (e.g. Hot(@2) )
			return 1;
		}

		int nGuaranteedObjs = arg1.getGuaranteedObjects().size();
		int coveredObjects = this.calcNumberOfCoveredObjects(evSpec);
		
		if (coveredObjects < evSpec.getGroupCount()) {
			return coveredObjects;
		} else {
			return evSpec.getGroupCount();
		}
	}
	
	/**
	 * Calculate the number of covered objects based on 1.) number of guaranteed objects and 2.) evidence coverage percentage.
	 * 
	 * @param evSpec Evidence spec
	 * @return int: Number of covered objects.
	 */
	public int calcNumberOfCoveredObjects(EvidenceSpec evSpec) {
		Type arg1 = extractFirstNonTimeArg(evSpec);
		int nGuaranteedObjs = arg1.getGuaranteedObjects().size();
		int coveredObjects = (int) Math.ceil(nGuaranteedObjs * evSpec.getCoverage());
		return coveredObjects;
	}

	/**
	 * Given an evidenceSpec, looks at the specified randvar and extracts the first argument type
	 * that is not 'timestep'. 
	 * 
	 * @param evSpec
	 * @return argument of class Type, null if no non-timestep argument is existent.
	 */
	protected Type extractFirstNonTimeArg(EvidenceSpec evSpec) {
		Type args[] = mw.getRandVarByName(evSpec.getRandVar()).getArgTypes();

		for (int i = 0; i < args.length; i++) {
			if (args[i].getName() != "Timestep") {
				return args[i];
			}
		}
		return null;
	}

	/**
	 * Creates a boolean 2d matrix representing the evidence = {true, false} states
	 * of each group in each timestep.
	 * 
	 * @return boolean[][] true false matrix.
	 */
	private boolean[][] createTFMatrix(EvidenceSpec evSpec) {
		// Matrix dimensions (x * y) = (realGroupCount * timesteps)
		int nGroups = this.calcRealGroupCount(evSpec);

		int nTimesteps = this.maxTimestep;

		// Matrix dimensions (x * y) = (realGroupCount * timesteps)
		boolean[][] tfMat = new boolean[nGroups][nTimesteps];
		// Initialize all to false
		for (int g = 0; g < tfMat.length; g++) {
			for (int t = 0; t < tfMat[0].length; t++) {
				tfMat[g][t] = false;
			}
		}

		// row 0: Initiate according to start show prob.
		int nStartShown = (int) Math.ceil(evSpec.getPercStartTrue() * nGroups);
		for (int g = 0; g < nStartShown; g++) {
			tfMat[g][0] = true;
		}

		// row 1 to nTimesteps: calc value based on previous one.
		for (int g = 0; g < nGroups; g++) {
			for (int t = 1; t < nTimesteps; t++) {
				tfMat[g][t] = evSpec.nextTimestepTrueFalse(tfMat[g][t - 1]);
			}
		}

		return tfMat;
	}

	/**
	 * Creates a boolean 2d matrix representing the evidence shown / hidden states
	 * of each group in each timestep.
	 * 
	 * <pre>
	 * true  = shown
	 * false = hidden
	 * </pre>
	 * 
	 * @return boolean[][] show hidden matrix.
	 */
	private boolean[][] createSHMatrix(EvidenceSpec evSpec) {
		// Todo: Implement
		int nGroups = this.calcRealGroupCount(evSpec);
		int nTimesteps = this.maxTimestep;

		// Matrix dimensions (x * y) = (realGroupCount * timesteps)
		boolean[][] shMat = new boolean[nGroups][nTimesteps];
		// Initialize all to false
		for (int g = 0; g < shMat.length; g++) {
			for (int t = 0; t < shMat[0].length; t++) {
				shMat[g][t] = false;
			}
		}

		// row 0: Initiate according to start show prob.
		int nStartShown = (int) Math.ceil(evSpec.getPercStartShown() * nGroups);
		for (int g = 0; g < nStartShown; g++) {
			shMat[g][0] = true;
		}

		// row 1 to nTimesteps: calc value based on previous one.
		for (int g = 0; g < nGroups; g++) {
			for (int t = 1; t < nTimesteps; t++) {
				shMat[g][t] = evSpec.nextTimestepShowHidden(shMat[g][t - 1]);
			}
		}

		return shMat;
	}
	
	/**
	 * Getter for ArrayList of loaded EvidenceSpecs.
	 * @return evSpecList
	 */
	public ArrayList<EvidenceSpec> getLoadedEvSpecs() {
		return this.evSpecList;
	}

	/**
	 * Method for debugging.
	 */
	public void printAllMatrices() {
		for (EvidenceSpec evSpec : evSpecList) {
			printSingleMatrix(evSpec);
		}
	}

	public String createSingleMatrixString(EvidenceSpec evSpec) {
		String ret = "";

		boolean[][] tfMat = createTFMatrix(evSpec);
		boolean[][] shMat = createSHMatrix(evSpec);

		for (int g = 0; g < tfMat.length; g++) {
			for (int t = 0; t < tfMat[0].length; t++) {
				String element = tfMat[g][t] ? "T" : "f";
				element = shMat[g][t] ? " " + element + " " : "(" + element + ")";
				ret += " " + element + " ";
			}
			ret += "\n";
		}

		return ret;
	}

	public void printSingleMatrix(EvidenceSpec evSpec) {
		System.out.println("Matrix for " + evSpec.getRandVar());
		int[] matSize = this.getMatrixSize(evSpec);
		System.out.println(String.format("   Axis: down: groups (%d), right: timesteps(%d)\n", matSize[0], matSize[1]));
		
		System.out.println(this.createSingleMatrixString(evSpec));
	}

	/**
	 * Returns a matrix dimension array with 2 elements
	 * 
	 * @param evSpec for which the matrix shall be created.
	 * @return int array of size two: <code>{nGroups, nTimesteps}</code>
	 */
	public int[] getMatrixSize(EvidenceSpec evSpec) {
		int nGroups = this.calcRealGroupCount(evSpec);
		int nTimesteps = this.maxTimestep;

		return new int[] { nGroups, nTimesteps };
	}
	
	
	/**
	 * Goes through the evidence specs and checks whether evidence would be shown for a given randvar,
	 * timestep and object. Returns true if evidence is shown.  
	 * 
	 * @param randVar to find evidence for
	 * @param timestep to find evidence for
	 * @param object to find evidence for
	 * @return true if evidence is existent
	 */
	public boolean hasEvidenceForRandvarTimestep(String randVar, int timestep, String object) {
		EvidenceSpec evSpec;
		for (int evSpecIndex=0; evSpecIndex < this.evSpecList.size(); evSpecIndex++) {
			evSpec = this.evSpecList.get(evSpecIndex);
			// Has evidence for randvar
			if (evSpec.getRandVar().equals(randVar)) {
				// empty object string? -> RandVar w/o logvar -> just 1 group
				int objGroup = object.isEmpty()? 0 : this.getGroupIndexForObject(object, evSpecIndex);
				if (objGroup != -1) {
					// Object covered by evidence.
					return this.checkSHMatrix(evSpecIndex, objGroup, timestep);
				} else {
					// Object not covered by evidence
					break;
				}
			}
		}
		return false;
	}
	
	private boolean checkSHMatrix(int evSpecIndex, int groupIndex, int timestep) {
		return this.shMats.get(evSpecIndex)[groupIndex][timestep-1];
	}

	/**
	 * Goes through the groups list (at evSpecIndex of this.groupsList) and returns the group index
	 * of the given object (represented as string).
	 * Returns -1 if the object is not found.
	 * 
	 * @param object to be searched in the groups list.
	 * @param evSpecIndex to select the correct groupsList
	 * @return index of group where object is found
	 */
	private int getGroupIndexForObject(String object, int evSpecIndex) {
		ArrayList<ArrayList<String>> relevantGroupsList = this.groupsList.get(evSpecIndex);
		ArrayList<String> currGroup;
		for (int i = 0; i < relevantGroupsList.size(); i++) {
			currGroup = relevantGroupsList.get(i);
			for (String element : currGroup) {
				if (element.equals(object)) {
					return i;
				}
			}
		}
		
		return -1;
	}


}
