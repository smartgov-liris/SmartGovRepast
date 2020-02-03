package policyagent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import environment.Structure;
import environment.city.EnvVar;
import policyagent.inneragent.DeepLocalLearner;
import policyagent.inneragent.InnerAgent;
import policyagent.inneragent.LocalLearner;
import policyagent.learning.strategy.NNBest;
import simulation.FilePath;
import simulation.GISComputation;
import simulation.manager.AbstractManager;
import simulation.parser.FilesManagement;

public class PolicyAgent extends AbstractPolicyAgent {

	private static final String SPLIT_STR    = "SPLIT";
	private static final String MERGE_STR    = "MERGE";
	private static final String ROLLBACK_STR = "ROLLBACK";
	private static final String KEEP_STR     = "KEEP";
	
	private PolicyAction lastAction            = PolicyAction.DO_NOTHING;
	private PolicyAction lastSpecialAction     = PolicyAction.NOTHING;
	private PolicyAction actionCurrentlyTested = PolicyAction.NOTHING;
	
	private double lastGain;
	private double highestGain;
	private double highestGainBeforeSplit;
	private double highestGainDuringSplit;
	private double highestSumGainBeforeMerge;
	private double highestGainBeforeMerge;
	
	private double epsilon;
	private double epsilonDecay;
	private double epsilonMin;
	
	private final String ACTION_FILE = "policyAgentActions.txt";
	private String LOCAL_ACTION_FILE;
	private String LOCAL_REWARD_FILE;
	private String LOCAL_VOTE_FILE;
	private String LOCAL_SEQUENCE_FILE;
	private String LOCAL_IDS_SEQUENCE_FILE;
	
	private boolean gainImproved = false;
	private boolean gainStatic   = false;
	private boolean gainBest     = false;
	
	private int positiveScoreUpdate =  1;
	private int negativeScoreUpdate = -1;
	
	private int iterationToBeConsideredHighestGain = 1;
	private int counterHigherGainReached           = 0;
	private int sizeOfPreviousGains                = 1;
	private String aggregateFunction = "average";
	private List<Double> previousHighestGains;
	private List<Double> previousIterationsGains;
	
	//Split & Merge
	private int bufferTrustAreaCounter;
	private int splitValidationCounter;
	private String policyAgentIDSplit;
	private String policyAgentBeforeSplit;
	private String policyAgentBeforeMerge;
	private boolean currentlyExperimenting;
	private boolean leaderExperimentation;
	private boolean sequenceAlreadyChecked;
	private int highestGainReachedCounter;
	private List<Integer> IDsForMerge;
	private Map<Integer, Integer> IDsPerIdenticalSequences;
	private Map<String, Perimeter> perimetersPerIDForMerge;
	private Map<String, List<InnerAgent>> innerAgentsPerIDForMerge;
	private Map<String, Double> higherGainPerIDForMerge;

	public PolicyAgent(String id, Perimeter perimeter, List<PolicyAction> actions) {
		super(id, perimeter, actions);
		
		initValues();
		
		String agentType = EnvVar.configFile.get("agent_type");
		String strategy = EnvVar.configFile.get("strategy");
		
		System.out.println(perimeter.getStructures().size());
		for(int indexOfStructure = 0; indexOfStructure < perimeter.getStructures().size(); indexOfStructure++) {
			System.out.println("Structure index: " + indexOfStructure);
			createLocalLearnerFor(perimeter.getStructures().get(indexOfStructure),
					agentType,
					strategy);
		}
		
		copyFiles();
	}
	
	public PolicyAgent(String id, Perimeter perimeter, List<PolicyAction> actions, List<PolicyAction> specialActions) {
		super(id, perimeter, actions, specialActions);
		initValues();
		
		String agentType = EnvVar.configFile.get("agent_type");
		String strategy = EnvVar.configFile.get("strategy");

		for(int indexOfStructure = 0; indexOfStructure < perimeter.getStructures().size(); indexOfStructure++) {
			createLocalLearnerFor(perimeter.getStructures().get(indexOfStructure),
					agentType,
					strategy);
		}
		
		copyFiles();
	}
	
	/**
	 * Should be use when validation is active.
	 * @param id
	 * @param perimeter
	 * @param actions
	 * @param specialActions
	 */
	public PolicyAgent(String id, Perimeter perimeter, List<PolicyAction> actions, List<PolicyAction> specialActions, int validation) {
		super(id, perimeter, actions, specialActions);
		initValues();
		
		String agentType = EnvVar.configFile.get("agent_type");
		String strategy = EnvVar.configFile.get("strategy");

		for(int indexOfStructure = 0; indexOfStructure < perimeter.getStructures().size(); indexOfStructure++) {
			createLocalLearnerFor(perimeter.getStructures().get(indexOfStructure),
					agentType,
					strategy);
		}
		
		copyFiles();
		epsilon = 0.0;
	}
	
	/**
	 * This constructor should be use when a policy agent is created after a merge rollback.
	 * @param id
	 * @param perimeter
	 * @param actions
	 * @param specialActions
	 * @param agents
	 * @param epsilon
	 */
	@SuppressWarnings("unchecked")
	public PolicyAgent(
			String id, 
			Perimeter perimeter, 
			List<PolicyAction> actions, 
			List<PolicyAction> specialActions,
			List<? extends InnerAgent> agents,
			double epsilon) {
		super(id, perimeter, actions, specialActions);
		initValues();
		
		this.epsilon = epsilon;
		
		innerAgents = (List<InnerAgent>) agents;
	}
	
	/**
	 * This constructor should be use when a policy agent is split.
	 * @param id
	 * @param perimeter
	 * @param actions
	 * @param agents
	 * @param epsilon
	 */
	@SuppressWarnings("unchecked")
	public PolicyAgent(
			String id, 
			Perimeter perimeter, 
			List<PolicyAction> actions, 
			List<PolicyAction> specialActions,
			List<? extends InnerAgent> agents,
			double epsilon,
			String linkedPolicyAgentID) {
		super(id, perimeter, actions, specialActions);
		initValues();
		
		this.epsilon = epsilon;
		
		innerAgents = (List<InnerAgent>) agents;
		
		policyAgentIDSplit = linkedPolicyAgentID;
		currentlyExperimenting = true;
	}
	
