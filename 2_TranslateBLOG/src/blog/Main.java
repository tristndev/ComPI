package blog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import blogConv.AbstrFileFactory;
import blogConv.DJTFileFactory;
import blogConv.DMLNFileFactory;
import blogConv.DJTQueryFactory;
import blogConv.MLNFileFactory;
import blogConv.ModelWrapper;
import common.Util;
import common.cmdline.BooleanOption;
import common.cmdline.Parser;
import common.cmdline.StringListOption;
import common.cmdline.StringOption;

public class Main {
	private static List filenames;
	private static Properties inferenceProps;
	private static boolean randomize;
	private static int numSamples;
	private static int numStatSamples;
	private static Model model;
	private static Evidence evidence;
	private static List<Query> queries;
	private static boolean generate;
	private static List packages;
	private static boolean verbose;
	private static boolean debug;
	private static String outputPath;
	private static int outputInterval;
	private static String histOut;
	private static List setupExtenders;
	public static boolean printToConsole;
	public static boolean shortFormAllowed;
	public static String outputFormat;

	static {
		Main.randomize = false;
		Main.printToConsole = false;
		Main.model = new Model();
		Main.evidence = new Evidence();
		Main.queries = new ArrayList<Query>();
		Main.setupExtenders = new ArrayList();
		Main.inferenceProps = new Properties();
		Main.outputFormat = "mln";
	}

	public static void main(final String[] array) {
		Main.debug = true;
		parseOptions(array);
		Util.setVerbose(Main.verbose);
		// Util.initRandom(Main.randomize);
		BLOGParser.setPackagesToSearch(Main.packages);

		// Handle given filename arguments
		ArrayList<String> filesToConvert = new ArrayList<String>();
		// Check if given is path is not a blog-file but a directory
		for (int i = 0; i < Main.filenames.size(); i++) {
			String currPath = (String) Main.filenames.get(i);

			File file = new File(currPath);

			if (file.exists()) {
				if (file.isDirectory()) {
					System.out.printf("Argument no. %d ('%s') is a directory. Looking for .blog-files there...\n", i,
							file);
					filesToConvert.addAll(findBLOGinDir(currPath));
				} else {
					System.out.printf("Argument no. %d ('%s') is a file.\n", i, file);
					filesToConvert.add(currPath);
				}
			} else {
				System.out.printf(
						"Argument no. %d ('%s') cannot be found in file system. Resuming with remaining arguments (if there are any).\n",
						i, file);
			}
		}

		System.out.println("\n### Starting file conversion...");
		String outputDirName = "_TranslateBLOG";
		createOutputDir(outputDirName, filesToConvert.get(0));
		for (int i = 0; i < filesToConvert.size(); i++) {
			System.out.printf("+++ file # %4d - %s\n", i + 1, filesToConvert.get(i));

			/*
			 * TODO: Remove from here resetModel();
			 * 
			 * setup(Main.model, Main.evidence, Main.queries,
			 * makeReaders(Arrays.asList(filesToConvert.get(i))), Main.setupExtenders,
			 * Util.verbose(), true); ModelWrapper mw = new ModelWrapper(Main.model,
			 * Main.queries);
			 * 
			 * 
			 * DMLNQueryFactory qf = new DMLNQueryFactory(filesToConvert.get(i), mw);
			 * BLOGQuerySpecParser sp = new BLOGQuerySpecParser(filesToConvert.get(i));
			 * 
			 * for (String line: qf.createQueryLines()) System.out.println(line);
			 * 
			 * 
			 * //sp.printExtractedInfo(mw);
			 * 
			 * 
			 * 
			 * // TODO: REMOVE TILL HERE
			 */

			handleSingleFile(filesToConvert.get(i), outputDirName);
		}

		System.out.println("\n>>> All work done!");
	}

