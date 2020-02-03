package environment;

import java.util.ArrayList;
import java.util.List;

import policyagent.PolicyAction;

/**
 * Action allows agents to interact with the environment.
 * The detail of the action is reserved by the actionable object that describes
 * what it can do with the specific action.
 * @see ActionableByMicroAgent
 * @author Simon Pageaud
 *
 */
public enum MicroAgentAction {
	
	IDLE(0),
	MOVE(1),
	ENTER(2),
	LEAVE(3),
	MOVETO(4);

	private final int index;
	
	//https://stackoverflow.com/questions/6692664/how-to-get-enum-value-from-index-in-java
	public static final List<PolicyAction> actions = new ArrayList<PolicyAction>() {/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	{ for (PolicyAction action : PolicyAction.values()) add(action); }};
	
	MicroAgentAction(int index){
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	
}
