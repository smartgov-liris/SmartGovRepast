package environment;

import microagent.AbstractMicroAgentBody;
import microagent.AbstractMover;
import microagent.AbstractSensor;
import microagent.actuator.GippsSteering;
import microagent.perception.Perception;
import microagent.simulation.Plan;
import repast.simphony.space.gis.Geography;
import simulation.Vector2D;

import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

import environment.city.EnvVar;
import environment.graph.Node;
import environment.style.TextureLibrary;

/**
 * This AgentBody is an implementation of the abstract class AbstractAgentBody used in every current scenario of SmartGov.
 * As an agent, this class contains sensors and actions.
 * @author Simon Pageaud
 *
 */
public class MicroAgentBody extends AbstractMicroAgentBody {
		
	private Plan plan;
	
	private double speedInMetersPerSecond; 
	
	private ActionableByMicroAgent objectToInteractWith;
	private AbstractSensor<MicroAgentBody> sensor;
	private Perceivable objectToPerceive;
	private Coordinate destination;
	
	private AbstractMover<Plan> mover;
	private GippsSteering steering;
	
	public MicroAgentBody(int agentID,
			String id,
			Geography<Object> geography,
			AbstractSensor<MicroAgentBody> sensor,
			AbstractMover<Plan> mover,
			GippsSteering steering) {
		super(agentID, id, geography);
		this.mover = mover;
		this.sensor = sensor;
		this.steering = steering;
	}
	
	/**
	 * Specify the behavior of the current agent in respect to actions.
	 */
	public void doAction(MicroAgentAction action){
		if(action == MicroAgentAction.MOVE){
			displaceAgent(this.mover.moveOn(plan, getPosition(), 10., this.speedInMetersPerSecond, this));
		} else if(action == MicroAgentAction.ENTER){
			displaceAgent(((WorldObject)objectToInteractWith).getPosition());
			objectToInteractWith.doMicroAgentAction(action);
		} else if(action == MicroAgentAction.LEAVE){
			setTexture(TextureLibrary.agentBodyTexture);
			if(objectToInteractWith != null){
				objectToInteractWith.doMicroAgentAction(action);
			}
		} else if(action.equals(MicroAgentAction.MOVETO)){
			displaceAgent(this.mover.moveOn(plan, getPosition(), destination, speedInMetersPerSecond, this));
		}
	}
	
	public void displaceAgent(Coordinate destination) {
		this.geography.move(this, EnvVar.GEOFACTORY.createPoint(destination));
		this.setPosition(destination);
	}

	@Override
	public Perception perceivedObject(AbstractMicroAgentBody agentBodyAbstract) {
		return new Perception(this);
	}

	public void setSpeed(double speed) {
		if(Double.isNaN(speed)){
			this.speedInMetersPerSecond = 0.0;
		} else {
			this.speedInMetersPerSecond = speed;
		}
	}
	
	public double getSpeed() {
		return speedInMetersPerSecond;
	}
	
	public Plan getPlan() {
		return plan;
	}
	
	public void setObjectToInteractWith(ActionableByMicroAgent objectToEnter) {
		this.objectToInteractWith = objectToEnter;
	}
	
	public void setObjectToPerceive(Perceivable objectToPerceive) {
		this.objectToPerceive = objectToPerceive;
	}
	
	public ActionableByMicroAgent getObjectToInteractWith() {
		return objectToInteractWith;
	}
	
	public void updatePlan(List<Node> nodes) {
		plan.update(nodes);
	}
	
	public Perception getPerception(){
		return objectToPerceive != null ? this.sensor.getPerceptions(objectToPerceive, this) : new Perception();
	}	

	public GippsSteering getSteering() {
		return steering;
	}
	
	public void setDestination(Coordinate destination) {
		this.destination = destination;
	}
	
	public int getAgentID() {
		return agentID;
	}
	
	public void setAgentID(int agentID) {
		this.agentID = agentID;
	}
	
	@Override
	public void init(int agentID){
		this.agentID = agentID;
		objectToPerceive = null;
		texture = TextureLibrary.agentBodyTexture;
		speedInMetersPerSecond = 0.0;
		direction = new Vector2D();
		plan = new Plan();
	}
	
}