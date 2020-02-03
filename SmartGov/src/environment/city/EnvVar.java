package environment.city;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;

import policyagent.PolicyAgent;
import policyagent.inneragent.InnerAgent;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import simulation.FilePath;
import simulation.manager.AbstractManager;
import simulation.scenario.ScenarioBasic;
import simulation.scenario.learningXP.ScenarioBasicLearning;
import simulation.scenario.learningXP.ScenarioBasicMerge;
import simulation.scenario.learningXP.ScenarioBasicSplit;
import simulation.scenario.learningXP.ScenarioIQN;
import simulation.scenario.learningXP.ScenarioSplitMerge;
import simulation.scenario.learningXP.ScenarioSplitMergeRandom;
import simulation.scenario.lowlayer.ScenarioEnvironment;
import simulation.scenario.lowlayer.ScenarioSimonLA;
import simulation.scenario.lowlayer.ScenarioVisualization;
import net.sf.javaml.core.kdtree.KDTree;

import com.vividsolutions.jts.geom.GeometryFactory;

import environment.Structure;
import environment.city.parking.BlockFace;
import environment.city.parking.ParkingSpot;
import environment.graph.Arc;
import environment.graph.Node;
import environment.graph.Road;
import environment.graph.SinkNode;
import environment.graph.EdgeOSM;
import environment.graph.SourceNode;
import microagent.AbstractMicroAgent;
import microagent.AbstractMicroAgentBody;
import microagent.actuator.CarMover;
import microagent.behavior.TravelCity;
import microagent.perception.CarDriverSensor;

/**
 * This class stores every global variables of the simulation. Due to Repast Simphony management of objects during simulation it is required to 
 * have a pool of available objects that are cleaned between simulations.
 * @author Simon Pageaud
 *
 */
public class EnvVar {
	
	//Scenario
	public static ScenarioBasic scenario;

	//Repast Static Variables
	public static final GeometryFactory GEOFACTORY = new GeometryFactory();
	public static final Parameters params = RunEnvironment.getInstance().getParameters();
	
	//Indicator's name
	public static final String SEARCHING_TIME = "Searching_Time";
	public static final String DISTANCE_FROM_WORK = "DistanceWork";
	public static final String OCCUPATION = "Occupation";
	public static final String PRICE = "Price";
	
	//Context elements
	public static List<Building> buildings = new ArrayList<>();
	public static List<Home> homes = new ArrayList<>();
	public static List<WorkOffice> offices = new ArrayList<>();
	
	public static List<AbstractMicroAgent<?>> agents = new ArrayList<>(); //Agents are ordered by their id
	public static List<AbstractMicroAgentBody> bodies = new ArrayList<>();
	
	public static List<ParkingSpot> parkingSpots = new ArrayList<>();
	public static List<ParkingSpot> correctSpots = new ArrayList<>(); //Spots with isFailed = false
	public static List<Road> roads = new ArrayList<>();
	public static List<Node> nodes = new ArrayList<>();
	public static List<Arc> arcs = new ArrayList<>();
	public static List<EdgeOSM> edgesOSM = new ArrayList<>();
	public static List<BlockFace> blockfaces = new ArrayList<>();
	public static List<SourceNode> sourceNodes = new ArrayList<>();
	public static List<SinkNode> sinkNodes = new ArrayList<>();
	public static List<EdgeOSM> edgesWithSpots = new ArrayList<>();
	public static List<Structure> structures = new ArrayList<>();
	
	//Policy Agent
	public static List<List<String>> idOfElementsPerGroup = new ArrayList<>();
	public static List<PolicyAgent> policyAgents = new ArrayList<>();
	public static List<InnerAgent> innerAgentsGlobal = new ArrayList<>();
	
	public static List<Structure> structuresGlobal = new ArrayList<>();
	
	public static AbstractManager manager;
	
	public static Map<String, String> configFile = new HashMap<>();
	public static Map<String, String> indicatorsFiles = new HashMap<>();
	
	public static TravelCity.Idle idle = null;
	
	public static CarMover carMover = new CarMover();
	public static CarDriverSensor carDriverSensor = new CarDriverSensor();
	
	public static List<String> idOfSourceNodes = new ArrayList<>();

	//Manage human agent creation and allocation
	public static int AGENT_MAX; //Max agents in the simulation (determine by parameters)
	public static Queue<Integer> agentStockId = new LinkedList<>(); //id of available agents for creation
	public static List<Queue<Integer>> agentStockIdT = new ArrayList<Queue<Integer>>();
	public static Queue<AbstractMicroAgent<?>> agentStock = new LinkedList<>();
	public static Queue<AbstractMicroAgentBody> bodiesStock = new LinkedList<>();
	
	//Manage policy agent creation and allocation
	public static int POLICY_AGENT_MAX; //Max policy agents in the simulation (not specified at the moment)
	public static List<String> policyAgentIDBuffer = new ArrayList<>(); //id of policy agents during merge or split
	public static Queue<Integer> policyAgentStockId = new LinkedList<>(); //id of available policy agents for creation
	public static Map<String, List<Integer>> policyAgentIDMerged = new HashMap<>();
	
	
	//File names
	public static String outputFile;
	public static String stateFile;
	public static String occupationTestFile;
	
