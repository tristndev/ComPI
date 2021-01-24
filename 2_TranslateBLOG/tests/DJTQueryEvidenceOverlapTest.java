
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import blogConv.DJTQueryFactory;
import blogConv.ModelWrapper;
import blogConv.EvidenceFactories.DJTDynamicEvidenceFactory;
import blogSpecs.BLOGEvidenceSpecParser;
import blogSpecs.EvidenceSpec;

class DJTQueryEvidenceOverlapTest {

	private static ModelWrapper mw1;
	private static ModelWrapper mw2;
	private static DJTDynamicEvidenceFactory ef1;
	private static DJTDynamicEvidenceFactory ef2;
	private static DJTQueryFactory qf1;
	private static DJTQueryFactory qf2;

	private static ArrayList<EvidenceSpec> specs;
	
	private static int maxTime = 5;

	DJTDynamicEvidenceFactory evFacLoader(String path, int maxTime) {
		TestHelper th = new TestHelper();
		ModelWrapper mw = th.getModelWrapperForFile(path);
		System.out.println("Model Wrapper(s) loaded for file "+path);

		BLOGEvidenceSpecParser parser = new BLOGEvidenceSpecParser(mw.getFilePath());
		ArrayList<EvidenceSpec> specs = parser.getEvidenceSpecs();
		System.out.println("EvidenceSpec(s) parsed & loaded.");
		
		return new DJTDynamicEvidenceFactory(mw, specs, maxTime); 
	}
	
	DJTQueryFactory queryFacLoader(String path, int maxTime) {
		TestHelper th = new TestHelper();
		ModelWrapper mw = th.getModelWrapperForFile(path);
		System.out.println("Model Wrapper(s) loaded for file "+path);
		DJTDynamicEvidenceFactory evFac = new DJTDynamicEvidenceFactory(mw);
		return new DJTQueryFactory(mw, true, evFac); 
	}
	
	@BeforeAll
	static void init() {
		TestHelper th = new TestHelper();
		mw1 = th.getModelWrapperForFile("tests/res/queryEvidenceOverlap.blog");
		mw2 = th.getModelWrapperForFile("tests/res/queryEvidenceOverlapNoLogVars.blog");
		System.out.println("Model Wrapper(s) loaded.");

		BLOGEvidenceSpecParser parser = new BLOGEvidenceSpecParser(mw1.getFilePath());
		specs = parser.getEvidenceSpecs();
		System.out.println("EvidenceSpec(s) parsed & loaded.");
		
		ef1 = new DJTDynamicEvidenceFactory(mw1, specs, maxTime);
		System.out.println("Evidence Factory instantiated.");
		
		qf1 = new DJTQueryFactory(mw1, true, ef1);
		
		parser = new BLOGEvidenceSpecParser(mw2.getFilePath());
		specs = parser.getEvidenceSpecs();
		System.out.println("EvidenceSpec(s) parsed & loaded.");
		
		ef2 = new DJTDynamicEvidenceFactory(mw2, specs, maxTime);
		System.out.println("Evidence Factory instantiated.");
		
		qf2 = new DJTQueryFactory(mw2, true, ef2);
	}
	
	@Test
	void printWrapper() {
		ModelWrapper[] mws = {mw1, mw2};
		DJTQueryFactory[] qfs = {qf1, qf2};
		DJTDynamicEvidenceFactory[] efs = {ef1, ef2};
		
		for (int i = 0; i < mws.length; i++) {
			ModelWrapper mw = mws[i];
			System.out.println("#### Creating outputs for model #"+i);
			printEvidenceSpecs(mw);
			
			printQueryLines(qfs[i]);
			
			printEvidenceLines(efs[i]);
		}
	}

	
	static void printEvidenceSpecs(ModelWrapper mw) {
		System.out.println("Starting test...");
		BLOGEvidenceSpecParser parser = new BLOGEvidenceSpecParser(mw1.getFilePath());

		ArrayList<EvidenceSpec> specs = parser.getEvidenceSpecs();

		System.out.println("Found these evidence specs: ");
		for (EvidenceSpec es : specs) {
			System.out.println(es.getJSONObj());
		}
	}


	static void printQueryLines(DJTQueryFactory qf) {
		System.out.println("Printing created query lines...");
		System.out.println(qf.createAllQuerySizes());
	}
	
	
	static void printEvidenceLines(DJTDynamicEvidenceFactory ef) {
		System.out.println("Printing created evidence lines...");
		System.out.println(ef.createEvidenceLines(maxTime));
	}
	
}
