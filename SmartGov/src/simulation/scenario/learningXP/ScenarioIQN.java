package simulation.scenario.learningXP;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import policyagent.PolicyAction;
import simulation.scenario.ScenarioDRL;

public class ScenarioIQN extends ScenarioDRL {

	public ScenarioIQN() {
		super();
		filename = "iqn.json";
	}
	
	/**
	 * Case 5 : 2 personalities 1,3;2,4
	 * Case 6 : 2 personalities 1,2;3,4 
	 */
	@Override
	protected String loadGISFile() {
		switch (scenarioID) {
		case 1:
			return "two_blocks" + File.separator;
		case 2:
			return "two_blocks" + File.separator;
		case 3:
			return "all_connected" + File.separator;
		case 4:
			return "all_connected" + File.separator;
		case 5:
			return "four_blocks" + File.separator;
		case 6:
			return "four_blocks" + File.separator;
		case 7:
			return "four_blocks" + File.separator;
		case 8:
			return "four_blocks_connected" + File.separator;
		case 9:
			return "four_blocks_connected" + File.separator;
		case 10:
			return "four_blocks_connected" + File.separator;
		default:
			return "";
		}
	}
	
	@Override
	protected List<PolicyAction> loadSpecialPolicyActions() {
		List<PolicyAction> policyActions = new ArrayList<>();
		return policyActions;
	}

}
