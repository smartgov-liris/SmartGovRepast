package environment;

import microagent.AbstractMicroAgentBody;
import microagent.perception.Perception;

/**
 * Implementing this interface allows the agent to perceive the current object as part of the environment.
 * @see Perception
 * @author Simon Pageaud
 *
 */
public interface Perceivable {

	/**
	 * Describes how the world object is perceived by other agents.
	 * @param agentBodyAbstract Gives a body with a set of sensors
	 * @return The perception used by the agent.
	 */
	Perception perceivedObject(AbstractMicroAgentBody agentBodyAbstract);
	
}
