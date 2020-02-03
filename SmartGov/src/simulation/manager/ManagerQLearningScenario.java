package simulation.manager;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Map.Entry;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import environment.Structure;
import environment.city.EnvVar;
import environment.city.parking.BlockFace;
import microagent.MicroAgent;
import microagent.properties.ParkProperties;
import policyagent.PolicyAction;
import policyagent.PolicyAgent;
import policyagent.Position;
import repast.simphony.context.Context;
import simulation.FileName;
import simulation.FilePath;
import simulation.SimulationTool;
import simulation.parser.FilesManagement;
import simulation.parser.JSONWriter;
import smartGov.ClockSingleton;

public class ManagerQLearningScenario extends AbstractManager {

	//Spot prices
	public static double priceForAllSpots = 3.5;

	//Behavior change
	private int windowSize = 5;
	private double lowBoundForStateChange = 0.6;
	private double highBoundForStateChange = 1.0;
	
	private String currentPhase;
	private int indexOfPrice;
	
	//Initial state
	private Map<String, String> initialStateConfig;
	
	public ManagerQLearningScenario(Context<Object> context){
		super();
		this.context = context;
		
		this.setBlockfacePrices(priceForAllSpots);
		indexOfAction = 0;
		
		initialStateConfig = new HashMap<>();

		if (Integer.parseInt(EnvVar.configFile.get("learning")) == 1) {
			currentPhase = "learning";
			Random rnd = new Random();
			indexOfPrice = rnd.nextInt(8);
			leaveObservationPhase();
			for(PolicyAgent policyAgent : EnvVar.policyAgents) {
				policyAgent.applyRandomActionToStructures();
			}
		} else if(Integer.parseInt(EnvVar.configFile.get("validation")) == 1) {
			currentPhase = "validation";
			validationPhase = true;
			initialStateConfig = parseInitialStateFile(FilePath.policyFolder + FileName.INITIAL_STATE);
			createInitialState();
			recentlyReset = true;
		}
		if(EnvVar.configFile.get("split").equals("1")) {
			EnvVar.policyAgents.get(0).splitControlGroup();
		}
		
		clearAgents(); //Force a reset
	}

	@Override
	public void live() {
		if(ClockSingleton.getInstance().getLocalTime() >= timeToReset){
			
			if(observationPhase) {
				callPolicyAgents();
				updateHistoricOfAllAgents();
				if(NUMBER_OF_ITERATIONS_BEFORE_APPLYING_POLICIES == currentTrialIndex){
					saveObservations();
					resetHistoricOfAgents();
					this.currentTrialIndex = 0;
					applyAction();
				}
				resetStateOfSimulation();
				currentTrialIndex++;
			} else {
				callPolicyAgents();
				updateHistoricOfAllAgents();
				if(NUMBER_OF_ITERATIONS_BEFORE_APPLYING_POLICIES == currentTrialIndex){
					currentIteration++;
					saveSpecificInformation();
					saveTime();
					resetHistoricOfAgents();
					currentTrialIndex = 0;
					randomStateGenerator(true);
				} else {
					currentTrialIndex++;
				}
				resetStateOfSimulation();
			}
			saveManagerCounters();
		}
	}
	
	private void callPolicyAgents() {
		for(PolicyAgent policyAgent : EnvVar.policyAgents) {
			if(policyAgent != null) {
				policyAgent.live();
			}
		}
	}

	private void applyAction() {
		if(EnvVar.configFile.get("iterateThroughPrices").equals("1")) {
			priceForAllSpots += 0.5;
			for(BlockFace blockface : EnvVar.blockfaces) {
				blockface.setPrice(priceForAllSpots);
			}
		}
		if(priceForAllSpots > 4.0) {
			SimulationTool.STOP();
		}
	}

	private void randomState() {
		System.out.println("------New random state from current state------");
		indexOfAction = -1;
		
		double price = 0.5*indexOfPrice + 0.5;
		int spotsModification = 0;
		
		if(EnvVar.configFile.containsKey("random_state")) {
			if(EnvVar.configFile.get("random_state").equals("price")) {
				price = randomPrice();
			} else if(EnvVar.configFile.get("random_state").equals("spot")) {
				spotsModification = randomAvailableSpotModification();
			} else {
				price = randomPrice();
				spotsModification = randomAvailableSpotModification();
			}
		} else {
			price = randomPrice();
			spotsModification = randomAvailableSpotModification();
		}
		
		for(BlockFace blockface : EnvVar.blockfaces) {
			blockface.enableAllSpots();
			blockface.setPrice(price);
			blockface.updateSpots(spotsModification);
		}
	}
	
