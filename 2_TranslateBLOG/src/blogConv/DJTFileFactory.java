package blogConv;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import blog.Function;
import blog.LogicalVar;
import blog.Term;
import blog.Type;
import fove.Parfactor;

public class DJTFileFactory extends AbstrFileFactory {
	
	/**
	 * Boolean value indicating whether the file should be created with
	 * 			a) dynamic timesteps (true) or b) statically rolled out (false)
	 */
	private boolean dynamicTimesteps;
	
	/**
	 * Evidence factory if evidence should be copied (and not created new). 
	 */
	private DJTEvidenceFactory evidenceFactory = null;
	
	/**
	 * 
	 * @param mw
	 * @param dynamicTimesteps boolean indicating whether the file should be created with
	 * 			a) dynamic timesteps (true) or b) statically rolled out (false)
	 */
	public DJTFileFactory(ModelWrapper mw, boolean dynamicTimesteps) {
		super(mw, "blog");
		this.dynamicTimesteps = dynamicTimesteps;
	}
	
	
	public DJTFileFactory(ModelWrapper mw, boolean dynamicTimesteps, DJTEvidenceFactory ef) {
		super(mw, "blog");
		this.dynamicTimesteps = dynamicTimesteps;
		this.evidenceFactory = ef;
	}

	@Override
	public String createModelFileString() {
		String str = "";

		// 1. Types
		str += createTypeLines();

		// 1b. Guaranteed objects
		str += createGuaranteedLines();

		// 2. RandVars
		str += createRandVarLines();

		// 3. ParFactors
		str += createParfacLines();
		
		// 4. Evidence
		// Evidence is handled in saveFile() and createEvidenceLines().
		
		// 5. Queries
		// Queries are handled in the saveFile() method below (due to multiple possible query settings)
				
		
//		System.out.println(str);
		return str;
	}
	
	/**
	 * Getter for evidence factory.
	 * @return
	 */
	public DJTEvidenceFactory getEvidenceFactory() {
		return this.evidenceFactory;
	}
	
	/**
	 * Create the lines for evidence creation. If no EvidenceFactory is set (this can be done on object instantiation, with the corresponding
	 * constructor) a new EvidenceFactory is created. 
	 * Otherwise takes the EvidenceFactory that was passed to the constructor. 
	 * 
	 * @param maxTimesteps
	 * @return evidence lines as String.
	 */
	public String createEvidenceLines(int maxTimesteps) {
		DJTEvidenceFactory ef = null;
		if (this.evidenceFactory == null) {
			ef = new DJTEvidenceFactory(mw, maxTimesteps);
		} else {
			ef = this.evidenceFactory;
		}
		return(ef.createEvidenceLines(this.dynamicTimesteps));
	}
	
	
	/**
	 * Creates a type line like
	 * 
	 * <pre>
	 * type Person;
	 * </pre>
	 * 
	 * @return String with the line, including line break.
	 */
	private String createTypeLines() {
		String ret = "";
		for (Type t : mw.getTypes()) {
			ret += String.format("type %s;\n", t.toString());
		}
		return ret + "\n";
	}

	/**
	 * Creates a guaranteed objects line like
	 * 
	 * <pre>
	 * guaranteed Person p[10];
	 * </pre>
	 * 
	 * @return String with the line, including line break.
	 */
	private String createGuaranteedLines() {
		String ret = "";
		for (Type t : mw.getTypes()) {
			ret += String.format("guaranteed %s %s[%d];\n", t.toString(), // Name (e.g. Person)
					t.getGuaranteedObject(0).toString().subSequence(0, 1), // First letter of object (e.g. p)
					t.getGuaranteedObjects().size()); // Number of objects (e.g. 10)

			// -> guaranteed Person p[10];

		}
		return ret + "\n";
	}

