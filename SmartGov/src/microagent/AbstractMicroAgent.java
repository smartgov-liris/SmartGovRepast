package microagent;

import repast.simphony.engine.schedule.ScheduledMethod;

/**
 * Abstract class of the micro agent.
 * @author Simon Pageaud
 *
 */
public abstract class AbstractMicroAgent<B extends AbstractMicroAgentBody> {
	
	protected int id;
	protected B body;
	
	public AbstractMicroAgent(int id, B body){
		this.id = id;
	}
	
	public AbstractMicroAgent(int id){
		this.id = id;
	}

	/**
	 * Live method, called every tick. All the agent activity is in here.
	 */
	@ScheduledMethod(start = 1, interval = 1)
	public abstract void live();
	
	public B getBody(){
		return body;
	}
	
	public void setBody(B body) {
		this.body = body;
	}
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public abstract void recycleAgent(int id);
}
