package microagent.perception;

import environment.MicroAgentBody;
import environment.Perceivable;
import microagent.AbstractSensor;

/**
 * Should only perceive roads and elements on roads (agents, spots, ...).
 * @author Simon Pageaud
 *
 */
public class CarDriverSensor extends AbstractSensor<MicroAgentBody> {

	@Override
	public Perception getPerceptions(Perceivable perceivableObject,
			MicroAgentBody agentBody) {		
		return perceivableObject.perceivedObject(agentBody);
	}
	
}
