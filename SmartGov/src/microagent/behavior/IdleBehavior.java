package microagent.behavior;

import microagent.AbstractMicroAgentBody;
import microagent.AbstractBehavior;
import microagent.AbstractPerception;
import microagent.AbstractProperties;
import microagent.perception.CommuterPerception;
import microagent.properties.ParkProperties;
import environment.MicroAgentAction;
import environment.MicroAgentBody;

/**
 * Static behavior to test special moves
 * @author Simon
 *
 */
public class IdleBehavior extends AbstractBehavior<CommuterPerception, ParkProperties, MicroAgentBody> {

	@Override
	public void init() {
		
	}

	@Override
	public MicroAgentAction provideAction(int id, AbstractPerception perceptions,
			AbstractProperties weights, AbstractMicroAgentBody agentBodyDummy) {
		
		return MicroAgentAction.IDLE;
	}

	@Override
	public void setInitialState() {
		
	}

	@Override
	public void setToFinalState() {
		
	}

}
