package environment.graph;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.gis.Geography;
import smartGov.OrientedGraphSingleton;

import com.vividsolutions.jts.geom.Coordinate;

import environment.MicroAgentBody;
import environment.city.EnvVar;
import environment.style.TextureLibrary;
import microagent.MicroAgent;
import microagent.properties.Properties;

public class SourceNode extends Node {

	private Context<Object> context;
	private SinkNode closestDespawnNode;

	public SourceNode(Geography<Object> geography, String id,
			Coordinate coordinate) {
		super(geography, id, coordinate);
		this.texture = TextureLibrary.spawnTexture;
	}

	public SourceNode(Geography<Object> geography, String id,
			Coordinate coordinate, Context<Object> context) {
		this(geography, id, coordinate);
		this.context = context;
	}

	@ScheduledMethod(start = 1, interval = 2)
	public void live(){
		regularBehavior();
	}
	
	private void regularBehavior(){
		if(!EnvVar.agentStockIdT.get(Integer.parseInt(getId())).isEmpty()){
			int id = EnvVar.agentStockIdT.get(Integer.parseInt(getId())).poll();
		
			MicroAgent agent = (MicroAgent) EnvVar.agents.get(id); //Should give a bodyless agent
			if(agent == null){
				agent = EnvVar.scenario.createAnAgentWithID(id, geography, this);
				EnvVar.agents.set(id, agent);
			} else {
				if(agent.getBody() == null){
					agent.setBody((MicroAgentBody) EnvVar.bodiesStock.poll());
					agent.getBody().setAgentID(id);
				}
			}
			agent.getBody().setTexture(TextureLibrary.agentBodyTexture);
			agent.getBody().updatePlan(OrientedGraphSingleton.getInstance().getGraph().shortestPath(this, (Node) ((Properties) agent.getProperties()).getWorldObjects().get("endNode")));
			agent.getBody().setPosition(this.getPosition());
			
			context.add(agent.getBody());
			geography.move(agent.getBody(), EnvVar.GEOFACTORY.createPoint(agent.getBody().getPosition()));
			context.add(agent);
		}
	}

	public void setContext(Context<Object> context) {
		this.context = context;
	}

	public void setClosestDespawnNode(SinkNode closestDespawnNode) {
		this.closestDespawnNode = closestDespawnNode;
	}

	public SinkNode getClosestDespawnNode() {
		return closestDespawnNode;
	}

}