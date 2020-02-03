package environment.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stores a list of edges and common attributes of these edges.
 * @author Simon Pageaud
 *
 */
public class Road {

	private String id;
	private Map<String, String> attributes;
	private List<String> edgesId;
	private List<String> nodesId;
	private List<Integer> agentId;
	private int lanes;
	
	public Road(List<String> edges){
		this.edgesId = edges;
		this.agentId = new ArrayList<Integer>();
	}
	
	public Road(List<String> edges, Map<String, String> attributes){
		this(edges);
		this.attributes = attributes;
		if(this.attributes.containsKey("lanes")){
			this.lanes = Integer.valueOf(this.attributes.get("lanes"));
		} 
	}
	
	public Road(String id, Map<String, String> attributes, List<String> nodesId){
		this.id = id;
		this.attributes = attributes;
		this.nodesId = nodesId;
		this.agentId = new ArrayList<Integer>();
		if(this.attributes.containsKey("lanes")){
			this.lanes = Integer.valueOf(this.attributes.get("lanes"));
		}
	}
	
	public Map<String, String> getAttributes() {
		return attributes;
	}
	
	public String getId() {
		return id;
	}
	
	public void setNodesId(List<String> nodesId) {
		this.nodesId = nodesId;
	}
	
	public void setEdgesId(List<String> edgesId) {
		this.edgesId = edgesId;
	}
	
	public List<String> getEdgesId() {
		return edgesId;
	}
	
	public List<String> getNodesId() {
		return nodesId;
	}
	
	public int getLanes() {
		return lanes;
	}
	
	public List<Integer> getAgentId() {
		return agentId;
	}
	
}
