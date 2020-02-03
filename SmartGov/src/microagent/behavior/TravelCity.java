package microagent.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import simulation.GISComputation;
import simulation.Vector2D;
import environment.MicroAgentAction;
import environment.MicroAgentBody;
import environment.city.EnvVar;
import environment.city.parking.ParkingSpot;
import environment.graph.Arc;
import environment.graph.EdgeOSM;
import environment.graph.Node;
import environment.style.TextureLibrary;
import microagent.AbstractMicroAgentBody;
import microagent.AbstractBehavior;
import microagent.AbstractPerception;
import microagent.AbstractProperties;
import microagent.perception.CommuterPerception;
import microagent.properties.ParkProperties;
/**
 * Finite state machine for exterior commuter looking for a parking spot near its office.
 * @author Simon
 *
 */
public class TravelCity extends AbstractBehavior<CommuterPerception, ParkProperties, MicroAgentBody> {

	private TravelStateSimplify travelState;
	
	//Inner classes for Finite State Machine
	private static Idle idle=null;
	private static MovingToWork movingToWork = null;
	private static MovingAndAware movingAndAware = null;
	private static LookingForParking lookingForParking = null;
	private static MoveToParking moveToParking = null;
	@SuppressWarnings("unused")
	private static Park park = null;
	private static Wander wander = null;
	
	//Speed
	private double speedMovingToWork;
	private double speedMovingAndAware;
	private double speedLooking;
	private double speedWander;
	private double speedMoveToParking;
	
	private static boolean firstone=true; //Use for singleton
	
	public TravelCity(){
		if (firstone){
			init();
		}
		
		this.travelState = new MovingToWork();
	}
	
	protected void init(){
		speedMovingToWork = Double.valueOf(EnvVar.configFile.get("movingToWork"));
		speedMovingAndAware = Double.valueOf(EnvVar.configFile.get("movingAndAware"));
		speedLooking = Double.valueOf(EnvVar.configFile.get("looking"));
		speedWander = Double.valueOf(EnvVar.configFile.get("wander"));
		speedMoveToParking = Double.valueOf(EnvVar.configFile.get("movingToParking"));
		
		idle = new Idle();	
		movingToWork = new MovingToWork();
		movingAndAware = new MovingAndAware();
		lookingForParking = new LookingForParking();
		moveToParking = new MoveToParking();
		park = new Park();
		wander = new Wander();
		firstone = false;
	}
	
	@Override
	public MicroAgentAction provideAction(int id, 
			AbstractPerception perceptions,
			AbstractProperties weights, 
			AbstractMicroAgentBody body) {
		
		return this.travelState.doAction(id, (CommuterPerception) perceptions, (ParkProperties) weights, this, (MicroAgentBody) body);
		
	}

	public interface TravelStateSimplify {

		public MicroAgentAction doAction(int id, CommuterPerception perceptions, ParkProperties weigths, TravelCity travelCity, MicroAgentBody body);

	}
	
	public class Idle implements TravelStateSimplify {

		@Override
		public MicroAgentAction doAction(int id, CommuterPerception perceptions,
				ParkProperties weigths,
				TravelCity travelCity, MicroAgentBody body) {
			return MicroAgentAction.IDLE;
		}
		
	}

	public class MovingToWork implements TravelStateSimplify {

		@Override
		public MicroAgentAction doAction(int id, CommuterPerception perceptions,
				ParkProperties weigths,
				TravelCity travelCity, MicroAgentBody body) {

			if(GISComputation.GPS2Meter(body.getPosition(), weigths.getWorldObjects().get("endNode").getPosition()) <= weigths.getAttributesD().get("dparkAwareness")){
				travelCity.setTravelStateSimplify(movingAndAware);
				body.setTexture(TextureLibrary.agentBodyAwareness);
			}

			updateAgentSpeed(perceptions, body, speedMovingToWork);
			return MicroAgentAction.MOVE;
		}

	}

	public class MovingAndAware implements TravelStateSimplify {

		@Override
		public MicroAgentAction doAction(int id, CommuterPerception perceptions,
				ParkProperties weigths,
				TravelCity travelCity, MicroAgentBody body) {

			updateAgentSpeed(perceptions, body, speedMovingAndAware);

			if(GISComputation.GPS2Meter(body.getPosition(), weigths.getWorldObjects().get("endNode").getPosition()) > weigths.getAttributesD().get("dpark")){
				return MicroAgentAction.MOVE;
			} else {
				travelCity.setTravelStateSimplify(lookingForParking);
				body.setTexture(TextureLibrary.agentBodyCloseToDestination);		
				return MicroAgentAction.MOVE;
			}
		}

	}

	public class LookingForParking implements TravelStateSimplify {

