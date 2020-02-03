package simulation.scenario.lowlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import environment.MicroAgentBody;
import environment.city.EnvVar;
import environment.graph.SourceNode;
import microagent.MicroAgent;
import microagent.behavior.BasicBehavior;
import microagent.perception.CommuterPerception;
import microagent.properties.BasicProperties;
import repast.simphony.context.Context;
import repast.simphony.space.gis.Geography;
import simulation.FileName;
import simulation.FilePath;
import simulation.parser.JSONReader;
import simulation.scenario.ScenarioLowerLayer;

/**
 * Creation of environment and human agents to help visualization of lower layer.
 * @author Simon
 *
 */
public class ScenarioEnvironment extends ScenarioLowerLayer {

	public ScenarioEnvironment() {
		super();
		specificGISFolder = FileName.GIS_ENVIRONMENT;
	}

	@Override
	public MicroAgent createAnAgentWithID(int id, Geography<Object> geography, SourceNode sourceNode) {
		Random rnd = new Random();
		
		MicroAgentBody body = (MicroAgentBody) EnvVar.bodiesStock.poll();
		body.setAgentID(id);
		
		sourceNode.getOutcomingArcs().get(0).getRoad().getAgentId().add(id);
		BasicProperties properties = new BasicProperties(sourceNode, EnvVar.nodes.get(rnd.nextInt(EnvVar.nodes.size())));
		MicroAgent agent = new MicroAgent(id, body, new BasicBehavior(), properties, new CommuterPerception());
		body.setSpeed(30.0);
		return agent;
	}

	@Override
	public Context<Object> loadWorld(Context<Object> context) {
		Geography<Object> geography = loadDisplay(context);
		//parseConfigFile();
		loadFeatures(geography);
		String positionParkingFile = 	FilePath.structuresFolder + "SensorPosition.json";
		String blockfaceFile       = 	FilePath.structuresFolder + "BlockfaceInfo.json";
		JSONReader.parseBlockFaces(geography, blockfaceFile, positionParkingFile);
		
		EnvVar.init();
		populateContext(context, geography, addElementToContext());
		createOrientedGraph();
		createSourceAndSinkNodes(context);
		
		EnvVar.AGENT_MAX = Integer.parseInt(EnvVar.configFile.get("agent_number"));
		createBodies(geography, EnvVar.carMover, EnvVar.carDriverSensor);
		createAgents();
		return context;
	}

	@Override
	public List<List<?>> addElementToContext() {
		List<List<?>> elementsToBeAdded = new ArrayList<>();
		elementsToBeAdded.add(EnvVar.nodes);
		elementsToBeAdded.add(EnvVar.edgesOSM);
		elementsToBeAdded.add(EnvVar.buildings);

		List<Object> spots = new ArrayList<>();
		for(int i = 0; i < EnvVar.parkingSpots.size(); i++){
			if(!EnvVar.parkingSpots.get(i).isFailed()){
				spots.add(EnvVar.parkingSpots.get(i));
			}
		}
		elementsToBeAdded.add(spots);
		
		return elementsToBeAdded;
	}

	@Override
	public void createAgents() {
		Random rnd = new Random();
		for(int i = 0; i < EnvVar.AGENT_MAX; i++){
			int idSourceNode = rnd.nextInt(EnvVar.idOfSourceNodes.size());
			EnvVar.agentStockIdT.get(Integer.valueOf(EnvVar.idOfSourceNodes.get(idSourceNode))).add(i);
			
			SourceNode sourceNode = EnvVar.sourceNodes.get(idSourceNode);
			BasicProperties properties = new BasicProperties(sourceNode, EnvVar.nodes.get(rnd.nextInt(EnvVar.nodes.size())));
			EnvVar.agents.add(new MicroAgent(i, new BasicBehavior(), properties, new CommuterPerception()));
		}
	}
}
