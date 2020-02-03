package environment.city;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;

import repast.simphony.space.gis.Geography;
import environment.MicroAgentAction;
import environment.ActionableByMicroAgent;
import environment.WorldObjectShape;
import environment.graph.Graph;
import environment.graph.Node;
import environment.style.TextureLibrary;
import gov.nasa.worldwind.render.SurfaceShape;

/**
 * Base class for every building of the simulation. Specifies actions available to agents and how they work on a building.
 * @author Simon Pageaud
 *
 */
public class Building extends WorldObjectShape implements ActionableByMicroAgent {
	
	@SuppressWarnings("serial")
	public static List<String> OFFICE_TYPE = new ArrayList<String>(){
		{
			add("retail");
			add("industrial");
			add("factory");
		}
	};
	
	public List<Node> closestNodesWithSpots;
	private Map<String, String> osmTags;
	private Coordinate[] polygon;
	private BuildingType type;
	private int closestNodeId;
	
	public Building(Geography<Object> geography, String id, Map<String, String> attributes, SurfaceShape shape, Coordinate[] polygon) {
		super(geography, id);
		this.shape = shape;
		this.osmTags = attributes;
		this.polygon = polygon;
		this.position = polygon[0];
	}
	
	@Override
	public Coordinate getPosition() {
		return polygon[0];
	}
	
	public Coordinate getBuildingEntrance(){
		return polygon[0];
	}
	
	public int getClosestNodeId() {
		return this.closestNodeId;
	}
	
	public void setClosestNodeId(Graph graph) {
		Node node = graph.getNearestNodeFrom(polygon[0]);
		this.closestNodeId = Integer.valueOf(node.getId());
	}
	
	@Override
	public Color getFillColor() {
		if(type == BuildingType.WORKOFFICE){
			return TextureLibrary.WORK_OFFICE_COLOR;
		} else if(type == BuildingType.HOME){
			return TextureLibrary.HOME_COLOR;
		} else if(type == BuildingType.MIXED){
			return TextureLibrary.MIXED_COLOR; 
		} else {
			return super.getFillColor();
		}
	}
	
	@Override
	public double getFillOpacity() {
		return 1.0;
	}
	
	@Override
	public double getLineWidth() {
		return 3.0;
	}
	
	@Override
	public double getLineOpacity() {
		return 1.0;
	}
	
	@Override
	public Color getLineColor() {
		return Color.BLACK;
	}
	
	public Map<String, String> getOsmTags() {
		return osmTags;
	}
	
	public void setOsmTags(Map<String, String> osmTags) {
		this.osmTags = osmTags;
	}
	
	public Coordinate[] getPolygon() {
		return polygon;
	}
	
	public BuildingType getType() {
		return type;
	}
	
	public void setType(BuildingType type) {
		this.type = type;
	}
	
	public Node getClosestNode(){
		return EnvVar.nodes.get(closestNodeId);
	}

	@Override
	public List<MicroAgentAction> getAvailableActions() {
		List<MicroAgentAction> actions = new ArrayList<>();
		actions.add(MicroAgentAction.ENTER);
		actions.add(MicroAgentAction.LEAVE);
		return actions;
	}

	@Override
	public void doMicroAgentAction(MicroAgentAction action) {
		if(action.equals(MicroAgentAction.ENTER)){
			
		}
	}
	
	public void setClosestNodesWithSpots(List<Node> closestNodesWithSpots) {
		this.closestNodesWithSpots = closestNodesWithSpots;
	}
	
	public List<Node> getClosestNodesWithSpots() {
		return closestNodesWithSpots;
	}

}
