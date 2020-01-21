package blogConv.newObjects;

import java.util.ArrayList;
import java.util.Locale;

import blog.LogicalVar;
import blog.Term;
import blogConv.MLNFileFactory;
import blogConv.ModelWrapper;
import fove.Parfactor;

public class MLNParFac {
	/**
	 * Logical variables, e.g. [W, X]
	 */
	private ArrayList<String> logVars = new ArrayList<String>();
	
	/**
	 * DimTerms, e.g. [hot(W), attends(W)]
	 */
	private ArrayList<String> dims = new ArrayList<String>();
	
	/**
	 * Index of the parfactor in the model.
	 */
	private int index;
	
	/**
	 * Number of variables in the parfactor.
	 */
	private int nVars;
	
	/**
	 * Unmodified BLOG parfactor.
	 */
	Parfactor blogParFac;
	
	ModelWrapper mw;
	
	/**
	 * Constructor for MLNParFac-Class. Pulls the BLOG-Parfactor out of the model wrapper 
	 * on its own.
	 * 
	 * @param i index of the ParFac. 
	 */
	public MLNParFac(ModelWrapper mw, int index) {
		this.mw = mw;
		blogParFac = mw.getParfactors().get(index);
		this.index = index;
		this.nVars = blogParFac.dimTerms().size();
		
		extractLogVars(blogParFac);
		extractDimTerms(blogParFac);
	}
	
	/**
	 * Method that handles LogVar extraction out of a parfactor.<br>
	 * Example LogVars: [W,X]
	 * 
	 * @param parf Parfactor to extract the logvars from.
	 */
	private void extractLogVars(Parfactor parf) {
		for(LogicalVar logvar: parf.logicalVars()) {
			logVars.add(MLNFileFactory.translateType(logvar.toString()));
		}
	}
	
