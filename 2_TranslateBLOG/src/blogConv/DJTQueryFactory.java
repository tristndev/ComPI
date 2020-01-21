package blogConv;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import blog.Function;
import blog.Type;
import blogSpecs.BLOGQuerySpecParser;

public class DJTQueryFactory {

	private File file;
	private ModelWrapper mw;

	private ArrayList<Function> randVars;

	/**
	 * Max. timestep used in query generation.
	 */
	int maxTime;

	/**
	 * Max timestep increase - for multiple query sizes (optional)
	 */
	int maxTimeInc;

	/**
	 * MaxTimeCutoff - determines whether queries that "look further into the
	 * future" than maxTime will be created or cut off.
	 */
	boolean maxTimeCutOff;
	
	/**
	 * QueryIntervalHandling - describes how query creation shall be handled 
	 * for queries that go outside the specified query interval.
	 * 
	 * One of "none", "cutoff" or "maxTConversion".
	 */
	String queryIntervalHandling;

	/**
	 * Set of querySizes (if multiple query size settings are specified)
	 */
	Set<Integer> querySizes;

	/**
	 * Time incrementation steps (i += timeInc)
	 */
	int timeInc;

	/**
	 * Time deltas for query generation (e.g. time deltas {-5} lead to querys
	 * q(@10, @10) and q(@10, @5))
	 */
	int[] timeDeltas;

	/**
	 * boolean indicating whether the file should be created with a) dynamic
	 * timesteps (true) or b) statically rolled out (false)
	 */
	boolean dynamicTimesteps;
	/**
	 * Construct holding information on RandVars and their argument objects.
	 */
	ArrayList<SimpleEntry<String, ArrayList<ArrayList<String>>>> randVarObjectConstr;

	/**
	 * Constructor
	 * 
	 * @param mw               ModelWrapper object of modelFile
	 * @param dynamicTimesteps boolean indicating whether the file should be created
	 *                         with a) dynamic timesteps (true) or b) statically
	 *                         rolled out (false)
	 */
	public DJTQueryFactory(ModelWrapper mw, boolean dynamicTimesteps) {
		this.file = new File(mw.getFilePath());
		this.mw = mw;

		this.randVars = mw.getRandVars();

		this.dynamicTimesteps = dynamicTimesteps;

		setDefaults();
		parseFile();

		/*
		 * System.out.println("> Query Strings:"); for (String str : createQueryLines())
		 * System.out.println(">>  "+str);
		 */
	}

	/**
	 * Central default value setter. If nothing else is specified in the file, these
	 * values will be used.
	 */
	private void setDefaults() {
		maxTime = 2;
		timeDeltas = new int[] { 0 };
	}

	/**
	 * Parse information from model file, which will be given in a comment in the
	 * following format:
	 * 
	 * <pre>
	 * Don't change the following line.
	 * [Query Spec]
	 * {
	 *   "maxTime": 10,
	 *   "timeStepDelta": 5,
	 *   "timeDeltas": [-10,5,0],
	 *   "includeVars": ["var1", "var2", "var3"],
	 *   "restrictQueries": [
	 *       {
	 *        "randvar": "Pub",
	 *        "objects": [["p1","p2","p3"],
	 *                        ["q1","q3"]]
	 *       },
	 *       {
	 *        "randvar": "AttC",
	 *        "objects": [["c1","c3"]]
	 *        },
	 *        {
	 *        "randvar": "DoR",
	 *        "objects": [[]]
	 *        }
	 *        ]
	 * }
	 * [/QuerySpec]
	 * </pre>
	 */
	private void parseFile() {
		BLOGQuerySpecParser sp = new BLOGQuerySpecParser(this.file.toString());

		this.maxTime = sp.getQueryAbsMaxTime();
		this.maxTimeInc = sp.getQueryMaxTimeInc();
		this.timeInc = sp.getQueryTimeInc();
		this.timeDeltas = sp.getQueryTimeDeltas();
		this.maxTimeCutOff = sp.getMaxTimeCutOff();
		this.queryIntervalHandling = sp.getQueryIntervalHandling();
		
		this.querySizes = sp.calcQuerySettingSizes();

		this.randVarObjectConstr = sp.getRandVarArgumentConstruct(mw);
	}

