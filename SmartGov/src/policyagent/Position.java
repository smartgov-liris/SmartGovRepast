package policyagent;

import java.util.ArrayList;
import java.util.List;

public class Position {

	private List<Double> coordinates;
	
	public Position() {
		this.coordinates = new ArrayList<>();
	}
	
	public Position(List<Double> coordinates) {
		this.coordinates = coordinates;
	}
	
	public List<Double> getCoordinates() {
		return coordinates;
	}
	
	public void addCoordinate(double coordinate) {
		coordinates.add(coordinate);
	}
	
	@Override
	public String toString() {
		String str = "";
		if(coordinates.size() > 1) {
			for(int indexOfCoordinate = 0; indexOfCoordinate < coordinates.size() - 1; indexOfCoordinate++) {
				str += coordinates.get(indexOfCoordinate) + " ";//"\t";
			}
			return str += coordinates.get(coordinates.size() - 1);
		} else {
			return str += coordinates.get(0);
		}
	}
	
	public void addPosition(Position position) {
		for(int i = 0; i< position.coordinates.size(); i++) {
			this.coordinates.set(i, this.coordinates.get(i) + position.getCoordinates().get(i));
		}
	}
	
}
