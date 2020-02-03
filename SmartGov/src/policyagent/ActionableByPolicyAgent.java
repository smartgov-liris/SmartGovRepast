package policyagent;

import java.util.List;

/**
 * Only structures may implement ActionableByPolicyAgent.
 * Provides available PolicyActions to policy agents.
 * @author Simon Pageaud
 *
 */
public interface ActionableByPolicyAgent {

	List<PolicyAction> getAvailablePolicyActions();
	
	void doPolicyAction(PolicyAction policyAction);
	
}
