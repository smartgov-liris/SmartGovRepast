package policyagent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import policyagent.inneragent.InnerAgent;
import policyagent.inneragent.LocalLearner;

public abstract class AbstractPolicyAgent {
	
	protected String id;
	protected List<Indicator> indicators;
	protected List<PolicyAction> actions;
	protected List<PolicyAction> specialActions;
	protected Perimeter perimeter;
	protected List<String> labelsForGlobalPerceptions;
	protected Map<PolicyAction, StateActionState> sasList;
	protected ActionsSequence sequence;
	protected List<ActionsSequence> sequences;
	
	protected List<InnerAgent> innerAgents;
	
	public AbstractPolicyAgent(String id){
		this.id = id;
		indicators                 = new ArrayList<>();
		actions                    = new ArrayList<>();
		specialActions             = new ArrayList<>();
		perimeter                  = new Perimeter();
		innerAgents                = new ArrayList<>();
		labelsForGlobalPerceptions = new ArrayList<>();
		sasList                    = new HashMap<>();
		sequence                   = new ActionsSequence();
		sequences                  = new ArrayList<>();
	}
	
	public AbstractPolicyAgent(String id, Perimeter perimeter){
		this(id);
		this.perimeter = perimeter;
	}
	
	public AbstractPolicyAgent(String id, Perimeter perimeter, List<PolicyAction> actions) {
		this(id, perimeter);
		this.actions = actions;
	}
	
	public AbstractPolicyAgent(String id, Perimeter perimeter, List<PolicyAction> actions, List<PolicyAction> specialActions) {
		this(id, perimeter, actions);
		this.specialActions = specialActions;
	}
	
	public abstract void live();
	
	public List<InnerAgent> getInnerAgents() {
		return innerAgents;
	}
	
	public List<LocalLearner> getLocalLearners() {
		List<LocalLearner> localLeanerAgents = new ArrayList<LocalLearner>();

		for(InnerAgent a : this.innerAgents){
			if(a instanceof LocalLearner)
				localLeanerAgents.add((LocalLearner) a);
		}

		return localLeanerAgents;
	}
	
	public String getId() {
		return id;
	}
	
	public Perimeter getPerimeter() {
		return perimeter;
	}
	
	public List<PolicyAction> getActions() {
		return actions;
	}
	
	public List<PolicyAction> getSpecialActions() {
		return specialActions;
	}
	
	public ActionsSequence getSequence() {
		return sequence;
	}
	
	protected void clear() {
		actions.clear();
		specialActions.clear();
		perimeter = null;
		sequence = null;
		indicators.clear();
		labelsForGlobalPerceptions.clear();
		sasList.clear();
		innerAgents.clear();
	}
	
}
