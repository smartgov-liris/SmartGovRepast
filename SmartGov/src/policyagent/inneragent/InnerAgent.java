package policyagent.inneragent;

import java.util.List;

import policyagent.PolicyAction;
import policyagent.PolicyPerception;
import policyagent.learning.strategy.NNBest;
import policyagent.learning.strategy.Strategy;

public abstract class InnerAgent {
	
	protected String id;
	
	protected PolicyPerception currentPerception;
	
	public void setCurrentPerception(PolicyPerception perception) {
		this.currentPerception = perception;
	}
	
	public abstract void setPerception();
	
	public PolicyPerception getCurrentPerception() {
		return currentPerception;
	}
	
	public String getId() {
		return id;
	}
	
	protected Strategy getStrategy(String strategy, 
			String id,
			int stateSize,
			int nbActions,
			List<String> labels,
			List<PolicyAction> policyActions) {
		if(strategy.equals("neuralnet")) {
			return new NNBest(id, stateSize, nbActions, labels, policyActions);
		} else {
			//TODO add a default strategy
			return null;
		}
	}

}
