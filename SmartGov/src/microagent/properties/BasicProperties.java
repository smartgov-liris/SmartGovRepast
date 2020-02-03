package microagent.properties;

import java.util.HashMap;
import java.util.Map;

import environment.graph.Node;

public class BasicProperties extends Properties {
	
	public BasicProperties() {
		init();
	}
	
	public BasicProperties(Node begin, Node end) {
		this();
		worldObjects.put("beginNode", begin);
		worldObjects.put("endNode", end);
	}
	
	@Override
	protected void init() {
		properties   = new HashMap<>();
		worldObjects = new HashMap<>();
		worldObjects.put("beginNode", null);
		worldObjects.put("endNode", null);
	}

	@Override
	public void resetProperties(int id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, Object> getAttributesOfAgent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double computeScore(Object objectToScore) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double computeSatisfaction(Object objectToScore) {
		// TODO Auto-generated method stub
		return 0;
	}

}