	private void initValues() {
		highestGain             = - Double.MAX_VALUE;
		lastGain                = - Double.MAX_VALUE;
		labelsForGlobalPerceptions.add("gain");
		LOCAL_ACTION_FILE       = "policyagent_" + id + "_actions.txt";
		LOCAL_REWARD_FILE       = "policyagent_" + id + "_global.txt";
		LOCAL_VOTE_FILE         = "policyagent_" + id + "_votes.txt";
		LOCAL_SEQUENCE_FILE     = "policyagent_" + id + "_sequences.txt";
		LOCAL_IDS_SEQUENCE_FILE = "policyagent_" + id + "_id_sequences.txt";
		bufferTrustAreaCounter  = 0;
		splitValidationCounter  = 0;
		currentlyExperimenting  = false;
		leaderExperimentation   = false;
		sequenceAlreadyChecked  = true;//false;
		policyAgentIDSplit      = "";
		highestSumGainBeforeMerge = 0;
		IDsForMerge             = new ArrayList<>();
		IDsPerIdenticalSequences = new HashMap<>();
		perimetersPerIDForMerge = new HashMap<>();
		innerAgentsPerIDForMerge = new HashMap<>();
		higherGainPerIDForMerge = new HashMap<>();
		policyAgentBeforeSplit  = "";
		policyAgentBeforeMerge  = "";
		positiveScoreUpdate     = Integer.parseInt(EnvVar.configFile.get("positive_trust_score"));
		negativeScoreUpdate     = Integer.parseInt(EnvVar.configFile.get("negative_trust_score"));
		iterationToBeConsideredHighestGain = Integer.parseInt(EnvVar.configFile.get("iteration_to_be_highest_gain"));
		sizeOfPreviousGains     = Integer.parseInt(EnvVar.configFile.get("number_of_previous_iterations"));
		aggregateFunction       = EnvVar.configFile.get("aggregate_function");
		previousHighestGains    = new ArrayList<>();
		previousIterationsGains = new ArrayList<>();
		parseConfigFile();
	}
	
	private void parseConfigFile() {
		epsilon = Double.parseDouble(EnvVar.configFile.get("epsilon"));
		epsilonDecay = Double.parseDouble(EnvVar.configFile.get("epsilon_decay"));
		epsilonMin = Double.parseDouble(EnvVar.configFile.get("epsilon_min"));
	}

	/**
	 * If the user specifies folders to copy results of the previous simulation, then read specified files to 
	 * set trust.
	 * Else write a new file.
	 */
	private void copyFiles() {
		if(!EnvVar.configFile.get("model_folder").equals("-") && Integer.valueOf(EnvVar.configFile.get("learning")) == 1) {
			setTrustPerLocalLearner();
		} else {
			FilesManagement.writeToFile(FilePath.currentLocalLearnerFolder, ACTION_FILE, "");
			FilesManagement.writeToFile(FilePath.currentLocalLearnerFolder, LOCAL_REWARD_FILE, "");
			FilesManagement.writeToFile(FilePath.currentLocalLearnerFolder, LOCAL_ACTION_FILE, "");
		}
	}

	/**
	 * Read "policyAgentActions.txt" file and set previous score to the correct localLearner.
	 */
	private void setTrustPerLocalLearner() {
		List<String> lines = FilesManagement.readFile(FilePath.currentLocalLearnerFolder, "policyAgentActions.txt");
		String lastLine = lines.get(lines.size() - 1);
		String[] agents = lastLine.split("\\)")[1].split(";");
		String[] agentsID = new String[agents.length];
		for(int i = 0; i < agents.length; i++) {
			agentsID[i] = agents[i].split(":")[0];
			for(LocalLearner localLearner : getLocalLearners()) {
				if(localLearner.getId().equals(agentsID[i])) {
					localLearner.setScore(Integer.valueOf(agents[i].split("_")[2]));
				}
			}
		}
	}

	@Override
	public void live() {
		//if(EnvVar.manager.getCurrentTrialIndex() >= 5 ) {
		if(EnvVar.manager.getCurrentTrialIndex() >= AbstractManager.NUMBER_OF_ITERATIONS_BEFORE_APPLYING_POLICIES) {
			updateLocalLearnerPerceptions();
			updateGain();
			updateActionsSequence();
			updateScoreOfLocalLearners();
			PolicyAction action = PolicyAction.NO_ACTION;
			PolicyAction specialAction = PolicyAction.NOTHING;
			if(EnvVar.manager.getCurrentlyExperimenting()) {
				if(currentlyExperimenting) {
					if(leaderExperimentation) {
						updateHighestGain();
						specialAction = updateControlGroup();
					}
					List<String> lines = new ArrayList<>();
					lines.add(EnvVar.manager.getCurrentIteration() + ") " + this.id + " is currently experimenting " + actionCurrentlyTested + " action.");
					FilesManagement.appendToFile(FilePath.currentLocalLearnerFolder, "Experimentation.txt", lines);
				}
			} else {
				if(epsilon <= Double.valueOf(EnvVar.configFile.get("epsilon_min"))) {
					specialAction = needSpecialAction();
				}
			}
			action = chooseActionToApplyToStructures();
			lastAction = action;
			lastSpecialAction = specialAction;
		}
	}
	
	private void updateActionsSequence() {
		if(EnvVar.manager.isRecentlyReset()) {
			sequenceAlreadyChecked = false;
			sequence = new ActionsSequence();
		} else if(!sequence.isHighestGainReached()) {
			sequence.addPolicyAction(lastAction);
			if(lastGain == highestGain) {
				sequence.setHighestGainReached(true);
			}
		}
		if(sequence.isHighestGainReached() && sequenceAlreadyChecked) {
			System.out.println(sequence);
			List<String> lines = new ArrayList<>();
			lines.add(EnvVar.manager.getCurrentIteration() + ")" + sequence + "; " + highestGain);
			FilesManagement.appendToFile(FilePath.currentLocalLearnerFolder, LOCAL_SEQUENCE_FILE, lines);
		}
	}

	/*/
	private void updateActionsSequence() {
		if(EnvVar.manager.isRecentlyReset()) {
			if(sequence.size() > 0) {
				sequences.add(sequence);
				if(sequences.size() > Integer.valueOf(EnvVar.configFile.get("identical_action_sequences"))) {
					sequences.remove(0);
				}
			}
			sequence = new ActionsSequence();
		} else if(!sequence.isHighestGainReached()) {
			sequence.addPolicyAction(lastAction);
			if(lastGain == highestGain) {
				sequence.setHighestGainReached(true);
			}
		}
		if(sequence.isHighestGainReached()) {
			System.out.println(sequence);
			List<String> lines = new ArrayList<>();
			lines.add(EnvVar.manager.getCurrentIteration() + ")" + sequence + "; " + highestGain);
			FilesManagement.appendToFile(FilePath.currentLocalLearnerFolder, LOCAL_SEQUENCE_FILE, lines);
		}
	}
	//*/
	
