package simulation.scenario;

import environment.MicroAgentBody;
import environment.city.EnvVar;
import environment.graph.Graph;
import environment.graph.OrientedGraph;
import environment.graph.SinkNode;
import microagent.AbstractMover;
import microagent.AbstractSensor;
import microagent.actuator.GippsSteering;
import microagent.simulation.Plan;
import net.sf.javaml.core.kdtree.KDTree;
import repast.simphony.context.Context;
import repast.simphony.space.gis.Geography;
import smartGov.OrientedGraphSingleton;

/**
 * Minimum working code required to start an instance of the lower layer
 * of SmartGov.
 * @author Simon
 *
 */
public abstract class ScenarioLowerLayer extends ScenarioBasic {
	
	protected Graph createGraph() {
		Graph roadGraph = new Graph(EnvVar.nodes, EnvVar.arcs);
		roadGraph.addParkingToRoad(EnvVar.parkingSpots, EnvVar.edgesOSM, EnvVar.nodes);
		return roadGraph;
	}
	
	public void createOrientedGraph() {
		long beginTime = System.currentTimeMillis();
		OrientedGraph orientedGraph = new OrientedGraph();
		System.out.println("Time to create an oriented graph: " + (System.currentTimeMillis() - beginTime) + "ms.");
		beginTime = System.currentTimeMillis();
		OrientedGraphSingleton.getInstance().setGraph(orientedGraph);
		System.out.println("Time to instanciate oriented graph singleton: " + (System.currentTimeMillis() - beginTime) + "ms.");
	}
	
	public void createSourceAndSinkNodes(Context<Object> context) {
		KDTree kdtreeForDespawnNode = new KDTree(2); //Create kdtree to have a sink node for a source node
		
		for(int i = 0; i < EnvVar.sinkNodes.size(); i++){
			if(EnvVar.sinkNodes.get(i)!=null){
				EnvVar.sinkNodes.get(i).setContext(context);
				kdtreeForDespawnNode.insert(EnvVar.sinkNodes.get(i).getPositionInDouble(), EnvVar.sinkNodes.get(i));
			}
		}
		
		for(int i = 0; i < EnvVar.sourceNodes.size(); i++){
			if(EnvVar.sourceNodes.get(i)!=null){
				for(int j = 0; j < EnvVar.roads.size(); j++) {
					if(EnvVar.roads.get(j).getNodesId().contains(EnvVar.sourceNodes.get(i).getId())){
						EnvVar.sourceNodes.get(i).getOutcomingArcs().get(0).setRoad(EnvVar.roads.get(j));
						break;
					}
				}
				
				EnvVar.sourceNodes.get(i).setContext(context);
				try {
					EnvVar.sourceNodes.get(i).setClosestDespawnNode((SinkNode)kdtreeForDespawnNode.nearest(EnvVar.sourceNodes.get(i).getCoordsInTable()));
				} catch (IllegalArgumentException e){
					e.printStackTrace();
					//no closest despawn node
				}
			}
		}
	}
	
	public abstract void createAgents();
	
	public void createBodies(Geography<Object> geography, AbstractMover<Plan> mover, AbstractSensor<MicroAgentBody> sensor) {
		long beginTime = System.currentTimeMillis();
		for(int i = 0; i < EnvVar.AGENT_MAX; i++) {
			MicroAgentBody body = new MicroAgentBody(i, i+"", geography, sensor, mover, new GippsSteering(1.0,1.5,-3.0));
			EnvVar.bodies.add(body);
			EnvVar.bodiesStock.add(body);
		}
		System.out.println("Time to create " + EnvVar.AGENT_MAX + " bodies: " + (System.currentTimeMillis() - beginTime) + "ms. Total bodies: " + EnvVar.bodies.size() + ". Total remaining bodies: " + EnvVar.bodiesStock.size() + ".");
	}
	
}