	/**
	 * Creates a randvar line like
	 * 
	 * <pre>
	 * random Boolean Hot1;
	 * random Boolean Hot2;
	 * </pre>
	 * 
	 * Includes checking whether the given randvar X is dynamic (i.e. contains
	 * 'Timestep' as argument). If that is the case, the randvar is split up into X1
	 * and X2 and 'Timestep' is removed from its arguments.
	 * 
	 * @return String with the line(s), including line break(s)
	 */
	private String createRandVarLines() {
		String ret = "";

		// For each randvar ...
		for (String rv_name : mw.getRandVarNames()) {
			boolean dyn = false;
			Function rv = mw.getRandVarByName(rv_name);

			// ... check if it has 'Timestep' as argument
			for (Type arg : rv.getArgTypes()) {
				if (arg.toString().equals("Timestep"))
					dyn = true;
			}

			if (dyn)
				ret += createDynamicRandVarLine(rv);
			else
				ret += String.format("random Boolean %s;\n", rv.getSig());
		}

		return ret + "\n";
	}

	/**
	 * Splits up a dynamic randvar 'X(Timestep, NotTimestep)' (i.e. randvar that has
	 * 'Timestep' as argument) into `X1(NotTimestep)` and `X2(NotTimestep)`.
	 * 
	 * <pre>
	 * DoR(Timestep,Person) -> DoR1(Person), DoR2(Person)
	 * Hot(Timestep) -> Hot1, Hot2
	 * </pre>
	 * 
	 * @param rv
	 * @return
	 */
	private String createDynamicRandVarLine(Function rv) {
		String ret = "";

		// 1. Extract all non-dynamic arguments
		ArrayList<String> non_time_args = new ArrayList<String>();
		for (Type arg : rv.getArgTypes()) {
			if (!arg.toString().equals("Timestep"))
				non_time_args.add(arg.toString());
		}

		// 2. Split up into two variables (Var1, Var2)

		for (int i = 1; i <= 2; i++) {
			ret += String.format("random Boolean %s%d", rv.getName().toString(), i);
			if (non_time_args.size() > 0) {
				ret += String.format("(%s)", String.join(",", non_time_args));
			}
			ret += ";\n";
		}

		return ret + "\n";
	}

	/**
	 * Wrapper function for parfac line creation. Includes following information:
	 * Non-dynamic parfacs, dynamic parfacs (1 timestep or timestep transition)
	 * 
	 * @return String representation of parfac lines (for printing in DJT file)
	 */
	private String createParfacLines() {
		String ret = "";

		for (Parfactor pf : mw.getParfactors()) {

			boolean contains_1 = false, contains_2 = false;

			for (Term dimTerm : pf.dimTerms()) {
				if (dimTerm.toString().contains("@1"))
					contains_1 = true;
				if (dimTerm.toString().contains("@2"))
					contains_2 = true;
			}

			// 1. Handle parfacs WITHOUT DYNAMIC RANDVARS
			// -> does not contain '@1'
			if (!contains_1 && !contains_2) {
				ret += "// Non-dynamic parfactor\n" + linesForNonDynParfac(pf);
			}
			// 2. Handle parfacs WITH (DIFFERENT) DYNAMIC RANDVARS AT ONE TIMESTEP
			// -> contains at least one '@1' (and no '@2')
			else if (contains_1 && !contains_2) {
				ret += "// Dynamic Parfactor (one timestep) \n" + linesForRedundantParfac(pf);
			}
			// 3. Handle parfacs WITH (DIFFERENT) DYNAMIC RANDVARS AT MULTIPLE TIMESTEPS
			// -> contains both '@1' and '@2'
			else if (contains_1 && contains_2) {
				ret += "// Dynamic Parfactor (timestep transition) \n" +linesForTransitionParfac(pf);
			}
			// 4. Contains only '@2'?`-> Invalid!
			else if (!contains_1 && contains_2) {
				String msg = String.format("Found dynamic parfactor with just '@2' (and no '@1') here: %s. \nInvalid input!", pf.getLocation());
				System.err.print(msg);
				System.exit(1);
			}
		}

		return ret;
	}