	private void updateLocalLearnerPerceptions() {
		for(LocalLearner localLearner : getLocalLearners()) {
			localLearner.setLastAction(lastAction);
			localLearner.setPerception();
		}
	}
	
	private void updateHighestGain() {
		if(actionCurrentlyTested == PolicyAction.SPLIT) {
			double cumulHighestGain = highestGain + EnvVar.policyAgents.get(Integer.valueOf(policyAgentIDSplit)).getHighestGain();
			if(highestGainDuringSplit < cumulHighestGain) {
				highestGainDuringSplit = cumulHighestGain;
			}
			double currentGain = lastGain + EnvVar.policyAgents.get(Integer.valueOf(policyAgentIDSplit)).getLastGain();
			if(currentGain > highestGainBeforeSplit) {
				highestGainReachedCounter++;
			}
		} else if(actionCurrentlyTested == PolicyAction.MERGE) {
			if(lastGain >= highestSumGainBeforeMerge) {
				highestGainReachedCounter++;
			}
		}
		
	}
	
	private PolicyAction updateSplit() {
		splitValidationCounter++;
		if(splitValidationCounter >= Integer.parseInt(EnvVar.configFile.get("iterations_to_check_split"))) {
			return PolicyAction.ROLLBACK;
		} else if(highestGainReachedCounter >= Integer.parseInt(EnvVar.configFile.get("highest_gain_reached"))) {
			return PolicyAction.KEEP;
		}
		return PolicyAction.NOTHING;
	}
	
	private PolicyAction updateMerge() {
		splitValidationCounter++;
		if(splitValidationCounter >= Integer.parseInt(EnvVar.configFile.get("iterations_to_check_merge"))) {
			return PolicyAction.ROLLBACK;
		} else if(highestGainReachedCounter >= Integer.parseInt(EnvVar.configFile.get("highest_gain_reached"))) {
			return PolicyAction.KEEP;
		}
		return PolicyAction.NOTHING;
	}
	
	private PolicyAction updateControlGroup() {
		PolicyAction action = PolicyAction.NOTHING;
		if(actionCurrentlyTested == PolicyAction.SPLIT) {
			action = updateSplit();
		} else if(actionCurrentlyTested == PolicyAction.MERGE) {
			action = updateMerge();
		}
		return action;
	}

	/**
	 * Actually stores the global reward over all structures
	 */
	private double setGlobalPerception() {
		Position globalReward = new Position();
		globalReward.addCoordinate(0.0);
		for(int indexOfStructure = 0; indexOfStructure < perimeter.getStructures().size(); indexOfStructure++) {
			Position position = new Position();
			List<Position> tempValues = new ArrayList<>();
			tempValues.add(perimeter.getStructures().get(indexOfStructure).getLocalPerformances(labelsForGlobalPerceptions));
			position = PolicyAgent.averagePosition(tempValues);
			globalReward.addPosition(position);
		}
		System.out.println("Global gain is: " + globalReward.getCoordinates().get(0));
		return globalReward.getCoordinates().get(0);
	}
	
	private void saveGlobalPerception(double currentReward) {
		String policyId = "policyagent_" + id + "_global";
		String iteration = String.valueOf(EnvVar.manager.getCurrentIteration()) + ")";
		String value = iteration + lastGain + "," + lastAction + "," + currentReward;
		FilesManagement.appendToFile(FilePath.currentLocalLearnerFolder, policyId+".txt", value);
	}

	public PolicyAction chooseActionToApplyToStructures() {
		//Remove epsilon-greedy strategy for policy agent
		Random rnd = new Random();
		PolicyAction action = PolicyAction.NO_ACTION;
		
		Map<PolicyAction, Integer> nbPerActions = new HashMap<>();
		Map<PolicyAction, Integer> averageTrust = new HashMap<>();
		Map<PolicyAction, List<String>> idsPerAction = new HashMap<>();
		for(LocalLearner localLearner : getLocalLearners()) {
			PolicyAction localAction = localLearner.proposeAction();
			
			if(!nbPerActions.containsKey(localAction)) {
				nbPerActions.put(localAction, 1);
				averageTrust.put(localAction, localLearner.getScore());
				List<String> ids = new ArrayList<>();
				ids.add(localLearner.getId());
				idsPerAction.put(localAction, ids);
			} else {
				int currentValue = nbPerActions.get(localAction);
				nbPerActions.put(localAction, currentValue+1);
				averageTrust.put(localAction, (averageTrust.get(localAction) + localLearner.getScore())/2);
				idsPerAction.get(localAction).add(localLearner.getId());
			}
		}
		int maxNumber = -1;
		boolean activeConcensus = false;
		List<String> lines = new ArrayList<>();
		lines.add("------[Iteration: " + EnvVar.manager.getCurrentIteration() + "]------");
		lines.add("Total voters: " + getLocalLearners().size() + ".");
		for(Entry<PolicyAction, Integer> entry : nbPerActions.entrySet()) {
			System.out.println("Action: " + entry.getKey() + ", Voters: " + entry.getValue() + ", Score: " + averageTrust.get(entry.getKey()));
			String ids = "";
			for(String id : idsPerAction.get(entry.getKey())) {
				ids += id + ",";
			}
			lines.add("Action: " + entry.getKey() + ", Voters: " + entry.getValue() + ", Score: " + averageTrust.get(entry.getKey()) + ", ids: " + ids);
			if(maxNumber < entry.getValue()) {
				maxNumber = entry.getValue();
				if(activeConcensus) {
					activeConcensus = false;
				}
				action = entry.getKey();
			} else if(maxNumber == entry.getValue()) {
				activeConcensus = true;
			}
		}
		
		if(rnd.nextDouble() < epsilon) { //Force exploration
			action = actions.get(rnd.nextInt(actions.size()));
			System.out.println("Apply action " + action + " randomly choose by policy agent.");
			lines.add("Apply action " + action + " randomly choose by policy agent.");
		} else {
			if(activeConcensus) {
				action = consensus(EnvVar.configFile.get("consensus"), nbPerActions, averageTrust, maxNumber);
				lines.add("Apply action " + action + " based on random consensus between two or more actions.");
			} else {
				System.out.println("Apply action " + action + " proposed by the majority of agents.");
				lines.add("Apply action " + action + " proposed by the majority of agents.");
			}
		}
		FilesManagement.appendToFile(FilePath.currentLocalLearnerFolder, LOCAL_VOTE_FILE, lines);
		decayEpsilon();
		
		return action;
	}
	
