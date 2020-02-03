package policyagent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ActionsSequence {

	private List<PolicyAction> actions;
	private boolean highestGainReached;
	
	public ActionsSequence() {
		actions = new ArrayList<>();
		highestGainReached = false;
	}
	
	public boolean compareTo(ActionsSequence sequence) {
		if(actions.size() == sequence.getActions().size()) {
			return compareNbOfActions(sequence.nbOfActions());
		} else {
			return false;
		}
	}
	
	public void addPolicyAction(PolicyAction action) {
		actions.add(action);
	}
	
	public List<PolicyAction> getActions() {
		return actions;
	}
	
	public void setActions(List<PolicyAction> actions) {
		this.actions = actions;
	}
	
	public boolean isHighestGainReached() {
		return highestGainReached;
	}
	
	public void setHighestGainReached(boolean highestGainReached) {
		this.highestGainReached = highestGainReached;
	}
	
	public void clear() {
		actions.clear();
		highestGainReached = false;
	}
	
	public Map<PolicyAction, Integer> nbOfActions(){
		Map<PolicyAction, Integer> nbOfActions = new HashMap<>();
		for(int i = 0; i < actions.size(); i++) {
			if(nbOfActions.containsKey(actions.get(i))) {
				nbOfActions.put(actions.get(i), nbOfActions.get(actions.get(i)) + 1);
			} else {
				nbOfActions.put(actions.get(i), 1);
			}
		}
		return nbOfActions;
	}
	
	public boolean compareNbOfActions(Map<PolicyAction, Integer> nbOfActions) {
		Map<PolicyAction, Integer> currentNbOfActions = nbOfActions();
		for(Entry<PolicyAction, Integer> entry : currentNbOfActions.entrySet()) {
			if(nbOfActions.containsKey(entry.getKey())) {
				if(nbOfActions.get(entry.getKey()) != entry.getValue()) {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}
	
	public int size() {
		return actions.size();
	}
	
	@Override
	public String toString() {
		String str = "Sequence:[";
		if(actions.size() > 0) {
			for(int i = 0; i < actions.size() - 1; i++) {
				str += actions.get(i).name() + ",";
			}
			str += actions.get(actions.size() - 1).name(); 
		}
		return str + "]";
	}

}
