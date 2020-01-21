package blogbuilder;

import java.util.ArrayList;

public class ProgressLogger {

	/**
	 * List of files where some randVar(s) have note been mentioned in a factor.
	 */
	private ArrayList<String> allMentionedFalseFiles = new ArrayList<String>();
	
	/**
	 * List of files where world deviates from specification.
	 */
	private ArrayList<String> specDeviationFiles = new ArrayList<String>();
	
	/**
	 * Prints a message to the console indicating the start of the handling of a world.
	 * @param globalCounter for current file / model.
	 */
	public void logWorldStart(int globalCounter) {
		System.out.println(
				String.format("# %3d - start...", globalCounter));
	}
	
	/**
	 * Prints a message to the console indicating the finish of handling a world.
	 * Includes the spec deviation check. 
	 * 
	 * @param globalCounter for current file / model.
	 * @param w World that was created.
	 */
	public void logWorldDone(int globalCounter, World w) {
		String specDeviationCheck = w.checkForSpecDeviations();
		if (!specDeviationCheck.equals("")) {
			System.err.println(specDeviationCheck);
		}
		System.out.println(
				String.format("# %3d - model done - file: %s", globalCounter, w.constructFilePath()));
	}
	
	/**
	 * Prints a message to the console indicating the finish of handling a world.
	 * Includes the spec deviation check. 
	 * 
	 * @param globalCounter for current file / model
	 * @param iIncVariable current index of variable that was increased ( == 0? -> baseWorld, else augmentWorld)
	 * @param w World that was created
	 */
	public void logWorldDone(int globalCounter, int iIncVariable, World w) {
		String specDeviationCheck = w.checkForSpecDeviations();
		if (!specDeviationCheck.equals("")) {
			System.err.println(specDeviationCheck);
		}
		System.out.println(
				String.format("# %3d - %s model done - file: %s", globalCounter, iIncVariable == 0 ? "base" : "augment", w.constructFilePath()));
	}
	
	/**
	 * Prints a world's specified parameters to the console.
	 * 
	 * @param w world to be inspected.
	 */
	public void logWorldSpecParams(World w) {
		System.out.println(String.format(
				"# [spec]: lv: %2d, rv: %3d, fac: %3d, maxRVargs: %d, maxRVocc: %d, facArgs: %d", 
				w.getSpecContainer().getLogVarCount(), w.getSpecContainer().getRandVarCount(), 
				w.getSpecContainer().getFactorCount(), w.getSpecContainer().getMaxRandVarArgs(), 
				w.getSpecContainer().getMaxRandVarOccurrences(), w.getSpecContainer().getFactorArgCount()));
	}
	
	/**
	 * Prints a world's real (i.e. not the specified!) parameters to the console.
	 * 
	 * @param w world to be inspected.
	 */
	public void logWorldRealParams(World w) {
		System.out.println(String.format(
				" real - lv: %2d, rv: %3d, fac: %3d, maxRVargs: %d, maxRVocc: %d, facArgs: %d, allRVMentioned: %b",
				w.getLogVars().size(), w.getRandVars().size(), w.searchRealFactors(), w.searchRealMaxRVArgs(),
				w.searchRealMaxRVOccurence(), w.searchRealMaxFactorArgCount(), w.checkAllRVMentioned()));
	}
	
	/**
	 * Prints a final report to the console.
	 * Includes a) files with non-mentioned randvars, b) files with spec deviations.
	 */
	public void logFinalReport() {
		boolean allOk = true;
		String msg = "\n### Final report ###\n";		
		
		if (this.allMentionedFalseFiles.size() == 0) {
			msg += "> All randvars have been mentioned for all files.\n\n";
		} else {
			msg += String.format("> Non mentioned randvars in these %d files:\n  ", this.allMentionedFalseFiles.size()) + String.join("\n  ", this.allMentionedFalseFiles) + "\n\n";
			allOk = false;
		}
		
		if (this.specDeviationFiles.size() == 0) {
			msg += "> No files where real world deviates from spec.\n";
		} else {
			msg += String.format("> Spec deviations in these %d files:\n  ", this.specDeviationFiles.size()) + String.join("\n  ", this.specDeviationFiles) + "\n\n";
			allOk = false;
		}
		
		if (allOk) {
			System.out.println(msg);
		} else {
			System.err.println(msg);
		}
	}
	
	/**
	 * Add a given filename to the list of files with allRandVarsMentioned == false
	 * @param filename to be added to the list.
	 */
	public void addToAllMentionedFalseFiles(String filename) {
		this.allMentionedFalseFiles.add(filename);
	}
	
	/**
	 * Add a given fileDeviationString (format: <code>filename: dev1, dev2, </code> to the list of files with spec deviations.
	 * 
	 * @param fileDeviationString to be added to the list.
	 */
	public void addToSpecDeviationFiles(String fileDeviationString) {
		this.specDeviationFiles.add(fileDeviationString);
	}
}
