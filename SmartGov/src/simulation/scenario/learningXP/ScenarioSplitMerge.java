package simulation.scenario.learningXP;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import policyagent.PolicyAction;
import simulation.scenario.ScenarioDRL;

public class ScenarioSplitMerge extends ScenarioDRL {

	public ScenarioSplitMerge() {
		super();
		filename = "splitmerge.json";
	}

	/**
	 * Case 1 : 2 personalities 1,3;2,4
	 * Case 2 :
	 * Case 3 : 2 personalities 1,2;3,4
	 * Case 4 : 1 personality
	 * Case 5 : 2 personalities 1;2
	 * Case 6 : 1 personality 
	 */
	@Override
	protected String loadGISFile() {
		switch (scenarioID) {
		case 1:
			return "four_blocks" + File.separator;
		case 2:
			return "four_blocks_all_connected" + File.separator;
		case 3:
			return "four_blocks" + File.separator;
		case 4:
			return "four_blocks" + File.separator;
		case 5:
			return "two_blocks" + File.separator;
		case 6:
			return "two_blocks" + File.separator;
		default:
			return "";
		}
	}

	@Override
	protected List<PolicyAction> loadSpecialPolicyActions() {
		List<PolicyAction> policyActions = new ArrayList<>();
		policyActions.add(PolicyAction.MERGE);
		policyActions.add(PolicyAction.SPLIT);
		policyActions.add(PolicyAction.ROLLBACK);
		policyActions.add(PolicyAction.KEEP);
		return policyActions;
	}
	
}
