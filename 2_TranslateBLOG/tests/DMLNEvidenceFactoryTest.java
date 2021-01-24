
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import blogConv.ModelWrapper;
import blogConv.EvidenceFactories.DJTDynamicEvidenceFactory;
import blogConv.EvidenceFactories.DMLNEvidenceFactory;
import blogSpecs.BLOGEvidenceSpecParser;
import blogSpecs.EvidenceSpec;

class DMLNEvidenceFactoryTest {

	private static ModelWrapper mw1;
	private static DMLNEvidenceFactory ef;

	private static ArrayList<EvidenceSpec> specs;

	DMLNEvidenceFactory evFacLoader(String path, int maxTime) {
		TestHelper th = new TestHelper();
		ModelWrapper mw = th.getModelWrapperForFile(path);
		System.out.println("Model Wrapper(s) loaded for file "+path);

		BLOGEvidenceSpecParser parser = new BLOGEvidenceSpecParser(mw.getFilePath());
		ArrayList<EvidenceSpec> specs = parser.getEvidenceSpecs();
		System.out.println("EvidenceSpec(s) parsed & loaded.");
		
		return new DMLNEvidenceFactory(mw, specs, maxTime); 
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
		ef = new DMLNEvidenceFactory(mw1, specs, maxTime);
		System.out.println("Evidence Factory instantiated.");
	}

	@Test
	void printEvidenceSpecs() {
		System.out.println("Printing evidence specs...");
		BLOGEvidenceSpecParser parser = new BLOGEvidenceSpecParser(mw1.getFilePath());
		ArrayList<EvidenceSpec> specs = parser.getEvidenceSpecs();

		for (EvidenceSpec es : specs) {
			System.out.println(es.getJSONObj());
		}
	}


	@Test
	void testLineCreation() {
		System.out.println("Printing created evidence lines...");
		System.out.println(ef.createEvidenceLines(20));
	}
	
	
}
