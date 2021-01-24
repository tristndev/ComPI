import java.util.HashMap;

import org.junit.jupiter.api.Test;

import blogConv.DJTQueryFactory;
import blogConv.ModelWrapper;
import blogConv.EvidenceFactories.DJTDynamicEvidenceFactory;

public class DJTQueryFactoryTest {

	DJTQueryFactory queryFacLoader(String path, int maxTime) {
		TestHelper th = new TestHelper();
		ModelWrapper mw = th.getModelWrapperForFile(path);
		System.out.println("Model Wrapper(s) loaded for file "+path);
		DJTDynamicEvidenceFactory evFac = new DJTDynamicEvidenceFactory(mw);
		return new DJTQueryFactory(mw, true, evFac); 
	}
	
	void printQueriesForFile(String path, int maxTime) {
		int queryIndex = 2;
		DJTQueryFactory fac = queryFacLoader(path, maxTime);
		HashMap <Integer,String> queryMap = fac.createAllQuerySizes();
		String firstQuerysize = queryMap.get(queryMap.keySet().toArray()[queryIndex]);
		System.out.println(firstQuerysize);
		
		System.out.printf("File: %s, query size: %d, # query lines: %d", 
				path, 
				queryMap.keySet().toArray()[queryIndex],
				firstQuerysize.split("\n").length); 
	}
	
	
	
	@Test
	void testQueryIntervalHandling() {
		//printQueriesForFile("tests/res/queryIntervalNone.blog", 10); 
		//printQueriesForFile("tests/res/queryIntervalCutoff.blog", 10); 
		printQueriesForFile("tests/res/queryIntervalMaxTConversion.blog", 10);
	}
	
	
	
}