	private double randomPrice() {
		Random rnd = new Random();
		indexOfPrice = rnd.nextInt(8);
		return 0.5*indexOfPrice + 0.5;
	}
	
	private int randomAvailableSpotModification() {
		Random rnd = new Random();
		int spotsModification = rnd.nextInt(5);
		if(rnd.nextDouble() > 0.5) {
			spotsModification *= -1;
		}
		return spotsModification;
	}

	private void updateHistoricOfAllAgents(){
		for(int agentIndex = 0; agentIndex < EnvVar.agents.size(); agentIndex++){
			MicroAgent agent = (MicroAgent) EnvVar.agents.get(agentIndex);

			//* Move to behavior
			//Update historic
			if(agent.getBody() != null){ //Agent took the car
				((ParkProperties)agent.getProperties()).updateHistoric(agent.getBody().getPosition());
			} else { //Agent took the bus
				((ParkProperties)agent.getProperties()).updateHistoric(null);
			}
		}
	}

	@Override
	protected void clearAgents() {
		for(int agentIndex = 0; agentIndex < EnvVar.agents.size(); agentIndex++){
			MicroAgent agent = (MicroAgent) EnvVar.agents.get(agentIndex);

			if(agent != null && agent.getBody() != null){ //Agent was in the simulation
				context.remove(EnvVar.agents.get(agentIndex).getBody());
				context.remove(EnvVar.agents.get(agentIndex));
				EnvVar.bodiesStock.add(agent.getBody());
				agent.setBody(null);
			}
			agent.recycleAgent(agentIndex);
			if(getAgentDecisionForAlternativeSolution(agentIndex)){ //Take the car
				EnvVar.agentStockIdT.get(Integer.valueOf(((ParkProperties)agent.getProperties()).getSourceNode().getId())).add(agentIndex); //New agent id available to creation
			} else { //Take an alternative solution
				// TODO agent behavior when taking alternative transport solution
			}
		}
	}

	private void leaveObservationPhase() {
		observationPhase = false;

		randomState();

		learningPhase = true;
	}

	@Override
	protected void resetStateOfSimulation() {
		System.out.println("------New Simulation------");
		
		resetClock();
		clearAgents();
		resetSpots();
		//Initialization of KDTree
		for(int i = 0; i < EnvVar.correctSpots.size(); i++){
			EnvVar.kdtreeWithSpots.insert(EnvVar.correctSpots.get(i).getPositionInDouble(), EnvVar.correctSpots.get(i));
		}
		this.iterationCounter++;
		System.out.println("Counter: " + this.iterationCounter);

	}

	protected void resetHistoricOfAgents() {
		for(int agentIndex = 0; agentIndex < EnvVar.agents.size(); agentIndex++){
			MicroAgent agent = (MicroAgent) EnvVar.agents.get(agentIndex);
			((ParkProperties)agent.getProperties()).clearHistoric();
			((ParkProperties)agent.getProperties()).setCurrentState(0); //Force to car
		}
	}

	private void setBlockfacePrices(double price){
		for(int i = 0; i < EnvVar.blockfaces.size(); i++){
			EnvVar.blockfaces.get(i).setPrice(price);
		}
	}

	private void resetSpots(){
		for(int spotIndex = 0; spotIndex < EnvVar.parkingSpots.size(); spotIndex ++){
			if(EnvVar.parkingSpots.get(spotIndex).isOccupied()){
				EnvVar.parkingSpots.get(spotIndex).setOccupied(false);
			}
		}
	}

