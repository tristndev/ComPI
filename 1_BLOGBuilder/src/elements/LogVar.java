package elements;

public class LogVar extends ModelObject {

	public LogVar(int index) {
		super.index = index;
		super.prefix = "LV";
	}

	@Override
	public String asLine() {
		return String.format("type %s;\nguaranteed %s x%dx[XXX];\n", 
				super.constructName(), 
				super.constructName(),
				super.index);
	}

}
