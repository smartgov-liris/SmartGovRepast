package policyagent.inneragent;

import java.util.List;

import environment.Structure;
import policyagent.PolicyAction;
import policyagent.learning.strategy.NNBest;

public class DeepLocalLearner extends LocalLearner {

	public DeepLocalLearner(
			Structure structure, 
			String id, 
			List<String> labels, 
			String strategy,
			int nbActions,
			List<PolicyAction> policyActions) {
		super(structure, id, labels, strategy, nbActions, policyActions);
		
	}
	
	@Override
	public PolicyAction proposeAction() {
		if(explorationMethod instanceof NNBest) {
			((NNBest) explorationMethod).setLastPerception(currentPerception.getPosition());
		}
		return explorationMethod.chooseAction();
	}
	
	@Override
	public void setLastAction(PolicyAction action) {
		this.action = action;
		explorationMethod.setLastAction(action);
	}

}
