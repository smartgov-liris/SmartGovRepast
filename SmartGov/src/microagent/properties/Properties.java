package microagent.properties;

import java.util.HashMap;
import java.util.Map;

import policyagent.Indicator;
import environment.WorldObject;
import environment.city.EnvVar;
import environment.city.Home;
import environment.city.WorkOffice;
import environment.graph.Node;
import microagent.AbstractProperties;

/**
 * General purpose properties for agents.
 * @author Simon Pageaud
 *
 */
public class Properties extends AbstractProperties {

	protected Map<String, Object> properties;
	protected Map<String, WorldObject> worldObjects;
	
	public Properties(){
		init();
	}
	
	@Override
	protected void init() {
		properties   = new HashMap<>();
		worldObjects = new HashMap<>();
		worldObjects.put("beginNode", null);
		worldObjects.put("endNode", null);
		worldObjects.put("office", null);
		worldObjects.put("home", null);
		properties.put("timeSearching", null);
		properties.put("distanceWork", null);
	}

	public Properties(Node beginNode, Node endNode){
		this();
		worldObjects.put("beginNode", beginNode);
		worldObjects.put("endNode", endNode);
	}
	
	public Properties(WorkOffice office) {
		this();
		worldObjects.put("office", office);
		properties.put("timeSearching", new Indicator(EnvVar.SEARCHING_TIME, 0.0));
		properties.put("distanceWork", new Indicator(EnvVar.DISTANCE_FROM_WORK, 1000.0));
	}
	
	public Properties(WorkOffice office, Home home) {
		worldObjects.put("office", office);
		worldObjects.put("home", home);
		properties.put("timeSearching", new Indicator(EnvVar.SEARCHING_TIME, 0.0));
		properties.put("distanceWork", new Indicator(EnvVar.DISTANCE_FROM_WORK, 1000.0));
	}
	
	public Map<String, Object> getProperties() {
		return properties;
	}
	
	public Map<String, WorldObject> getWorldObjects() {
		return worldObjects;
	}
	
	public void setWorldObject(String name, WorldObject object) {
		worldObjects.put(name, object);
	}
	
	public void setProperty(String name, Object object) {
		properties.put(name, object);
	}

	@Override
	public double computeScore(Object objectToScore) {
		return 0;
	}

	@Override
	public double computeSatisfaction(Object objectToScore) {
		return 0;
	}

	@Override
	public Map<String, Object> getAttributesOfAgent() {
		return null;
	}

	@Override
	public void resetProperties(int id) {
		if(id == -1){
			((Indicator)properties.get("timeSearching")).setValue(0.0);
			((Indicator)properties.get("distanceWork")).setValue(1000.0);
		}
	}

}
