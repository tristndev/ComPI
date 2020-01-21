
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import blogConv.DJTEvidenceFactory;
import blogConv.ModelWrapper;
import blogSpecs.BLOGEvidenceSpecParser;
import blogSpecs.EvidenceSpec;

class DJTEvidenceFactoryTest {

	private static ModelWrapper mw1;
	private static DJTEvidenceFactory ef;

	private static ArrayList<EvidenceSpec> specs;

	DJTEvidenceFactory evFacLoader(String path, int maxTime) {
		TestHelper th = new TestHelper();
		ModelWrapper mw = th.getModelWrapperForFile(path);
		System.out.println("Model Wrapper(s) loaded for file "+path);

		BLOGEvidenceSpecParser parser = new BLOGEvidenceSpecParser(mw.getFilePath());
		ArrayList<EvidenceSpec> specs = parser.getEvidenceSpecs();
		System.out.println("EvidenceSpec(s) parsed & loaded.");
		
		return new DJTEvidenceFactory(mw, specs, maxTime); 
	}
	
	@BeforeAll
	static void init() {
		TestHelper th = new TestHelper();
		mw1 = th.getModelWrapperForFile("tests/res/evidenceBase.blog");
		System.out.println("Model Wrapper(s) loaded.");

		BLOGEvidenceSpecParser parser = new BLOGEvidenceSpecParser(mw1.getFilePath());
		specs = parser.getEvidenceSpecs();
		System.out.println("EvidenceSpec(s) parsed & loaded.");
		
		int maxTime = 20;
		ef = new DJTEvidenceFactory(mw1, specs, maxTime);
		System.out.println("Evidence Factory instantiated.");
	}

	//@Test
	void printEvidenceSpecs() {
		System.out.println("Starting test...");
		BLOGEvidenceSpecParser parser = new BLOGEvidenceSpecParser(mw1.getFilePath());

		ArrayList<EvidenceSpec> specs = parser.getEvidenceSpecs();

		for (EvidenceSpec es : specs) {
			System.out.println(es.getJSONObj());
		}
	}

	@Test
	void printMatrixAndAnalysis() {
		EvidenceSpec evSpec = specs.get(0);
		ef.printSingleMatrix(evSpec);
		
		evSpec.prettyPrint();
		
		analyzeMatrix(evSpec);
	}

	private void analyzeMatrix(EvidenceSpec evSpec) {
		String mat = ef.createSingleMatrixString(evSpec);
		
		int[] matSize = ef.getMatrixSize(evSpec);
		int N = matSize[0] * (matSize[1]-1);
		
		String regexT2T = "T.{4}T";
		int nT2T = countMatches(mat, regexT2T);
		int nT2x = countMatches(mat, "T.{4}(T|f)");
		
		String regexF2F = "f.{4}f";
		int nF2F = countMatches(mat, regexF2F);
		int nF2x = countMatches(mat, "f.{4}(f|T)");
		
		String regexS2S = "(T {4}T|T {4}|f {4}f|f {4}T)";
		int nS2S = countMatches(mat, regexS2S);
		int nS2x = countMatches(mat, "(T|f)( {3}\\(| {4}(T|f))");
		
		String regexH2H = "\\(.\\) {2}\\(.\\)";
		int nH2H = countMatches(mat, regexH2H);
		int nH2x = countMatches(mat, "(\\)  \\(|\\) {3}(T|f))");
		
		System.out.println("Matrix analysis:");
		System.out.println(String.format("percT2T: %.2f\t\t| percF2F: %.2f\n"
				+ "percS2S: %.2f\t\t| percH2H: %.2f", 
				(float) nT2T / nT2x, (float) nF2F / nF2x,
				(float) nS2S / nS2x, (float) nH2H / nH2x));
		
	}
	
	//@Test
	void testCountMatches() {
		assertEquals(2, countMatches("(f)  (f)   f","f.{4}f"));
	}
	
	private int countMatches(String searchString, String regex) {
		int count = 0;
		int i = 0;
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(searchString);
		
		while (m.find(i)) {
			count ++;
			i = m.start()+1;
		}
		return count;
	}
	
	
	//@Test
	void testGroupDistribution() {
		System.out.println(">>> Group Distribution Test:");
		
		EvidenceSpec spec = specs.get(0);
		System.out.println("# Groups: "+ef.getMatrixSize(spec)[0]);
		ef.getMatrixSize(spec);
		ArrayList<ArrayList<String>> groups = ef.distributeGuarObjsToGroups(spec);
		
		for (int i = 0; i<groups.size(); i++) {
			System.out.printf("#%2d - Size: %d\n",i+1, groups.get(i).size());
			System.out.println("    "+String.join("; ", groups.get(i)));
		}
	}
	
	//@Test
	void testLineCreation() {
		System.out.println(ef.createEvidenceLines(false));
	}
	
	@Test
	void testBoolFlip() {
		DJTEvidenceFactory ev100 = evFacLoader("tests/res/boolFlip100.blog", 20);
		DJTEvidenceFactory ev70 = evFacLoader("tests/res/boolFlip70.blog", 20);
		
		int n = 100;
		
		System.out.println("Testing 100% bool flip chance:");
		int count100 = 0;
		for (int i = 0; i<n; i++) {
			boolean flipped = ev100.boolFlipByChance(false, ev100.getLoadedEvSpecs().get(0));
			String currStr = String.format("%2d - flipped %b", i, flipped);
			//System.out.println(currStr);
			
			if (flipped) {
				count100++;
			}
		}
		
		System.out.printf("%d of %d flipped -> %.2f perc\n", count100, n, count100*1.0/n);
		
		System.out.println("Testing 70% bool flip chance:");
		int count70 = 0;
		for (int i = 0; i<n; i++) {
			boolean flipped = ev70.boolFlipByChance(false, ev70.getLoadedEvSpecs().get(0));
			String currStr = String.format("%d - flipped %b", i, flipped);
			//System.out.println(currStr);
			
			if (flipped) {
				count70++;
			}
		}
		
		System.out.printf("%d of %d flipped -> %.2f perc\n", count70, n, count70*1.0/ n);
	}
}