		@Override
		public MicroAgentAction doAction(int id, CommuterPerception perceptions,
				ParkProperties weigths,
				TravelCity travelCity, MicroAgentBody body) {
			
			if(!body.getPlan().isPathComplete()){
				if(!weigths.isAlreadyChecked()){
					//Search a suitable parking slot
					List<ParkingSpot> spots = parkingPerceived(perceptions);
					
					//found a parking and move to it
					if(!spots.isEmpty()){
						List<ParkingSpot> goodSpots = new ArrayList<>();
						Random rnd = new Random();
						//Use score for one parking
						if(rnd.nextDouble() < weigths.computeScore(spots.get(0))){
							weigths.setUtilityWhenParked(weigths.computeScore(spots.get(0)));
							for(int i = 0; i < spots.size(); i++){
								goodSpots.add(spots.get(i));
								
							}
							if(parkingSearch(weigths, goodSpots, body)){
								travelCity.setTravelStateSimplify(moveToParking);
								updateAgentSpeed(perceptions, body, speedMoveToParking);
								return MicroAgentAction.MOVETO;
							}
						}
						
					}
					weigths.setAlreadyChecked(true);
				}
				weigths.getTimeSearching().updateValue(1);
				updateAgentSpeed(perceptions, body, speedLooking);
				return MicroAgentAction.MOVE;
			} else {
				weigths.getTimeSearching().updateValue(1);
				travelCity.setTravelStateSimplify(wander);//new Wander());
				body.setTexture(TextureLibrary.agentBodyWander);
				return MicroAgentAction.IDLE;
			}
		}

	}

	public class MoveToParking implements TravelStateSimplify {

		@Override
		public MicroAgentAction doAction(int id, CommuterPerception perceptions,
				ParkProperties weigths,
				TravelCity travelCity, MicroAgentBody body) {
			
			//Check if spot is occupied
			if(weigths.getParkingSpot().isOccupied()){
				weigths.setUtilityWhenParked(0.0); //reset utility
				travelCity.setTravelStateSimplify(wander);//new Wander());
				body.setTexture(TextureLibrary.agentBodyWander);
				return MicroAgentAction.MOVE;
			} else {
				//if Agent reaches parking spot
				if(body.getPosition().equals2D(weigths.getParkingSpot().getProjectionOnEdge())){
					//*/ Move to Park
					travelCity.setTravelStateSimplify(idle);
					weigths.setParked(true);
					body.setTexture(TextureLibrary.agentBodyParked);
					weigths.getDistanceWork().setValue(GISComputation.GPS2Meter(body.getPosition(), weigths.getWorkOffice().getPosition()));
					weigths.updateHistoric(body.getPosition());
					weigths.getParkingSpot().setIdOfAgentParked(id);
					return MicroAgentAction.ENTER;
					/*/
					travelCity.setTravelStateSimplify(park);
					return Action.IDLE;
					//*/
				} else { //go to parking spot
					updateAgentSpeed(perceptions, body, speedMoveToParking);
					return MicroAgentAction.MOVETO;
				}
			}
		}

	}
	
	public class Park implements TravelStateSimplify {

		@Override
		public MicroAgentAction doAction(int id, CommuterPerception perceptions, 
				ParkProperties weigths, 
				TravelCity travelCity,
				MicroAgentBody body) {
			if(weigths.getTimeToPark() == weigths.getCurrentTimeToPark()) {
				travelCity.setTravelStateSimplify(idle);
				weigths.setParked(true);
				body.setTexture(TextureLibrary.agentBodyParked);
				weigths.getDistanceWork().setValue(GISComputation.GPS2Meter(body.getPosition(), weigths.getWorkOffice().getPosition()));
				weigths.updateHistoric(body.getPosition());
				weigths.getParkingSpot().setIdOfAgentParked(id);
				return MicroAgentAction.ENTER;
			} else {
				weigths.setCurrentTimeToPark();
				return MicroAgentAction.IDLE;
			}
		}
		
	}

	public class Wander implements TravelStateSimplify {
		
		@Override
		public MicroAgentAction doAction(int id, CommuterPerception perceptions,
				ParkProperties weigths,
				TravelCity travelCity, MicroAgentBody body) {
			
			if(body.getPlan().isPathComplete()){
				research(weigths, body, travelCity);
				if(body.getPlan().getCurrentNode().getOutcomingArcs().size() > 1){ //crossroad
				//if(body.getPlan().getCurrentNode().isNeedToSlow()){ //create a bug when agent can no longer park in spots
					updateAgentSpeed(perceptions, body, 0.0); //force arc change
					return MicroAgentAction.MOVE;
				}
			}
			
			
			if(!weigths.isAlreadyChecked()){
				//Search a suitable parking slot
				List<ParkingSpot> spots = parkingPerceived(perceptions);
				
				//found a parking and move to it
				if(!spots.isEmpty()){
					List<ParkingSpot> goodSpots = new ArrayList<>();
					Random rnd = new Random();
					//Use score for one parking
					if(rnd.nextDouble() < weigths.computeScore(spots.get(0))){
						weigths.setUtilityWhenParked(weigths.computeScore(spots.get(0)));
						for(int i = 0; i < spots.size(); i++){
							goodSpots.add(spots.get(i));
						}
						if(parkingSearch(weigths, goodSpots, body)){
							travelCity.setTravelStateSimplify(moveToParking);
							updateAgentSpeed(perceptions, body, speedMoveToParking);
							return MicroAgentAction.MOVETO;
						}
					}
					
				}
				weigths.setAlreadyChecked(true);
			}
			weigths.getTimeSearching().updateValue(1);
			updateAgentSpeed(perceptions, body, speedWander);
			return MicroAgentAction.MOVE;
		}
		
