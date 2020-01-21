package blogbuilder;

import java.util.Random;

public class ConfigSingle {

	private static ConfigSingle instance = null;
	
	private Random random;
	
	public boolean verbose = false;
	
	private ProgressLogger progressLogger;
	
	private ConfigSingle() {
		this.random = new Random(123);
		this.progressLogger = new ProgressLogger();
	}
	
	public static ConfigSingle getInstance() {
		if (instance == null) {
			instance = new ConfigSingle();
		}
		return instance;
	}
	
	public Random getRandom() {
		return this.random;
	}
	
	public ProgressLogger getProgressLogger() {
		return this.progressLogger;
	}
	
}
