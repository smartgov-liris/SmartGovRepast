package environment.graph;

import java.util.ArrayList;
import java.util.List;

import org.graphstream.algorithm.AStar;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.ElementNotFoundException;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.Path;
import org.graphstream.graph.implementations.MultiGraph;

import environment.city.EnvVar;

public class OrientedGraph {
	
	private MultiGraph orientedGraph;
	
	private List<Node> nodes;
	
	public OrientedGraph(){
		this.nodes = EnvVar.nodes;
		MultiGraph g= new MultiGraph("graph");
		g.setStrict(true);
        for(int i = 0; i < EnvVar.nodes.size(); i++){
        	g.addNode(EnvVar.nodes.get(i).getId());
        }
        
       for(int i =0; i<EnvVar.nodes.size();i++){
        	Node node =EnvVar.nodes.get(i);
        	for(int j = 0; j < node.getOutcomingArcs().size(); j++){       	
        		try {
       				g.addEdge(node.getOutcomingArcs().get(j).getId(), 
    				node.getId(),
    				node.getOutcomingArcs().get(j).getTargetNode().getId(), true)
    				.setAttribute("distance", node.getOutcomingArcs().get(j).getDistance());
       			} catch(IdAlreadyInUseException | ElementNotFoundException |EdgeRejectedException er ){
       				System.out.println(er.getMessage());
       			};
        	}	
       }
        orientedGraph = g;
	}
	
	
	public OrientedGraph(List<Node> nodes){
		this.nodes = nodes;
		MultiGraph g= new MultiGraph("graph");
		g.setStrict(true);
        for(int i = 0; i < nodes.size(); i++){
        	g.addNode(nodes.get(i).getId());
        }
        
       for(int i =0; i<nodes.size();i++){
        	Node node =nodes.get(i);
        	for(int j = 0; j < node.getOutcomingArcs().size(); j++){       	
        		try {
       				g.addEdge(node.getOutcomingArcs().get(j).getId(), 
    				node.getId(),
    				node.getOutcomingArcs().get(j).getTargetNode().getId(), true)
    				.setAttribute("distance", node.getOutcomingArcs().get(j).getDistance());
       			} catch(IdAlreadyInUseException | ElementNotFoundException |EdgeRejectedException er ){
       				System.out.println(er.getMessage());
       			};
        	}	
       }
        orientedGraph = g;
	}
	
	
	private List<String> pathBetween(Node from, Node to){
		 AStar astar = new AStar(orientedGraph);
		 astar.setCosts(new AStar.DefaultCosts("distance"));
		 astar.compute(from.getId(), to.getId());
		 Path path = astar.getShortestPath();
		 List<String> nodesId=new ArrayList<>();
		 if(path!=null && !path.empty()){
			 for(org.graphstream.graph.Node n: path.getNodePath())
				 nodesId.add(n.getId());
		 } else {
			 nodesId.add(from.getId());
		 }
		 return nodesId;
	}
	
	private List<Node> pathStringToNode(List<String> nodesId){
		List<Node> nodesPath = new ArrayList<>();
		for(int i = 0; i < nodesId.size(); i++){
			nodesPath.add(nodes.get(Integer.parseInt(nodesId.get(i))));
		}
		return nodesPath;
	}
	
	public List<Node> shortestPath(Node from, Node to){
		return pathStringToNode(pathBetween(from, to));
	}
	
}