		/**
		 * Search algorithm using euclidian distance between destination and target node of current arc using number of time an agent use this road.
		 * @param properties
		 * @param body
		 * @param travelCity
		 */
		public void research(ParkProperties properties, MicroAgentBody body, TravelCity travelCity){
			//TODO increase area of research based on time spent looking
			
			properties.setAlreadyChecked(false);
			Node currentNode = body.getPlan().getLastNode();
			List<Arc> availableArcs = currentNode.getOutcomingArcs();
			for(int i = 0; i < availableArcs.size(); i++){
				if(!properties.getNumberOfRoadsCrossed().containsKey(availableArcs.get(i).getId())){
					properties.getNumberOfRoadsCrossed().put(availableArcs.get(i).getId(), 0);
				}
			}
			double min = Double.MAX_VALUE;
			int index = -1;
			if(availableArcs.size() == 1){
				index = 0;
			} else {
				for(int indexOfArc = 0; indexOfArc < availableArcs.size(); indexOfArc++){
					if(availableArcs.get(indexOfArc).getTargetNode() != body.getPlan().getPreviousNode()){
						double distance = GISComputation.GPS2Meter(availableArcs.get(indexOfArc).getTargetNode().getPosition(), properties.getWorkOffice().getBuildingEntrance());
						double score = distance * (1 + properties.getNumberOfRoadsCrossed().get(availableArcs.get(indexOfArc).getId()));
						//if(distance < threshold && score < min){
						if(score < min){
							min = score;
							index = indexOfArc;
						}
					}
				}
			}
			
			properties.getNumberOfRoadsCrossed().put(availableArcs.get(index).getId(), properties.getNumberOfRoadsCrossed().get(availableArcs.get(index).getId()) + 1);
			body.getPlan().addANode(availableArcs.get(index).getTargetNode());
			
		}

	}

	public void setTravelStateSimplify(TravelStateSimplify travelState) {
		this.travelState = travelState;
	}
	
	protected boolean parkingSearch(ParkProperties properties, List<ParkingSpot> spots, MicroAgentBody body){
		double minDistance = Double.MAX_VALUE;
		int spotIndex = -1;
		boolean foundParking = false;
		if(!spots.isEmpty()){
			for(int i = 0; i < spots.size(); i++){
				if(!spots.get(i).isOccupied()){
					Vector2D directionToParkingSlot = GISComputation.directionOfTwoCoordinates(body.getPosition(), spots.get(i).getProjectionOnEdge());
					double angle = Math.floor(Math.abs(body.getDirection().normalize().angleInDeg(directionToParkingSlot.normalize())));
					double distanceToParking = GISComputation.GPS2Meter(body.getPosition(), spots.get(i).getProjectionOnEdge());
					if(angle == 0.0 && distanceToParking < minDistance){
						minDistance = distanceToParking;
						spotIndex = i;
						foundParking = true;
					}
				}
			}
		}
		if(foundParking){
			properties.setParkingSpot(spots.get(spotIndex));
			properties.setSatisfaction(properties.computeSatisfaction(spots.get(spotIndex)));
			body.setDestination(spots.get(spotIndex).getProjectionOnEdge());
			body.setObjectToInteractWith(properties.getParkingSpot());
			body.setTexture(TextureLibrary.agentBodyMovingToSpot);
		} else {
			properties.setSatisfaction(0.0);
		}
		return foundParking;
	}
	
	@SuppressWarnings("unchecked")
	public List<ParkingSpot> parkingPerceived(CommuterPerception perceptions){
		if(perceptions.getPerceivedObjects().get(EdgeOSM.class.getSimpleName()) != null && !perceptions.getPerceivedObjects().get(EdgeOSM.class.getSimpleName()).isEmpty()){
			return (List<ParkingSpot>) perceptions.getPerceivedObjects().get(ParkingSpot.class.getSimpleName());
		}
		return new ArrayList<ParkingSpot>();
	}	

	private void updateAgentSpeed(CommuterPerception perceptions, MicroAgentBody body, double maxSpeed){
		body.setSpeed(body.getSteering().getSpeed(body.getSpeed(), maxSpeed, body.getPlan().getCurrentNode(), 0.0, 7.5, -3, body.getPosition()));
	}

	@Override
	public void setInitialState() {
		this.travelState = movingToWork;
	}

	@Override
	public void setToFinalState() {
		this.travelState = idle;
	}

}
