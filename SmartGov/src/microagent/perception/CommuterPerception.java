package microagent.perception;

import java.util.ArrayList;

import environment.MicroAgentBody;
import environment.city.parking.ParkingSpot;
import environment.graph.EdgeOSM;
import environment.graph.Node;
import microagent.AbstractPerception;

/**
 * CommuterPerception stores objects a commuter perceives during the simulation.
 * @author Simon Pageaud
 *
 */
public class CommuterPerception extends AbstractPerception {

	public CommuterPerception(){
		super();
		this.perceivedObjects.put(EdgeOSM.class.getSimpleName(), new ArrayList<>());
		this.perceivedObjects.put(MicroAgentBody.class.getSimpleName(), new ArrayList<>());
		this.perceivedObjects.put(ParkingSpot.class.getSimpleName(), new ArrayList<>());
		this.perceivedObjects.put(Node.class.getSimpleName(), new ArrayList<>());
	}
	
}