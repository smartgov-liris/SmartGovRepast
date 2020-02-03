package microagent.actuator;

import simulation.GISComputation;

import com.vividsolutions.jts.geom.Coordinate;

import environment.graph.Arc;
import environment.graph.SinkNode;
import environment.graph.EdgeOSM;
import microagent.AbstractMicroAgentBody;
import microagent.AbstractMover;
import microagent.simulation.Plan;

/**
 * Car mover is an actuator allowing MicroAgents to move in a simulation by following the current road network.
 * @author Simon Pageaud
 *
 */
public class CarMover extends AbstractMover<Plan> {
	
	@Override
	public Coordinate moveOn(Plan plan, Coordinate currentPosition, double currentSpeed, double remainingDistanceToTravel, AbstractMicroAgentBody agentBody){
		Arc arc = plan.getCurrentArc();
		if(arc != null){
			Coordinate destination = plan.getNextNode().getPosition();
			updateAgent(arc, agentBody);
			double remainingDistanceToNode = GISComputation.GPS2Meter(currentPosition, destination);
			if(remainingDistanceToNode < remainingDistanceToTravel){
				((EdgeOSM)arc).getAgentsInRoad().remove(agentBody);
				currentPosition = destination;
				plan.reachANode();
				arc = plan.getCurrentArc();
				if(arc != null){
					updateAgent(arc, agentBody);
					return moveOn(plan, currentPosition, currentSpeed, (remainingDistanceToTravel - remainingDistanceToNode), agentBody);
				}
			} else {
				double dx = (destination.x - currentPosition.x)/remainingDistanceToNode;
				double dy = (destination.y - currentPosition.y)/remainingDistanceToNode;
				Coordinate newCoordinates = new Coordinate(dx*remainingDistanceToTravel, dy*remainingDistanceToTravel);
				return new Coordinate(currentPosition.x + newCoordinates.x, currentPosition.y + newCoordinates.y);
			}
		} else {
			plan.setPathComplete(true);
			if(plan.getLastNode() instanceof SinkNode){
				((SinkNode)plan.getLastNode()).addAgentBody(agentBody);
			}
		}
		return currentPosition;
	}
	
	public void updateAgent(Arc edge, AbstractMicroAgentBody agentBody){
		if(!((EdgeOSM)edge).getAgentsInRoad().contains(agentBody)){
			((EdgeOSM)edge).getAgentsInRoad().add(agentBody);
		}
		agentBody.setObjectToPerceive((EdgeOSM) edge);
		agentBody.setDirection(edge.getDirection());
	}

	@Override
	public Coordinate moveOn(Plan objectToMoveOn, Coordinate currentPosition,
			Coordinate destination, double currentSpeed, AbstractMicroAgentBody agentBody) {
		double distanceToTravel = GISComputation.GPS2Meter(currentPosition, destination);
		if(distanceToTravel <= currentSpeed){
			return destination;
		} else {
			return moveOn(objectToMoveOn, currentPosition, currentSpeed, currentSpeed, agentBody);
		}
	}
}