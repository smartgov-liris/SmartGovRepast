package microagent;

import environment.Perceivable;
import microagent.perception.Perception;

/**
 * The frustum represents the vision of the agent, or the way agent can
 * perceive his environment.
 * @author Simon Pageaud
 *
 */
public abstract class AbstractSensor<B extends AbstractMicroAgentBody> {
	
	public abstract Perception getPerceptions(Perceivable perceivableObject, B agentBody);
	
}