	//KDTree for spatial research
	public static KDTree kdtreeArcsWithSpots = new KDTree(2);
	public static KDTree kdtreeWithSpots = new KDTree(2);
	
	public static void init(){
		resetSpecialList();
		for(int i = 0; i < EnvVar.sourceNodes.size(); i++){
			if(EnvVar.sourceNodes.get(i) != null){
				agentStockIdT.add(new LinkedList<Integer>());
				idOfSourceNodes.add(EnvVar.sourceNodes.get(i).getId());
			} else {
				agentStockIdT.add(null);
			}
		}
	}
	
	public static void clear(){
		buildings.clear();
		homes.clear();
		offices.clear();
		agents.clear(); //Agents are ordered by their id
		bodies.clear();
		parkingSpots.clear();
		correctSpots.clear();
		idOfSourceNodes.clear();
		roads.clear();
		nodes.clear();
		arcs.clear();
		edgesOSM.clear();
		blockfaces.clear();
		structures.clear();
		sourceNodes.clear();
		sinkNodes.clear();
		edgesWithSpots.clear();
		policyAgents.clear();
		innerAgentsGlobal.clear();
		structuresGlobal.clear();
		agentStockId.clear();
		agentStock.clear();
		policyAgentIDBuffer.clear();
		policyAgentStockId.clear();
		kdtreeArcsWithSpots = new KDTree(2);
		kdtreeWithSpots = new KDTree(2);
		policyAgentIDMerged.clear();
	}
	
	public static void resetSpecialList(){
		if(agentStockIdT.size() > 0){
			for(int i = 0; i < agentStockIdT.size(); i++){
				if(agentStockIdT.get(i) != null){
					agentStockIdT.get(i).clear();
				}
			}
			agentStockIdT.clear();
		}
	}
	
	public static Map<String, String> parseConfig(String filestr) {
		Map<String, String> config = new HashMap<>();
		File file = new File(filestr);
		Scanner input;
		try {
			input = new Scanner(file);
			
			while(input.hasNext()) {
			    String nextLine = input.nextLine();
			    if(!nextLine.contains("#")){
			    	if(nextLine.contains(",")){
			    		//Indicators
			    		String lines[] = nextLine.split(":");
			    		String indicators[] = lines[1].split(",");
			    		for(int indicatorIndex = 0; indicatorIndex < indicators.length; indicatorIndex ++){
			    			indicatorsFiles.put(indicators[indicatorIndex], FilePath.humanIndicatorFolder + indicators[indicatorIndex] + ".json");
			    		}
			    	} else {
			    		String lines[] = nextLine.split(":");
				    	config.put(lines[0], lines[1]);
			    	}
			    }
			}
			input.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return config;
	}
	
	public static ScenarioBasic loadScenarioNames(String scenarioName) {
		switch (scenarioName) {
			case "ScenarioSimonLA":
				return new ScenarioSimonLA();
			case "ScenarioBasicLearning":
				return new ScenarioBasicLearning();
			case "ScenarioBasicSplit":
				return new ScenarioBasicSplit();
			case "ScenarioBasicMerge":
				return new ScenarioBasicMerge();
			case "ScenarioSplitMerge":
				return new ScenarioSplitMerge();
			case "ScenarioSplitMergeRandom":
				return new ScenarioSplitMergeRandom();
			case "ScenarioIQN":
				return new ScenarioIQN();
			case "Visualization":
				return new ScenarioVisualization();
			case "LowerLayer":
				return new ScenarioEnvironment();
			default:
				return new ScenarioSimonLA();
		}
	}
	
	public static void updatePolicyAgentBuffer(String id) {
		if(!policyAgentIDBuffer.contains(id)) {
			policyAgentIDBuffer.add(id);
		} else {
			policyAgentIDBuffer.remove(id);
		}
	}
	
	/**
	 * Remove policy agent from the list and store its id for the duration of the merge.
	 * @param mergeManager
	 * @param idOfMergedAgent
	 */
	public static void storePolicyAgentIDForMerge(String mergeManager, int idOfMergedAgent) {
		policyAgents.set(idOfMergedAgent, null);
		if(policyAgentIDMerged.containsKey(mergeManager)) {
			policyAgentIDMerged.get(mergeManager).add(idOfMergedAgent);
		} else {
			List<Integer> idsOfMergedAgents = new ArrayList<>();
			idsOfMergedAgents.add(idOfMergedAgent);
			policyAgentIDMerged.put(mergeManager, idsOfMergedAgents);
			
		}
	}
	
	/**
	 * Delete stored IDs for merge.
	 * @param mergeManager
	 */
	public static void clearMergeAgents(String mergeManager) {
		if(policyAgentIDMerged.containsKey(mergeManager)) {
			policyAgentIDMerged.remove(mergeManager);
		}
	}
	
	public static void removePolicyAgentFromList(int index) {
		policyAgents.set(index, null);
	}
	
	public static String requestPolicyAgentID() {
		if(policyAgentStockId.peek()==null) {
			return String.valueOf(policyAgents.size());
		} else {
			return String.valueOf(policyAgentStockId.poll());
		}
	}
	
}
