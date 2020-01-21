package blogSpecs;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import blog.Function;
import blog.Type;
import blogConv.ModelWrapper;

/**
 * Helper class that enables query spec parsing from a BLOG file. Needed for
 * dynamic BLOG Translation & Query generation.
 * 
 * @author trist
 *
 */
public class BLOGQuerySpecParser extends BLOGSpecParser{

	// --- Definition of TAGS & DEFAULT VALUES
	private String tagMaxTime = "absMaxTime";
	private int defaultMaxTime = 3;
	
	private String tagMaxTimeInc = "maxTimeInc";
	private int defaultMaxTimeInc = -1;

	private String tagTimeInc = "timeSkip";
	private int defaultTimeInc = 1;
	
	private String tagQueryIntervalHandling = "queryIntervalHandling";
	private String defaultQueryIntervalHandling = "none";
	private String[] modesQueryDefaultHandling = new String[] {"none", "cutoff", "maxTConversion"};
	
	private String tagMaxTimeCutOff = "maxTimeCutOff";
	private boolean defaultMaxTimeCutoff = false;

	private String tagTimeDeltas = "timeDeltas";
	private int[] defaultTimeDeltas = new int[] { 0 };

	private String tagIncludeVars = "includeVars";

	private String tagQueryRestrictions = "restrictQueries";


	public BLOGQuerySpecParser(String filename) {
		super(filename,"[Query Spec]", "[/Query Spec]");		
	}
		
	/**
	 * Getter for specified / default maxTime.
	 * 
	 * @return int maxTime
	 */
	public int getQueryAbsMaxTime() {
		int val = specJson.has(tagMaxTime) ? specJson.getInt(tagMaxTime) : defaultMaxTime;
		return val;
	}
	
	/**
	 * Getter for queryIntervalHandling mode.
	 * Includes checking if the specified value is allowed.
	 * 
	 * @return string queryIntervalHandling mode
	 */
	public String getQueryIntervalHandling() {
		String val = specJson.has(tagQueryIntervalHandling)? specJson.getString(tagQueryIntervalHandling) : defaultQueryIntervalHandling;
		
		boolean valAllowed = false;
		for (String mode : modesQueryDefaultHandling) {
			if (val.toLowerCase().equals(mode.toLowerCase())) {
				valAllowed = true;
			}
		}
		if (!valAllowed) {
			System.err.println(String.format("Specified value for %s is not allowed. Chose one of %s", tagQueryIntervalHandling, String.join(", ", modesQueryDefaultHandling)));
			System.exit(1);
		}
		return val;
	}
	
	public int getQueryMaxTimeInc() {
		int val = specJson.has(tagMaxTimeInc)? specJson.getInt(tagMaxTimeInc) : defaultMaxTimeInc;
		return val;
	}
	
	/**
	 * Getter for specified maxTimeCutOff handling.
	 * @return boolean value.
	 */
	public boolean getMaxTimeCutOff() {
		boolean val = specJson.has(tagMaxTimeCutOff)? specJson.getBoolean(tagMaxTimeCutOff) : defaultMaxTimeCutoff;
		return val;
	}

	/**
	 * Getter for specified / default time(step) increment size.
	 * 
	 * @return int timeInc
	 */
	public int getQueryTimeInc() {
		int val = specJson.has(tagTimeInc) ? specJson.getInt(tagTimeInc) : defaultTimeInc;
		return val;
	}

	/**
	 * Getter for specified / default timeDeltas. e.g.:
	 * 
	 * <pre>
	 *  [-5, -2, 0]
	 * </pre>
	 * 
	 * @return int[] Array time deltas.
	 */
	public int[] getQueryTimeDeltas() {
		if (specJson.has(tagTimeDeltas)) {
			JSONArray arr = specJson.getJSONArray(tagTimeDeltas);
			int[] iArr = new int[arr.length()];
			for (int i = 0; i < arr.length(); i++)
				iArr[i] = arr.getInt(i);
			return iArr;
		} else {
			return defaultTimeDeltas;
		}
	}

	/**
	 * Wrapper method for randVar & object specification.
	 * QueryRestriction has higher priority in Spec, if includeVars AND restrictQueries are defined. 
	 * 
	 * Structure of returned ArrayList construct:
	 * <pre>
	 * |
	 * | - randvar1
	 * |	    |-- p1, p2, p3
	 * |	    |-- q3, q8, q100
	 * |
	 * | - randvar2
	 *          |-- p4, p5, p6
	 * </pre>
	 * 
	 * @param mw ModelWrapper
	 * @return Deep ArrayList-SimpleEntry construct with information on RandVars and Argument Objects
	 */
	public ArrayList<SimpleEntry<String, ArrayList<ArrayList<String>>>> getRandVarArgumentConstruct(ModelWrapper mw) {
		if (specJson.has(tagQueryRestrictions)) {
			return getQueryRestrictions(mw);
		} else {
			return createArrayListConstruct(getQueryIncludeVars(mw));
		}
	}
	
