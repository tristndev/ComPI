package elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;

import blogbuilder.ConfigSingle;

public class Factor extends ModelObject {

	/**
	 * ArrayList of arguments of this factor object.
	 */
	private ArrayList<RandVar> args = new ArrayList<RandVar>();

	/**
	 * A HashMap with the corresponding variables for a LogVar. 
	 * Filled before asLine translation.
	 */
	HashMap<LogVar, String> variables;

	public Factor(int index, ArrayList<RandVar> args) {
		super.prefix = "Fac";
		super.index = index;
		this.args = args;

	}

	/**
	 * Create the line representation of the factor according to the following
	 * rules:
	 * <ol>
	 * <li>Has 1 or more non parameterized randvars (e.g. <code>hot</code>) ->
	 * <code>factor</code></li>
	 * <li>Else -> <code>parfactor</code> (at least 1 parameterized randvar, e.g.
	 * <code>App(x)</code>)</li>
	 * </ol>
	 */
	@Override
	public String asLine() {
		this.variables = allocateVariables(this.collectUniqueArgLogVars());
		String probsString = this.generateProbString();
		String rvString = this.generateRVString();

		/**
		 * Pattern: (varDeclareString) (probsString) parfactor LV2 A, LV1 B .
		 * MultiArrayPotential[[0.5,0.6,0.7,0.8,0.1,0.2,0.3,0.4]] (RV1,RV2(A),RV5(A,B));
		 * (rvString)
		 *
		 * 
		 * OR (if no args)
		 * 
		 * factor MultiArrayPotential[[0.1,0.9]](RV1);
		 */
		String ret = String.format("// Factor #%d\n", this.index);
		String varDeclareString = this.generateVarDeclareString();
		ret += String.format("%s %s MultiArrayPotential[[%s]](%s);\n",
				// args.size() > 1? "parfactor" : args.get(0).getArgs().size() == 0?"factor" :
				// "parfactor",
				this.isParFactor() ? "parfactor" : "factor", varDeclareString.equals("") ? "" : varDeclareString + ".",
				probsString, rvString);
		return ret;
	}

	/**
	 * Helper function that checks whether this factor is a parfactor. Criterium:
	 * Exists at least one RandVar argument which is parameterized by logvars?
	 * 
	 * @return true if is parfactor, else false
	 */
	private boolean isParFactor() {
		boolean isParFac = false;
		for (RandVar rv : args) {
			if (rv.getArgs().size() > 0) {
				isParFac = true;
			}
		}
		return isParFac;
	}

	/**
	 * For each logvar lookup the variable (in the variables hashmap) and construct
	 * a out of that.
	 * 
	 * Generates something like: <code> "LV2 A, LV1 B" </code>
	 * 
	 * @return String varDeclareString
	 */
	private String generateVarDeclareString() {
		ArrayList<String> partStrings = new ArrayList<String>();
		for (LogVar lv : this.variables.keySet()) {
			partStrings.add(String.format("%s %s", lv.constructName(), this.variables.get(lv)));
		}
		return String.join(", ", partStrings);
	}

	/**
	 * For each argument RandVar, collect corresponding variables for all argument
	 * LogVars.
	 * 
	 * Generates something like: <code>"RV1,RV2(A),RV5(A,B)"</code>
	 * 
	 * @return String RVString
	 */
	private String generateRVString() {
		ArrayList<String> partStrings = new ArrayList<String>();
		for (RandVar rv : this.args) {
			ArrayList<String> lvList = new ArrayList<String>();
			for (LogVar lv : rv.getArgs()) {
				lvList.add(this.variables.get(lv));
			}
			String argString = rv.getArgs().size() == 0 ? "" : String.format("(%s)", String.join(",", lvList));
			partStrings.add(String.format("%s%s", rv.constructName(), argString));
		}
		return String.join(",", partStrings);
	}

	/**
	 * Generates random probabilities between 0 and 1 and returns them in a double
	 * array.
	 * 
	 * @return double[] array of randomly generated probabilties. Length = argCount
	 *         ^ 2
	 */
	private double[] generateProbabilities() {
		int probCount = (int) Math.pow(2.0, (double) this.args.size());
		double[] probs = new double[probCount];
		int intervalMin = 0;
		int intervalMax = 1;

		Random r = ConfigSingle.getInstance().getRandom();

		for (int i = 0; i < probCount; i++) {
			probs[i] = r.nextDouble() * (intervalMax - intervalMin) + intervalMin;
		}

		return probs;
	}

