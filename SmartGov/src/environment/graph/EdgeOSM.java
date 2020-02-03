package environment.graph;

import environment.Perceivable;
import environment.city.parking.ParkingSpot;
import microagent.AbstractMicroAgentBody;
import microagent.perception.Perception;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.space.gis.Geography;

import com.vividsolutions.jts.geom.MultiLineString;

public class EdgeOSM extends Arc implements Perceivable {

	protected List<AbstractMicroAgentBody> agentsInRoad;
	protected List<ParkingSpot> spots;
	protected ParkingSpot closeSpot;
	protected int lanes;
	protected String type;
	
	public EdgeOSM(Geography<Object> geography, String id, Node startNode,
			Node targetNode, double distance, boolean backward,
			boolean forward, MultiLineString polyLine,
			int lanes, String type) {
		super(geography, id, startNode, targetNode, distance, backward, forward, polyLine);
		agentsInRoad = new ArrayList<>();
		this.lanes = lanes;
		this.type = type;
		this.spots = new ArrayList<>();
	}
	
	public EdgeOSM(Geography<Object> geography, String id, Node startNode,
			Node targetNode, double distance, MultiLineString polyLine,
			int lanes, String type) {
		super(geography, id, startNode, targetNode, distance, polyLine);
		agentsInRoad = new ArrayList<>();
		this.lanes = lanes;
		this.type = type;
		this.spots = new ArrayList<>();
	}
	
	public List<AbstractMicroAgentBody> getAgentsInRoad() {
		return agentsInRoad;
	}

	@Override
	public Perception perceivedObject(AbstractMicroAgentBody agentBodyAbstract) {
		Perception perception = new Perception();
		
		perception.addPerceivedObject(this);
		
		//Add agent leader in percepts
		perception.addPerceivedObject(agentBodyAbstract);
		if(agentsInRoad.indexOf(agentBodyAbstract) - 1 > -1){
			perception.addPerceivedObject(agentsInRoad.get(agentsInRoad.indexOf(agentBodyAbstract) - 1));
		}
		if(!spots.isEmpty()){
			for(ParkingSpot parkingSpot : spots){
				if(!parkingSpot.isUnavailable()){
					perception.addPerceivedObject(parkingSpot);
				}
			}
		}
		return perception;
	}
	
	public void addParking(ParkingSpot spot){
		this.spots.add(spot);
	}
}