	/**
	 * Creates the lines for a non-dynamic parfactor (i.e. doesn't contain '@1' or
	 * '@2').
	 * 
	 * Sample output (equivalent to input!):
	 * 
	 * <pre>
	 * parfactor Person X, Conference C. MultiArrayPotential[[8, 7, 6, 5, 4, 3, 2, 1]]
	 * 				(Hot, AttC(C), DoR(X));
	 * </pre>
	 * 
	 * @param pf Parfactor to create the lines for
	 * 
	 * @return lines as String
	 */
	private String linesForNonDynParfac(Parfactor pf) {
		String ret = String.format("parfactor %s. MultiArrayPotential[[%s]]\n", parfacTypeString(pf),
				parfacValueString(pf));

		// Translate dimTerms into arrayList
		ArrayList<String> dimTerms = new ArrayList<String>();
		for (Term dt : pf.dimTerms()) {
			String name = dimTermName(dt);

			ArrayList<String> args = new ArrayList<String>();
			for (Object arg : dt.getSubExprs()) {
				args.add(arg.toString());
			}

			if (args.size() > 0)
				dimTerms.add(String.format("%s(%s)", name, String.join(", ", args)));
			else
				dimTerms.add(name);
		}

		ret += String.format("\t(%s);\n", String.join(", ", dimTerms)) + "\n";

		return ret;
	}

	/**
	 * Helper function: Creates the value string for a parfactor representation.
	 * Example:
	 * 
	 * <pre>
	 * 8, 7, 6, 5, 4, 3, 2, 1
	 * </pre>
	 * 
	 * Position:
	 * 
	 * <pre>
	 * parfactor {type string}.MultiArrayPotential [[{value string}]]
	 * 
	 * </pre>
	 * 
	 * @param pf parfactor to extract the string from.
	 * @return value string
	 */
	private String parfacValueString(Parfactor pf) {
		ArrayList<String> values = new ArrayList<String>();
		for (String bool_str : createValueBools(pf.dimTerms().size())) {
			DecimalFormat format = new DecimalFormat("0.#");
			values.add(format.format(mw.getValueFromBinaryString(bool_str, pf)));
		}

		return String.join(", ", values);
	}

	/**
	 * Helper function: Creates the type string for a parfactor representation.
	 * Example:
	 * 
	 * <pre>
	 * X Person, P Publication, C Converence
	 * </pre>
	 * 
	 * Position:
	 * 
	 * <pre>
	 * parfactor {type string}.MultiArrayPotential [[{value string}]]
	 * 
	 * </pre>
	 * 
	 * @param pf parfactor to extract the string from.
	 * @return type string
	 */
	private String parfacTypeString(Parfactor pf) {
		ArrayList<String> types = new ArrayList<String>();

		for (LogicalVar lv : pf.logicalVars()) {
			types.add(lv.getType().toString() + " " +lv.getName());
		}

		return String.join(", ", types);
	}

	/**
	 * Extracts the name of a given dimterm dt <br>
	 * Example: dimterm = 'AttC(@1, P)' -> result: AttC
	 * 
	 * @param dt dimterm to extract the name from
	 * @return name of dt as String.
	 */
	private String dimTermName(Term dt) {
		String str = dt.toString();
		if (str.contains("("))
			return str.substring(0, str.indexOf("("));
		else
			return str;
	}

	/**
	 * Example input:
	 * 
	 * <pre>
	 * parfactor Person X, Conference C. MultiArrayPotential[[8, 7, 6, 5, 4, 3, 2, 1]]
	 * 				(Hot(@1), AttC(@1,C), DoR(@1,X));
	 * </pre>
	 * 
	 * Example output:
	 * 
	 * <pre>
	 * parfactor Person X, Conference C. MultiArrayPotential[[8, 7, 6, 5, 4, 3, 2, 1]]
	 * 				(Hot1, AttC1(C), DoR1(X));
	 * 
	 * parfactor Person X, Conference C. MultiArrayPotential[[8, 7, 6, 5, 4, 3, 2, 1]]
	 * 				(Hot2, AttC2(C), DoR2(X));
	 * </pre>
	 * 
	 * @return String containing all lines including line breaks
	 */
	private String linesForRedundantParfac(Parfactor pf) {
		String ret = "";

		// Do everything twice (once for Var1, once for Var2)
		for (int i = 1; i <= 2; i++) {
			// First line stays the same.
			String first_line = String.format("parfactor %s. MultiArrayPotential[[%s]]\n", parfacTypeString(pf),
					parfacValueString(pf));

			// Translate dimTerms into arrayList
			ArrayList<String> dimTerms = new ArrayList<String>();
			for (Term dt : pf.dimTerms()) {
				String name = dt.toString().contains("@1") ? dimTermName(dt) + i : dimTermName(dt);

				ArrayList<String> args = new ArrayList<String>();
				for (Object arg : dt.getSubExprs()) {
					if (!arg.toString().contains("@1"))
						args.add(arg.toString());
				}

				if (args.size() > 0)
					dimTerms.add(String.format("%s(%s)", name, String.join(", ", args)));
				else
					dimTerms.add(name);
			}

			String sec_line = String.format("\t(%s);\n", String.join(", ", dimTerms));

			ret += first_line + sec_line + "\n";

		}

		return ret + "\n";
	}

