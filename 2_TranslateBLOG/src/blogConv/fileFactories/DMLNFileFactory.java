package blogConv.fileFactories;

import java.io.PrintStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;

import blog.Main;
import blog.Query;
import blog.Type;
import blogConv.DJTQueryFactory;
import blogConv.ModelWrapper;
import blogConv.EvidenceFactories.AbstrEvidenceFactory;
import blogConv.EvidenceFactories.DMLNEvidenceFactory;
import blogConv.newObjects.MLNParFac;
import blogConv.newObjects.MLNRandVar;
import blogConv.newObjects.MLNType;

public class DMLNFileFactory extends AbstrDynamicFileFactory{

	// #############################
	//			HEADER	
	// #############################
	
	ArrayList<SimpleEntry<String, MLNType>> dmlnTypes = new ArrayList<SimpleEntry<String, MLNType>>();
	ArrayList<SimpleEntry<String, MLNRandVar>> dmlnRandVars = new ArrayList<SimpleEntry<String, MLNRandVar>>();
	ArrayList<MLNParFac> dmlnParFacs = new ArrayList<MLNParFac>();
	
	
	public DMLNFileFactory (ModelWrapper mw) {
		super(mw, "mln");
		extractTypes();
		extractRandVars();
		extractParFactors();
		this.evidenceFactory = new DMLNEvidenceFactory(mw);
	}
	
	public DMLNFileFactory(ModelWrapper mw, AbstrEvidenceFactory abstrEF) {
		super(mw, "mln");
		this.evidenceFactory = new DMLNEvidenceFactory(mw, abstrEF);
	}

	
	@Override
	public String createModelFileString() {
		return createModelFileString(3);
	}
	
	
	public String createModelFileString(int maxTimestep) {
		StringBuffer sb = new StringBuffer();
		
		// 0. Header
		sb.append("// Automatically translated from BLOG into DMLN (dynamic MLN) format for UUMLN:\n");
		
		// 1. RandVars
		for(SimpleEntry<String, MLNRandVar> rv : dmlnRandVars) {
			sb.append(rv.getValue().toString()).append("\n");
		}
		sb.append("\n");
		
		// 2. Types
		for(SimpleEntry<String, MLNType> t : dmlnTypes) {
			sb.append(t.getValue().toString()).append("\n");
		}
		// [DMLN] -> Add Time type
		sb.append("\n// Mandatory Time Type").append("\n");
		sb.append(this.createTimeTypeString(maxTimestep)).append("\n");
		
		// 3. Parfactors
		for(MLNParFac parfac : dmlnParFacs) {
			sb.append(parfac.toString()).append("\n\n");
		}
		
		// 4. Queries (if existent in BLOG file)
		if (mw.getQueries().size()>0) 
			sb.append(createQueryString());
		
		// 5. Evidence if existent
		// > Written in extra .db-file for DMLNs.
		
		return sb.toString();
	}
	
	/**
	 * Central method that defines how a type name should be translated from BLOG to the new 
	 * 
	 * @param blogString input String that comes from the BLOG file.
	 * @return String representation of the transformed string.
	 */
	public static String translateType(String blogString) {
			return blogString.trim().toLowerCase();
	}
	

	public static String translateGuaranteedObj(String blogString) {
		return Character.toUpperCase(blogString.trim().charAt(0)) + blogString.toString().trim().substring(1);
	}
	
	/**
	 * Creates the mandatory TimeTypeString for the dmln file. 
	 * @return Time type line for the output dmln file as String.
	 */
	private String createTimeTypeString(int maxTimeStep) {
		return String.format("Time = {1-%d}", maxTimeStep);
	}
	
	
	// #############################
	//			TYPES
	// #############################
	
	/**
	 * Method to extract types from the modelwrapper and add them to the mlnTypes ArrayList.
	 */
	private void extractTypes() {
		for (Type t: mw.getTypes()) {
			dmlnTypes.add(new SimpleEntry<String, MLNType>(t.getName(), new MLNType(t)));
		}
	}
		
	// #############################
	//			RANDVARS
	// #############################
	
	private void extractRandVars() {
		// RandVars (e.g. hot(Workshop), attends(Person), series) are stored as functions
		// Functions have a signature. (see printFunctions).
		ArrayList<String> randVarStr = new ArrayList<String>(mw.getRandVarNames());
		
		for (String str : randVarStr) {
			dmlnRandVars.add(new SimpleEntry<String, MLNRandVar> (str, new MLNRandVar(super.mw, str, "Timestep", "Time")));
		}
				
		// mln-Output:  series 			-- without parameter
		//				hot(w)			-- with parameter
	}
	
	// #############################
	//			PARFACTORS 				(with MultiArrayPotentials)
	// #############################
	
	private void extractParFactors() {
		for (int i=0; i < mw.getParfactors().size(); i++) {
			dmlnParFacs.add(new MLNParFac(super.mw, i));
		}
	}
		
