package blogConv.newObjects;

import java.util.ArrayList;
import java.util.Arrays;

import blog.Type;
import blogConv.ModelWrapper;

public class MLNRandVar {
	private String name;
	private ArrayList<String> args = new ArrayList<String>();
	private ModelWrapper mw;
	
	public MLNRandVar(ModelWrapper mw, String name) {
		this.name = name;
		this.mw = mw;
		extractSig(name);
	}
	
	/**
	 * Overloaded constructor that gives the variable possibility to replace certain argument types.
	 * For this argument type, there will be a special handling in the translation of arguments
	 * (and a deviation from the standard procedure).
	 * 
	 * Needed for dynamic MLN handling.
	 * 
	 * 
	 * @param mw ModelWrapper object
	 * @param name name of randvar
	 * @param replace extra Argument (e.g. 'Timestep')
	 * @param replaceWith extra Argument replacement (e.g. 'Time)
	 */
	public MLNRandVar(ModelWrapper mw, String name, String replace, String replaceWith) {
		this.name = name;
		this.mw = mw;
		extractSigReplace(name, replace, replaceWith);
	}
	
	
	/**
	 * Copy of extractSig() with deviation handling for arguments with name replace.
	 * Those are replaced with replaceWith instead of the default translation process.
	 * @param name
	 * @param replace
	 * @param replaceWith
	 */
	private void extractSigReplace(String name, String replace, String replaceWith) {
		// sig = mw.getRandVarByName(name).getSig().toString();
		ArrayList<Type> args = new ArrayList<Type>(Arrays.asList(mw.getRandVarByName(name).getArgTypes()));
		for (Type arg : args) {
			if (arg.toString().contains(replace))
				this.args.add(arg.toString().replace(replace, replaceWith));
			else 
				this.args.add(arg.toString().toLowerCase());
		}
	}
	
	
	
	
	private void extractSig(String name) {
		// sig = mw.getRandVarByName(name).getSig().toString();
		ArrayList<Type> args = new ArrayList<Type>(Arrays.asList(mw.getRandVarByName(name).getArgTypes()));
		for (Type arg : args) {
			this.args.add(arg.toString().toLowerCase());
		}
	}
	
	@Override
	public String toString() {
		if (args.size() == 0) {
			return name;
		} else {
			return name +"("+ String.join(",", args)+")";
		}
	}
}
