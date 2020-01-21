import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import blog.Evidence;
import blog.Main;
import blog.Model;
import blog.Query;
import blogConv.ModelWrapper;
import common.Util;

public class TestHelper {
	
	/**
	 * Empty constructor.
	 */
	public TestHelper() {
		
	}

	private static Model model;
	private static Evidence evidence;
	private static List<Query> queries;

	private static List setupExtenders;

	/**
	 * Resets all static variables needed for model creation.
	 */
	private void resetAndInit() {
		model = new Model();
		evidence = new Evidence();
		queries = new ArrayList<Query>();

		setupExtenders = new ArrayList();
	}
	
	/**
	 * Parses a file into a model given a filename string.
	 * @param filename
	 */
	private void testSetup(String filename) {
		List readers = Main.makeReaders(Arrays.asList(filename));
		Main.setup(model, evidence, queries, readers, setupExtenders, Util.verbose(), true);
	}
	
	/**
	 * Wraps all needed actions to create and return a ModelWrapper given a filename string.
	 * @param filename 
	 * @return ModelWrapper for the given model file.
	 */
	public ModelWrapper getModelWrapperForFile(String filename) {
		this.resetAndInit();
		this.testSetup(filename);
		
		return new ModelWrapper(model, queries, filename);
	}

}
