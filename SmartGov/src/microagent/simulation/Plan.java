package microagent.simulation;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import environment.graph.Arc;
import environment.graph.Node;

/**
 * A plan provides the MicroAgent a list of nodes to travel through during the simulation. The plan is created using the current road network with origin
 * and destination of a MicroAgent.
 * @author Simon Pageaud
 *
 */
public class Plan {

	private List<Node> nodes;
	private Queue<Node> remainingNodes;
	private Node currentNode;
	private int indexOfCurrentNode;
	private boolean pathComplete;
	
	/**
	 * Empty plan for agent body pool. Need to be updated.
	 */
	public Plan() {
		
	}
	
	public Plan(List<Node> nodes){
		this.indexOfCurrentNode = 0;
		this.nodes = nodes;
		this.currentNode = nodes.get(indexOfCurrentNode);
		this.pathComplete = false;
		this.remainingNodes = new LinkedList<>();
		for(int i = 1; i < nodes.size(); i++){
			remainingNodes.add(nodes.get(i));
		}
	}
	
	public Arc getCurrentArc() {
		List<Arc> arcs = currentNode.getOutcomingArcs();
		Node nextNode = remainingNodes.peek();
		if(nextNode != null){
			if(arcs.size() == 1){
				return arcs.get(0);
			} else {
				for(int i = 0; i < arcs.size(); i++){
					if(arcs.get(i).getTargetNode().getId().equals(nextNode.getId())){
						return arcs.get(i);
					}
				}
			}
		}
		return null; //Should not happen !
	}
	
	public void update(List<Node> nodes) {
		this.indexOfCurrentNode = 0;
		this.nodes = nodes;
		this.currentNode = nodes.get(indexOfCurrentNode);
		this.pathComplete = false;
		this.remainingNodes = new LinkedList<>();
		for(int i = 1; i < nodes.size(); i++){
			remainingNodes.add(nodes.get(i));
		}
	}
	
	public void setPathComplete(boolean pathComplete) {
		this.pathComplete = pathComplete;
	}
	
	public boolean isPathComplete() {
		return pathComplete;
	}
	
	public Node getNextNode(){
		return remainingNodes.peek();
	}
	
	public Arc getNextArc(){
		List<Arc> arcs = remainingNodes.peek().getOutcomingArcs();
		try {
			Node futurNode = nodes.get(indexOfCurrentNode+2);
			if(futurNode != null){
				if(arcs.size() == 1){
					return arcs.get(0);
				} else {
					for(int i = 0; i < arcs.size(); i++){
						if(arcs.get(i).getTargetNode().getId().equals(futurNode.getId())){
							return arcs.get(i);
						}
					}
				}
			}
		} catch (IndexOutOfBoundsException e){
			
		}
		return null;
	}
	
	public Node getLastNode() {
		return nodes.get(nodes.size() - 1);
	}
	
	public Node getPreviousNode() {
		return nodes.get(nodes.size() - 2);
	}
	
	public Node getCurrentNode() {
		return currentNode;
	}
	
	public List<Node> getNodes() {
		return nodes;
	}
	
	public void reachANode(){
		this.currentNode = remainingNodes.poll();
		this.indexOfCurrentNode++;
	}
	
	public void addANode(Node node){
		this.nodes.add(node);
		this.remainingNodes.add(node);
		this.pathComplete = false;
	}
	
	@Override
	public String toString() {
		String s = new String();
		for(int indexOfNode = 0; indexOfNode < nodes.size(); indexOfNode++){
			s+= indexOfNode + ") " + nodes.get(indexOfNode).getId() + ", ";
		}
		return s;
	}
	
}