	/**
	 * Decay for policy agent exploration
	 */
	private void decayEpsilon() {
		if(epsilon > epsilonMin) {
			epsilon *= epsilonDecay;
		}
	}
	
	private void updateGain() {
		double currentGain = setGlobalPerception();
		updatePreviousIterationsGains(currentGain);
		if(lastGain != - Double.MAX_VALUE) {
			gainBest = false;
			gainImproved = false;
			gainStatic = false;
			
			if(currentGain >= highestGain && iterationToBeConsideredHighestGain == 1) {
				sasList.clear();
				
				highestGain = currentGain;
				gainBest = true;
				sasList.put(lastAction, new StateActionState(lastGain, lastAction, currentGain));
			} else if(currentGain >= highestGain && iterationToBeConsideredHighestGain > 1) {
				previousHighestGains.add(currentGain);
				counterHigherGainReached++;
				gainBest = true;
				sasList.put(lastAction, new StateActionState(lastGain, lastAction, currentGain));
				if(counterHigherGainReached == iterationToBeConsideredHighestGain) {
					highestGain = Math.max(highestGain, computeHighestGain(previousHighestGains));
					previousHighestGains.clear();
					counterHigherGainReached = 0;
				}
			} else if(currentGain > aggregateLastGains()) {
				gainImproved = true;
			} else if(currentGain == aggregateLastGains() && currentGain < highestGain) {
				gainStatic = true;
			}
			saveGlobalPerception(currentGain);
		}
		lastGain = currentGain;
	}
	
	private void updatePreviousIterationsGains(double currentGain) {
		if(EnvVar.manager.isRecentlyReset()) {
			previousIterationsGains.clear();
		}
		previousIterationsGains.add(currentGain);
		if(previousIterationsGains.size() > sizeOfPreviousGains) {
			previousIterationsGains.remove(0);
		}
	}
	
	private double aggregateLastGains() {
		double aggregateGain = 0.0;
		if(aggregateFunction.equals("average")) {
			for(int i = 0; i < previousIterationsGains.size(); i++) {
				aggregateGain += previousIterationsGains.get(i);
			}
			aggregateGain /= previousIterationsGains.size();
		} else if(aggregateFunction.equals("min")) {
			aggregateGain = Double.MAX_VALUE;
			for(int i = 0; i < previousIterationsGains.size(); i++) {
				if(previousIterationsGains.get(i) < aggregateGain) {
					aggregateGain = previousIterationsGains.get(i);
				}
			}
		} else if(aggregateFunction.equals("max")) {
			aggregateGain = - Double.MAX_VALUE;
			for(int i = 0; i < previousIterationsGains.size(); i++) {
				if(previousIterationsGains.get(i) > aggregateGain) {
					aggregateGain = previousIterationsGains.get(i);
				}
			}
		}
		return aggregateGain;
	}

	private double computeHighestGain(List<Double> gains) {
		//*/ Highest number of current saved gains and minimum of the values of highest numbers in case of equality
		Map<Double, Integer> counterPerGain = new HashMap<>();
		int maxNumberOfSavedGains = Integer.MIN_VALUE;
		List<Double> values = new ArrayList<>();
		for(int i = 0; i < gains.size(); i++) {
			if(counterPerGain.containsKey(gains.get(i))) {
				counterPerGain.put(gains.get(i), counterPerGain.get(gains.get(i)) + 1);
			} else {
				counterPerGain.put(gains.get(i), 1);
			}
		}
		for(Entry<Double, Integer> gain : counterPerGain.entrySet()) {
			if(gain.getValue() > maxNumberOfSavedGains) {
				maxNumberOfSavedGains = gain.getValue();
				values.clear();
				values.add(gain.getKey());
			} else if(gain.getValue() == maxNumberOfSavedGains){
				values.add(gain.getKey());
			}
		}
		return GISComputation.min(values);
		//*/
		
		/*/ Min gain
		return GISComputation.min(gains);
		//*/
	}

	public void applyActionToStructure(PolicyAction policyAction) {
		for(Structure structure : perimeter.getStructures()) {
			if(structure instanceof ActionableByPolicyAgent) {
				((ActionableByPolicyAgent) structure).doPolicyAction(policyAction);
			}
		}
	}

	public void applyRandomActionToStructures() {
		Random rnd = new Random();
		applyActionToStructure(actions.get(rnd.nextInt(actions.size())));
	}

	public String getIndexOfBestAction(Map<String, Integer> numberOfPerceptionForEachAction) {
		int max = -1;
		String bestAction = "";
		for(Entry<String, Integer> action : numberOfPerceptionForEachAction.entrySet()) {
			action.getValue();
			if(action.getValue() > max) {
				max = action.getValue();
				bestAction = action.getKey();
			}
		}
		return bestAction;
	}