	/**
	 * Extracts all necessary information from the given model wrapper.
	 */
	private void extractInformationFromMW() {
		System.out.println("Types:");
		System.out.println(mw.getTypes().iterator().next().getGuaranteedObjects());
		// mw.getTypeByName(name);

		System.out.println("Randvars:");
		String randVarName = mw.getRandVarNames().iterator().next();
		System.out.println(mw.getRandVarByName(randVarName).getSig().getArgTypes()[1]);
		System.out.println(mw.getRandVars());
		// mw.getRandVarByName(name);
	}

	/**
	 * Wrapper Method for query line creation
	 * 
	 * @param locMaxTime maxTime for this query creation call
	 * @return ArrayList of Strings, each one describing one query line
	 */
	private ArrayList<String> createQueryLines(int locMaxTime) {
		ArrayList<String> list = new ArrayList<String>();
		for (SimpleEntry<String, ArrayList<ArrayList<String>>> entry : this.randVarObjectConstr) {
			String rvName = entry.getKey();
			ArrayList<ArrayList<String>> randVarList = entry.getValue();

			list.addAll(createArgumentsArray("", rvName, randVarList, locMaxTime));
		}
		return list;
	}

	/**
	 * Removes the 'Tiemstep' argument from a given array of argumements.
	 * 
	 * @param argTypes Type array to remove the 'Timestep' argument from
	 * @return arguments array without 'Timestep' argument
	 */
	private Type[] removeTime(Type[] argTypes) {
		List<Type> argList = new ArrayList<Type>(Arrays.asList(argTypes));

		ListIterator<Type> it = argList.listIterator();
		while (it.hasNext()) {
			if (it.next().toString().equals("Timestep")) {
				it.remove();
			}
		}

		Type[] retList = new Type[argList.size()];
		return argList.toArray(retList);
	}

	private ArrayList<String> createArgumentsArray(String prefix, String randVar, ArrayList<ArrayList<String>> argTypes,
			int locMaxTime) {
		// System.out.println(String.format("Called - pref: '%s', randVar: %s, argLength
		// %d", prefix, randVar.toString(), argTypes.size()));

		ArrayList<String> list = new ArrayList<String>();
		if (argTypes.size() == 0) {
			list.addAll(constructSingleQueries(randVar, prefix, locMaxTime));
		} else {
			ArrayList<String> firstArgObjList = argTypes.get(0);
			// System.out.println(String.format("§§ first: %s, n_guarObj: %d",
			// first.toString(), first.getGuaranteedObjects().size()));

			for (String obj : firstArgObjList) {
				list.addAll(createArgumentsArray(prefix + obj.toString() + ", ", randVar,
						new ArrayList<ArrayList<String>>(argTypes.subList(1, argTypes.size())), locMaxTime));
			}
		}
		return list;
	}