	/**
	 * Helper function that creates a list of binary terms given a number of dim
	 * terms. In those strings in the returned ArrayList, there will be counted from
	 * 000, 001 ... to (2^n)-1 in binary representation
	 * 
	 * @param numberDimTerms integer describing the number of dimTerms of a
	 *                       parfactor.
	 * @return ArrayList of Strings with binary numbers.
	 */
	private ArrayList<String> createValueBools(int numberDimTerms) {
		ArrayList<String> result = new ArrayList<String>();

		for (int i = (int) Math.pow(2, numberDimTerms) - 1; i >= 0; i--) {
			result.add(String.format("%" + numberDimTerms + "s", Integer.toBinaryString(i)).replace(" ", "0"));
		}
		return result;
	}

	/**
	 * Creates the lines for a dynamic parfactor that describes a timestep
	 * transition (i.e. contains '@1' AND '@2').
	 * 
	 * Sample input:
	 * 
	 * <pre>
	 * parfactor Person X, Publication P, Conference C. MultiArrayPotential[[8, 7, 6, 5, 4, 3, 2, 1, 8, 7, 6, 5, 4, 3, 2, 1]]
	 * 				(Hot(@1), Hot(@2), Pub(@1,X,P), AttC(@1,C));
	 * </pre>
	 * 
	 * Sample output:
	 * 
	 * <pre>
	 * parfactor Person X, Publication P, Conference C. MultiArrayPotential[[8, 7, 6, 5, 4, 3, 2, 1, 8, 7, 6, 5, 4, 3, 2, 1]]
	 * 				(Hot1, Hot2, Pub1(X,P), AttC1(C));
	 * </pre>
	 * 
	 * @return
	 */
	private String linesForTransitionParfac(Parfactor pf) {
		String ret = String.format("parfactor %s. MultiArrayPotential[[%s]]\n", parfacTypeString(pf),
				parfacValueString(pf));

		// Translate dimTerms into arrayList
		ArrayList<String> dimTerms = new ArrayList<String>();
		for (Term dt : pf.dimTerms()) {
			String name = dimTermName(dt);
			if (dt.toString().contains("@1")) {
				name = dimTermName(dt) + "1";
			} else if (dt.toString().contains("@2")) {
				name = dimTermName(dt) + "2";
			}

			ArrayList<String> args = new ArrayList<String>();
			for (Object arg : dt.getSubExprs()) {
				if (!arg.toString().contains("@1") && !arg.toString().contains("@2"))
					args.add(arg.toString());
			}

			if (args.size() > 0)
				dimTerms.add(String.format("%s(%s)", name, String.join(", ", args)));
			else
				dimTerms.add(name);
		}

		ret += String.format("\t(%s);\n", String.join(", ", dimTerms)) + "\n";

		return ret;
	}
	
	
	/**
	 * Wrapper function for Query line creation.
	 * @return
	 */
	private String createQueryLines() {
		return "";
	}
	

	@Override
	public String createEvidenceFileString() {
		System.err.println("Evidence not supported yet for DMLNs");
		System.exit(1);
		return null;
	}
	
	@Override
	public void saveFile(String path) {
		
		String fileCont = this.createModelFileString();
		
		DJTQueryFactory qf = new DJTQueryFactory(mw, this.dynamicTimesteps);
		HashMap<Integer, String> map = qf.createAllQuerySizes();
		if (map.size() > 1) {
			System.out.printf(">> Creating %d output blog files with different query sizes.\n", map.size());
		}
		for (Map.Entry<Integer, String> entry : map.entrySet()) {
			String usePath = map.size() == 1? path : insertQuerySizeIntoPath(path, entry.getKey());

			System.out.println("Output file: "+usePath);
			super.saveFile(usePath, fileCont +"\n" + this.createEvidenceLines(entry.getKey())+ "\n" + entry.getValue());
		}
	}
	

}
