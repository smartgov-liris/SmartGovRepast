package environment.graph;

import java.util.LinkedList;
import java.util.Queue;

import environment.city.EnvVar;
import environment.style.TextureLibrary;
import microagent.AbstractMicroAgent;
import microagent.AbstractMicroAgentBody;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.space.gis.Geography;

import com.vividsolutions.jts.geom.Coordinate;

public class SinkNode extends Node {

	private Context<Object> context;
	private Queue<AbstractMicroAgentBody> agentsOnNode;

	public SinkNode(Geography<Object> geography, String id,
			Coordinate coordinate) {
		super(geography, id, coordinate);
		this.texture = TextureLibrary.sinkTexture;
		this.agentsOnNode = new LinkedList<>();
	}

	public SinkNode(Geography<Object> geography, String id,
			Coordinate coordinate, Context<Object> context) {
		this(geography, id, coordinate);
		this.context = context;
	}

	@ScheduledMethod(start = 1, interval = 1)
	public void live(){
		sinkBehaviorRegular();
	}

	/**
	 * Sink behavior for regular applications
	 */
	private void sinkBehaviorRegular(){
		//Keep agent order by setting leaving agent position in list to null
		while(agentsOnNode.peek() != null){	
			AbstractMicroAgentBody agentBody = agentsOnNode.poll();
			int agentIndex = Integer.valueOf(agentBody.getId());
			((EdgeOSM)this.getIncomingArcs().get(0)).getAgentsInRoad().remove(agentBody);
			AbstractMicroAgent<?> agent = EnvVar.agents.get(agentIndex);
			if(EnvVar.agents.get(agentIndex) != null) {
				if(EnvVar.agents.get(agentIndex).getBody() != null) {
					context.remove(EnvVar.agents.get(agentIndex).getBody());
				}
				context.remove(EnvVar.agents.get(agentIndex));
				agent.setBody(null);
			}
		}
	}

	public void addAgentBody(AbstractMicroAgentBody agentBody){
		if(!agentsOnNode.contains(agentBody)){
			agentsOnNode.add(agentBody);
		}
	}

	public void setContext(Context<Object> context) {
		this.context = context;
	}

}
