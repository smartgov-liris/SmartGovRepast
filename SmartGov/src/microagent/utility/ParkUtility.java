package microagent.utility;

import microagent.AbstractUtility;
import microagent.properties.ParkProperties;

import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;

import simulation.GISComputation;
import simulation.HumanIndicator;
import environment.city.parking.ParkingSpot;

public class ParkUtility extends AbstractUtility<ParkProperties> {
	
	public ParkUtility(Map<String, String> jsonFiles, String profile, String areaCoefFile){
		super(jsonFiles, profile, areaCoefFile);
	}

	@Override
	public double getScore(Object objectToScore, ParkProperties properties) {
		if(objectToScore instanceof ParkingSpot){
			return getScore("car", objectToScore, properties, ((ParkingSpot) objectToScore).getBlockFaceTag());
		}
		return 0;
	}
	
	@Override
	public double getSatisfaction(Object objectToScore, ParkProperties properties){
		if(objectToScore instanceof ParkingSpot){			
			return getSatisfaction("car", objectToScore, properties);
		} else if(objectToScore == null){
			return getSatisfaction("bus", objectToScore, properties);
		}
		return 0;
	}
	
	@Override
	public double parseCategorieFromIndicator(Object objectToScore, ParkProperties properties, HumanIndicator indicator) {
		if (indicator.getCategorie().equals("distance")){
			String text = indicator.getText();
			String[] values = text.split(",");
			return GISComputation.GPS2Meter((Coordinate)parseTextFromIndicator(objectToScore, properties, values[0]), (Coordinate)parseTextFromIndicator(objectToScore, properties, values[1]));
		} else if (indicator.getCategorie().equals("value")){
			return (double) parseTextFromIndicator(objectToScore, properties, indicator.getText());
		}
		return -1; //When a categorie is not implemented
	}
	
	@Override
	public Object parseTextFromIndicator(Object objectToScore, ParkProperties properties, String text) throws NullPointerException {
		if (text.equals("position_stop")) {
			if (objectToScore instanceof ParkingSpot) {
				return ((ParkingSpot) objectToScore).getPosition();
			} else {
				return new Coordinate(34.0343, -118.2497); //Bus Stop
			}
		} else if (text.equals("office")) {
			return properties.getWorkOffice().getBuildingEntrance();
		} else if (text.equals("price")) {
			if (objectToScore instanceof ParkingSpot) {
				return ((ParkingSpot) objectToScore).getPrice();
			} else {
				return 2.0; //Bus price
			}
		} else if (text.equals("time")) {
			if (objectToScore instanceof ParkingSpot) {
				return properties.getTimeSearching().getValue();
			} else {
				return GISComputation.GPS2Meter(new Coordinate(34.0343, -118.2497), properties.getWorkOffice().getBuildingEntrance()) * 1.11;
			}
		}
		return null; //When a special text is not implemented
	}

}