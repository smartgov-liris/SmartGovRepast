package simulation.scenario;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.joda.time.LocalTime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import environment.MicroAgentBody;
import environment.Structure;
import environment.city.Building;
import environment.city.EnvVar;
import environment.city.WorkOffice;
import environment.city.parking.BlockFace;
import environment.city.parking.ParkingSpot;
import environment.graph.Arc;
import environment.graph.EdgeOSM;
import environment.graph.Graph;
import environment.graph.Node;
import environment.graph.OrientedGraph;
import environment.graph.SinkNode;
import environment.graph.SourceNode;
import microagent.MicroAgent;
import microagent.behavior.TravelCity;
import microagent.perception.CommuterPerception;
import microagent.properties.ParkProperties;
import microagent.utility.ParkUtility;
import net.sf.javaml.core.kdtree.KDTree;
import policyagent.Perimeter;
import policyagent.PolicyAction;
import policyagent.PolicyAgent;
import repast.simphony.context.Context;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import simulation.FileName;
import simulation.FilePath;
import simulation.manager.ManagerNoSimulationScenario;
import simulation.manager.ManagerQLearningScenario;
import simulation.parser.FilesManagement;
import simulation.parser.JSONReader;
import simulation.parser.JSONWriter;
import simulation.socket.ClientCommunication;
import simulation.socket.Server;
import smartGov.ClockSingleton;
import smartGov.OrientedGraphSingleton;

public abstract class ScenarioDRL extends ScenarioSmartGov {

	protected Random rnd;
	
	protected String filename;
	
	protected Map<String, ParkUtility> utilityByName;
	public static final String REGULAR     =      "Regular";
	public static final String LOWINCOME   =   "Low_Income";
	public static final String ECOFRIENDLY = "Eco-Friendly";
	public static final String IMPATIENT   =    "Impatient";
	public static final String HURRIED     =      "Hurried";
	
	
	protected ParkUtility regularUtility;
	protected ParkUtility lowIncomeUtility;
	protected ParkUtility sustainableUtility;
	protected ParkUtility hurryUtility;
	protected ParkUtility lateUtility;

	protected double proba_lateUtility = 0;
	protected double proba_sustainableUtility = 0;
	protected double proba_lowIncomeUtility = 0;
	protected double proba_hurryUtility = 0;
	protected double proba_regularUtility = 0;

	protected double probaLate;
	protected double probaSust;
	protected double probaLowI;
	protected double probaHurr;
	protected double probaRegu; //Should always be equal to 100

	/*--------------------Scenario Specific--------------------*/
	protected int scenarioID;
	protected Map<String, List<Integer>> buildingsIds;
	protected Map<String, List<Integer>> sourceNodesIds;
	protected Map<String, Integer> maxAgentsPerIds;
	protected Map<String, Integer> currentAgentsPerBlock;
	protected Map<String, Integer> currentAgentsPerUtility;
	protected Map<String, Integer> maxAgentsPerUtility;
	protected Map<String, List<String>> idsBlockPerUtility;
	
	protected String population;

	protected List<String[]> synPopData;

	public static String confiantBehaviorFile = FilePath.behaviorFolder + "Confiant.txt";
	public static String prudentBehaviorFile  = FilePath.behaviorFolder + "Prudent.txt";
	
	protected boolean learning = true;
	protected boolean validation = false;

	public ScenarioDRL() {
		utilityByName = new HashMap<>();
		
		buildingsIds = new HashMap<>();
		sourceNodesIds = new HashMap<>();
		maxAgentsPerIds = new HashMap<>();
		currentAgentsPerBlock = new HashMap<>();
		maxAgentsPerUtility = new HashMap<>();
		idsBlockPerUtility = new HashMap<>();
		currentAgentsPerUtility = new HashMap<>();
		rnd = new Random();
		
		if(Integer.parseInt(EnvVar.configFile.get("learning")) == 0) {
			learning = false;
			validation = true;
		}
	}
	
	public void createScenarioSpecificItems() {
		parseScenarioStructures(FilePath.structuresFolder + filename);
		computeMaxAgents();
	}
	
	protected void loadUtility() {
		parseUtility(FilePath.scenarioFolder + filename);
		computeAgentsPerUtility();
	}
	
