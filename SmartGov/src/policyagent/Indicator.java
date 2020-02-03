package policyagent;

public class Indicator {
	
	protected final String genericIndicatorName; //main category of the indicator
	protected String specificIndicatorName; //specific category of this indicator
	protected double value;
	
	public Indicator(String name, double value){
		this.genericIndicatorName = name;
		this.specificIndicatorName = null;
		this.value = value;
	}
	
	public Indicator(String name, String specificName, double value){
		this.genericIndicatorName = name;
		this.specificIndicatorName = specificName;
		this.value = value;
	}
	
	public double getValue() {
		return value;
	}
	
	public void updateValue(double value){
		this.value += value;
	}
	
	public void setValue(double value){
		this.value = value;
	}
}