	/**
	 * Method that handles DimTerm extraction out of a parfactor. <br>
	 * Example translated DimTerm: 'hot(workshop)'
	 * 
	 * @param parf Parfactor to extract the dimterms from.
	 */
	private void extractDimTerms(Parfactor parf) {
		for(Term term: parf.dimTerms()) {
			String mlnStr = term.toString();
			// Replace variable names with translated ones - one after another.
			for(LogicalVar logvar: parf.logicalVars()) {					
				mlnStr = translateVarsInDimTerm(mlnStr, logvar);					
			}
			
			// Replace timestep literals @1 and @2 (needed for dynamic mlns)
			mlnStr = replaceTimestepLiterals(mlnStr);
			
			dims.add(mlnStr);
			

			
		}
	}
	
	
	/**
	 * Replaces timestep literals '@1' and '@2' in a dimterm string.
	 * Examples:
	 * <pre>
	 * hot(@1) -> hot(t)
	 * hot(@2) -> hot(t+1)
	 * </pre>
	 * 
	 * Needed for dynamic mlns, but not deactivated for normal non-dynamic mlns. 
	 * 
	 * @param str of the dimterm
	 * @return modified dimterm String
	 */
	private String replaceTimestepLiterals(String str) {		
		if (str.contains("@1") || str.contains("@2")) {
			str = str.replace("@1", "t").replace("@2", "t+1");
		}
		
		return str;
	}
	
	
	/**
	 * Helper method that translates the variables given in a dim term. <br>
	 * Example:
	 * <pre>
	 * # Dimterm Conversion: Att(X)->Att(person)
	 * # Dimterm Conversion: Pub(X, P)->Pub(person,journal)
	 * </pre>
	 * 
	 * 
	 * @param str String of dimTerm to be translated
	 * @param logVar LogVar to be replaced. 
	 * @return translated string.
	 */
	private String obsoleteTranslateVarsInDimTerm(String str, LogicalVar logVar) {
		String replace = logVar.toString();
		String replaceWith = MLNFileFactory.translateType(logVar.getType().toString());
		if (str.contains("(") && str.contains(")")) {
			String beginning = str.substring(0,str.indexOf("("));
			String [] objects = str.substring(str.indexOf("(")+1, str.indexOf(")")).split(",");
			
			for(int i = 0; i<objects.length; i++) {
				if (objects[i].trim().equals(replace.trim())) {
					objects[i] = replaceWith;
				}
			}
			
			return beginning + "(" + String.join(",", objects) + ")";
		} else {
			return str;
		}
	}
	
	
	/**
	 * New translate method. For old method see {@link #obsoleteTranslateVarsInDimTerm(String, LogicalVar)}. <br>
	 * 
	 * New conversion (just converts capital letters from blog files to lower case letters):
	 * <pre>
	 * # Dimterm Conversion: Att(X)->Att(x)
	 * # Dimterm Conversion: Pub(X, P)->Pub(x,p)
	 * </pre>
	 * @param str String of dimTerm to be translated
	 * @param logVar LogVar to be replaced. 
	 * @return translated string.
	 */
	private String translateVarsInDimTerm(String str, LogicalVar logVar) {
		String replace = logVar.toString();
		String replaceWith = logVar.toString().toLowerCase();
		if (str.contains("(") && str.contains(")")) {
			String beginning = str.substring(0,str.indexOf("("));
			String [] objects = str.substring(str.indexOf("(")+1, str.indexOf(")")).split(",");
			
			for(int i = 0; i<objects.length; i++) {
				if (objects[i].trim().equals(replace.trim())) {
					objects[i] = replaceWith;
				}
			}
			
			return beginning + "(" + String.join(",", objects) + ")";
		} else {
			return str;
		}
	}

	
	/**
	 * Method that creates an ArrayList of Strings with the weighted formula per
	 * entry. <br>
	 * Example content of one ArrayList element: '-0.2231435513 !hot(workshop) ^
	 * !attends(person)'
	 * 
	 * @return ArrayList<String> with 1 conjunctive formula per entry.
	 */
	private ArrayList<String> createConjClauses() {
		ArrayList<String> clauseStrings = new ArrayList<String>();
		int nClauses = (int) Math.pow(2, nVars);
		for (int i = 0; i < nClauses; i++) {
			String boolStr = String.format("%" + nVars + "s", Integer.toBinaryString(i)).replace(" ", "0");

			double mlnValue = convertValue(mw.getValueFromBinaryString(boolStr, index));
			
			String mlnClause = binaryToClause(boolStr);
			clauseStrings.add(String.format(Locale.ROOT, "%.10f %s", mlnValue, mlnClause));
		}

		return clauseStrings;
	}
	
	/**
	 * Method that converts the value from a BLOG MultiArrayPotential into the 
	 * weight for a logical formula in MLN.
	 * <br>
	 * The Formula is: MLN-Weight = ln(BLOG-Value)
	 * 
	 * @param blogVal long 
	 * @return double value in MLN format
	 */
	private double convertValue(double blogVal) {
		if (blogVal == 0) {
			return -Double.MAX_VALUE;
		} else {
			return Math.log(blogVal);
		}	
	}
	
	/**
	 * Method that takes a binary string (only containing 1's and 0's) and converts it into a clause
	 * based on the dims that are stored in the MLNParFactor Object.
	 * <br>
	 * Example dims: {hot(w), attends(w)}, binaryString = "01" -> clause: "!hot(w) ^ attends(w)"
	 * 
	 * @param binStr String consisting only of 1's and 0's, 
	 * 				length must be equal to the number of dims of the parfactor
	 * @return a String representation of a the clause.
	 */
	private String binaryToClause(String binStr) {
		int n = binStr.length();
		ArrayList<String> literals = new ArrayList<String>();
		for(int i = 0; i<n; i++) {
			if (binStr.charAt(i)== '0') {
				literals.add("!" + dims.get(i));
			} else if (binStr.charAt(i) == '1') {
				literals.add(dims.get(i));
			} else {
				throw new IllegalArgumentException("String passed to binaryToClause is not a binary string");
			}
		}
		return String.join(" ^ ", literals);
	}
	
	
	@Override
	public String toString() {
		return String.join("\n", createConjClauses());
	}	

}