	// #############################
	//			QUERIES
	// #############################
	// In MLN Queries cannot actually be included in the input files' semantic.
	// We will just convert queries from BLOG files to comments in the mln file.
	
	/**
	 * Wrapper method that creates one big string for the queries, including an
	 * explanation why they are put in comments.
	 * 
	 * @return String representation of all query elements
	 */
	private String createQueryString() {
		String result = "// [Queries]\n"+
				"// Queries are commented out, because they need to be passed via the console, not in the file.\n"
				+ "// The corresponding query strings are (1 per line):\n";
		ArrayList<String> queries = transformQueries();
		
		for (String q: queries) {
			result += "// " + q + "\n";
		}
		
		return result;
	}
	
	
	/**
	 * Method that pulls the queries from the ModelWrapper and transforms them into the MLN syntax.
	 * 
	 * @return ArrayList<String> of transformed query strings.
	 */
	private ArrayList<String> transformQueries() {
		ArrayList<Query> blogQueries = mw.getQueries();
		ArrayList<String> convQueries = new ArrayList<String>();
		
		for (Query q: blogQueries) {
			String str = q.toString();
			// if the query contains a variable (e.g. 'hot(p25)'), we need to translate it.			
			if (str.contains("(") && str.contains(")")) {
				str = translateObjectsInQuery(q);
			}
			convQueries.add(str);
		}
		return convQueries;
	}
	
	/**
	 * Method that translates the referenced objects in a query.
	 * 
	 * @param blogString String representation of whole query as in BLOG.
	 * @return String representation of the whole query in MLN syntax.
	 */
	private String translateObjectsInQuery(Query q) {
		String blogString = q.toString();
		String beginning = blogString.substring(0, blogString.indexOf("("));
		String objectsStr = blogString.substring(blogString.indexOf("(")+1, blogString.indexOf(")"));
		
		String[] objects = objectsStr.split(",");
		for (int i = 0; i < objects.length; i++) {
			int size = getTypeByObject(objects[i]).getGuaranteedObjects() != null ?getTypeByObject(objects[i]).getGuaranteedObjects().size() : -1; 
			if (size < 0) {
				// Not found in guaranteed objects.
				System.out.printf("There was an error in query translation: %s was not found as guaranteed object.", objects[i]);
				System.exit(1);
			} else if (Main.shortFormAllowed && size > 10) {
				// Remove all non-numeric characters if big size (short form in declaration is used then)
				objects[i] = objects[i].replaceAll("[^\\d]", "");
			} else {
				objects[i] = translateGuaranteedObj(objects[i]);
			}
		}
		
		
		return String.format("%s(%s)", beginning, String.join(",", objects));
	}
	
	
	/**
	 * Method that goes through the MLNTypes in this FileFactory and tries to find
	 * out if a given object entity is existent in one of the types' guaranteed
	 * objects. Returns the type if that is the case - returns null otherwise.
	 * 
	 * We need this to determine how the object shall be represented in a query
	 * (depending on the size and representation of the type in its declaration)
	 * 
	 * @param object String to be searched.
	 * @return MLNType object of the containing type, or null if object not found
	 */
	private MLNType getTypeByObject(String object) {
		object = translateGuaranteedObj(object);
		
//		System.out.println("### looking for: ["+object+"]");
		
		for (SimpleEntry<String, MLNType> entry : dmlnTypes) {
			ArrayList<String> guaranteedObj = entry.getValue().getGuaranteedObjects();
			for (String go : guaranteedObj) {
//				System.out.println("["+go+"]");
				if (object.equals(go)) {
//					System.out.println("Found a match!");
					return entry.getValue();
				}
			}
		}
		
		return null;
	}

	@Override
	public void saveFile(String path) {
		
		DJTQueryFactory qf = new DJTQueryFactory(mw, false, this.evidenceFactory);
		int[] querySizes = qf.getRawQuerySizesArray();
		
		if (querySizes.length > 1) {
			System.out.printf(">> Creating %d output mln files with different query sizes.\n", querySizes.length);
		} 
		
		for (int qs : querySizes) {
			String usePath = querySizes.length == 1? path : insertQuerySizeIntoPath(path, qs);
			
			String fileCont = this.createModelFileString(qs);
			System.out.println("Output file: "+usePath);
			super.saveFile(usePath, fileCont);
			 
			// Create extra .db files for evidence
			String evidenceLines = this.createEvidenceLines(qs);
			if (!evidenceLines.equals("")) {
				// Just create file in case there are evidence lines.
				usePath = usePath.replace(".mln", ".db");
				super.saveFile(usePath, evidenceLines);
			}
		}
	}
	
	/**
	 * Create the lines for evidence creation. 
	 * 
	 * @param maxTimesteps 
	 * @return evidence lines as String.
	 */
	public String createEvidenceLines(int maxTimesteps) {
		return(this.evidenceFactory.createEvidenceLines(maxTimesteps));
	}
	
	@Override
	public String createEvidenceFileString() {
		System.err.println("Evidence not supported yet for DMLNs");
		System.exit(1);
		return null;
	}
	
	
}
