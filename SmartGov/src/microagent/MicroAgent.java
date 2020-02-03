package microagent;

import environment.MicroAgentBody;
import environment.style.TextureLibrary;

/**
 * The MicroAgent is the main component of the microscopic layer of the simulation. It is described by a behavior, a set of perceptions and properties. 
 * @author Simon Pageaud
 *
 */
public class MicroAgent extends AbstractMicroAgent<MicroAgentBody> {

	private AbstractBehavior<? extends AbstractPerception, ? extends AbstractProperties, ? extends AbstractMicroAgentBody> behavior;
	private AbstractPerception perceptions;
	private AbstractProperties properties;
	
	public MicroAgent(int id, MicroAgentBody body, AbstractBehavior<? extends AbstractPerception, ? extends AbstractProperties, ? extends AbstractMicroAgentBody> behavior, AbstractProperties properties, AbstractPerception perceptions) {
		super(id, body);
		this.body.setTexture(TextureLibrary.agentBodyTexture);
		this.body.setSpeed(0.0);
		
		this.perceptions = perceptions;
		this.behavior = behavior;
		this.properties = properties;
	}
	
	public MicroAgent(int id, AbstractBehavior<? extends AbstractPerception, ? extends AbstractProperties, ? extends AbstractMicroAgentBody> behavior, AbstractProperties properties, AbstractPerception perceptions) {
		super(id);
		this.perceptions = perceptions;
		this.behavior = behavior;
		this.properties = properties;
	}

	/**
	 * At each tick of the simulation, the MicroAgent perceives its environment and acts accordingly.
	 */
	@Override
	public void live() {
		perceptions.filterPerception(this.getBody().getPerception());
		this.getBody().doAction(behavior.provideAction(id, perceptions, properties, this.getBody()));
		perceptions.clear();
	}
	
	public AbstractProperties getProperties() {
		return properties;
	}
	
	public AbstractBehavior<? extends AbstractPerception, ? extends AbstractProperties, ? extends AbstractMicroAgentBody> getBehavior() {
		return behavior;
	}

	@Override
	public void recycleAgent(int id) {
		this.setId(id);
		this.getProperties().resetProperties(id);
		this.behavior.setInitialState();
		if(this.body != null){
			this.body.init(id);
		}
		
	}
	
}