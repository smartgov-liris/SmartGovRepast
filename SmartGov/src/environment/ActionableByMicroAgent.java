package environment;

import java.util.List;

/**
 * Implementing this interface on a world object allows an object to be the target of a specific Action.
 * An actionable object gives a list of available actions one can do to it.
 * @see MicroAgentAction
 * @author Simon Pageaud
 *
 */
public interface ActionableByMicroAgent {

	/**
	 * @return A list of AgentAction an agent can do on this world object.
	 */
	List<MicroAgentAction> getAvailableActions();
	
	/**
	 * Specify the behavior of the current world object in regards to the specify action.
	 * @param action chose by the Agent.
	 */
	void doMicroAgentAction(MicroAgentAction action);

}