	private void createLocalLearnerFor(Structure structure,
			String agentType,
			String strategy) {
		//*/
		List<List<String>> allLabels = new ArrayList<>();
		
		List<String> labels1 = new ArrayList<>();
		labels1.add("utility");
		labels1.add("satisfaction");
		
		List<String> labels2 = new ArrayList<>();
		labels2.add("distance");
		labels2.add("timesearching");
		
		List<String> labels3 = new ArrayList<>();
		labels3.add("satisfaction");
		labels3.add("distance");
		
		List<String> labels4 = new ArrayList<>();
		labels4.add("timesearching");
		labels4.add("satisfaction");
		
		List<String> labels5 = new ArrayList<>();
		labels5.add("occupation");
		labels5.add("satisfaction");

		List<String> labels6 = new ArrayList<>();
		labels6.add("utility");
		labels6.add("timesearching");
		
		List<String> labels7 = new ArrayList<>();
		labels7.add("utility");
		labels7.add("satisfaction");
		labels7.add("distance");
		labels7.add("timesearching");
		labels7.add("occupation");
		
		List<String> labels8 = new ArrayList<>();
		labels8.add("price");
		labels8.add("numberOfPlaces");
		labels8.add("reward");
		
		List<String> labels9 = new ArrayList<>();
		labels9.add("utility");
		labels9.add("satisfaction");
		labels9.add("distance");
		labels9.add("timesearching");
		labels9.add("occupation");
		labels9.add("price");
		labels9.add("numberOfPlaces");
		labels9.add("reward");
		
		List<String> labels10 = new ArrayList<>();
		labels10.add("reward");
		
		//allLabels.add(labels1);
		//allLabels.add(labels2);
		//allLabels.add(labels3);
		//allLabels.add(labels4);
		//allLabels.add(labels5);
		//allLabels.add(labels6);
		//allLabels.add(labels7);
		//allLabels.add(labels8);
		allLabels.add(labels9);
		
		List<PolicyAction> policyActions = new ArrayList<>();
		for(int i = 0; i < actions.size(); i++) {
			policyActions.add(actions.get(i));
		}
		
		for(int i = 0; i < allLabels.size(); i++) {
			if(agentType.equals("normal")) {
				innerAgents.add(new LocalLearner(structure, structure.getID(), allLabels.get(i), strategy, actions.size(), policyActions));
			} else if(agentType.equals("deep")) {
				DeepLocalLearner deepLL = new DeepLocalLearner(structure, structure.getID(), allLabels.get(i), strategy, actions.size(), policyActions);
				innerAgents.add(deepLL);
				/*/
				deepLL.getPredictionForAllStates();
				for(int j = 0; j < 10; j++) {
					deepLL.training();
				}
				deepLL.getPredictionForAllStates();
				//*/
				//innerAgents.add(new DeepLocalLearner(structure, structure.getID(), labels9, strategy, learningMethod, actions.size(), parkingMDP));
			}
		}
		for(InnerAgent innerAgent : innerAgents) {
			EnvVar.innerAgentsGlobal.add(innerAgent);
		}
		//*/
		/*/
		List<String> labels9 = new ArrayList<>();

		labels9.add("utility");
		labels9.add("satisfaction");
		labels9.add("distance");
		labels9.add("timesearching");
		labels9.add("occupation");
		labels9.add("price");
		labels9.add("numberOfPlaces");
		labels9.add("reward");
		
		ParkingMDP parkingMDP = new ParkingMDP(0, actions.size(), 0.9, (BlockFace) structure, "prices");
		if(agentType.equals("normal")) {
			innerAgents.add(new LocalLearner(structure, structure.getID(), labels9, strategy, learningMethod, actions.size(), parkingMDP));
		} else if(agentType.equals("deep")) {
			DeepLocalLearner deepLL = new DeepLocalLearner(structure, structure.getID(), labels9, strategy, learningMethod, actions.size(), parkingMDP);
			innerAgents.add(deepLL);
			//deepLL.getPredictionForAllStates();
			//innerAgents.add(new DeepLocalLearner(structure, structure.getID(), labels9, strategy, learningMethod, actions.size(), parkingMDP));
		}
		//*/
	}

	public void updateLocalLearners(PolicyAction action) {
		for(LocalLearner localLearner : getLocalLearners()) {
			localLearner.setLastAction(action);
		}
	}
	
	/**
	 * Update only when local learner is not exploring (not a random action)
	 */
	private void updateScoreOfLocalLearners() {
		for(LocalLearner localLearner : getLocalLearners()) {
			if(!localLearner.getExplorationMethod().isStillExploration()) {
				if(gainBest) {
					boolean updated = false;
					List<PolicyAction> similarActions = getSimilarActions(lastAction);
					for(int i = 0; i < similarActions.size(); i++) {
						if(similarActions.get(i) == localLearner.getExplorationMethod().getLastPredictedAction()) {
							localLearner.updateScore(positiveScoreUpdate);
							updated = true;
							break;
						}
					}
					if(!updated) {
						localLearner.updateScore(negativeScoreUpdate);
					}
				} else if(gainImproved) {
					if(localLearner.getExplorationMethod().getLastPredictedAction() == lastAction) {
						localLearner.updateScore(positiveScoreUpdate);
					} else {
						localLearner.updateScore(negativeScoreUpdate);
					}
				} else {
					if(localLearner.getExplorationMethod().getLastPredictedAction() == lastAction) {
						localLearner.updateScore(negativeScoreUpdate);
					}
				}
			}
		}
		/*/
		if(gainBest) {
			for(LocalLearner localLearner : getLocalLearners()) {
				boolean updated = false;
				if(!localLearner.getExplorationMethod().isStillExploration()) {
					List<PolicyAction> similarActions = getSimilarActions(lastAction);
					for(int i = 0; i < similarActions.size(); i++) {
						if(similarActions.get(i) == localLearner.getExplorationMethod().getLastPredictedAction()) {
							localLearner.updateScore(positiveScoreUpdate);
							updated = true;
							break;
						}
					}
					if(!updated) {
						localLearner.updateScore(negativeScoreUpdate);
					}
				}
			}
		} else if(gainImproved) {
			for(LocalLearner localLearner : getLocalLearners()) {
				if(!localLearner.getExplorationMethod().isStillExploration()) {
					if(localLearner.getExplorationMethod().getLastPredictedAction() == lastAction) {
						localLearner.updateScore(positiveScoreUpdate);
					} else {
						localLearner.updateScore(negativeScoreUpdate);
					}
				}
			}
		} else {
			for(LocalLearner localLearner : getLocalLearners()) {
				if(!localLearner.getExplorationMethod().isStillExploration() && localLearner.getExplorationMethod().getLastPredictedAction() == lastAction) {
					localLearner.updateScore(negativeScoreUpdate);
				}
			}
		}
		//*/
	}
	
	public String applyPolicyAction(PolicyAction action) {
		if(action == PolicyAction.SPLIT) {
			return splitControlGroup();
		} else if(action == PolicyAction.MERGE) {
			return mergeControlGroup(IDsForMerge);
		} else if(action == PolicyAction.KEEP) {
			return keepControlGroup();
		} else if(action == PolicyAction.ROLLBACK) {
			return rollbackControlGroup();
		} else {
			applyActionToStructure(action);
			fetchAnswersFromAgents();
			return id + ":" + action + ",";
		}
	}
	
	public PolicyAction askLocalLearners() {
		PolicyAction action = chooseActionToApplyToStructures();
		applyActionToStructure(action);
		fetchAnswersFromAgents();
		return action;
	}
	
	public void askAndUpdateLocalLearners() {
		updateLocalLearners(askLocalLearners());
	}

	public static Position averagePosition(List<Position> positions) {
		Position averagePosition = new Position();
		
		for(int indexOfDimension = 0; indexOfDimension < positions.get(0).getCoordinates().size(); indexOfDimension++) {
			double averageForDimension = 0.0;
			for(int indexOfPosition = 0; indexOfPosition < positions.size(); indexOfPosition++) {
				averageForDimension += positions.get(indexOfPosition).getCoordinates().get(indexOfDimension);
			}
			averagePosition.addCoordinate(averageForDimension/positions.size());
		}
		return averagePosition;
	}
	
