package blogConv;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import blog.Function;
import blog.Model;
import blog.Query;
import blog.Type;
import fove.Parfactor;
import ve.Potential;

public class ModelWrapper {
	
	private Model model;
	private ArrayList<Query> queries;
	private String filePath;
			
	public ModelWrapper(Model model, List<Query> queries, String filePath) {
		this.model = model;
		this.queries = new ArrayList<Query>(queries);
		this.filePath = filePath;
	}
	
	/**
	 * Method that prints a small interview for the model.
	 * Includes: Types, Parfactors, Functions
	 */
	public void printParams() {
    	String div = "'''''''''''''''''''''''''\n";
    	String res = "";
    	
    	res += "\ngetTypes():\n"+div;
    	res += model.getTypes().toString();
    	res += "\ngetParfactors():\n"+div;
    	res += model.getParfactors().toString();
    	res += "\ngetFunctions():\n"+div;
    	res += model.getFunctions().toString();
    	res += "\n";
    	System.out.println(res);
	}
	
	public String getFilePath() {
		return this.filePath;
	}
		
	public Collection<Type> getTypes() {
		return model.getTypes();
	}
	
	public Type getTypeByName(String name) {
		return model.getType(name);
	}
	
	public Collection getFunctions() {
		return model.getFunctions();
	}
	
	
	public Object getFunctionByName(String name) {
		return model.getFuncsWithName(name);
	}
	
	
	public void printFuncs() {
		StringBuffer sb = new StringBuffer();
		ArrayList <Function> funcs = new ArrayList<Function> (model.getFunctions());
		for (Function f : funcs) {
			
			String name = f.getName();
			sb.append(name).append(": ");
			String sig = f.getSig().toString();			
			sb.append(sig).append("\n");
			
		}
		System.out.println(sb.toString());
	}
	
	
	/**
	 * Method that returns a model's randvars.
	 * Achieved by taking all functions and excluding the type-ranges.
	 */
	public Collection<String> getRandVarNames() {
		ArrayList<Type> types = new ArrayList<Type>(model.getTypes());
		ArrayList<String> funcStrings = new ArrayList<String>();
		
		for(Object f: model.getFunctions()) {
			funcStrings.add(f.toString());
		}
		
		for(Type t: types) {		

			for(Object obj : t.range()) {
				funcStrings.remove(obj.toString());
			}
		}
		
		return funcStrings;
	}
	
	public Function getRandVarByName(String name) {
		ArrayList<Function> rvs = new ArrayList<Function>(model.getFuncsWithName(name));
		if (rvs.size() == 0) {
			System.out.println(String.format("Randvar %s not found in model functions.", name));
			return null;
		} else if (rvs.size() > 1) {
			System.out.println(String.format("Functions contain more than 1 randvar with the name %s.", name));
			return null;
		}
		
		return rvs.get(0);
	}
	
	public ArrayList<Function> getRandVars() {
		ArrayList<Function> list = new ArrayList<Function>();
		
		Collection<String> randVarNames = getRandVarNames();
		
		Iterator it = randVarNames.iterator();
		
		while (it.hasNext()) {
			list.add(getRandVarByName(it.next().toString()));
		}
		
		return list;		
	}
	
	
	public void printMultiArrayPotential(int iParfac) {
        System.out.println(String.format("MultiArrayPotential [%d]", iParfac));
        
        System.out.println("Logvars: \t"+model.getParfactors().get(iParfac).logicalVars());
        System.out.println("Types: \t"+model.getParfactors().get(iParfac).logicalVars().get(0).getType());
        System.out.println("dimTerms: \t"+model.getParfactors().get(iParfac).dimTerms());
        
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream ps = new PrintStream(baos, true, "UTF-8")) {
        	model.getParfactors().get(iParfac).potential().print(ps);
        } catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
        String output = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        System.out.println(output);
	}
	
	public List<Parfactor> getParfactors() {
		return model.getParfactors();
	}
	
	
	/**
	 * Returns the potential attribute of the i-th parfactor of the model (in our case a MultiArrayPotential).
	 * Can be further queried using *.getValue(ArrayList<Type> args), e.g. with args = {Boolean.TRUE, Boolean.FALSE}
	 * 
	 * @param i index of the parfactor whose potential shall be returned.
	 * @return the potential attribute of the selected parfactor (e.g. a MultiArrayPotential).
	 */
	public Potential getMultiArrayPotential(int i) {
		return  model.getParfactors().get(i).potential();
	}
	
	
	/**
	 * Method that returns the value of a parfactor multiarray table (specified by parfacInd) given a binary string
	 * that defines the truth values of the entries in the table. 
	 * 
	 * @param binStr binary String, must only consist of 1's and 0's
	 * @param parfacInd int Index of the parfactor in the model
	 * @return double value of the entry that was given in the input file
	 */
	public double getValueFromBinaryString(String binStr, int parfacInd) {
		ArrayList<Boolean> args = new ArrayList<Boolean>();
		for (int i = 0; i < binStr.length(); i++) {
			if (binStr.charAt(i) == '0') {
				args.add(Boolean.FALSE);
			} else if (binStr.charAt(i) == '1') {
				args.add(Boolean.TRUE);
			} else {
				throw new IllegalArgumentException("String passed to getValueFromBinaryString is not a binary string.");
			}
		}
		
		return model.getParfactors().get(parfacInd).potential().getValue(args);
	}
	
	/**
	 * Same method as above, but with parfactor as argument (instead of pf_index)
	 * @param binStr binary String, must only consist of 1's and 0's
	 * @param pf parfactor to lookup
	 * @return double value of the entry that was given in the input file
	 */
	public double getValueFromBinaryString(String binStr, Parfactor pf) {
		ArrayList<Boolean> args = new ArrayList<Boolean>();
		for (int i = 0; i < binStr.length(); i++) {
			if (binStr.charAt(i) == '0') {
				args.add(Boolean.FALSE);
			} else if (binStr.charAt(i) == '1') {
				args.add(Boolean.TRUE);
			} else {
				throw new IllegalArgumentException("String passed to getValueFromBinaryString is not a binary string.");
			}
		}
		
		return pf.potential().getValue(args);
	}
	
	
	/**
	 * Returns the dims of the i-th MultiArrayPotential.
	 * @param i index of the parfactor (MAP).
	 * @return a List of Types, e.g. for a MAP with 2 variables [Boolean, Boolean]
	 */
	public List<Type> getMAPDims(int i) {
		return model.getParfactors().get(i).potential().getDims();
	}
	
	
	// #########################################
	// +++++ Queries
	// #########################################
	public ArrayList<Query> getQueries() {
		return queries;
	}
	
	
	
}
