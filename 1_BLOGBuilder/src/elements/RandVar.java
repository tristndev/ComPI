package elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import blogbuilder.ConfigSingle;

public class RandVar extends ModelObject {

	/**
	 * Collection of logvar arguments for this randvar.
	 */
	private ArrayList<LogVar> args = new ArrayList<LogVar>();

	/**
	 * A set of factors this RandVar occurs in.
	 */
	private HashSet<Factor> occurrenceFactors = new HashSet<Factor>();

	/**
	 * Indicator whether randvar is in a factor.
	 */
	private boolean hasFactor = false;

	/**
	 * Constructor for RandVar objects.
	 * 
	 * @param index for the new randVar (can be obtained by the world object)
	 * @param args  list of logvar arguments.
	 */
	public RandVar(int index, ArrayList<LogVar> args) {
		super.prefix = "RV";
		super.index = index;
		this.args = args;
	}

	@Override
	public String asLine() {
		List<String> argNames = args.stream().map(x -> x.constructName()).collect(Collectors.toList());
		String argstring = args.size() == 0 ? "" : String.format("(%s)", String.join(", ", argNames));
		return String.format("random Boolean %s%s;\n", this.constructName(), argstring);
	}

	/**
	 * Getter for the occurence counter.
	 * 
	 * @return int number of occurences.
	 */
	public int getOccurrences() {
		return this.occurrenceFactors.size();
	}

	/**
	 * Handles all side effects that occur when a RandVar is added to a Factor:  Add factor's args to connectedRandars list.
	 * 
	 * @param fac that RandVar is added to.
	 */
	public void addToFactor(Factor fac) {
		this.occurrenceFactors.add(fac);
	}
	
	/**
	 * Handles removing this RandVar from a Factor (i.e. removes the Factor from this RandVar's occurrenceFactors set.
	 * 
	 * @param fac to be removed from the set.
	 */
	public void removeFromFactor(Factor fac) {
		if (!this.occurrenceFactors.remove(fac)) {
			System.out.println(String.format("Tried to remove factor %d from RandVar %d occurrenceFactors list, but factor was not found.",
					fac.index, this.index));
		}
	}

	/**
	 * Getter for args list.
	 * 
	 * @return ArrayList<LogVar> of args (logVars) for this randvar.
	 */
	public ArrayList<LogVar> getArgs() {
		return this.args;
	}

	/**
	 * Goes through all occurrenceFactors and collects all the connected RandVars
	 * (i.e. those that occurr together with this RandVar in at least 1
	 * (Par)Factor).
	 * 
	 * @return RandVar Hashset with connected RandVars (connected = together in at
	 *         least 1 factor)
	 */
	public HashSet<RandVar> getConnectedRandVars() {
		HashSet<RandVar> connectedRandVars = new HashSet<RandVar>();

		for (Factor fac : this.occurrenceFactors) {
			connectedRandVars.addAll(fac.getArgs());
		}

		return connectedRandVars;
	}

	/**
	 * Returns the boolean flag which shows whether there is already a factor that
	 * has been created for this randvar.
	 * 
	 * @return boolean flag
	 */
	public boolean hasFactor() {
		return this.hasFactor;
	}

	/**
	 * Setter for the hasFactors flag.
	 * 
	 * @param b
	 */
	public void setHasFactor(boolean b) {
		this.hasFactor = b;
	}

	/**
	 * Replaces a (randomly chosen) LogVar from this RandVars's arguments.
	 * 
	 * @param newLV new LogVar that replaces one of the existing arguments.
	 */
	public void replaceKickOutLogVar(LogVar newLV) {
		int rnd = ConfigSingle.getInstance().getRandom().nextInt(this.args.size());
		this.args.set(rnd, newLV);
	}

	public ArrayList<Factor> getOccurrenceFactors() {
		return new ArrayList<Factor>(this.occurrenceFactors);
	}
}