	public void fetchAnswersFromAgents() {
		String allAnswersForOneIteration = String.valueOf(EnvVar.manager.getCurrentIteration()) + ")";
		for(LocalLearner localLearner : getLocalLearners()) {
			if(localLearner instanceof DeepLocalLearner) {
				String answer = ((DeepLocalLearner) localLearner).getExplorationMethod().getLastAnswer();
				String[] splits = answer.split("_");
				allAnswersForOneIteration += splits[0]+":"+splits[1]+"_"+splits[2]+"_"+localLearner.getScore()+";";
			}
		}
		FilesManagement.appendToFile(FilePath.currentLocalLearnerFolder, ACTION_FILE, allAnswersForOneIteration);
		FilesManagement.appendToFile(FilePath.currentLocalLearnerFolder, LOCAL_ACTION_FILE, allAnswersForOneIteration);
	}
	
	/**
	 * The policy agent uses this function to split his control group (lower levels policy agents)
	 * and create a new policy agent of the same level with one of the split control group.
	 */
	public String splitControlGroup() {
		List<List<LocalLearner>> groupsIDs = groupIdentificationBasedOnTrust();
		resetTrust();
		Perimeter splitPerimeter = createPerimeterWithAgents(groupsIDs.get(0));
		policyAgentIDSplit = EnvVar.requestPolicyAgentID();
		
		if(Integer.parseInt(policyAgentIDSplit) < EnvVar.policyAgents.size()) {
			EnvVar.policyAgents.set(Integer.parseInt(policyAgentIDSplit), 
					new PolicyAgent(
							policyAgentIDSplit, 
							splitPerimeter, 
							actions, 
							specialActions,
							groupsIDs.get(0),
							epsilon,
							id)
					);
		} else {
			//Need to be sure that the ID of the newly created PolicyAgent is equal to its position in the list.
			EnvVar.policyAgents.add(new PolicyAgent(
					policyAgentIDSplit, 
					splitPerimeter, 
					actions, 
					specialActions,
					groupsIDs.get(0),
					epsilon,
					id)
					);
		}
		removeSplitAgents(groupsIDs.get(0));
		highestGainBeforeSplit = highestGain;
		highestGain = 0.0;
		previousHighestGains.clear();
		splitValidationCounter = 0;
		bufferTrustAreaCounter = 0;
		highestGainReachedCounter = 0;
		EnvVar.manager.setCurrentlyExperimenting(true);
		currentlyExperimenting = true;
		leaderExperimentation = true;
		actionCurrentlyTested = PolicyAction.SPLIT;
		return SPLIT_STR + "[" + id + "->" + id + "," + policyAgentIDSplit + "],";
	}
	
	public String mergeControlGroup(List<Integer> IDs) {
		resetTrust();
		String str = "[" + id + ",";
		highestSumGainBeforeMerge = highestGain;
		highestGainBeforeMerge = highestGain;
		for(int i = 0; i < IDs.size(); i++) {
			PolicyAgent policyAgent = EnvVar.policyAgents.get(IDs.get(i));
			policyAgent.resetTrust();
			perimetersPerIDForMerge.put(policyAgent.getId(), new Perimeter(policyAgent.getPerimeter()));
			innerAgentsPerIDForMerge.put(policyAgent.getId(), policyAgent.getInnerAgents());
			higherGainPerIDForMerge.put(policyAgent.getId(), policyAgent.getHighestGain());
			perimeter.mergePerimeters(policyAgent.getPerimeter());
			innerAgents.addAll(policyAgent.getInnerAgents());
			highestSumGainBeforeMerge += policyAgent.getHighestGain();
			if(i == IDs.size() - 1) {
				str += policyAgent.getId();
			} else {
				str += policyAgent.getId() + ",";
			}
			EnvVar.storePolicyAgentIDForMerge(id, IDs.get(i));
			//EnvVar.updatePolicyAgentsList(IDs.get(i)); //Remove agent from plot 
			policyAgentBeforeMerge += policyAgent.getId() + ",";
			//EnvVar.updatePolicyAgentBuffer(String.valueOf(IDs.get(i)));
		}
		highestGain = 0.0;
		EnvVar.manager.setCurrentlyExperimenting(true);
		currentlyExperimenting = true;
		leaderExperimentation = true;
		actionCurrentlyTested = PolicyAction.MERGE;
		IDsPerIdenticalSequences.clear();
		IDsForMerge.clear();
		return MERGE_STR + str + "->" + id + "],";
	}
	
	public String rollbackControlGroup() {
		if(actionCurrentlyTested == PolicyAction.SPLIT) {
			return rollbackFromSplit();
		} else if(actionCurrentlyTested == PolicyAction.MERGE) {
			return rollbackFromMerge();
		}
		return ""; //Should not happen
	}
	
	public String rollbackFromSplit() {
		highestGain = highestGainBeforeSplit;
		String str = "[" + id + "," + policyAgentIDSplit + "->" + id + "],";
		int policyAgentIndex = Integer.valueOf(policyAgentIDSplit);
		PolicyAgent policyAgent = EnvVar.policyAgents.get(policyAgentIndex);
		perimeter.mergePerimeters(policyAgent.getPerimeter());
		innerAgents.addAll(policyAgent.getInnerAgents());
		EnvVar.removePolicyAgentFromList(policyAgentIndex);
		EnvVar.manager.setCurrentlyExperimenting(false);
		resetTrust();
		resetSpecialActionTrial();
		return ROLLBACK_STR + str;
	}
	
	public String rollbackFromMerge() {
		//Should correct the policyAgentIDSplit == ""
		String str = "[" + id + "->";
		for(String id : policyAgentBeforeMerge.split(",")) {
			str += id + ",";
			/*/ Wrong constructor
			EnvVar.policyAgents.set(Integer.valueOf(id),
					new PolicyAgent(
					id, 
					perimetersPerIDForMerge.get(id), 
					actions, 
					specialActions,
					innerAgentsPerIDForMerge.get(id),
					epsilon,
					id)
					);
			//*/
			EnvVar.policyAgents.set(Integer.valueOf(id),
					new PolicyAgent(
					id, 
					perimetersPerIDForMerge.get(id), 
					actions, 
					specialActions,
					innerAgentsPerIDForMerge.get(id),
					epsilon)
					);
			EnvVar.updatePolicyAgentBuffer(id);
			EnvVar.policyAgents.get(Integer.valueOf(id)).setHighestGain(higherGainPerIDForMerge.get(id));
			innerAgents.removeAll(innerAgentsPerIDForMerge.get(id));
			perimeter.removeStructures(perimetersPerIDForMerge.get(id).getStructures());
		}
		//String str = "[" + id + "->" + policyAgentIDSplit + "," + id + "],";
		str += id + "],";
		EnvVar.clearMergeAgents(id);
		EnvVar.manager.setCurrentlyExperimenting(false);
		//TODO not correct, create several agents
		resetTrust();
		
		//PolicyAgent policyAgent = EnvVar.policyAgents.get(Integer.valueOf(policyAgentIDSplit));
		//perimeter.mergePerimeters(policyAgent.getPerimeter());
		highestGain = highestGainBeforeMerge;//0.0;
		resetSpecialActionTrial();
		return ROLLBACK_STR + str;
	}
	