	public static ArrayList<String> findBLOGinDir(String dirPath) {
		ArrayList<String> stringPaths = new ArrayList<String>();
		File dir = new File(dirPath);
		File[] blogFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".blog"));

		for (File f : blogFiles) {
			stringPaths.add(f.toString());
		}
		System.out.printf("Found %d .blog-files in directory '%s'.\n", stringPaths.size(), dirPath);
		return stringPaths;
	}

	/**
	 * Method to handle a single file. This includes: Model creation & output.
	 * 
	 * @param String
	 *            filename of the file to be handled.
	 * @param String
	 *            name of the target (output) dir
	 */
	private static void handleSingleFile(String filename, String targetDir) {
		resetModel();

		setup(Main.model, Main.evidence, Main.queries, makeReaders(Arrays.asList(filename)), Main.setupExtenders,
				Util.verbose(), true);
		ModelWrapper mw = new ModelWrapper(Main.model, Main.queries, filename);

		// --- OVERVIEW
		// mw.printParams();

		// --- TYPES
		// System.out.println(mw.getTypeByName("Person"));

		// --- FUNCTIONS / RANDVARS
		// mw.printFuncs();

		// 1. Collect all randvar-names
		// System.out.println(mw.getRandVarNames());
		// 2. Get information per randvar by accessing the function with the
		// randvar-name.
		// System.out.println(mw.getRandVarByName("DoR").getSig());

		// --- PARFACTORS
		// mw.printMultiArrayPotential(0);
		// mw.printMultiArrayPotentials(1);

		// mw.getMultiArrayPotential(0);

		// --- EVIDENCE
		// System.out.println("Printing evidence...");
		// System.out.println(Main.evidence);

		// System.out.println(">> getEvidenceVars()");
		// System.out.println(Main.evidence.getEvidenceVars());

		// for (Object obj: Main.evidence.getEvidenceVars()) {
		// BayesNetVar var = (BayesNetVar) obj;
		// System.out.println(var);
		// }

		// System.out.println(">> getValueEvidence()");
		// System.out.println(Main.evidence.getValueEvidence());

		// for (Object obj: Main.evidence.getValueEvidence()) {
		// ValueEvidenceStatement ev = (ValueEvidenceStatement) obj;
		// System.out.println("Var: "+ ev.getObservedVar() +" | Value: " +
		// ev.getObservedValue().toString());
		// }

		// ## FILE FACTORIES ##

		AbstrFileFactory fileFac;
		String outPath = "";
		if (Main.outputFormat.toLowerCase().equals("mln")) {
			System.out.println(">  Chosen output format: 'mln' (default)");
			outPath = generateOutputPath(filename, "mln", targetDir);
			fileFac = new MLNFileFactory(mw, Main.evidence);

			fileFac.saveFile(outPath);
			System.out.println(filename + " done.");
		} else if (Main.outputFormat.toLowerCase().equals("dmln")) {
			System.out.println(">  Chosen output format: 'dmln' (dynamic MLN for UUMLN)");

			// File 1: DMLN file
			String tempPath = generateOutputPath(filename, "mln", targetDir);
			outPath = generateOutputPath(filename, "mln", targetDir + "/MLN");
			createOutputDir("MLN", tempPath);
			fileFac = new DMLNFileFactory(mw);
			fileFac.saveFile(outPath);

			// File 2: BLOG file for DJT application
			createOutputDir("BLOG_dyn", tempPath);
			outPath = generateOutputPath(filename, "blog", targetDir + "/BLOG_dyn");
			fileFac = new DJTFileFactory(mw, true);
			fileFac.saveFile(outPath);

			// File 3: BLOG file for static DJT
			createOutputDir("BLOG_stat", tempPath);
			outPath = generateOutputPath(filename, "blog", targetDir + "/BLOG_stat");
			// TODO: Hand over evidence factory from dynamic file.
			fileFac = new DJTFileFactory(mw, false, ((DJTFileFactory) fileFac).getEvidenceFactory());
			fileFac.saveFile(outPath);

			System.out.println(filename + " done.");
		} else {
			System.err.println("> Error in creating the right file factory.");
			System.err.println(String.format("  Chosen outputFormat is: '%s'", Main.outputFormat));
			System.exit(1);
			return;
		}

		// 1. Generate model, parse BLOG
		// 2. Translate to MLN / whatever
		// 3. Output handling
		// a) if (Main.printToConsole) -> print to console
		// b) Save file to Main.outputPath
	}

	/**
	 * Takes a filePath (i.e. path to a file, not a dir) and a dirName, goes up to
	 * the file's parent directory and creates a new directory with name dirName.
	 * 
	 * @param dirName
	 * @param filePath
	 */
	private static void createOutputDir(String dirName, String filePath) {
		File file = new File(filePath);
	
		System.out
				.println("### Setting output folder (will be created if needed): " + file.getParent() + "/" + dirName);
		new File(file.getParent() + "/" + dirName).mkdirs();
	}

	
	private static void createDir(String dirName, String dirPath) {
		File dir = new File(dirPath);

		System.out.println("### Creating folder if needed: " + dir + "/" + dirName);
		
		new File(dir + "/" + dirName).mkdirs();
	}

	/**
	 * Method to reset
	 */
	private static void resetModel() {
		Main.model = new Model();
		Main.evidence = new Evidence();
		Main.queries = new ArrayList<Query>();
	}

	/**
	 * @author Tristan Potten
	 * 
	 *         Small helper function that is called if no output file name is
	 *         specified. Removes the ".blog" extension of the input file name and
	 *         replaces it with a choosable new extension.
	 * 
	 * @param inputFilename
	 *            input string given to the jar
	 * @param newExtension
	 *            desired new extension
	 * @param outputDir
	 *            target directory
	 * @return filename with the old extension replaced with the new one.
	 */
	public static String generateOutputPath(String inputFilename, String newExtension, String outputDir) {
		// 1. Insert output directory
		String parentPath = new File(inputFilename).getParent();
		String fileName = new File(inputFilename).getName();
		String targetPath = parentPath + "/" + outputDir + "/" + fileName;

		System.out.println("Filename: " + fileName);

		int startInd = targetPath.lastIndexOf(".");

		String ret = targetPath.substring(0, startInd) + "." + newExtension;

		return ret;
	}

	// added <String>
	public static List makeReaders(final Collection<String> collection) {
		final LinkedList<Object[]> list = new LinkedList<Object[]>();
		for (final String s : collection) {
			try {
				list.add(new Object[] { new FileReader(s), s });
			} catch (FileNotFoundException ex) {
				System.err.println("File not found: " + s);
				Util.fatalError((Throwable) ex);
			}
		}
		return list;
	}

	private static boolean semanticsCorrect(final Model model, final Evidence evidence, final List list) {
		boolean b = true;
		if (!model.checkTypesAndScope()) {
			b = false;
		}
		if (!evidence.checkTypesAndScope(model)) {
			b = false;
		}
		final Iterator<Query> iterator = list.iterator();
		while (iterator.hasNext()) {
			if (!iterator.next().checkTypesAndScope(model)) {
				b = false;
			}
		}
		if (!model.checkCompleteness()) {
			b = false;
		}
		return b;
	}

	private static void generateWorlds() {
		final RejectionSampler rejectionSampler = new RejectionSampler(Main.model, Main.inferenceProps);
		rejectionSampler.initializeCompleteSampling();
		System.out.println("Sampling " + Main.numSamples + " worlds from prior...");
		System.out.println();
		for (int i = 0; i < Main.numSamples; ++i) {
			rejectionSampler.nextSample();
			rejectionSampler.getLatestWorld().print(System.out);
			System.out.println();
		}
	}

	private static void parseOptions(final String[] array) {
		// Set Parser description
		Parser.setProgramDesc(">> TranslateBLOG - File converter");
		// Set basic usage line (printed by printUsage).
		Parser.setUsageLine(">  Usage: java -jar TranslateBLOG.jar [args] [<file> | <directory>] ");

		// A) ------ OPTION DEFINITION --------

		// ##########################
		// # Custom options #
		// ##########################
		final StringOption outputOption = new StringOption("o", "output", (String) null, "Create output file <s>");
		final BooleanOption consoleOption = new BooleanOption("c", "console_mode", false, "Show output in console");

		final StringOption targetFormatOption = new StringOption("f", "format", "mln",
				"Select target format out of {mln, dmln}");

		final BooleanOption shortFormOption = new BooleanOption("s", "short_form", false,
				"Allow short forms in output (if available)");

		// ##########################
		// # GCFove Options #
		// ##########################
		final StringListOption stringListOption = new StringListOption("k", "package",
				"Parser looks for classes in package <s>");
		final BooleanOption booleanOption3 = new BooleanOption("v", "verbose", false,
				"Print info about every world sampled");
		final BooleanOption booleanOption4 = new BooleanOption("g", "debug", false,
				"Print model, evidence, and queries");

		// B) ------ OPTION HANDLING --------
		// -- gcfove options
		Main.filenames = Parser.parse(array);

		// Check if output format is specified.
		if (!Arrays.asList(new String[] { "mln", "dmln" }).contains(targetFormatOption.getValue().toLowerCase())) {
			System.err.println(
					String.format("Specified output format (%s) not supported.", targetFormatOption.getValue()));
			Parser.printUsage(System.err);
			System.exit(1);
		}

		Main.outputFormat = targetFormatOption.getValue();

		if (Main.filenames.isEmpty()) {
			System.err.println("Error: no BLOG input files specified.");
			Parser.printUsage(System.err);
			System.exit(1);
		}
		Main.packages = stringListOption.getValue();
		Main.verbose = booleanOption3.getValue();
		Main.debug = booleanOption4.getValue();

		// -- custom options
		Main.outputPath = outputOption.getValue();
		Main.printToConsole = consoleOption.getValue();
		Main.shortFormAllowed = shortFormOption.getValue();
	}

	/*
	 * ---- UNTOUCHED parseOptions method private static void parseOptions(final
	 * String[] array) { // was: final HashMap<String, StringOption> hashMap =
	 * (HashMap<String, StringOption>)new HashMap<Object, Option>(); final
	 * HashMap<String, Option> hashMap = new HashMap<String, Option>();
	 * 
	 * // Set Parser description
	 * Parser.setProgramDesc("Bayesian Logic (BLOG) inference engine");
	 * 
	 * // Set basic usage line (printed by printUsage).
	 * Parser.setUsageLine("Usage: runblog <file1> ... <fileN>");
	 * 
	 * 
	 * 
	 * final BooleanOption booleanOption = new BooleanOption("r", "randomize",
	 * false, "Use clock time as random seed"); final StringOption stringOption =
	 * new StringOption("e", "engine", "blog.SamplingEngine",
	 * "Use inference engine class <s>");
	 * 
	 * hashMap.put("engineClass", (Option)stringOption); final IntOption intOption =
	 * new IntOption("n", "num_samples", 10000,
	 * "Run inference engine for <n> samples"); hashMap.put("numSamples",
	 * (Option)intOption); final IntOption intOption2 = new IntOption("b",
	 * "burn_in", 0, "Treat first <n> samples as burn-in"); hashMap.put("burnIn",
	 * (Option)intOption2); final StringOption stringOption2 = new StringOption("s",
	 * "sampler", "blog.LWSampler", "Use sampler class <s>");
	 * hashMap.put("samplerClass", (Option)stringOption2); final StringOption
	 * stringOption3 = new StringOption("p", "proposer", "blog.GenericProposer",
	 * "Use Metropolis-Hastings proposer class <s>"); hashMap.put("proposerClass",
	 * (Option)stringOption3); final IntOption intOption3 = new IntOption("t",
	 * "num_trials", 1, "Do <n> independent runs of inference"); final BooleanOption
	 * booleanOption2 = new BooleanOption((String)null, "generate", false,
	 * "Sample worlds from prior and print them"); final StringListOption
	 * stringListOption = new StringListOption("k", "package",
	 * "Parser looks for classes in package <s>"); final BooleanOption
	 * booleanOption3 = new BooleanOption("v", "verbose", false,
	 * "Print info about every world sampled"); final BooleanOption booleanOption4 =
	 * new BooleanOption("g", "debug", false, "Print model, evidence, and queries");
	 * final StringOption stringOption4 = new StringOption("w", "write",
	 * (String)null, "Write sampling results to file <s>"); final IntOption
	 * intOption4 = new IntOption("i", "interval", 0,
	 * "Write results after every <n> samples"); final StringOption stringOption5 =
	 * new StringOption("h", "histogram_output", (String)null,
	 * "Write histogram output to file <s>"); final PropertiesOption
	 * propertiesOption = new PropertiesOption("P", (String)null, (Properties)null,
	 * "Set inference configuration properties"); final StringListOption
	 * stringListOption2 = new StringListOption("x", "extend",
	 * "Extend setup with object of class <s>"); final IntOption intOption5 = new
	 * IntOption("m", "num_moves", 1,
	 * "Use <m> moves per rejuvenation step (PF only)"); Main.filenames =
	 * Parser.parse(array); if (Main.filenames.isEmpty()) {
	 * System.err.println("Error: no BLOG input files specified.");
	 * Parser.printUsage(System.err); System.exit(1); } Main.randomize =
	 * booleanOption.getValue(); Main.numStatSamples = intOption3.getValue();
	 * Main.generate = booleanOption2.getValue(); Main.packages =
	 * stringListOption.getValue(); Main.verbose = booleanOption3.getValue();
	 * Main.debug = booleanOption4.getValue(); Main.outputPath =
	 * stringOption4.getValue(); if (Main.outputPath != null) { Main.outputInterval
	 * = intOption4.getValue(); if (Main.outputInterval == 0) { Main.outputInterval
	 * = Math.max(intOption.getValue() / 100, 1); } } else if
	 * (intOption4.wasPresent()) { System.err.
	 * println("Warning: ignoring --interval option because no output file specified."
	 * ); } Main.histOut = stringOption5.getValue(); Main.inferenceProps =
	 * propertiesOption.getValue(); //for (final String s : ((Hashtable<String,
	 * V>)Main.inferenceProps).keySet()) {
	 * 
	 * for (final Object o : Main.inferenceProps.keySet()) { String s =
	 * o.toString(); final Option option = hashMap.get(s); if (option != null) {
	 * Util.fatalError("Can't use -P to set value for \"" + s +
	 * "\".  Use special-purpose " + "option " + option + " instead.", false); } }
	 * Main.inferenceProps.setProperty("engineClass", stringOption.getValue());
	 * Main.inferenceProps.setProperty("numSamples",
	 * String.valueOf(intOption.getValue())); Main.numSamples =
	 * intOption.getValue(); Main.inferenceProps.setProperty("burnIn",
	 * String.valueOf(intOption2.getValue()));
	 * Main.inferenceProps.setProperty("samplerClass", stringOption2.getValue());
	 * Main.inferenceProps.setProperty("proposerClass", stringOption3.getValue());
	 * final Iterator iterator2 = stringListOption2.getValue().iterator(); while
	 * (iterator2.hasNext()) { addSetupExtender((String) iterator2.next()); // Added
	 * String cast } }
	 */

	private static void addSetupExtender(final String s) {
		int i = s.indexOf(44);
		final String s2 = (i == -1) ? s : s.substring(0, i);
		final Properties properties = new Properties();
		while (i != -1) {
			final int index = s.indexOf(44, i + 1);
			final String s3 = (index == -1) ? s.substring(i + 1) : s.substring(i + 1, index);
			final int index2 = s3.indexOf(61);
			if (index2 == -1) {
				Util.fatalError("Setup extender parameter \"" + s3 + "\" is not of the form key=value.", false);
			}
			properties.setProperty(s3.substring(0, index2), s3.substring(index2 + 1));
			i = index;
		}
		SetupExtender setupExtender = null;
		try {
			setupExtender = (SetupExtender) Class.forName(s2).getConstructor(Properties.class).newInstance(properties);
		} catch (ClassNotFoundException ex2) {
			Util.fatalError("Setup extender class not found: " + s2, false);
		} catch (NoSuchMethodException ex3) {
			Util.fatalError("Setup extender class " + s2 + " does not have a constructor with a single "
					+ "argument of type java.util.Properties.", false);
		} catch (ClassCastException ex4) {
			Util.fatalError("Setup extender class " + s2 + " does not " + "implement the SetupExtender interface.",
					false);
		} catch (Exception ex) {
			Util.fatalError((Throwable) ex, true);
		}
		Main.setupExtenders.add(setupExtender);
	}

	public static void printTimes(final PrintStream printStream, final String s, final int n) {
		for (int i = 0; i < n; ++i) {
			printStream.print(s);
		}
		printStream.println();
	}

	public static PrintStream filePrintStream(final String s) {
		try {
			final File file = new File(s);
			if (!file.createNewFile()) {
				// System.err.println("Cannot create file (already exists): " + file.getPath());
				System.out.println("Cannot create file (already exists): " + file.getPath());
				System.out.println("Deleting the file and writing to new file.");
				file.delete();
				file.createNewFile();
				// System.exit(1);
			}
			if (!file.canWrite()) {
				System.err.println("Cannot write to file: " + file.getPath());
				System.exit(1);
			}
			return new PrintStream(new FileOutputStream(file));
		} catch (Exception ex) {
			System.err.println("Cannot create/open a file for output: " + s);
			System.err.println(ex);
			System.exit(1);
			return null;
		}
	}

	// added <Object[]> added <SetupExtender>
	public static void setup(final Model model, final Evidence evidence, final List queries,
			final Collection<Object[]> collection, final Collection<SetupExtender> collection2, final boolean b,
			final boolean parsingFromMessage) {
		for (final Object[] array : collection) {
			final Reader reader = (Reader) array[0];
			final String originName = (String) array[1];
			try {
				BLOGParser.parseReader(model, evidence, queries, Main.debug, reader, originName, parsingFromMessage);
			} catch (Exception ex) {
				System.err.println("Error parsing file: " + originName);
				Util.fatalError((Throwable) ex);
			}
		}
		for (final SetupExtender setupExtender : collection2) {
			try {
				setupExtender.extendSetup(model, evidence, queries);
			} catch (Exception ex2) {
				System.err.println("Error running setup extender: " + setupExtender.getClass().getName());
				Util.fatalError((Throwable) ex2);
			}
		}
		if (Main.debug || b) {
			System.out.println("............................................");
			model.print(System.out);
			System.out.println("............................................");
			System.out.println("\nEvidence:");
			evidence.print(System.out);
			System.out.println("............................................");
			System.out.println("\nQueries:");
			final Iterator<Query> iterator3 = queries.iterator();
			while (iterator3.hasNext()) {
				System.out.println(iterator3.next());
			}
		}
		if (!semanticsCorrect(model, evidence, queries)) {
			System.err.println("The model failed one or more checks.");
			System.err.println("Quitting...");
			System.exit(1);
		}
		int n = model.compile() + evidence.compile();
		final Iterator<Query> iterator4 = queries.iterator();
		while (iterator4.hasNext()) {
			n += iterator4.next().compile();
		}
		if (n > 0) {
			System.err.println("Encountered " + n + " errors in compilation phase.");
			System.exit(1);
		}
	}

	public static void stringSetup(final Model model, final Evidence evidence, final List list, final String s) {
		final StringReader stringReader = new StringReader(s);
		final String abbreviation = Util.abbreviation(s);
		final LinkedList<Object[]> list2 = new LinkedList<Object[]>();
		list2.add(new Object[] { stringReader, abbreviation });
		setup(model, evidence, list, list2, new LinkedList(), false, false);
	}

	public static String outputPath() {
		return Main.outputPath;
	}

	public static int outputInterval() {
		return Main.outputInterval;
	}

	public static int numSamples() {
		return Main.numSamples;
	}

	public static int numTrials() {
		return Main.numStatSamples;
	}

	public static String histOut() {
		return Main.histOut;
	}

}