	/**
	 * Last step of query line creation. Gets a randVar, the argument prefixes and
	 * merges them into a query line. Handles dynamic and non-dynamic queries (i.e.
	 * includes Timestep literals, or not)
	 * 
	 * @param randVar
	 * @param prefix
	 * @param locMaxTime
	 * @return
	 */
	private ArrayList<String> constructSingleQueries(String randVar, String prefix, int locMaxTime) {
		ArrayList<String> ret = new ArrayList<String>();

		if (isDynamicRandVar(randVar)) {
			for (int i = 1; i <= locMaxTime; i += timeInc) {
				for (int delta : timeDeltas) {

					int[] timesteps = timestepConversion(i, delta, locMaxTime);
					
					// If 2nd Timestep Y of (X,Y) is = 1 -> Look at RandVar1, else RandVar2
					String locRandVar = randVar + String.valueOf(((timesteps[1] == 1) ? 1 : 2));
					
					if (timesteps[0] != -1 && timesteps[1] != -1) {
						if (this.dynamicTimesteps) {
							// >> Mode 1: Dynamic timesteps -> format 'query randVar(var1,var2,@10,@13)'
							ret.add(String.format("query %s(%s@%d, @%d);", locRandVar, prefix, timesteps[0], timesteps[1]));
						} else {
							// >> Mode 2: Static rollout -> format 'query randVar13(var1,var2)'
							if (prefix.length() > 0) {
								// At least 1 Argument query randvar
								ret.add(String.format("query %s%d(%s);", randVar, timesteps[1],
										prefix.substring(0, prefix.length() - 2)));
							} else {
								// No argument in query randvar
								ret.add(String.format("query %s%d;", randVar, timesteps[1]));
							}
						}
					}
				}
			}
		} else {
			prefix = prefix.substring(0, prefix.length() - 2);
			ret.add(String.format("query %s(%s);", randVar.toString(), prefix));
		}
		return ret;
	}

	/**
	 * Helper function that handles the timestep conversion (i.e. is a query contained in
	 * the defined timestep intervals).
	 * 
	 * @param i       current timestep
	 * @param delta   timestep delta
	 * @param maxTime timestep maximum
	 * @return int array of size 2 with [from_timestep, to_timestep]. Both are -1 if one of them would have gotten out of bounds.
	 */
	private int[] timestepConversion(int i, int delta, int maxTime) {
		if (i + delta > 0) {
			switch (this.queryIntervalHandling) {
			case "none":
				return new int[] {i, i+delta};
			case "cutoff":
				if (i + delta <= maxTime) {
					return new int[] {i, i+delta};
				} else {
					return new int[] {-1, -1};
				}
			
			case "maxTConversion":
				if (i + delta <= maxTime) {
					return new int[] {i, i+delta};
				} else {
					return new int[] {i, maxTime};
				}
				
			default:
				System.err.println("Unspecified query interval conversion mode: " + this.queryIntervalHandling);
				System.exit(1);
				return new int[] {-10, -10};
			}
		} else {
			if (this.queryIntervalHandling.equals("maxTConversion")) {
				return new int[] {i, 1};
			} else {
				return new int[] {-1, -1};
			}
		}
	}

	/**
	 * Helper function to determine whether a given randVar is dynamic (i.e. has a
	 * Timestep as argument) or not.
	 * 
	 * @param randVarStr randVar to look at
	 * @return boolean true if dynamic, false otherwise
	 */
	private boolean isDynamicRandVar(String randVarStr) {
		Type[] types = mw.getRandVarByName(randVarStr).getArgTypes();

		for (int i = 0; i < types.length; i++) {
			if (types[i].toString().equals("Timestep")) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Wrapper function that creates all query settings.
	 * 
	 * @return a HashMap<Integer, String> with the querySize as key and the
	 *         querylines as value string
	 */
	public HashMap<Integer, String> createAllQuerySizes() {
		HashMap<Integer, String> querySets = new HashMap<Integer, String>();

		Iterator<Integer> it = this.querySizes.iterator();
		while (it.hasNext()) {
			int nextSize = it.next();
			querySets.put(nextSize, String.join("\n", createQueryLines(nextSize)));
		}
		return querySets;
	}

	/**
	 * Helper function that creates and returns an int array containing the
	 * different query sizes. Needed for MLN file creation.
	 * 
	 * @return int array of query sizes.
	 */
	public int[] getRawQuerySizesArray() {
		int[] arr = new int[this.querySizes.size()];
		Iterator<Integer> it = this.querySizes.iterator();
		int i = 0;
		while (it.hasNext()) {
			arr[i] = it.next();
			i++;
		}
		return arr;
	}

	/**
	 * Getter for maxTime value from query spec.
	 * 
	 * @return maxTime int
	 */
	public int getMaxTime() {
		return this.maxTime;
	}
}