	protected void policyAgentsCreation() {
		copyFiles();
		int numberOfPolicyAgents = 0;
		if(validation) {
			numberOfPolicyAgents = numberOfPolicyAgentsAfterLearning();
		} else {
			numberOfPolicyAgents = numberOfPolicyAgents();
		}
		if(numberOfPolicyAgents == 0) {
			List<PolicyAction> policyActions = loadPolicyActions();
			List<PolicyAction> specialPolicyActions = loadSpecialPolicyActions();
			Map<String, List<String>> IDsBlockfaces = createStructuresPerID();
			Map<String, Perimeter> perimetersPerID = createPerimeterForIds(IDsBlockfaces);
			for(Entry<String, Perimeter> entry : perimetersPerID.entrySet()) {
				EnvVar.policyAgents.add(new PolicyAgent(entry.getKey(), entry.getValue(), policyActions, specialPolicyActions));
			}
			JSONWriter.writePolicyAgents(FilePath.currentLocalLearnerFolder, 
					FileName.PolicyAgentsFile,
					EnvVar.policyAgents);
		} else {
			if(learning) {
				for(int indexOfPolicyAgent = 0; indexOfPolicyAgent < numberOfPolicyAgents; indexOfPolicyAgent++) {
					List<PolicyAction> policyActions = loadPolicyActions();
					List<PolicyAction> specialPolicyActions = loadSpecialPolicyActions();
					Perimeter perimeter = loadPerimeter(indexOfPolicyAgent);
					EnvVar.policyAgents.add(new PolicyAgent(String.valueOf(indexOfPolicyAgent), perimeter, policyActions, specialPolicyActions));
				}
				JSONWriter.writePolicyAgents(
						FilePath.currentLocalLearnerFolder, 
						FileName.PolicyAgentsFile, 
						EnvVar.policyAgents);
			} else if(validation) {
				parseAgentFileAfterLearning(fileForPolicyAgentsAfterLearning());

				JSONWriter.writePolicyAgents(
						FilePath.currentLocalLearnerFolder, 
						FileName.PolicyAgentsFile, 
						EnvVar.policyAgents);
			}
			
		}
	}
	
	protected Map<String, Perimeter> createPerimeterForIds(Map<String, List<String>> IDsBlockfaces){
		Map<String, List<Structure>> structuresPerID = new HashMap<>();
		Map<String, Perimeter> perimetersPerID = new HashMap<>();
		for(Entry<String, List<String>> entry : IDsBlockfaces.entrySet()) {
			structuresPerID.put(entry.getKey(), new ArrayList<Structure>());
		}
		for(int i = 0; i < EnvVar.blockfaces.size(); i++){
			if(!EnvVar.blockfaces.get(i).isFailed()){
				for(Entry<String, List<String>> entry : IDsBlockfaces.entrySet()) {
					if(entry.getValue().contains(EnvVar.blockfaces.get(i).getID())) {
						structuresPerID.get(entry.getKey()).add(EnvVar.blockfaces.get(i));
					} 
				}
				EnvVar.structuresGlobal.add(EnvVar.blockfaces.get(i));
			}
		}
		for(Entry<String, List<Structure>> entry : structuresPerID.entrySet()) {
			perimetersPerID.put(entry.getKey(), new Perimeter(entry.getValue()));
		}
		return perimetersPerID;
	}
	
	protected Map<String, List<String>> createStructuresPerID(){
		return parsePerimeter(FilePath.perimeterFolder + filename);
	}
	
	protected void loadSpecificScenario(Integer id, ParkProperties properties) {
		fillBlockByUtility(id, properties);
	}
	
	protected ParkUtility setUtility(String profileName) {
		for(Entry<String, Integer> entry : currentAgentsPerUtility.entrySet()) {
			if(entry.getValue() < maxAgentsPerUtility.get(entry.getKey())) {
				entry.setValue(entry.getValue() + 1);
				return utilityByName.get(entry.getKey());
			}
		}
		return utilityByName.get(REGULAR);
	}
	
	protected abstract String loadGISFile();
	
	protected void loadUtilityFiles() {
		regularUtility     = new ParkUtility(EnvVar.indicatorsFiles,      REGULAR, FilePath.areaCoefFolder+"area.json");
		lowIncomeUtility   = new ParkUtility(EnvVar.indicatorsFiles,    LOWINCOME, FilePath.areaCoefFolder+"area.json");
		sustainableUtility = new ParkUtility(EnvVar.indicatorsFiles,  ECOFRIENDLY, FilePath.areaCoefFolder+"area.json");
		hurryUtility       = new ParkUtility(EnvVar.indicatorsFiles,    IMPATIENT, FilePath.areaCoefFolder+"area.json");
		lateUtility        = new ParkUtility(EnvVar.indicatorsFiles,      HURRIED, FilePath.areaCoefFolder+"area.json");
		utilityByName.put(REGULAR, regularUtility);
		utilityByName.put(LOWINCOME, lowIncomeUtility);
		utilityByName.put(ECOFRIENDLY, sustainableUtility);
		utilityByName.put(IMPATIENT, hurryUtility);
		utilityByName.put(HURRIED, lateUtility);
	}
	
	protected String createPopulation() {
		probaLate = proba_lateUtility;
		probaSust = probaLate + proba_sustainableUtility;
		probaLowI = probaSust + proba_lowIncomeUtility;
		probaHurr = probaLowI + proba_hurryUtility;
		probaRegu = probaHurr + proba_regularUtility;
		return (int) proba_lateUtility + "-" +
				(int) proba_sustainableUtility + "-" + 
				(int) proba_lowIncomeUtility + "-" +
				(int) proba_hurryUtility + "-" +
				(int) proba_regularUtility;
	}