	private boolean getAgentDecisionForAlternativeSolution(int id){
		MicroAgent agent = (MicroAgent) EnvVar.agents.get(id);
		ParkProperties properties = (ParkProperties) agent.getProperties();
		//TODO Improve discountFactors representation with generic description
		List<Double> discountFactors = new ArrayList<>();
		discountFactors.add(0.9);
		discountFactors.add(0.5);
		discountFactors.add(0.5);
		discountFactors.add(0.25);
		discountFactors.add(0.1);
		//TODO Dynamically change bounds for state modification
		if(properties.changeCurrentState(
				properties.getAverageOfSatisfaction(properties.getListOfPreviousSatisfaction(), windowSize, discountFactors), 
				lowBoundForStateChange, 
				highBoundForStateChange)){
			//Change modal choice
			properties.reinitSatisfactions();
			if(properties.getCurrentState() == 0){
				properties.setCurrentState(1); //take the bus
				//System.out.println("Agent " + id + " now takes the bus.");
				return false;
			} else {
				properties.setCurrentState(0); //take the car
				//System.out.println("Agent " + id + " now takes the car.");
				return true;
			}
		} else {
			//Do not change modal choice
			if(properties.getCurrentState() == 0){ //take the car
				return true;
			} else {
				properties.setCurrentState(1); //take the bus
				return false;
			}
		}
	}

	@Override
	protected void init() {
		parseConfigFile(EnvVar.configFile);
		this.iterationCounter = 1;
		this.restartCounter = 0;
		
		iterationCounter = 0;
		currentTrialIndex = 0;
		currentSimulationIndex = 0;
		currentSimulationIndex = 1;
		saveManagerCounters();
		timeStamp = createTimeStamp();
		saveConfigOfSimulation(EnvVar.scenario.getAdditionnalInfo());
	}

	@Override
	protected void parseConfigFile(Map<String, String> configFile) {
		super.parseConfigFile(configFile);
		priceForAllSpots = Double.parseDouble(EnvVar.configFile.get("initial_price"));

		String startSimulationParam = EnvVar.configFile.get("start");
		String endSimulationParam = EnvVar.configFile.get("end");
		String[] parseStart = startSimulationParam.split("h");
		this.startSimulationAt = new DateTime("2017-01-01T"+parseStart[0]+":"+parseStart[1]+":01.000");
		ClockSingleton.getInstance().setDateTime(startSimulationAt);
		String[] parseEnd = endSimulationParam.split("h");
		this.pauseSimulationAt = new LocalTime(Integer.parseInt(parseEnd[0]), Integer.parseInt(parseEnd[1]));
		this.timeToReset = pauseSimulationAt.getHourOfDay()*60 + pauseSimulationAt.getMinuteOfHour();
	}

	public int getIndexOfPrice() {
		return indexOfPrice;
	}
	
	protected void saveConfigOfSimulation(List<String> additionnalInfo) {
		System.out.println("Save parameters for current simulations.");
		List<String> lines = new ArrayList<>();
		lines.add("Time: " + timeStamp + ".");
		for(Entry<String, String> value : EnvVar.configFile.entrySet()) {
			lines.add(value.getKey() + ": " + value.getValue());
		}
		lines.add("Scenario: " + EnvVar.scenario.getClass().getSimpleName());
		lines.add("GIS Folder: " + EnvVar.scenario.getGISFolder());
		FilesManagement.writeToFile(FilePath.currentLocalLearnerFolder, FileName.MANAGER_PARAMETERS_FILE, lines);
		if(additionnalInfo != null && !additionnalInfo.isEmpty()) {
			
		}
	}
	