	/**
	 * Calculates the maxTime values if multiple query size settings are specified.
	 * @return int array with the maxTime values. 
	 */
	public Set<Integer> calcQuerySettingSizes() {
		Set<Integer> maxTimes = new HashSet<Integer>();
		if (this.getQueryMaxTimeInc() == this.defaultMaxTimeInc) {
			// No maxTimeInc specified -> Just one query size!
			maxTimes.add(this.getQueryAbsMaxTime());
		} else {
			// maxTimeInc specified -> multiple query sizes!
			maxTimes.add(1);
			maxTimes.add(this.getQueryAbsMaxTime());
			
			for (int i = this.getQueryMaxTimeInc(); i < this.getQueryAbsMaxTime(); i += this.getQueryMaxTimeInc()) {
				maxTimes.add(i);
			}
		}
		return maxTimes;
	}
	
	
	/**
	 * Getter for specified / default randVars. Needs a ModelWrapper instance in
	 * order to align specified randVars and detect errors in query specification.
	 * 
	 * @param mw ModelWrapper instance
	 * @return ArrayList<Function> containing the specified / default RandVars
	 */
	private ArrayList<Function> getQueryIncludeVars(ModelWrapper mw) {
		if (specJson.has(tagIncludeVars) && specJson.getJSONArray(tagIncludeVars).length() >= 1) {
			// Only change something if Spec has this argument
			// Target: Align with ModelWrapper randvars
			JSONArray specVars = specJson.getJSONArray(tagIncludeVars);
			ArrayList<Function> mwVars = mw.getRandVars();
			ArrayList<Function> includeVars = new ArrayList<Function>();

			for (int i = 0; i < specVars.length(); i++) {
				String sv = specVars.getString(i);
				boolean added = false;
				for (Function mv : mwVars) {
					if (sv.toString().toLowerCase().equals(mv.toString().toLowerCase())) {
						includeVars.add(mv);
						added = true;
					}
				}
				if (!added) {
					System.err.print(String.format(
							"> Specified RandVar '%s' couldn't be found in the model's RandVars. Aborting...",
							sv.toString()));
					System.exit(1);
				}
			}
			return includeVars;
		} else {
			return mw.getRandVars();
		}
	}
	
	private ArrayList<SimpleEntry<String, ArrayList<ArrayList<String>>>> createArrayListConstruct(ArrayList<Function> randVars) {
		ArrayList<SimpleEntry<String, ArrayList<ArrayList<String>>>> list = new ArrayList<SimpleEntry<String, ArrayList<ArrayList<String>>>>();

		for (Function rv: randVars) {
			ArrayList<ArrayList<String>> argObjects = getAllObjectsFromModel(rv, -1);
			SimpleEntry<String, ArrayList<ArrayList<String>>> entry = new SimpleEntry<String, ArrayList<ArrayList<String>>>(rv.toString(), argObjects); 
			list.add(entry);
		}
		return list;
	}
	
	

