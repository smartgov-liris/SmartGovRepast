package policyagent.inneragent;

import java.util.ArrayList;
import java.util.List;

import environment.Structure;
import policyagent.PolicyAction;
import policyagent.PolicyAgent;
import policyagent.PolicyPerception;
import policyagent.Position;
import policyagent.learning.strategy.Strategy;

public class LocalLearner extends InnerAgent {
	
	protected List<Structure> structures;
	protected Structure structure;
	protected List<String> labels;
	protected Strategy explorationMethod;
	protected PolicyAction action;
	protected double lastReward;
	
	/**
	 * Varies between -10 and 10 where -10 is an untrustworthy local agent and 10 is a trustfull agent.
	 * When an agent proposes an action, its score is updated.
	 */
	protected int score;
	private final int MAX_SCORE =  10;
	private final int MIN_SCORE = -10;
	
	public LocalLearner(
			Structure structure, 
			String id, 
			List<String> labels,
			String strategy,
			int nbActions,
			List<PolicyAction> policyActions) {
		this.id = id;
		this.structure = structure;
		structures = new ArrayList<>();
		structures.add(structure);
		this.labels = labels;
		explorationMethod = getStrategy(strategy, id, (labels.size() - 1), nbActions, labels, policyActions);
		action = PolicyAction.NO_ACTION;
		score = 0;
		lastReward = 0.0;
	}

	@Override
	public void setPerception() {
		Position position = new Position();
		List<Position> tempValues = new ArrayList<>();
		for(int i = 0; i < structures.size(); i++) {
			tempValues.add(structures.get(i).getLocalPerformances(labels));
		}
		position = PolicyAgent.averagePosition(tempValues);
		currentPerception = new PolicyPerception(position);
		Position perception = currentPerception.getPosition();
		lastReward = perception.getCoordinates().get(perception.getCoordinates().size() - 1);
	}
	
	public PolicyAction proposeAction() {
		return explorationMethod.chooseAction();
	}
	
	public Strategy getExplorationMethod() {
		return explorationMethod;
	}
	
	public Structure getStructure() {
		return structure;
	}
	
	public void setLastAction(PolicyAction action){
		this.action = action;
		this.explorationMethod.setLastAction(action);
	}
	
	public PolicyAction getLastAction() {
		return this.action;
	}
	
	public int getScore() {
		return score;
	}

	public void updateScore(int modification) {
		score = (int) Math.max(Math.min(score + modification, MAX_SCORE), MIN_SCORE);
	}
	
	public void setScore(int score) {
		this.score = score;
	}
	
	public double getLastReward() {
		return lastReward;
	}
	
	public void setLastReward(double lastReward) {
		this.lastReward = lastReward;
	}
	
	public List<Structure> getStructures() {
		return structures;
	}
}
