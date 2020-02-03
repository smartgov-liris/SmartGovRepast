package simulation.scenario.learningXP;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import policyagent.PolicyAction;
import simulation.scenario.ScenarioDRL;

/**
 * This scenario is used to compare basic learning methods without policy agent actions.
 * Experience of experience 0 where we try deep reinforcement learning in very basic cases.
 * @author Simon
 *
 */
public class ScenarioBasicLearning extends ScenarioDRL {

	public ScenarioBasicLearning(){
		super();
		filename = "basiclearning.json";
	}
	
	@Override
	protected List<PolicyAction> loadSpecialPolicyActions() {
		List<PolicyAction> policyActions = new ArrayList<>();
		return policyActions;
	}
	
	@Override
	protected String loadGISFile() {
		return "one_block_basic_learning" + File.separator;
	}

}