	/**
	 * Getter for specified / default objects and query restrictions on them.
	 * Example: 'Only create queries for Persons p1, p2, p3, not for all 100 of
	 * them'. Needs a ModelWrapper instance in order to align specified objects and
	 * detect errors in query specification.<br>
	 * 
	 * Structure of returned ArrayList construct:
	 * 
	 * <pre>
	 * |
	 * | - randvar1
	 * |	    |-- p1, p2, p3
	 * |	    |-- q3, q8, q100
	 * |
	 * | - randvar2
	 *          |-- p4, p5, p6
	 * </pre>
	 * 
	 * 
	 * @param mw ModelWrapper instance
	 * @return ArrayList<Function> containing the specified / default RandVars
	 */
	private ArrayList<SimpleEntry<String, ArrayList<ArrayList<String>>>> getQueryRestrictions(ModelWrapper mw) {
		ArrayList<SimpleEntry<String, ArrayList<ArrayList<String>>>> list = new ArrayList<SimpleEntry<String, ArrayList<ArrayList<String>>>>();
				
		if (specJson.has(tagQueryRestrictions)) {
			// 1. Only continue if is specified..
			JSONArray rvArr = specJson.getJSONArray(tagQueryRestrictions);

			for (int i = 0; i < rvArr.length(); i++) {
				JSONObject rvJson = rvArr.getJSONObject(i);

				String rvName = rvJson.getString("randvar");
				JSONArray argArr = rvJson.getJSONArray("objects");

				ArrayList<ArrayList<String>> argObjects = new ArrayList<ArrayList<String>>();

				// 1. find rvName in ModelWrapper
				Function rv = mw.getRandVarByName(rvName);

				if (rv == null) {
					System.err.printf("> Error in query specification. RandVar %s does not exist.\n", rvName);
					System.exit(1);
				}

				// 2. For each argument go through a sub-array of the objArray and align with
				// types from MW.
				// Structure: RandVar -> Arguments of Types -> Guaranteed Objects

				Type[] rvArgTypes = rv.getArgTypes();
				
				//System.out.println(String.format("RandVar: %s, # rvArgs: %d, # rvArgs w/o t %d, # specArgs: %d",
				//		rvName, rvArgTypes.length, numberNonTimestepArgs(rvArgTypes), argArr.length()));

				// Check if all argTypes have been specified?
				if (numberNonTimestepArgs(rvArgTypes) == argArr.length() && arrayListHasDeepElement(argArr)) {
					for (int j = 0; j < argArr.length(); j++) {
						JSONArray objArray = argArr.getJSONArray(j);
						ArrayList<String> extractObjects = new ArrayList<String>();

						if (objArray.length() == 0) {
							argObjects.addAll(getAllObjectsFromModel(rv, j));					
						} else {
							Type matchType = findMatchingType(rvArgTypes, objArray);

							if (matchType == null) {
								System.err.printf("> No matching objects found for specified RandVar-Object combination:\n"
										+ "\t RandVar: %s, Object: %s\n", rvName, objArray.getString(0));
								System.exit(1);
							}

							for (int k = 0; k < objArray.length(); k++) {
								// For each specified object, check if it exists in the
								// rvArgTypes.getGuaranteedObjects()
								if (guarObjContainsObj(matchType.getGuaranteedObjects(), objArray.getString(k))) {
									extractObjects.add(objArray.getString(k));
								} else {
									System.err.printf(
											"> Specified object '%s' for RandVar '%s' not found in model. Continuing with remaining objects...\n",
											objArray.getString(k), rvName);
								}
							}
							argObjects.add(extractObjects);
						}
					}
				} else if (argArr.length() == 0 || ! arrayListHasDeepElement(argArr)) {
					// Randvar specified with no entries as objects? -> All objects!
					argObjects.addAll(getAllObjectsFromModel(rv, -1));					
				} else {
					System.err.printf(
							"> %d types of argument objects where specified for RandVar %s, but it has %d in the model (excluding 'Timestep').",
							argArr.length(), rvName, numberNonTimestepArgs(rvArgTypes));
					System.exit(1);
				}

				SimpleEntry<String, ArrayList<ArrayList<String>>> entry = new SimpleEntry<String, ArrayList<ArrayList<String>>>(
						rvName, argObjects);
				list.add(entry);

			}

		} else {
			// 2. else: Defaults (include all from model wrapper)
			for (Function mw_rv : mw.getRandVars()) {
				String rvString = mw_rv.toString();

				Type[] argTypes = mw_rv.getArgTypes();
				ArrayList<ArrayList<String>> argObjects = new ArrayList<ArrayList<String>>();

				for (Type t : argTypes) {
					System.out.printf("### rv: %s, argtype: %s\n", rvString, t.toString());
					
					if (!t.toString().equals("Timestep")) {
						ArrayList<String> objList = new ArrayList<String>();
						for (Object obj : t.getGuaranteedObjects()) {
							objList.add(obj.toString());
						}

						argObjects.add(objList);
					}
				}

				SimpleEntry<String, ArrayList<ArrayList<String>>> entry = new SimpleEntry<String, ArrayList<ArrayList<String>>>(
						rvString, argObjects);
				list.add(entry);
			}
		}

		return list;
	}
	
	/**
	 * Returns all found guaranteed Objects from the model wrapper for the given randVar.
	 * 
	 * If argInd = -1 -> Guaranteed Objects are returned for all argTypes
	 * Else -> Guaranteed Objects are just returned for the argument with index argInd (counted without Timestep)
	 * @param randVar RandomVariable to extract guaranteed Objects from
	 * @param argInd Index of target argument (-1 for all arguments)
	 * 
	 * @return ArrayList<ArrayList<String>> with 1 ArrayList per argument type.
	 */
	private ArrayList<ArrayList<String>> getAllObjectsFromModel(Function randVar, int argInd) {
		ArrayList<ArrayList<String>> ret = new ArrayList<ArrayList<String>>();
		
		Type[] argTypes = randVar.getArgTypes();
		
		if (argInd == -1) {
			for (Type t : argTypes) {
				if (!t.toString().equals("Timestep")) {
					ArrayList<String> extractObjects = new ArrayList<String>();
					for (Object obj : t.getGuaranteedObjects()) {
						extractObjects.add(obj.toString());
					}
					ret.add(extractObjects);
				}
			}
		} else {
			// First filter out 'Timestep' arguments (if present)
			ArrayList <Type> types = new ArrayList<Type>(Arrays.asList(randVar.getArgTypes()));
			for (int i = 0; i < types.size(); i++) {
				if (types.get(i).toString().equals("Timestep"))
					types.remove(i);
			}
			
			Type t = types.get(argInd);
			ArrayList<String> extractObjects = new ArrayList<String>();
			
			for (Object obj : t.getGuaranteedObjects()) {
					extractObjects.add(obj.toString());
			}
			ret.add(extractObjects);
		}
		
		return ret;
	}

