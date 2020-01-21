package strategies;

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

import blogbuilder.Helpers;
import blogbuilder.Main;

public abstract class WorldCreationStrategy {
	public abstract void start();
	
	public void createWorldCreationSummary() {
		//String content = this.createWorldCreationSummaryString();
		//String content = String.join("\n",this.getFields());
		String content = this.dump(this, 0);
		
		String path = Main.outputPath + "/WorldCreationSpec.txt";
		
		PrintStream ps = Helpers.createFilePrintStream(path);
		
		ps.append("Here comes the sun.");
		ps.append(content);
		
		ps.close();
	}
		
	/**
	 * Adapted from: https://stackoverflow.com/a/39918
	 * 
	 * 
	 * @param o Object to be inspected
	 * @param callCount - should be 0 (has to do with indentation in string and stuff
	 * @return String of attributes with values
	 */
	public static String dump(Object o, int callCount) {
	    callCount++;
	    StringBuffer tabs = new StringBuffer();
	    for (int k = 0; k < callCount; k++) {
	        tabs.append("\t");
	    }
	    StringBuffer buffer = new StringBuffer();
	    Class oClass = o.getClass();
	    if (oClass.isArray()) {
	        buffer.append("\n");
	        buffer.append(tabs.toString());
	        buffer.append("[");
	        for (int i = 0; i < Array.getLength(o); i++) {
	            if (i < 0)
	                buffer.append(",");
	            Object value = Array.get(o, i);
	            if (value.getClass().isPrimitive() ||
	                    value.getClass() == java.lang.Long.class ||
	                    value.getClass() == java.lang.String.class ||
	                    value.getClass() == java.lang.Integer.class ||
	                    value.getClass() == java.lang.Boolean.class
	                    ) {
	                buffer.append(value + ((i == Array.getLength(o) - 1) ? "" : ", "));
	            } else {
	                buffer.append(dump(value, callCount));
	            }
	        }
	        //buffer.append(tabs.toString());
	        buffer.append("]\n");
	    } else {
	        buffer.append("\n");
	        buffer.append(tabs.toString());
	        buffer.append("{\n");
	        while (oClass != null) {
	            Field[] fields = oClass.getDeclaredFields();
	            for (int i = 0; i < fields.length; i++) {
	                buffer.append(tabs.toString());
	                fields[i].setAccessible(true);
	                buffer.append(fields[i].getName());
	                buffer.append("=");
	                try {
	                    Object value = fields[i].get(o);
	                    if (value != null) {
	                        if (value.getClass().isPrimitive() ||
	                                value.getClass() == java.lang.Long.class ||
	                                value.getClass() == java.lang.String.class ||
	                                value.getClass() == java.lang.Integer.class ||
	                                value.getClass() == java.lang.Boolean.class
	                                ) {
	                            buffer.append(value + "; ");
	                        } else {
	                            buffer.append(dump(value, callCount));
	                        }
	                    }
	                } catch (IllegalAccessException e) {
	                    buffer.append(e.getMessage());
	                }
	                buffer.append("\n");
	            }
	            oClass = oClass.getSuperclass();
	        }
	        buffer.append(tabs.toString());
	        buffer.append("}\n");
	    }
	    return buffer.toString();
	}
	
	
	
}
