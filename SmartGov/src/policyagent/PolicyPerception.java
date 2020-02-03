package policyagent;

public class PolicyPerception {

	private Position position = new Position();
	
	public PolicyPerception(Position variables) {
		this.position = variables;
	}
	
	public Position getPosition() {
		return this.position;
	}
	
	@Override
	public String toString() {
		String str = "[";
		for(int i = 0; i < position.getCoordinates().size() - 1; i++) {
			str += position.getCoordinates().get(i) + ", ";
		}
		str += position.getCoordinates().get(position.getCoordinates().size() - 1) + "]";
		return str;
	}
	
}