	/**
	 * Helper function: Checks if a given Object string corresponds with one of the
	 * argType's guaranteedObjects strings. The match is determined by comparing
	 * their first character. <br>
	 * Goes through the specStrArray JSONArray and returns a type once it finds a match.
	 * 
	 * @param argTypes Type array to be searched for a matching object
	 * @param specStrArr  JSONArray of Strings, specifying objects
	 * @return matching Type if found. else: null
	 */
	private Type findMatchingType(Type[] argTypes, JSONArray specStrArr) {
		for (Type t : argTypes) {
			for (int i = 0; i < specStrArr.length(); i++) {
				if (t.getGuaranteedObject(0).toString().substring(0, 1).equals(specStrArr.getString(i).substring(0, 1))) {
					return t;
				}
			}
		}
		return null;
	}

	/**
	 * Helper function: Searches a guaranteedObjects list for a given object String.
	 * 
	 * @param guarObjs  Object list which will be searched
	 * @param searchObj String of the searched (specified) object
	 * @return true if found, else false
	 */
	private boolean guarObjContainsObj(List<Object> guarObjs, String searchObj) {
		for (Object obj : guarObjs) {
			if (obj.toString().equals(searchObj)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Counts the number of non 'Timestep' types in a given Type array.
	 * 
	 * @param args arguments Type array.
	 * @return number of non 'timestep' types in the array.
	 */
	private int numberNonTimestepArgs(Type[] args) {
		int count = 0;
		for (int i = 0; i < args.length; i++) {
			if (args[i].toString() != "Timestep")
				count++;
		}
		return count;
	}

	/**
	 * Tester function. Prints all info (in a nice way) that was extracted and aligned with the given model wrapper.
	 * @param mw ModelWrapper for specification alignment
	 */
	public void printExtractedInfo(ModelWrapper mw) {
		System.out.println("->>> QuerySpecParser Info");
		ArrayList<String> lines = new ArrayList<String>();

		lines.add(String.format("\t maxTime: %d", getQueryAbsMaxTime()));
		lines.add(String.format("\t maxTimeCutOff: %b", getMaxTimeCutOff()));
		lines.add(String.format("\t timeInc: %d", getQueryTimeInc()));
		lines.add(String.format("\t timeDeltas: %s", Arrays.toString(getQueryTimeDeltas())));

		ArrayList<Function> inclVars = getQueryIncludeVars(mw);
		lines.add("\t includeVars:");
		for (Function var : inclVars)
			lines.add("\t\t" + var.toString());

		ArrayList<SimpleEntry<String, ArrayList<ArrayList<String>>>> restQueries = getQueryRestrictions(mw);
		lines.add("\t queryRestrictions:");
		for (int i=0; i<restQueries.size(); i++) {
			SimpleEntry<String, ArrayList<ArrayList<String>>> curr = restQueries.get(i);
			
			lines.add(String.format("\t\t %d - %s",i+1, curr.getKey()));
			
			ArrayList<ArrayList<String>> objs = curr.getValue();
			for (int j = 0; j < objs.size(); j++) {
				lines.add(String.format("\t\t   Arg type #%d", j+1));
				lines.add(String.format("\t\t     %s", 
						String.join(", ", objs.get(j))));
				
			}
		}
		
		for (String l : lines)
			System.out.println(l);
	}
	
	/**
	 * Checks if an ArrayList of ArrayLists has at least one "real" element at the deepest layer (instead of just empty ArrayLists).
	 * @param al ArrayList of ArrayLists
	 * @return boolean true if list has deep element.
	 */
	private boolean arrayListHasDeepElement(JSONArray al) {
		boolean res = false;
		
		for (int i = 0; i < al.length(); i++) {
			JSONArray b = al.getJSONArray(0);
			if (!b.isEmpty()) {
				res = true;
			}
		}
		
		return res;
	}

}
