package blogConv.newObjects;

import java.util.ArrayList;

import blog.Main;
import blog.Type;
import blogConv.MLNFileFactory;

public class MLNType {
	/* BLOG: 	[p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16]
	 * MLN:		p = {P1,P2,P3}
	 */
	
	String name; 
	ArrayList<String> guaranteedObjects = new ArrayList<String>();
	
	
	public ArrayList<String> getGuaranteedObjects() {
		return guaranteedObjects;
	}
	
	/**
	 * Constructor that creates a MLNType given a BLOGType.
	 * @param BLOGType object the MLN Type shall be generated for.
	 */
	public MLNType(Type BLOGType) {
		name = MLNFileFactory.translateType(BLOGType.getName());
		extractRange(BLOGType);
	}
	
	/**
	 * Extract the range out of the given BLOGType, i.e. the 
	 * guaranteed objects for a type. 
	 * Stores the result as Strings in the ArrayList guaranteedObjects.
	 * @param BLOGType to extract the range from.
	 */
	private void extractRange(Type BLOGType) {
		for (Object obj: BLOGType.getGuaranteedObjects()) {
			// make the first character upper case
			String upperCased = MLNFileFactory.translateGuaranteedObj(obj.toString());
			guaranteedObjects.add(upperCased);
		}
	}
	
	/**
	 * Method to create the short mln form for the guaranteed objects.<br>
	 * Example:
	 * 
	 * <pre>
	 * p = {1, ..., 100}
	 * </pre>
	 * 
	 * @return String as representation of the guaranteed objects. Contains the
	 *         content of the curly brackets of the example, so in this case: '1,
	 *         ..., 100'
	 */
	private String guaranteedObjectsShortForm() {
		int n = guaranteedObjects.size();
		return "1, ..., "+n;
	}
	
	@Override
	public String toString() {
		if (Main.shortFormAllowed && guaranteedObjects.size() > 10) {
			return name + " = {" + guaranteedObjectsShortForm() +"}";
		} else { 
			return name + " = {" + String.join(", ", guaranteedObjects) +"}";
		}
	}

}
