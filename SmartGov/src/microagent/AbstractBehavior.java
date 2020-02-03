package microagent;

import environment.MicroAgentAction;

/**
 * Provide an action for specified perceptions, properties and utility
 * @author Simon Pageaud
 *
 * @param <T> AbstractPerception
 * @param <W> AbstractProperties
 * @param <U> AbstractUtility
 */
public abstract class AbstractBehavior<T extends AbstractPerception, W extends AbstractProperties, B extends AbstractMicroAgentBody> {

	public abstract MicroAgentAction provideAction(int id, AbstractPerception perceptions, AbstractProperties properties, AbstractMicroAgentBody body);
	
	protected abstract void init();
	
	public abstract void setInitialState();
	
	public abstract void setToFinalState();
	
}
