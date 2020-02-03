package environment.graph;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.space.gis.Geography;

import com.vividsolutions.jts.geom.Coordinate;

import environment.WorldObjectTexture;
import environment.style.TextureLibrary;

public class Node extends WorldObjectTexture {
	
	private List<Arc> outcomingArcs;
	private List<Arc> incomingArcs;
	
	public Node(Geography<Object> geography, String id, Coordinate coordinate){
		super(geography, coordinate, id);
		this.outcomingArcs = new ArrayList<>();
		this.incomingArcs = new ArrayList<>();
		this.texture = TextureLibrary.nodeTexture;
	}
	
	public Node(Geography<Object> geography, String id, Coordinate coordinate, List<Arc> forwardArcs, List<Arc> backwardArcs) {
		super(geography, coordinate, id);
		this.outcomingArcs = forwardArcs;
		this.incomingArcs = backwardArcs;
		this.texture = TextureLibrary.nodeTexture;
	}
	
	public Node(Geography<Object> geography, String id, double latitude, double longitude, List<Arc> forwardArcs, List<Arc> backwardArcs){
		this(geography, id,  new Coordinate(latitude, longitude), forwardArcs, backwardArcs);
	}
	
	public void setIncomingArcs(List<Arc> incomingArcs) {
		this.incomingArcs = incomingArcs;
	}
	
	public void setOutcomingArcs(List<Arc> outcomingArcs) {
		this.outcomingArcs = outcomingArcs;
	}
	
	public void addAIncomingArc(Arc incomingArc){
		if(!incomingArcs.contains(incomingArc)){
			incomingArcs.add(incomingArc);
		}
	}
	
	public void addAOutcomingArc(Arc outcomingArc){
		if(!outcomingArcs.contains(outcomingArc)){
			outcomingArcs.add(outcomingArc);
		}
	}
	
	public String getId() {
		return id;
	}
	
	public List<Arc> getIncomingArcs() {
		return incomingArcs;
	}
	
	public List<Arc> getOutcomingArcs() {
		return outcomingArcs;
	}
	
	public double[] getCoordsInTable(){
		double[] coords = new double[2];
		coords[0] = this.getPosition().x;
		coords[1] = this.getPosition().y;
		return coords;
	}
	
}