	protected void randomStateGenerator(boolean generateRandomState) {
		if(generateRandomState) {
			//Try with a limited number of trials and a cumulative reward
			if(NUMBER_OF_SIMULATIONS_BEFORE_RESTART == currentSimulationIndex) {
				recentlyReset = true;
				if(currentPhase.equals("learning")) {
					randomState();
				} else if(currentPhase.equals("validation")) {
					createInitialState();
				}
				currentSimulationIndex = 0;
			} else {
				List<String> actions = new ArrayList<>();
				List<PolicyAgent> agentsWithSpecificActions = new ArrayList<>();

				boolean keepAction = false;
				
				for(PolicyAgent policyAgent : EnvVar.policyAgents) {
					if(policyAgent != null) {
						//1) Do the special action first
						PolicyAction currentSpecificAction = policyAgent.getLastSpecialAction();
						if(currentSpecificAction != PolicyAction.NOTHING) {
							agentsWithSpecificActions.add(policyAgent);
						}
						if(currentSpecificAction == PolicyAction.KEEP) {
							keepAction = true;
						}
					}
				}
				//*/ TODO: Bug source ? Double fusion 29/06/19
				for(int i = 0; i < agentsWithSpecificActions.size(); i++) {
					if(agentsWithSpecificActions.get(i) != null) {
						PolicyAgent policyAgent = agentsWithSpecificActions.get(i);
						actions.add(policyAgent.applyPolicyAction(policyAgent.getLastSpecialAction()));
					}
				}
				//*/
				if(keepAction) {
					JSONWriter.writePolicyAgents(FilePath.currentLocalLearnerFolder, 
							currentIteration + "_" + FileName.PolicyAgentsFile,
							EnvVar.policyAgents);
				}
				
				for(PolicyAgent policyAgent : EnvVar.policyAgents) {
					if(policyAgent != null) {
						//2) Do the normal action of the remaining agents
						PolicyAction currentAction = policyAgent.getLastAction();
						actions.add(policyAgent.applyPolicyAction(currentAction));
						if(currentPhase.equals("learning")) {
							policyAgent.updateLocalLearners(currentAction);
						}
					}
				}
				recentlyReset = false;
				currentSimulationIndex++;
				saveActionsPerIteration(actions);
			}
		} else {
			//No limited number of trials before hard reset
			for(PolicyAgent policyAgent : EnvVar.policyAgents) {
				if(policyAgent != null) {
					if(currentPhase.equals("learning")) {
						policyAgent.askAndUpdateLocalLearners();
					} else if(currentPhase.equals("validation")) {
						policyAgent.askLocalLearners();
					}
				}
			}
			currentSimulationIndex++;
		}
	}
	
	protected void saveTime() {
		Instant now = Instant.now();
		Date date = new Date();
		String datelog = "[Time] (" + currentIteration + ")| Date: " + date.toString() + " | Iteration time: " + formatter.format(Duration.between(currentInstant, now).toMillis()) + " | Total time: " + formatter.format((System.currentTimeMillis() - beginTime)) + " |";
		currentInstant = now;
		FilesManagement.appendToFile(FilePath.currentLocalLearnerFolder, FileName.MANAGER_LOGS, datelog);
	}
	
	protected void saveObservations() {
		List<String> labels = new ArrayList<>();
		labels.add("utility");
		labels.add("timesearching");
		labels.add("distance");
		labels.add("price");
		labels.add("occupation");
		List<Position> positions = new ArrayList<>();
		for(Structure structure : EnvVar.structures) {
			if(structure instanceof BlockFace) {
				positions.add(structure.getLocalPerformances(labels));
				System.out.println(structure.getID() + ") " + structure.getLocalPerformances(labels));
			}
		}
		Position averageOfSimulation = PolicyAgent.averagePosition(positions);
		System.out.println(averageOfSimulation);
		FilesManagement.appendToFile(FilePath.currentLocalLearnerFolder, FileName.MANAGER_STRUCTURES_AVERAGE, averageOfSimulation.toString());
	}
	