	public String keepControlGroup() {
		String str = "[" + id;
		if(actionCurrentlyTested == PolicyAction.SPLIT) {
			if(!policyAgentIDSplit.equals("")) {
				str += "," + policyAgentIDSplit;
				EnvVar.policyAgents.get(Integer.valueOf(policyAgentIDSplit)).resetSpecialActionTrial(); //Remove experimenting tag
			}
		} else if(actionCurrentlyTested == PolicyAction.MERGE) {
			EnvVar.clearMergeAgents(id);
			for(String id : policyAgentBeforeSplit.split(",")) {
				str += "," + id;
			}
		}
		resetSpecialActionTrial();
		EnvVar.manager.setCurrentlyExperimenting(false);
		return KEEP_STR + str + "],";
		/*
		String str = "[" + id;
		if(!policyAgentIDSplit.equals("")) {
			str += "," + policyAgentIDSplit;
		}
		resetSpecialActionTrial();
		return KEEP_STR + str + "],";
		*/
	}
	
	private void resetSpecialActionTrial() {
		actionCurrentlyTested     = PolicyAction.NOTHING;
		currentlyExperimenting    = false;
		leaderExperimentation     = false;
		splitValidationCounter    = 0;
		bufferTrustAreaCounter    = 0;
		highestGainReachedCounter = 0;
		policyAgentIDSplit        = "";
		highestGainBeforeSplit    = 0;
		highestGainDuringSplit    = 0;
		highestSumGainBeforeMerge = 0;
		policyAgentBeforeSplit    = "";
		policyAgentBeforeMerge    = "";
		highestGainBeforeMerge    = 0.0;
		perimetersPerIDForMerge   = new HashMap<>();
		innerAgentsPerIDForMerge  = new HashMap<>();
		higherGainPerIDForMerge   = new HashMap<>();
		
	}
	
	/**
	 * Subtract from current policy agent the inner agents and structures attach to them.
	 * @param agents that will be removed from the current policy agent.
	 */
	private void removeSplitAgents(List<LocalLearner> agents) {
		List<InnerAgent> agentsToBeRemoved = new ArrayList<>();
		List<Structure> structuresToBeRemoved = new ArrayList<>();
		for(LocalLearner localLearner : agents) {
			for(InnerAgent innerAgent : innerAgents) {
				if(localLearner == innerAgent) {
					agentsToBeRemoved.add(innerAgent);
					for(Structure structure : localLearner.getStructures()) {
						structuresToBeRemoved.add(structure);
					}
				}
			}
		}
		innerAgents.removeAll(agentsToBeRemoved);
		perimeter.removeStructures(structuresToBeRemoved);
		
	}
	
	private Perimeter createPerimeterWithAgents(List<LocalLearner> agents) {
		List<Structure> structures = new ArrayList<>();
		for(LocalLearner localLearner : agents) {
			for(Structure structure : localLearner.getStructures()) {
				structures.add(structure);
			}
		}
		return new Perimeter(structures);
	}
	
	private List<List<LocalLearner>> groupIdentificationBasedOnTrust() {
		List<List<LocalLearner>> groupsIDs = new ArrayList<>();
		//Split in two groups, therefore loop for two steps.
		for(int i = 0; i < 2; i++) {
			groupsIDs.add(new ArrayList<LocalLearner>());
		}
		for(LocalLearner localLearner : getLocalLearners()) {
			double score = localLearner.getScore();
			if(score >= Integer.parseInt(EnvVar.configFile.get("buffer_trust_area_max"))) {
				groupsIDs.get(0).add(localLearner);
			} else {
				//score <= Integer.parseInt(EnvVar.configFile.get("buffer_trust_area_min"))
				groupsIDs.get(1).add(localLearner);
			}
		}
		return groupsIDs;
	}
	
	private PolicyAction consensus(String type, 
			Map<PolicyAction, Integer> nbPerActions, 
			Map<PolicyAction, Integer> averageTrust,
			int maxNumber) {
		List<PolicyAction> actions = new ArrayList<>();
		int counter = 0;
		String actionsId = "";
		double maxTrust = -1.0;
		PolicyAction maxTrustGroupId = PolicyAction.NO_ACTION;
		for(Entry<PolicyAction, Integer> entry : nbPerActions.entrySet()) {
			if(maxNumber == entry.getValue()) {
				actions.add(entry.getKey());
				counter++;
				actionsId += entry.getKey() + " ";
				if(maxTrust < averageTrust.get(entry.getKey())) {
					maxTrust = averageTrust.get(entry.getKey());
					maxTrustGroupId = entry.getKey();
				}
			}
		}
		if(type.equals("trust")) {
			if(maxTrust > 0.0){
				System.out.println("Apply action " + maxTrustGroupId + " based on trust consensus between actions: " + actionsId + ".");
				return maxTrustGroupId;
			}
		}
		Random rnd = new Random();
		PolicyAction action = actions.get(rnd.nextInt(counter));
		System.out.println("Apply action " + action + " based on random consensus between actions: " + actionsId + ".");
		return action;
	}
	
	@SuppressWarnings("unused")
	private void modifyRewardOfLocalLearners(double currentGain) {
		if(gainStatic) {
			for(LocalLearner localLearner : getLocalLearners()) {
				if(localLearner.getExplorationMethod() instanceof NNBest) {
					int size = localLearner.getCurrentPerception().getPosition().getCoordinates().size();
					localLearner.getCurrentPerception().getPosition().getCoordinates().set(size - 1, -1.0);
					localLearner.setLastReward(-1.0);
					((NNBest) localLearner.getExplorationMethod()).setLastPerception(localLearner.getCurrentPerception().getPosition());
				}
			}
		}
	}
	
	private List<PolicyAction> getSimilarActions(PolicyAction action) {
		List<PolicyAction> similarActions = new ArrayList<>();
		StateActionState sas = sasList.get(action);
		for(Entry<PolicyAction, StateActionState> entryToCompare : sasList.entrySet()) {
			if(sas.compareTo(entryToCompare.getValue())) {
				if(!similarActions.contains(entryToCompare.getKey())) {
					similarActions.add(entryToCompare.getKey());
				}
			}
		}
		return similarActions;
	}
	
