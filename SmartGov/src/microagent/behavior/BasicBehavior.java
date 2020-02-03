package microagent.behavior;

import java.util.List;
import java.util.Random;

import environment.MicroAgentBody;
import environment.MicroAgentAction;
import environment.graph.Arc;
import environment.graph.Node;
import environment.style.TextureLibrary;
import microagent.AbstractMicroAgentBody;
import microagent.AbstractBehavior;
import microagent.AbstractPerception;
import microagent.AbstractProperties;
import microagent.perception.CommuterPerception;
import microagent.properties.BasicProperties;

public class BasicBehavior extends AbstractBehavior<CommuterPerception, BasicProperties, MicroAgentBody>{

	@Override
	public MicroAgentAction provideAction(int id, AbstractPerception perceptions, AbstractProperties properties,
			AbstractMicroAgentBody body) {
		return doAction((CommuterPerception) perceptions, (BasicProperties) properties, (MicroAgentBody) body);
	}
	
	private MicroAgentAction doAction(CommuterPerception perceptions, BasicProperties properties, MicroAgentBody body){
		updateAgentSpeed(perceptions, body, 40.0);
		
		if(body.getPlan().isPathComplete()){
			Node currentNode = body.getPlan().getLastNode();
			List<Arc> availableArcs = currentNode.getOutcomingArcs();
			int index = -1;
			if(availableArcs.size() == 1){
				index = 0;
			} else {
				if(availableArcs.size() == 0) {
					body.setTexture(TextureLibrary.agentBodyParked);
					return MicroAgentAction.IDLE;
				}
				Random rnd = new Random();
				index = rnd.nextInt(availableArcs.size());
			}
			body.getPlan().addANode(availableArcs.get(index).getTargetNode());
		}
		return MicroAgentAction.MOVE;

	}

	@Override
	protected void init() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInitialState() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setToFinalState() {
		// TODO Auto-generated method stub
		
	}
	
	private void updateAgentSpeed(CommuterPerception perceptions, MicroAgentBody body, double maxSpeed){
		body.setSpeed(body.getSteering().getSpeed(body.getSpeed(), maxSpeed, body.getPlan().getCurrentNode(), 0.0, 7.5, -3, body.getPosition()));
		
	}

}
