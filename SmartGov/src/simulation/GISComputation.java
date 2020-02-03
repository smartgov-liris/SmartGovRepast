package simulation;

import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

import environment.city.EnvVar;

public class GISComputation {
	
	public static void checkNodesInRoad() {
		int counter = 0;
		for(int indexOfNode = 0; indexOfNode < EnvVar.nodes.size(); indexOfNode++) {
			if(EnvVar.nodes.get(indexOfNode).getIncomingArcs().size() == 1 && EnvVar.nodes.get(indexOfNode).getOutcomingArcs().size() == 1) {
				counter ++;
			} else if(EnvVar.nodes.get(indexOfNode).getIncomingArcs().size() == 2 && EnvVar.nodes.get(indexOfNode).getOutcomingArcs().size() == 2) {
				counter ++;
			}
		}
		System.out.println("Number of nodes that need to be corrected: " + counter);
	}
	
	public static double GPS2Meter(double latitudeInDegA, double longitudeInDegA, double latitudeInDegB, double longitudeInDegB){
		//haversine formula https://en.wikipedia.org/wiki/Haversine_formula
		double earthRadius = 6371000;
		double lat1 = Math.toRadians(latitudeInDegA);
		double lon1 = Math.toRadians(longitudeInDegA);
		double lat2 = Math.toRadians(latitudeInDegB);
		double lon2 = Math.toRadians(longitudeInDegB);
		double a = Math.pow(Math.sin((lat1 - lat2)/2),2) + Math.cos(lat1)*Math.cos(lat2)*Math.pow(Math.sin((lon1-lon2)/2),2);
		double c = 2*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		double d = earthRadius*c;
		return d;
	}
	
	public static Vector2D directionOfTwoCoordinates(Coordinate a, Coordinate b){
		return new Vector2D(b.x - a.x, b.y - a.y);
	}
	
	public static double GPS2Meter(Coordinate a, Coordinate b){
		return GPS2Meter(a.x, a.y, b.x, b.y);
	}
	
	public static Double min(List<Double> numbers) {
		Double minDouble = Double.MAX_VALUE;
		for(int i = 0; i < numbers.size(); i++) {
			if(numbers.get(i) < minDouble) {
				minDouble = numbers.get(i);
			}
		}
		return minDouble;
	}
	
}
