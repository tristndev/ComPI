package elements;

public abstract class ModelObject {
	
	/**
	 * Each ModelObject has a index as ID. Will be obtained by the world Object.
	 */
	protected int index;
	
	/**
	 * Prefix string for each ModelObject subclass (e.g. "RV" for RandVars).
	 */
	protected String prefix;
	
	/**
	 * Returns the representation in the model file.
	 * @return String with line representation.
	 */
	public abstract String asLine();
	
	/**
	 * Getter for index of object.
	 * @return
	 */
	public int getIndex() {
		return this.index;
	}
	
	/**
	 * Constructs the object's name based on prefix and index.
	 * @return String name of the object.
	 */
	public String constructName() {
		return this.prefix + this.index;
	}
	
	/**
	 * Updates the object's index attribute.
	 * @param newInd new index value.
	 */
	public void updateIndex(int newInd) {
		this.index = newInd;
	}
}
