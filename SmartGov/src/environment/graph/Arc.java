package environment.graph;

import com.vividsolutions.jts.geom.MultiLineString;

import repast.simphony.space.gis.Geography;
import simulation.Vector2D;
import environment.WorldObjectShape;
import gov.nasa.worldwind.render.SurfacePolyline;

/**
 * Represents a straight line between two nodes.
 * @author Simon Pageaud
 *
 */
public class Arc extends WorldObjectShape {

	protected final Node startNode;
	protected final Node targetNode;
	protected Road road;
	protected double distance;
	protected boolean incoming;
	protected boolean outcoming;
	protected final Vector2D direction;
	protected MultiLineString polyLine;
	
	public Arc(Geography<Object> geography, String id, Node startNode, Node targetNode, double distance, MultiLineString polyLine){
		super(geography, id);
		this.shape = new SurfacePolyline();
		this.startNode = startNode;
		this.targetNode = targetNode;
		this.distance = distance;
		this.polyLine = polyLine;
		this.direction = new Vector2D(startNode.getPosition(), targetNode.getPosition());
		this.direction.normalize();
	}
	
	public Arc(Geography<Object> geography, String id, Node startNode, Node targetNode, double distance, boolean incoming, boolean outcoming, MultiLineString polyLine){
		this(geography, id, startNode, targetNode, distance, polyLine);
		this.incoming = incoming;
		this.outcoming = outcoming;
	}
	
	public Arc(Geography<Object> geography, String id, Road road, Node startNode, Node targetNode, double distance, MultiLineString polyLine){
		this(geography, id, startNode, targetNode, distance, polyLine);
		this.road = road;
	}
	
	public Arc(Geography<Object> geography, String id, Road road, Node startNode,
			Node targetNode, double distance, boolean incoming, boolean outcoming,
			MultiLineString polyLine) {
		this(geography, id, startNode, targetNode, distance, incoming, outcoming, polyLine);
		this.road = road;
	}

	public Node getStartNode() {
		return startNode;
	}
	
	public Node getTargetNode() {
		return targetNode;
	}
	
	public MultiLineString getPolyLine() {
		return polyLine;
	}
	
	public boolean isIncoming() {
		return incoming;
	}
	
	public boolean isOutcoming() {
		return outcoming;
	}
	
	public double getDistance() {
		return distance;
	}
	
	public Vector2D getDirection() {
		return direction;
	}
	
	@Override
	public String toString() {
		return new String("| " + startNode.getId() + " | " + targetNode.getId() + " | " + isOutcoming() + " | " + isIncoming() + " | " + distance + " |");
	}
	
	public Road getRoad() {
		return road;
	}
	
	public void setRoad(Road road) {
		this.road = road;
	}
	
}