	/**
	 * Wrapper function for probability string generation (i.e. what stands inside
	 * the [[...]] of the MultiArrayPotential).
	 * 
	 * @return String of probabilties, delimited by commas (e.g.
	 *         <code>0.12, 0.34</code>).
	 */
	private String generateProbString() {
		double[] probs = this.generateProbabilities();
		String str = "";
		for (int i = 0; i < probs.length; i++) {
			str += String.format(Locale.ROOT, "%.2f%s", probs[i], i < probs.length - 1 ? ", " : "");
		}
		return str;
	}

	/**
	 * Collects unique LogVars from the arguments (i.e. duplicates occur just once
	 * in the returned list).
	 * 
	 * @return list of unique logVars
	 */
	private ArrayList<LogVar> collectUniqueArgLogVars() {
		HashSet<LogVar> logVars = new HashSet<LogVar>();
		for (RandVar rv : args) {
			for (LogVar lv : rv.getArgs()) {
				logVars.add(lv);
			}
		}

		return new ArrayList<LogVar>(logVars);
	}

	/**
	 * Creates relations between logVars and variables in a hashmap.
	 * 
	 * @param logVars to allocate variables to
	 * @return a HashMap <LogVar, String> with the variable names as String values.
	 */
	private HashMap<LogVar, String> allocateVariables(ArrayList<LogVar> logVars) {
		HashMap<LogVar, String> map = new HashMap<LogVar, String>();
		char A = 'A';
		for (int i = 0; i < logVars.size(); i++) {
			map.put(logVars.get(i), Character.toString((char) (A + i)));

			if (i > 25) {
				System.err.println(String.format("More arguments for factor '%s' than letters available for variables.",
						this.constructName()));
				System.exit(1);
			}
		}
		return map;
	}
	
	/**
	 * Checks if this factor has at least 1 argument RandVar with > 1 occurrences.
	 * 
	 * @return true if that is the case.
	 */
	public boolean hasMultipleOccurringArg() {
		boolean hasMultOcc = false;
		for (RandVar rv : this.args) {
			if (rv.getOccurrences() > 1) {
				hasMultOcc = true;
				break;
			}
		}
		return hasMultOcc;
	}

	/**
	 * Getter for args List
	 * 
	 * @return ArrayList<RandVar> of Args of this factor object.
	 */
	public ArrayList<RandVar> getArgs() {
		return this.args;
	}

	/**
	 * Augments the factor's randVars with the given randvar.
	 * 
	 * @param rv RandVar to augment the factor with.
	 */
	public void augmentWithRandVar(RandVar rv) {
		this.args.add(rv);
		rv.addToFactor(this);
	}
	

	/**
	 * Replaces a (randomly chosen) RandVar from this factor's arguments. Includes
	 * decrementing the replaced randvar's occurrence counter.
	 * 
	 * @param rv RandVar that replaces one of the existing arguments.
	 */
	public void replaceKickOutRandVar(RandVar rv) {		
		ArrayList <Integer> candidateIndices = this.searchKickOutCandidatesIndices();
		if (candidateIndices.size()>0) {
			int rnd = ConfigSingle.getInstance().getRandom().nextInt(candidateIndices.size());
			RandVar replaced = this.args.set(candidateIndices.get(rnd), rv);
			replaced.removeFromFactor(this);
			rv.addToFactor(this);
		} else {
			System.err.println(String.format("Factor #%d: no RandVar kickout candidates found.", this.index));
			System.exit(1);
		}
	}
	
	/**
	 * Creates a ArrayList of Integers with possible KickOut candidate indices
	 * (i.e. arg RandVars with occurence > 1)
	 * 
	 * @return ArrayList<Integer> with possible kickout candidate indices
	 */
	private ArrayList<Integer> searchKickOutCandidatesIndices() {
		ArrayList<Integer> candInd = new ArrayList<Integer>();
		for (int i=0; i<this.args.size(); i++) {
			if (this.args.get(i).getOccurrences() > 1) {
				candInd.add(i);
			}
		}
		return candInd;
	}

}
