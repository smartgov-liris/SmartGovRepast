package policyagent;

import java.util.ArrayList;
import java.util.List;

/**
 * Provide a list of actions available to PolicyAgent.
 * A AgentAction can be used instead of a PolicyAction with specific rules.
 * AgentAction only applies to PolicyAgent.
 * @author spageaud
 *
 */
public enum ControlAgentAction {

	SPLIT(0),
	MERGE(1);

	private final int index;
	
	//https://stackoverflow.com/questions/6692664/how-to-get-enum-value-from-index-in-java
	public static final List<PolicyAction> actions = new ArrayList<PolicyAction>() {/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	{ for (PolicyAction action : PolicyAction.values()) add(action); }};
	
	ControlAgentAction(int index){
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	
}