	private boolean isScoreInBufferArea() {
		for(LocalLearner localLearner : getLocalLearners()) {
			int score = localLearner.getScore();
			if(score < Integer.valueOf(EnvVar.configFile.get("buffer_trust_area_max")) 
					&& score > Integer.valueOf(EnvVar.configFile.get("buffer_trust_area_min"))) {
				return false;
			}
		}
		return true;
	}
	
	private boolean isTrustPositiveAndNegative() {
		boolean positive = false;
		boolean negative = false;
		for(LocalLearner localLearner : getLocalLearners()) {
			if(localLearner.getScore() > 0) {
				positive = true;
			}
			if(localLearner.getScore() < 0) {
				negative = true;
			}
			if(negative && positive) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Used to check if policy agent needs to split or merge.
	 */
	private PolicyAction needSpecialAction() {
		PolicyAction action = PolicyAction.NOTHING;
		if(specialActions.contains(PolicyAction.SPLIT)) {
			action = trackBufferTrustArea();
		}
		if(specialActions.contains(PolicyAction.MERGE) && action == PolicyAction.NOTHING) {
			action = trackActionSequence();
		}
		return action;
	}
	
	/**
	 * Used to check if policy agent needs to split.
	 * @return SPLIT or NOTHING
	 */
	private PolicyAction trackBufferTrustArea() {
		if(isScoreInBufferArea()) {
			if(isTrustPositiveAndNegative()) {
				bufferTrustAreaCounter++;
			}

			if(bufferTrustAreaCounter >= Integer.parseInt(EnvVar.configFile.get("trust_stability"))) {
				return PolicyAction.SPLIT;
			}
		} else {
			bufferTrustAreaCounter = 0;
		}
		return PolicyAction.NOTHING;
	}
	
	/**
	 * Compare action sequences to find suitable policy agents to merge with
	 * @return Merge if the action sequences are similar, nothing else.
	 */
	private PolicyAction trackActionSequence() {
		
		//*/
		boolean merge = false;
		if(sequence.isHighestGainReached() && !sequenceAlreadyChecked) {
			sequenceAlreadyChecked = true;
			updateIDsForMerge(compareActionSequences());
			for(Entry<Integer, Integer> IdenticalSequencePerID : IDsPerIdenticalSequences.entrySet()) {
				if(IdenticalSequencePerID.getValue() >= Integer.parseInt(EnvVar.configFile.get("identical_action_sequences"))) {
					IDsForMerge.add(IdenticalSequencePerID.getKey());
					merge = true;
				}
			}
		}
		if(merge) {
			return PolicyAction.MERGE;
		}
		//*/
		
		//TODO Select other policy agents and set mutex to true to them
		/*/
		IDsForMerge = compareActionSequences();
		if(!IDsForMerge.isEmpty()) {
			identicalActionSequences++;
		}
		if(identicalActionSequences >= Integer.parseInt(EnvVar.configFile.get("identical_action_sequences"))) {
			return PolicyAction.MERGE;
		}
		//*/
		return PolicyAction.NOTHING;
	}
	
	private void updateIDsForMerge(List<Integer> newIDsForMerge) {
		if(!newIDsForMerge.isEmpty()) {
			String str = "";
			for(int i = 0; i < newIDsForMerge.size(); i++) {
				if(IDsPerIdenticalSequences.containsKey(newIDsForMerge.get(i))) {
					IDsPerIdenticalSequences.put(newIDsForMerge.get(i), IDsPerIdenticalSequences.get(newIDsForMerge.get(i)) + 1);
				} else {
					IDsPerIdenticalSequences.put(newIDsForMerge.get(i), 1);
				}
				str += newIDsForMerge.get(i) + ": " + IDsPerIdenticalSequences.get(newIDsForMerge.get(i)) + "; ";
			}

			List<String> lines = new ArrayList<>();
			lines.add(EnvVar.manager.getCurrentIteration() + ") " + str);
			FilesManagement.appendToFile(FilePath.currentLocalLearnerFolder, LOCAL_IDS_SEQUENCE_FILE, lines);
		}
	}
	
	private List<Integer> compareActionSequences() {
		//TODO new control agents are not considered in the observation of similar action sequences.
		List<Integer> IDs = new ArrayList<>();
		//boolean similarSequences = false;
		for(PolicyAgent policyAgent : EnvVar.policyAgents) {
			if(policyAgent != null) {
				if(!policyAgent.getId().equals(id)) {
					if(!policyAgent.isCurrentlyExperimenting()) {
						String str = "";
						if(policyAgent.getSequence().isHighestGainReached()) {
							
						//if(!policyAgent.getSequences().isEmpty()) {
							str = EnvVar.manager.getCurrentIteration() + ") Compare sequences of agent " + this.id + " with agent " + policyAgent.getId() + ": (size: " + sequence.size() + ") " + sequence + " vs (size: "
									+ policyAgent.getSequence().size() + ") " + policyAgent.getSequence() + ".";
							//for(int indexOfSequence = 0; indexOfSequence < policyAgent.getSequences().size(); indexOfSequence++) {
								if(policyAgent.getSequence().compareTo(sequence)) {
								//if(!policyAgent.getSequences().get(indexOfSequence).compareTo(sequences.get(indexOfSequence))) {
									//similarSequences = true;
									IDs.add(Integer.parseInt(policyAgent.getId()));
									str += " Similar sequences.";
									//break;
								}
							//}
						//} else {
						//	similarSequences = false;
						//}
						
						//if(similarSequences) {
						//	IDs.add(Integer.parseInt(policyAgent.getId()));
						//	str += " Similar sequences.";
							//policyAgent.setMutex(true);
						//}
							List<String> lines = new ArrayList<>();
							lines.add(str);
							FilesManagement.appendToFile(FilePath.currentLocalLearnerFolder, "Sequence_global_comparison.txt", lines);
						}
					}
				}
			}
		}
		return IDs;
	}
	
	public double getHighestGain() {
		return highestGain;
	}
	
	public void resetTrust() {
		for(LocalLearner localLearner : getLocalLearners()) {
			localLearner.setScore(0);
		}
	}
	
	public PolicyAction getLastAction() {
		return lastAction;
	}
	
	public PolicyAction getLastSpecialAction() {
		return lastSpecialAction;
	}
	
	public double getLastGain() {
		return lastGain;
	}
	
	protected void clear() {
		super.clear();
		
	}
	
	public void setHighestGain(double highestGain) {
		this.highestGain = highestGain;
	}
	
	public boolean isCurrentlyExperimenting() {
		return currentlyExperimenting;
	}

}
