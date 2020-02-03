package simulation.scenario.lowlayer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.joda.time.LocalTime;

import policyagent.Perimeter;
import policyagent.PolicyAction;
import policyagent.PolicyAgent;
import net.sf.javaml.core.kdtree.KDTree;
import repast.simphony.context.Context;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import simulation.FileName;
import simulation.FilePath;
import simulation.manager.ManagerQLearningScenario;
import simulation.parser.FilesManagement;
import simulation.parser.JSONWriter;
import simulation.scenario.ScenarioSmartGov;
import simulation.socket.ClientCommunication;
import simulation.socket.Server;
import smartGov.ClockSingleton;
import smartGov.OrientedGraphSingleton;
import environment.MicroAgentBody;
import environment.Structure;
import environment.city.Building;
import environment.city.EnvVar;
import environment.city.parking.BlockFace;
import environment.city.parking.ParkingSpot;
import environment.graph.Arc;
import environment.graph.Graph;
import environment.graph.Node;
import environment.graph.OrientedGraph;
import environment.graph.SinkNode;
import environment.graph.EdgeOSM;
import environment.graph.SourceNode;
import microagent.MicroAgent;
import microagent.behavior.TravelCity;
import microagent.perception.CommuterPerception;
import microagent.properties.ParkProperties;
import microagent.utility.ParkUtility;

public class ScenarioSimonLA extends ScenarioSmartGov {
	
	private ParkUtility regularUtility;
	private ParkUtility lowIncomeUtility;
	private ParkUtility sustainableUtility;
	private ParkUtility hurryUtility;
	private ParkUtility lateUtility;
	
	private double proba_lateUtility;
	private double proba_sustainableUtility;
	private double proba_lowIncomeUtility;
	private double proba_hurryUtility;
	private double proba_regularUtility;
	
	private double probaLate;
	private double probaSust;
	private double probaLowI;
	private double probaHurr;
	private double probaRegu; //Should always be equal to 100
	
	private String population;

	private List<String[]> synPopData;

	public static String confiantBehaviorFile = FilePath.behaviorFolder + "Confiant.txt";
	public static String prudentBehaviorFile  = FilePath.behaviorFolder + "Prudent.txt";

	public ScenarioSimonLA(){

	}

	@Override
	public Context<Object> loadWorld(Context<Object> context){

		createFolder();
		startServer();
		FilePath.currentAgentDetailsFolder = FilePath.humanAgentFolder + "scenario" + File.separator + 
				EnvVar.configFile.get("scenario") + File.separator;
		//Loading display
		GeographyParameters<Object> geoParams = new GeographyParameters<>();
		Geography<Object> geography = GeographyFactoryFinder.createGeographyFactory(null)
				.createGeography("geography", context, geoParams);

		//Load simulation parameters
		RunEnvironment.getInstance().getParameters();

		//Clock Singleton creation
		ClockSingleton clock = ClockSingleton.getInstance();
		context.add(clock);
		
		specificGISFolder = "";

		loadFeatures(geography);

		Graph roadGraph = createGraph();
		setCorrectBlockfaces();
		correctSpotsToKDTree(EnvVar.correctSpots);
		
		EnvVar.init();

		populateContext(context, geography, addElementToContext());
		
		createPolicyAgent();
		
		regularUtility     = new ParkUtility(EnvVar.indicatorsFiles,      "Regular", FilePath.areaCoefFolder+"area.json");
		lowIncomeUtility   = new ParkUtility(EnvVar.indicatorsFiles,   "Low_Income", FilePath.areaCoefFolder+"area.json");
		sustainableUtility = new ParkUtility(EnvVar.indicatorsFiles, "Eco-Friendly", FilePath.areaCoefFolder+"area.json");
		hurryUtility       = new ParkUtility(EnvVar.indicatorsFiles,    "Impatient", FilePath.areaCoefFolder+"area.json");
		lateUtility        = new ParkUtility(EnvVar.indicatorsFiles,      "Hurried", FilePath.areaCoefFolder+"area.json");
		
		proba_lateUtility = Integer.valueOf(EnvVar.configFile.get("proba_lateUtility"));
		proba_sustainableUtility = Integer.valueOf(EnvVar.configFile.get("proba_sustainableUtility"));
		proba_lowIncomeUtility = Integer.valueOf(EnvVar.configFile.get("proba_lowIncomeUtility"));
		proba_hurryUtility = Integer.valueOf(EnvVar.configFile.get("proba_hurryUtility"));
		proba_regularUtility = Integer.valueOf(EnvVar.configFile.get("proba_regularUtility"));
		probaLate = proba_lateUtility;
		probaSust = probaLate + proba_sustainableUtility;
		probaLowI = probaSust + proba_lowIncomeUtility;
		probaHurr = probaLowI + proba_hurryUtility;
		probaRegu = probaHurr + proba_regularUtility;
		population = (int) proba_lateUtility + "-" +
				(int) proba_sustainableUtility + "-" + 
				(int) proba_lowIncomeUtility + "-" +
				(int) proba_hurryUtility + "-" +
				(int) proba_regularUtility;

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
		
		//Instantiate Agents
		beginTime = System.currentTimeMillis();
		EnvVar.AGENT_MAX = Integer.parseInt(EnvVar.configFile.get("agent_number"));//(Integer) params.getValue("commuter_count");
		
		//Read ZipCodes
		synPopData = FilesManagement.parseCVS(FilePath.synPopFolder + "zipLA.csv", "", ",");
		createAgents();

		new TravelCity();
		createManager(context);
		
		/*/
		for(Node node : EnvVar.nodes) {
			System.out.println("Node " + node.getId() + ", " + node.getClass().getSimpleName());
			for(Arc arc : node.getOutcomingArcs()) {
				System.out.println("Arc " + arc.getId() + ", target : " + arc.getTargetNode().getId());
			}
		}
		//*/
		return context;
	}

