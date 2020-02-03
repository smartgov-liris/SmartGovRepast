package environment;

import com.vividsolutions.jts.geom.Coordinate;

import repast.simphony.space.gis.Geography;

/**
 * WorldObject is the basic abstract class for every element of the simulation.
 * In order to be displayed in the simulation, a class need to extend WorldObject.
 * WorldObject has a geography and a coordinate to locate it in a GIS space.
 * @author Simon Pageaud
 *
 */
public abstract class WorldObject {
	
	protected String id;
	protected Geography<Object> geography;
	protected Coordinate position;
	
	public WorldObject(Geography<Object> geography, String id){
		this.geography = geography;
		this.id = id;
	}
	
	public WorldObject(Geography<Object> geography, Coordinate position, String id){
		this(geography, id);
		this.position = position;
	}
	
	public Geography<Object> getGeography() {
		return geography;
	}
	
	public Coordinate getPosition() {
		return position;
	}
	
	public double[] getPositionInDouble() {
		double[] coords = new double[2];
		coords[0] = position.x;
		coords[1] = position.y;
		return coords;
	}
	
	public void setPosition(Coordinate position) {
		this.position = position;
	}
	
	public void setPosition(double x, double y) {
		this.position.x = x;
		this.position.y = y;
	}
	
	public String getId() {
		return id;
	}
	
}