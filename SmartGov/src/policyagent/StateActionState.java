package policyagent;

/**
 * Stores a couple (state, action, state') to track if two actions have similar effect in the same states.
 * @author spageaud
 *
 */
public class StateActionState {

	private double lastState;
	private PolicyAction lastAction;
	private double currentState;
	
	public StateActionState(double lastState, PolicyAction action, double currentState) {
		this.lastState = lastState;
		lastAction = action;
		this.currentState = currentState;
	}
	
	public boolean compareTo(StateActionState sas) {
		boolean similar = false;
		if(lastState == sas.getLastState() && currentState == sas.getCurrentState()) {
			return true;
		}
		return similar;
	}
	
	public double getCurrentState() {
		return currentState;
	}
	
	public double getLastState() {
		return lastState;
	}
	
	public PolicyAction getLastAction() {
		return lastAction;
	}
	
}