	private void setClosestNodeIdForBuildingList(List<? extends Building> buildings, Graph graph){
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

	private void correctSpotsToKDTree(List<ParkingSpot> spots){
		long beginTime = System.currentTimeMillis();
		for(int i = 0; i < spots.size(); i++){
			EnvVar.kdtreeWithSpots.insert(spots.get(i).getPositionInDouble(), spots.get(i));
		}
		System.out.println("Time to allocate spots to KDTree: " + (System.currentTimeMillis() - beginTime) + "ms.");
	}

	private void setCorrectBlockfaces(){
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

	@SuppressWarnings("unused")
	private void setAllBlockFaceToOccupied(){
		for(int i = 0; i < EnvVar.blockfaces.size(); i++){
			BlockFace blockface = EnvVar.blockfaces.get(i);
			for(int j = 0; j < blockface.getParkingSpots().size(); j++){
				blockface.getParkingSpots().get(j).setOccupied(true);
			}
		}
	}
	
	public MicroAgent createAgentWithoutBodyWithID(int id){
		Random rnd = new Random();
		//int integerId = Integer.valueOf(id);
		LocalTime begin = new LocalTime(ClockSingleton.getInstance().getDateTime().getHourOfDay(), 0);
		LocalTime end = new LocalTime((ClockSingleton.getInstance().getDateTime().getHourOfDay() + (rnd.nextInt(7 + 1 - 5) + 5))%24, 0);
		ParkProperties properties = new ParkProperties(confiantBehaviorFile, begin, end);
		properties.setWorkOffice(EnvVar.offices.get(rnd.nextInt(EnvVar.offices.size())));
		//properties.setEndNode(EnvVar.nodes.get(properties.getWorkOffice().getClosestNodeId()));
		properties.setWorldObject("endNode", EnvVar.nodes.get(properties.getWorkOffice().getClosestNodeId()));

		properties.setSourceNode(EnvVar.sourceNodes.get(Integer.valueOf(EnvVar.idOfSourceNodes.get(rnd.nextInt(EnvVar.idOfSourceNodes.size())))));
		//System.out.println(properties.getSourceNode());
		String[] info = synPopData.get(rnd.nextInt(synPopData.size()-1)+1);
		properties.getInfos().put("ZipCode", info[0]);
		//https://stackoverflow.com/questions/343584/how-do-i-get-whole-and-fractional-parts-from-double-in-jsp-java
		double timeInSeconds = (Double.parseDouble(info[1]) % 1) * 60 + 60 * (Double.parseDouble(info[1]) - (Double.parseDouble(info[1]) % 1));
		properties.getInfos().put("AverageCommuteTime", timeInSeconds);
		
		//Set their initial state probability
		//A specific number of these agents will always take the bus and some wille always take the car. The other updates probabilities
		//https://www.moneycrashers.com/worst-us-cities-traffic-commute-time/
		//if(rnd.nextDouble() > 0.842){
		//	System.out.println("Agent " + id + " takes the bus");
		//	properties.setProbaStates("bus");
		//} else {
			properties.setProbaStates("");
		//}
		if(properties.getCurrentState() == 0){
			//EnvVar.agentStockIdT.get(Integer.valueOf(((ParkProperties)((Agent)EnvVar.agents.get(integerId)).getProperties()).getSourceNode().getId())).add(integerId+"");
			//EnvVar.agentStockIdT.get(Integer.valueOf(properties.getSourceNode().getId())).add(integerId+"");
		}
		
		properties.setUtility(setUtility(rnd.nextInt(100)));
		return new MicroAgent(id, new TravelCity(), properties, new CommuterPerception());
	}

	@Override
	public MicroAgent createAnAgentWithID(int id, Geography<Object> geography, SourceNode sourceNode){
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
	
	private ParkUtility setUtility(int value){
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

	public String getPopulation() {
		return population;
	}
	
	public List<List<?>> addElementToContext(){
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
	
	public void storeBlockFaces(String file){
		String pathFile = new String("input\\" + file);
		List<String> lines = new ArrayList<>();
		for(int i = 0; i < EnvVar.blockfaces.size(); i++){
			if(!EnvVar.blockfaces.get(i).isFailed()){
				lines.add("Blockface " + EnvVar.blockfaces.get(i).getId() + " has the following spots:");
				for(int j = 0; j < EnvVar.blockfaces.get(i).getParkingSpots().size(); j++){
					if(!EnvVar.blockfaces.get(i).getParkingSpots().get(j).isFailed()){
						lines.add("Spot " + EnvVar.blockfaces.get(i).getParkingSpots().get(j).getId());
					}
				}
			}
		}
		try {
			Files.write(Paths.get(pathFile), lines, Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void createPolicyAgent() {
		List<PolicyAction> actions = new ArrayList<>();
		actions.add(PolicyAction.INCREASE_PRICES);
		actions.add(PolicyAction.DECREASE_PRICES);
		actions.add(PolicyAction.DO_NOTHING);		
		//actions.add("add");
		//actions.add("remove");
		List<Structure> structures = new ArrayList<>();
		for(int i = 0; i < EnvVar.blockfaces.size(); i++){
			if(!EnvVar.blockfaces.get(i).isFailed()){
				structures.add(EnvVar.blockfaces.get(i));
				EnvVar.structures.add(EnvVar.blockfaces.get(i));
			}
		}
		
		Perimeter perimeter = new Perimeter(structures);
		
		System.out.println("0.a");
		
		EnvVar.policyAgents.add(new PolicyAgent("0", perimeter, actions));
	}
	
	@Override
	public void createAgents(){
		long beginTime = System.currentTimeMillis();
		for(int i = 0; i < EnvVar.AGENT_MAX; i++){
			EnvVar.agents.add(createAgentWithoutBodyWithID(i));
		}
		//JSONWriter.writeAgents(EnvVar.agents, FilePath.currentAgentDetailsFolder + FileName.AgentFile);
		JSONWriter.writeAgents(EnvVar.agents, FilePath.currentLocalLearnerFolder + FileName.AgentFile);
		System.out.println("Time to create " + EnvVar.AGENT_MAX + " agents: " + (System.currentTimeMillis() - beginTime) + "ms. Total agents: " + EnvVar.agents.size());
	}
	
	public void createManager(Context<Object> context){
		EnvVar.manager = new ManagerQLearningScenario(context);
		context.add(EnvVar.manager);
	}
	
	@Override
	public Map<String, Number> getInfos() {
		Map<String, Number> info = new HashMap<String, Number>();
		info.put("Impatient", proba_lateUtility);
		info.put("Eco-Friendly", proba_sustainableUtility);
		info.put("Low_Income", proba_lowIncomeUtility);
		info.put("Hurried", proba_hurryUtility);
		info.put("Regular", proba_regularUtility);
		info.put("SimulationNumber", Double.valueOf(EnvVar.configFile.get("total_number_of_simulations")));
		info.put("SimulationPerAction", Double.valueOf(EnvVar.configFile.get("simulations_per_action")));
		info.put("SpotNumber",  EnvVar.correctSpots.size());
		return info;
	}
	
	protected void startServer() {
		//create folder for current simulation
		//createFolder();
		FilePath.currentAgentDetailsFolder = FilePath.humanAgentFolder + "scenario//" + 
				EnvVar.configFile.get("scenario") + "//" +
				EnvVar.configFile.get("scenarioID") + "//";
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

}