	protected void startServer() {
		//create folder for current simulation
		//createFolder();
		FilePath.currentAgentDetailsFolder = FilePath.scenarioFolder + 
				EnvVar.configFile.get("scenario") + File.separator +
				EnvVar.configFile.get("scenarioID") + File.separator;
		//Update server port with simulationIndex
		//*/
		ClientCommunication.port += simulationIndex;
		if(EnvVar.configFile.get("server_debug").equals("0")) {
			Server.startServer(FilePath.externalSourceFolder + "server.py -p " + ClientCommunication.port, "python3");
		} else {
			//Use this when bug in python server
			try {
				Runtime.getRuntime().exec("cmd /c start extsrc\\server.bat " + ClientCommunication.port);
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
		//*/ Add 5 seconds delay to load Tensorflow in server
		try {
			TimeUnit.SECONDS.sleep(5);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		//*/
	}
	
	@Override
	protected List<PolicyAction> loadPolicyActions() {
		List<PolicyAction> policyActions = new ArrayList<>();
		if(EnvVar.configFile.containsKey("localagent_action")) {
			String[] localActions = EnvVar.configFile.get("localagent_action").split(";");
			for(int i = 0; i < localActions.length; i++) {
				policyActions.add(PolicyAction.getActionFrom(localActions[i]));
			}
		} else {
			policyActions.add(PolicyAction.INCREASE_PRICES);
			policyActions.add(PolicyAction.DECREASE_PRICES);
			policyActions.add(PolicyAction.DO_NOTHING);
			policyActions.add(PolicyAction.INCREASE_PLACES);
			policyActions.add(PolicyAction.DECREASE_PLACES);
		}
		return policyActions;
	}
	
	@Override
	protected List<PolicyAction> loadSpecialPolicyActions() {
		List<PolicyAction> policyActions = new ArrayList<>();
		policyActions.add(PolicyAction.MERGE);
		policyActions.add(PolicyAction.ROLLBACK);
		policyActions.add(PolicyAction.KEEP);
		return policyActions;
	}
	
	protected ParkUtility setUtility(int value){
		if(value < probaLate){
			return lateUtility;
		} else if(value < probaSust){
			return sustainableUtility;
		} else if(value < probaLowI){
			return lowIncomeUtility;
		} else if(value < probaHurr){
			return hurryUtility;
		} else if(value < probaRegu){
			return regularUtility;
		}
		return null; //Should not happen
	}
	
	@Override
	public Map<String, Number> getInfos() {
		Map<String, Number> info = new HashMap<String, Number>();
		info.put(IMPATIENT, proba_lateUtility);
		info.put(ECOFRIENDLY, proba_sustainableUtility);
		info.put(LOWINCOME, proba_lowIncomeUtility);
		info.put(HURRIED, proba_hurryUtility);
		info.put(REGULAR, proba_regularUtility);
		info.put("SimulationNumber", Double.valueOf(EnvVar.configFile.get("total_number_of_simulations")));
		info.put("SimulationPerAction", Double.valueOf(EnvVar.configFile.get("simulations_per_action")));
		info.put("SpotNumber",  EnvVar.correctSpots.size());
		return info;
	}
	
	@Override
	public void createAgents(){
		long beginTime = System.currentTimeMillis();
		for(int i = 0; i < EnvVar.AGENT_MAX; i++){
			EnvVar.agents.add(createAgentWithoutBodyWithID(i));
		}
		if(EnvVar.configFile.get("model_folder").equals("-")) {
			JSONWriter.writeAgents(EnvVar.agents, FilePath.currentLocalLearnerFolder + FileName.AgentFile); //Also stores it in localLearner folder	
		}
		
		System.out.println("Time to create " + EnvVar.AGENT_MAX + " agents: " + (System.currentTimeMillis() - beginTime) + "ms. Total agents: " + EnvVar.agents.size());
	}
	
	protected void setCorrectBlockfaces(){
		for(int i = 0; i < EnvVar.blockfaces.size(); i++){
			BlockFace blockface = EnvVar.blockfaces.get(i);
			int nullSpotCounter = 0;
			for(int j = 0; j < blockface.getParkingSpots().size(); j++){
				if(blockface.getParkingSpots().get(j).isFailed()){
					nullSpotCounter++;
					blockface.setMaxNumberPlaces(blockface.getMaxNumberPlaces() - 1);
					blockface.setCurrentNumberPlaces(blockface.getCurrentNumberPlaces() - 1);
				} else {
					EnvVar.correctSpots.add(blockface.getParkingSpots().get(j));
				}
			}
			if(nullSpotCounter == blockface.getParkingSpots().size()){
				blockface.setFailed(true);
			}
		}
	}
	
	protected void correctSpotsToKDTree(List<ParkingSpot> spots){
		long beginTime = System.currentTimeMillis();
		for(int i = 0; i < spots.size(); i++){
			EnvVar.kdtreeWithSpots.insert(spots.get(i).getPositionInDouble(), spots.get(i));
		}
		System.out.println("Time to allocate spots to KDTree: " + (System.currentTimeMillis() - beginTime) + "ms.");
	}
	
	protected void setClosestNodeIdForBuildingList(List<? extends Building> buildings, Graph graph){
		for(int i = 0; i < buildings.size(); i++){
			buildings.get(i).setClosestNodeId(graph);
		}
	}
	
	/**
	 * Allocates closest nodes of roads with parking spots to work offices
	 * @param edgeWithSpot
	 */
	public void closestRoadWithSpot(List<EdgeOSM> edgeWithSpot){
		long beginTime = System.currentTimeMillis();
		for(int i = 0; i < edgeWithSpot.size(); i++){
			EnvVar.kdtreeArcsWithSpots.insert(edgeWithSpot.get(i).getStartNode().getPositionInDouble(), edgeWithSpot.get(i));
		}
		for(int i = 0; i < EnvVar.offices.size(); i++){
			//Get closest edges by their start position

			try {
				Object[] closestNodes = EnvVar.kdtreeArcsWithSpots.nearest(EnvVar.nodes.get(EnvVar.offices.get(i).getClosestNodeId()).getPositionInDouble() , 3);
				List<Node> nodes = new ArrayList<>();
				for(int j = 0; j < closestNodes.length; j++){
					nodes.add((Node) ((Arc)closestNodes[j]).getStartNode());
				}
				EnvVar.offices.get(i).setClosestNodesWithSpots(nodes);
			} catch (IllegalArgumentException e){
				EnvVar.offices.get(i).setClosestNodesWithSpots(new ArrayList<Node>());
			}


		}
		System.out.println("Time to allocate closest nodes to work offices: " + (System.currentTimeMillis() - beginTime) + "ms.");
	}
	
	protected void createManager(Context<Object> context){
		EnvVar.manager = new ManagerNoSimulationScenario(context);
		//EnvVar.manager = new ManagerQLearningScenario(context);
		context.add(EnvVar.manager);
	}
	
	@Override
	public List<List<?>> addElementToContext() {
		List<List<?>> elementsToBeAdded = new ArrayList<>();
		elementsToBeAdded.add(EnvVar.nodes);
		elementsToBeAdded.add(EnvVar.buildings);
		List<Object> spots = new ArrayList<>();
		for(int i = 0; i < EnvVar.parkingSpots.size(); i++){
			if(!EnvVar.parkingSpots.get(i).isFailed()){
				spots.add(EnvVar.parkingSpots.get(i));
			}
		}
		elementsToBeAdded.add(spots);
		elementsToBeAdded.add(EnvVar.edgesOSM);
		return elementsToBeAdded;
	}
	
	protected int numberOfPolicyAgents() {
		List<String> filesToCheck = new ArrayList<>();
		filesToCheck.add("_global.txt");
		int counter = 0;
		File sourceFolder = new File(FilePath.currentLocalLearnerFolder);
		for (final File fileEntry : sourceFolder.listFiles()) {
	        if (!fileEntry.isDirectory()) {
	            String filename = fileEntry.getName();
	            for(String stringToCheck : filesToCheck) {
	            	if(filename.contains(stringToCheck)) {
	            		counter++;
	            	}
	            }
	        }
	    }
		return counter;
	}
	
	protected String fileForPolicyAgentsAfterLearning() {
		List<String> filesToCheck = new ArrayList<>();
		filesToCheck.add("_PolicyAgents.json");
		int maxIterationValue = Integer.MIN_VALUE;
		File sourceFolder = new File(FilePath.localLearnerFolder + EnvVar.configFile.get("model_folder"));//FilePath.currentLocalLearnerFolder);
		for (final File fileEntry : sourceFolder.listFiles()) {
	        if (!fileEntry.isDirectory()) {
	            String filename = fileEntry.getName();
	            for(String stringToCheck : filesToCheck) {
	            	if(filename.contains(stringToCheck)) {
	            		int iterationValue = Integer.parseInt(filename.split("_PolicyAgents.json")[0]);
	            		if(maxIterationValue < iterationValue) {
	            			maxIterationValue = iterationValue;
	            		}
	            	}
	            }
	        }
	    }
		return FilePath.localLearnerFolder + EnvVar.configFile.get("model_folder") + File.separator + maxIterationValue + "_PolicyAgents.json";
	}
	
	protected int numberOfPolicyAgentsAfterLearning() {
		List<String> filesToCheck = new ArrayList<>();
		filesToCheck.add("_PolicyAgents.json");
		int maxIterationValue = Integer.MIN_VALUE;
		File sourceFolder = new File(FilePath.localLearnerFolder + EnvVar.configFile.get("model_folder"));//FilePath.currentLocalLearnerFolder);
		for (final File fileEntry : sourceFolder.listFiles()) {
	        if (!fileEntry.isDirectory()) {
	            String filename = fileEntry.getName();
	            for(String stringToCheck : filesToCheck) {
	            	if(filename.contains(stringToCheck)) {
	            		int iterationValue = Integer.parseInt(filename.split("_PolicyAgents.json")[0]);
	            		if(maxIterationValue < iterationValue) {
	            			maxIterationValue = iterationValue;
	            		}
	            	}
	            }
	        }
	    }
		
		JsonParser parser = new JsonParser();
		try {
			JsonObject object = (JsonObject) parser.parse(new FileReader(FilePath.localLearnerFolder + EnvVar.configFile.get("model_folder") + File.separator + maxIterationValue + "_PolicyAgents.json"));
			for(Entry<String, JsonElement> policyAgent : object.entrySet()) {
				if(policyAgent.getKey().equals("Info")) {
					return policyAgent.getValue().getAsJsonObject().get("PolicyAgentNumber").getAsInt();
				}
			}
		} catch (Exception e) {
			System.out.println("Error in parsing perimeter for policy agents: ");
			e.printStackTrace();
		}
		return 0;
	}
	
	protected MicroAgent createAgentWithoutBodyWithID(int id) {
		Random rnd = new Random();
		LocalTime begin = new LocalTime(ClockSingleton.getInstance().getDateTime().getHourOfDay(), 0);
		LocalTime end = new LocalTime((ClockSingleton.getInstance().getDateTime().getHourOfDay() + (rnd.nextInt(7 + 1 - 5) + 5))%24, 0);
		ParkProperties properties = new ParkProperties(confiantBehaviorFile, begin, end);
		if(!EnvVar.configFile.get("model_folder").equals("-")) {
			//Load Agents.json
			Map<String, String> attributes = new HashMap<>();
			if(!EnvVar.configFile.get("agent_file").equals("-")) {
				attributes = JSONReader.parseAgentFile(id, FilePath.populationFolder + EnvVar.configFile.get("agent_file"));
			} else {
				attributes = JSONReader.parseAgentFile(id, FilePath.currentLocalLearnerFolder + FileName.AgentFile);
			}
			properties.setWorkOffice((WorkOffice) EnvVar.buildings.get(Integer.parseInt(attributes.get("Office"))));
			properties.setSourceNode(EnvVar.sourceNodes.get(Integer.valueOf(attributes.get("SourceNode"))));
			properties.setUtility(setUtility(attributes.get("Profile")));
		} else if(!EnvVar.configFile.get("agent_file").equals("-")) {
			//Load Agents.json and allow simulation reproductability
			Map<String, String> attributes = JSONReader.parseAgentFile(id, FilePath.populationFolder + EnvVar.configFile.get("agent_file"));
			properties.setWorkOffice((WorkOffice) EnvVar.buildings.get(Integer.parseInt(attributes.get("Office"))));
			properties.setSourceNode(EnvVar.sourceNodes.get(Integer.valueOf(attributes.get("SourceNode"))));
			properties.setUtility(setUtility(attributes.get("Profile")));
		} else {
			//int utility = rnd.nextInt(100);
			//properties.setUtility(setUtility(utility));
			
			//*/ Random Setup
			for(Entry<String, Integer> agentsPerUtility : currentAgentsPerUtility.entrySet()) {
				if(agentsPerUtility.getValue() < maxAgentsPerUtility.get(agentsPerUtility.getKey())) {
					agentsPerUtility.setValue(agentsPerUtility.getValue() + 1);
					properties.setUtility(utilityByName.get(agentsPerUtility.getKey()));
					break;
				}
			}
			//*/
			
			loadSpecificScenario(scenarioID, properties );
		}

		properties.setWorldObject("endNode", EnvVar.nodes.get(properties.getWorkOffice().getClosestNodeId()));

		String[] info = synPopData.get(rnd.nextInt(synPopData.size()-1)+1);
		properties.getInfos().put("ZipCode", info[0]);
		//https://stackoverflow.com/questions/343584/how-do-i-get-whole-and-fractional-parts-from-double-in-jsp-java
		double timeInSeconds = (Double.parseDouble(info[1]) % 1) * 60 + 60 * (Double.parseDouble(info[1]) - (Double.parseDouble(info[1]) % 1));
		properties.getInfos().put("AverageCommuteTime", timeInSeconds);
		properties.getInfos().put("Profile", properties.getUtility().getProfileName());
		properties.setProbaStates("");
		if(properties.getCurrentState() == 0){

		}
		return new MicroAgent(id, new TravelCity(), properties, new CommuterPerception());
	}
	
	protected void fillBlock(Integer id, ParkProperties properties) {
		for(Entry<String, Integer> entry : currentAgentsPerBlock.entrySet()) {
			if (entry.getValue() < maxAgentsPerIds.get(entry.getKey())) {
				fillBlockWith(id, properties, entry.getKey());
				entry.setValue(entry.getValue() + 1);
				//System.out.println("id) " + entry.getKey() + ", " + entry.getValue());
				break;
			}
			
		}
	}
	
	protected void fillBlockWith(Integer id, ParkProperties properties, String index) {
		properties.setWorkOffice(EnvVar.offices.get(buildingsIds.get(index).get(rnd.nextInt(buildingsIds.get(index).size()))));
		properties.setSourceNode(EnvVar.sourceNodes.get(sourceNodesIds.get(index).get(rnd.nextInt(sourceNodesIds.get(index).size()))));
	}
	
	protected void fillBlockByUtility(Integer id, ParkProperties properties) {
		for(Entry<String, List<String>> idsUtility : idsBlockPerUtility.entrySet()) {
			if(properties.getUtility().getProfileName().equals(idsUtility.getKey())) {
				for(String idBlock : idsUtility.getValue()) {
					if(currentAgentsPerBlock.get(idBlock) < maxAgentsPerIds.get(idBlock)) {
						fillBlockWith(id, properties, idBlock);
						currentAgentsPerBlock.replace(idBlock, currentAgentsPerBlock.get(idBlock) + 1);
						break;
					}
				}
			}
		}
	}
	
	@Override
	public MicroAgent createAnAgentWithID(int id, Geography<Object> geography, SourceNode sourceNode) {
		Random rnd = new Random();
		MicroAgentBody body = (MicroAgentBody) EnvVar.bodiesStock.poll();
		body.setAgentID(id);
		LocalTime begin = new LocalTime(ClockSingleton.getInstance().getDateTime().getHourOfDay(), 0);
		LocalTime end = new LocalTime((ClockSingleton.getInstance().getDateTime().getHourOfDay() + (rnd.nextInt(7 + 1 - 5) + 5))%24, 0);
		ParkProperties properties = new ParkProperties(confiantBehaviorFile, begin, end);
		properties.setWorkOffice(EnvVar.offices.get(rnd.nextInt(EnvVar.offices.size())));

		properties.setWorldObject("endNode", EnvVar.nodes.get(properties.getWorkOffice().getClosestNodeId()));

		properties.setUtility(setUtility(rnd.nextInt(100)));
		return new MicroAgent(id, body, new TravelCity(), properties, new CommuterPerception());
	}
	
	protected Perimeter loadPerimeter(int indexOfPolicyAgent) {
		List<Structure> structures = new ArrayList<>();
		if(indexOfPolicyAgent == -1) {
			//No policyAgent specified
			for(int i = 0; i < EnvVar.blockfaces.size(); i++){
				if(!EnvVar.blockfaces.get(i).isFailed()){
					structures.add(EnvVar.blockfaces.get(i));
					EnvVar.structuresGlobal.add(EnvVar.blockfaces.get(i));
				}
			}
		} else {
			String perimeterFileName = "policyagent_" + indexOfPolicyAgent + "_actions.txt";
			List<String> lines = FilesManagement.readFile(FilePath.currentLocalLearnerFolder, perimeterFileName);
			for(String element : lines.get(0).split("\\)")[1].split(";")) {
				int structureID = Integer.valueOf(element.split(":")[0]);
				for(int i = 0; i < EnvVar.blockfaces.size(); i++) {
					if(EnvVar.blockfaces.get(i).getId() == structureID) {
						structures.add(EnvVar.blockfaces.get(i));
						EnvVar.structuresGlobal.add(EnvVar.blockfaces.get(i));
						break;
					}
				}
			}
		}
		
		return new Perimeter(structures);
	}
	
	@Override
	public Context<Object> loadWorld(Context<Object> context){

		createFolder();
		startServer();
		//Loading display
		GeographyParameters<Object> geoParams = new GeographyParameters<>();
		Geography<Object> geography = GeographyFactoryFinder.createGeographyFactory(null)
				.createGeography("geography", context, geoParams);

		//Clock Singleton creation
		ClockSingleton clock = ClockSingleton.getInstance();
		context.add(clock);
		
		//Instantiate Agents
		//beginTime = System.currentTimeMillis();
		EnvVar.AGENT_MAX = Integer.parseInt(EnvVar.configFile.get("agent_number"));//(Integer) params.getValue("commuter_count");
		
		/**
		 * Main folder for the basic split example
		 */
		specificGISFolder = FileName.GIS_BASIC_MERGE;
		
		additionnalInfo = new ArrayList<>();
		
		scenarioID = Integer.parseInt(EnvVar.configFile.get("scenarioID"));
		String subfolderGIS = loadGISFile();
		
		specificGISFolder += subfolderGIS;
		additionnalInfo.add("GISsubfolder: " + subfolderGIS);

		loadFeatures(geography);
		
		createScenarioSpecificItems();

		//Create Graph
		Graph roadGraph = createGraph();
		setCorrectBlockfaces();
		correctSpotsToKDTree(EnvVar.correctSpots);
		
		EnvVar.init();

		populateContext(context, geography, addElementToContext());
		
		policyAgentsCreation();		
		
		loadUtilityFiles();
		loadUtility();
		population = createPopulation();

		//Give context to source and sink nodes
		KDTree kdtreeForDespawnNode = new KDTree(2); //Create kdtree to have a sink node for a source node

		for(int i = 0; i < EnvVar.sourceNodes.size(); i++){
			if(EnvVar.sourceNodes.get(i)!=null){
				EnvVar.sourceNodes.get(i).setContext(context);
				try {
					EnvVar.sourceNodes.get(i).setClosestDespawnNode((SinkNode)kdtreeForDespawnNode.nearest(EnvVar.sourceNodes.get(i).getCoordsInTable()));
				} catch (IllegalArgumentException e){
					//no closest despawn node
				}
			}
		}
		for(int i = 0; i < EnvVar.sinkNodes.size(); i++){
			if(EnvVar.sinkNodes.get(i)!=null){
				EnvVar.sinkNodes.get(i).setContext(context);
			}
		}

		//Add buildings on the graph
		long beginTime = System.currentTimeMillis();
		setClosestNodeIdForBuildingList(EnvVar.homes, roadGraph);
		setClosestNodeIdForBuildingList(EnvVar.offices, roadGraph);	
		System.out.println("Time to process buildings for graph construction: " + (System.currentTimeMillis() - beginTime) + "ms.");

		//Create specific KDTree for closest road with parking
		closestRoadWithSpot(EnvVar.edgesWithSpots);

		//Create OrientedGraph
		beginTime = System.currentTimeMillis();
		OrientedGraph orientedGraph = new OrientedGraph();
		System.out.println("Time to create an oriented graph: " + (System.currentTimeMillis() - beginTime) + "ms.");
		beginTime = System.currentTimeMillis();
		OrientedGraphSingleton.getInstance().setGraph(orientedGraph);
		System.out.println("Time to instanciate oriented graph singleton: " + (System.currentTimeMillis() - beginTime) + "ms.");

	

		//Read ZipCodes
		synPopData = FilesManagement.parseCVS(FilePath.synPopFolder + "zipLA.csv", "", ",");
		createBodies(geography, EnvVar.carMover, EnvVar.carDriverSensor);
		createAgents();

		new TravelCity(); //Create TravelCity singleton
		createManager(context);
		if(EnvVar.configFile.get("initNN").equals("0")) {
			EnvVar.manager.setRecentlyReset(true);
		}
		return context;
	}
	
	public void parseScenarioStructures(String file) {
		JsonParser parser = new JsonParser();
		try {
			JsonObject structuresObject = (JsonObject) parser.parse(new FileReader(file));
			for(Entry<String, JsonElement> element : structuresObject.entrySet()) {
				JsonArray scenario = element.getValue().getAsJsonObject().getAsJsonArray("ScenarioID");
				for(int i = 0; i < scenario.size(); i++) {
					if(scenarioID == scenario.get(i).getAsInt()) {
						JsonObject blocks = element.getValue().getAsJsonObject().getAsJsonObject("blocks");
						for(Entry<String, JsonElement> block : blocks.entrySet()) {
							JsonArray buildingsArray = block.getValue().getAsJsonObject().getAsJsonObject("Structures").getAsJsonArray("buildings");
							buildingsIds.put(block.getKey(), addElementsOfArray(buildingsArray));
							JsonArray sourceNodesArray = block.getValue().getAsJsonObject().getAsJsonObject("Structures").getAsJsonArray("sourcenodes");
							sourceNodesIds.put(block.getKey(), addElementsOfArray(sourceNodesArray));
							maxAgentsPerIds.put(block.getKey(), block.getValue().getAsJsonObject().getAsJsonObject("Info").getAsJsonPrimitive("max").getAsInt());
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Error in parsing scenario structures: ");
			e.printStackTrace();
		}
	}
	
	protected List<Integer> addElementsOfArray(JsonArray array) {
		List<Integer> list = new ArrayList<>();
		for(int j = 0; j < array.size(); j++) {
			list.add(array.get(j).getAsInt());
		}
		return list;
	}
	
	public void parseUtility(String file) {
		JsonParser parser = new JsonParser();
		try {
			JsonObject object = (JsonObject) parser.parse(new FileReader(file));
			for(Entry<String, JsonElement> element : object.entrySet()) {
				JsonArray scenario = element.getValue().getAsJsonObject().getAsJsonArray("ScenarioID");
				for(int i = 0; i < scenario.size(); i++) {
					if(scenarioID == scenario.get(i).getAsInt()) {
						JsonObject utilities = element.getValue().getAsJsonObject().getAsJsonObject("Utility");
						for(Entry<String, JsonElement> utility : utilities.entrySet()) {
							List<String> utilityList = new ArrayList<>();
							for(int j = 0; j < utility.getValue().getAsJsonArray().size(); j++) {
								utilityList.add(utility.getValue().getAsJsonArray().get(j).getAsString());
							}
							idsBlockPerUtility.put(utility.getKey(), utilityList);
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Error in parsing utility file: ");
			e.printStackTrace();
		}
	}
	
	public Map<String, List<String>> parsePerimeter(String file) {
		Map<String, List<String>> structuresIDsPerID = new HashMap<>();
		JsonParser parser = new JsonParser();
		try {
			JsonObject structuresObject = (JsonObject) parser.parse(new FileReader(file));
			for(Entry<String, JsonElement> element : structuresObject.entrySet()) {
				JsonArray scenario = element.getValue().getAsJsonObject().getAsJsonArray("ScenarioID");
				for(int i = 0; i < scenario.size(); i++) {
					if(scenarioID == scenario.get(i).getAsInt()) {
						JsonObject perimeters = element.getValue().getAsJsonObject().getAsJsonObject("Perimeters");
						for(Entry<String, JsonElement> perimeter : perimeters.entrySet()) {
							List<String> perimeterList = new ArrayList<>();
							JsonArray perimeterArray = perimeter.getValue().getAsJsonObject().get("blockface").getAsJsonArray();
							for(int j = 0; j < perimeterArray.size(); j++) {
								perimeterList.add(perimeterArray.get(j).getAsString());
							}
							structuresIDsPerID.put(perimeter.getKey(), perimeterList);
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Error in parsing perimeter for policy agents: ");
			e.printStackTrace();
		}
		
		return structuresIDsPerID;
	}
	
	protected void computeMaxAgents() {
		int sumAgents = 0;
		for(Entry<String, Integer> entry : maxAgentsPerIds.entrySet()) {
			currentAgentsPerBlock.put(entry.getKey(), 0);
			sumAgents += entry.getValue();
		}
		EnvVar.AGENT_MAX = sumAgents;
	}
	
	protected void computeAgentsPerUtility() {
		for(Entry<String, List<String>> idsPerUtility : idsBlockPerUtility.entrySet()) {
			currentAgentsPerUtility.put(idsPerUtility.getKey(), 0);
			int sumOfAgents = 0;
			for(String idBlock : idsPerUtility.getValue()) {
				sumOfAgents += maxAgentsPerIds.get(idBlock);
			}
			maxAgentsPerUtility.put(idsPerUtility.getKey(), sumOfAgents);
		}
	}
	
	protected void parseAgentFileAfterLearning(String file) {
		JsonParser parser = new JsonParser();
		try {
			JsonObject object = (JsonObject) parser.parse(new FileReader(file));
			for(Entry<String, JsonElement> policyAgent : object.entrySet()) {
				if(!policyAgent.getKey().equals("Info")) {
					String id = policyAgent.getValue().getAsJsonObject().get("ID").getAsString();
					
					JsonArray specialActionsJA = policyAgent.getValue().getAsJsonObject().getAsJsonArray("special_actions");
					List<PolicyAction> specialActions = new ArrayList<>();
					for(int i = 0; i < specialActionsJA.size(); i++) {
						specialActions.add(PolicyAction.valueOf(specialActionsJA.get(i).getAsString()));
					}
					
					JsonArray actionsJA = policyAgent.getValue().getAsJsonObject().getAsJsonArray("actions");
					List<PolicyAction> actions = new ArrayList<>();
					for(int i = 0; i < actionsJA.size(); i++) {
						actions.add(PolicyAction.valueOf(actionsJA.get(i).getAsString()));
					}
					
					JsonObject perimeterJO = policyAgent.getValue().getAsJsonObject().getAsJsonObject("perimeter");
					Map<String, List<Structure>> structuresByName = new HashMap<>();
					for(Entry<String, JsonElement> element : perimeterJO.entrySet()) {
						Structure structure = null;
						for(int i = 0; i < EnvVar.blockfaces.size(); i++) {
							if(EnvVar.blockfaces.get(i).getId() == Integer.valueOf(element.getKey())) {
								structure = EnvVar.blockfaces.get(i);
								EnvVar.structuresGlobal.add(EnvVar.blockfaces.get(i));
								break;
							}
						}
						if(structure == null) {
							System.out.println("Error while searching structure.");
						}
						if(!structuresByName.containsKey(element.getValue().getAsString())) {
							List<Structure> structures = new ArrayList<>();
							structures.add(structure);
							structuresByName.put(element.getValue().getAsString(), structures);
						} else {
							List<Structure> structures = structuresByName.get(element.getValue().getAsString());
							structures.add(structure);
							structuresByName.put(element.getValue().getAsString(), structures);
						}
					}
					Perimeter perimeter = new Perimeter(structuresByName);

					EnvVar.policyAgents.add(new PolicyAgent(id, perimeter, actions, specialActions, 1));
				}
			}
		} catch (Exception e) {
			System.out.println("Error in parsing perimeter for policy agents: ");
			e.printStackTrace();
		}
	}

}
