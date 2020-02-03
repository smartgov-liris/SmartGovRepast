package policyagent.learning.strategy;

import java.util.List;

import policyagent.PolicyAction;

/**
 * Strategy for the exploration/exploitation tradeoff in reinforcement
 * learning.
 * @author Simon
 *
 */
public abstract class Strategy {
	
	protected PolicyAction lastAction;
	protected String lastAnswer; //String version of last action plus relevant pieces of information such as random action
	protected PolicyAction lastPredictedAction;
	protected boolean stillExploration;
	protected List<PolicyAction> policyActions;
	
	public Strategy(List<PolicyAction> policyActions) {
		this.policyActions = policyActions;
	}
	
	public abstract PolicyAction chooseAction();
	
	public void setLastAction(PolicyAction action) {
		this.lastAction = action;
	}
	
	public String getLastAnswer() {
		return lastAnswer;
	}
	
	public PolicyAction getLastPredictedAction() {
		return lastPredictedAction;
	}
	
	public boolean isStillExploration() {
		return stillExploration;
	}
	
}
