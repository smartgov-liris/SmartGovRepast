package simulation.scenario.learningXP;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import policyagent.PolicyAction;
import simulation.scenario.ScenarioDRL;

public class ScenarioBasicSplit extends ScenarioDRL {

	public ScenarioBasicSplit() {
		super();
		filename = "basicsplit.json";
	}
	
	@Override
	protected List<PolicyAction> loadSpecialPolicyActions() {
		List<PolicyAction> policyActions = new ArrayList<>();
		policyActions.add(PolicyAction.SPLIT);
		policyActions.add(PolicyAction.ROLLBACK);
		policyActions.add(PolicyAction.KEEP);
		return policyActions;
	}

	@Override
	protected String loadGISFile() {
		switch (scenarioID) {
		case 1:
			return "two_blocks" + File.separator;
		case 2:
			return "all_connected" + File.separator;
		default:
			return "";
		}
	}

}
