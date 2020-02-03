package smartGov;

import environment.graph.OrientedGraph;

/**
 * The OrientedGraph is built this way to allow every agent to get an instance and find their way through the graph. Only one instance of graph is available at the moment.
 * It could be possible to add several layers of graphs to represent different dynamics in the simulation (e.g. pedestrians layer, subway layer, car layer, ...).
 * @author Simon
 *
 */
public class OrientedGraphSingleton {

	private static OrientedGraphSingleton instance = null;
	private OrientedGraph graph;
	
	private OrientedGraphSingleton() { }
	
	public static synchronized OrientedGraphSingleton getInstance(){
		if(instance == null){
			instance = new OrientedGraphSingleton();
		}
		return instance;
	}
	
	public OrientedGraph getGraph() {
		return graph;
	}
	
	public void setGraph(OrientedGraph graph) {
		this.graph = graph;
	}
}