	protected void saveSpecificInformation() {
		List<String> lines = new ArrayList<>();
		lines.add("------[Iteration: " + currentIteration + "]------");
		int totalPlaces = 0;
		int currentPlaces = 0;
		int totalParked = 0;
		double totalGain = 0.0;
		Map<String, Integer> parkedAgentsPerUtilityTotal = new HashMap<>();
		for(PolicyAgent policyAgent : EnvVar.policyAgents) {
			if(policyAgent != null) {
				String policyAgentStr = "";
				double localTotalGain = 0.0;
				int localTotalPlaces = 0;
				int localCurrentPlaces = 0;
				int localParked = 0;
				Map<String, Integer> localAgentsPerUtilityParked = new HashMap<>();
				List<Structure> blockfaces = policyAgent.getPerimeter().getStructuresWithName(BlockFace.class.getSimpleName());
				for(int i = 0; i < blockfaces.size(); i++) {
					BlockFace blockface = (BlockFace) blockfaces.get(i);
					String str = "[" + blockface.getID() + "] Price:" + blockface.getPrice() + "," + blockface.getOccupancy()
							+ "/" + blockface.getCurrentNumberPlaces() + "/" + blockface.getMaxNumberPlaces()
							+ "(parked/available/total)";
					totalPlaces += blockface.getMaxNumberPlaces();
					currentPlaces += blockface.getCurrentNumberPlaces();
					totalParked += blockface.getOccupancy();
					
					localTotalGain += blockface.getOccupancy() * blockface.getPrice();
					localTotalPlaces += blockface.getMaxNumberPlaces();
					localCurrentPlaces += blockface.getCurrentNumberPlaces();
					localParked += blockface.getOccupancy();
					Map<String, Integer> agentsPerUtilityParked = blockface.agentPerUtilityParked();
					if(!agentsPerUtilityParked.isEmpty()) {
						for(Entry<String, Integer> entry : agentsPerUtilityParked.entrySet()) {
							str += ";" + entry.getKey() + ":" + entry.getValue();
							if(parkedAgentsPerUtilityTotal.containsKey(entry.getKey())) {
								parkedAgentsPerUtilityTotal.put(entry.getKey(), parkedAgentsPerUtilityTotal.get(entry.getKey()) + entry.getValue());
							} else {
								parkedAgentsPerUtilityTotal.put(entry.getKey(), entry.getValue());
							}
							if(localAgentsPerUtilityParked.containsKey(entry.getKey())) {
								localAgentsPerUtilityParked.put(entry.getKey(), localAgentsPerUtilityParked.get(entry.getKey()) + entry.getValue());
							} else {
								localAgentsPerUtilityParked.put(entry.getKey(), entry.getValue());
							}
						}
					}
					
					str += ".";
					lines.add(str);
					//System.out.println(str);
				}
				policyAgentStr = "["+policyAgent.getId()+"] Gain:" + localTotalGain + "," 
						+ localParked + "/" + localCurrentPlaces + "/" + localTotalPlaces
						+ "(parked/available/total)";
				for(Entry<String, Integer> entry : localAgentsPerUtilityParked.entrySet()) {
					policyAgentStr += ";" + entry.getKey() + ":" + entry.getValue();
				}
				policyAgentStr += ".";
				lines.add(policyAgentStr);
				totalGain += localTotalGain;
			}
		}
		String totalStr = totalParked + "/" + currentPlaces + "/" + totalPlaces + "(parked/available/total), Gain:" + totalGain;
		for(Entry<String, Integer> entry : parkedAgentsPerUtilityTotal.entrySet()) {
			totalStr += ";" + entry.getKey() + ":" + entry.getValue();
		}
		totalStr += ".";
		lines.add(totalStr);
		FilesManagement.appendToFile(FilePath.currentLocalLearnerFolder, FileName.MANAGER_BLOCKFACES, lines);
	}
	
	protected void saveActionsPerIteration(List<String> actions) {
		List<String> lines = new ArrayList<>();
		String line = currentIteration + ")";
		for(int i = 0; i < actions.size(); i++) {
			line += actions.get(i);
		}
		lines.add(line);
		FilesManagement.appendToFile(FilePath.currentLocalLearnerFolder, FileName.MANAGER_ACTIONS, lines);
	}
	
	private Map<String, String> parseInitialStateFile(String initialStateFile) {
		Map<String, String> initialStateConfig = new HashMap<>();
		File file = new File(initialStateFile);
		Scanner input;
		try {
			input = new Scanner(file);
			
			while(input.hasNext()) {
			    String nextLine = input.nextLine();
			    if(!nextLine.contains("#")){
		    		String lines[] = nextLine.split(":");
		    		initialStateConfig.put(lines[0], lines[1]);
			    }
			}
			input.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return initialStateConfig;
	}
	
	private void createInitialState() {
		System.out.println("------Set initial state------");
		indexOfAction = -1;
		
		double price = Double.valueOf(initialStateConfig.get("price"));
		int spotsModification = Integer.valueOf(initialStateConfig.get("spots"));
		
		for(BlockFace blockface : EnvVar.blockfaces) {
			blockface.enableAllSpots();
			blockface.setPrice(price);
			blockface.updateSpots(spotsModification);
		}
	}

}