package environment.graph;

import java.util.ArrayList;
import java.util.List;

import simulation.GISComputation;
import simulation.Vector2D;

import com.vividsolutions.jts.geom.Coordinate;

import environment.city.EnvVar;
import environment.city.parking.ParkingSpot;
import environment.style.TextureLibrary;
import net.sf.javaml.core.kdtree.KDTree;

public class Graph {

	private KDTree kdtree;
	
	private final int NEIGHBOOR_NODE_NUMBER = 5; //Change when map is reduced, usually 10
	private final int NEIGHBOOR_NODE_NUMBER_EXTENDED = 6; //Change when map is reduced, usually 20
	
	/**
	 * 
	 * @param nodes
	 * @param arcs
	 * @see <a href="http://stackoverflow.com/questions/10803005/how-do-i-cast-a-list-from-a-subclass-generic-to-a-parent-class-generic">Unchecked cast exception </a>
	 */
	public Graph(List<Node> nodes, List<? extends Arc> arcs){
		this.kdtree = new KDTree(2);
		for(int i = 0; i < nodes.size(); i++){
			this.kdtree.insert(nodes.get(i).getPositionInDouble(), nodes.get(i));
		}
	}
	
	public Node getNearestNodeFrom(Coordinate coord){
		double[] coords = new double[2];
		coords[0] = coord.x;
		coords[1] = coord.y;
		return (Node) this.kdtree.nearest(coords);
	}
	
	public Object[] getNearestNodesFrom(Coordinate coord, int numberOfNearestNodes){
		double[] coords = new double[2];
		coords[0] = coord.x;
		coords[1] = coord.y;
		return this.kdtree.nearest(coords, numberOfNearestNodes);
	}
	
	private Arc getNearestEdgeFromList(List<List<Arc>> arcs, ParkingSpot spot, double min){
		Arc arc = null;
		Coordinate coord = spot.getPosition();
		for(int j = 0; j < arcs.size(); j ++){
			List<Arc> currentList = arcs.get(j);
			for(int i = 0; i < currentList.size(); i++){
				Arc arcTemp = currentList.get(i);
				Node startNode = arcTemp.getStartNode();
				Node targetNode = arcTemp.getTargetNode();
				
				//*
				double X1 = startNode.getPosition().x;
				double X2 = targetNode.getPosition().x;
				double Y1 = startNode.getPosition().y;
				double Y2 = targetNode.getPosition().y;
				double XX = X2 - X1 ;
				double YY = Y2 - Y1 ;
				double X3 = coord.x;
				double Y3 = coord.y;
				double ShortestLength = ((XX * (X3 - X1)) + (YY * (Y3 - Y1))) / ((XX * XX) + (YY * YY)) ;
				
				//Projection of coordinate to edge
				double X4 = X1 + XX * ShortestLength ;
				double Y4 = Y1 + YY * ShortestLength ;
				Coordinate projectionToArc = new Coordinate(X4, Y4);
				Vector2D lineStart = new Vector2D(projectionToArc, startNode.getPosition());
				Vector2D lineEnd = new Vector2D(projectionToArc, targetNode.getPosition());
				Vector2D line = new Vector2D(startNode.getPosition(), targetNode.getPosition());
				if(lineStart.length() <= line.length() && lineEnd.length() <= line.length()){
					
					Vector2D v = new Vector2D(new Coordinate(X4, Y4), coord);
					
					//Check direction of the parking spot to the road
					if (v.x*line.y >= 0){	
						
						if(GISComputation.GPS2Meter(coord, projectionToArc) < min && GISComputation.GPS2Meter(coord, new Coordinate(X4,Y4)) < 15.){
							arc = arcTemp;
							min = GISComputation.GPS2Meter(coord, projectionToArc);
							spot.setProjectionOnEdge(projectionToArc);
						}
					} else { //Check if the road is oneway or not
						boolean doNotExist = true;
						for(int k = 0; k < targetNode.getOutcomingArcs().size(); k++){
							if(targetNode.getOutcomingArcs().get(k).getTargetNode() == startNode){
								doNotExist = false;
								break;
							}
						}
						if(doNotExist){
							if(GISComputation.GPS2Meter(coord, projectionToArc) < min ){
								arc = arcTemp;//node.getIncomingEdges().get(i);	
								min = GISComputation.GPS2Meter(coord, projectionToArc);
								spot.setProjectionOnEdge(projectionToArc);
							}
						}
					}
				}
			}
		}
		return arc;
	}
	
	
	//http://gis.stackexchange.com/questions/11409/calculating-the-distance-between-a-point-and-a-virtual-line-of-two-lat-lngs
	public Arc getNearestRoad(ParkingSpot spot, Node node){
		double min = 15;
		
		List<List<Arc>> tempArcs = new ArrayList<>();
		tempArcs.add(node.getIncomingArcs());
		tempArcs.add(node.getOutcomingArcs());
		
		return getNearestEdgeFromList(tempArcs, spot, min);
	}
	
	public Arc getNearestRoads(ParkingSpot spot, List<Node> nodes){
		Arc arc = null;
		Arc nearestArc=null;
		double distanceToArc=Double.MAX_VALUE;
		for(int i = 0; i < nodes.size(); i++){
			arc = getNearestRoad(spot, nodes.get(i));
			if( arc!=null && GISComputation.GPS2Meter(spot.getPosition(),spot.getProjectionOnEdge())<distanceToArc)
				nearestArc =arc;
		}
		return nearestArc;
	}
	
	public void addParkingToRoad(List<ParkingSpot> spots, List<EdgeOSM> edges, List<Node> nodes){
		long beginTime = System.currentTimeMillis();
		
		int failure = 0;
		for(int i = 0; i < spots.size(); i++){
			Object[] nearestNodesObjects = getNearestNodesFrom(spots.get(i).getPosition(), NEIGHBOOR_NODE_NUMBER);
			ArrayList<Node> nearestNodes = new ArrayList<>();
			for(Object obj:nearestNodesObjects)
				nearestNodes.add((Node)obj);
			EdgeOSM edge = (EdgeOSM) getNearestRoads(spots.get(i), nearestNodes);
			

			if(edge!=null){
				Node node = null;
				if(GISComputation.GPS2Meter(edge.getStartNode().getPosition(), spots.get(i).getPosition())< GISComputation.GPS2Meter(edge.getTargetNode().getPosition(), spots.get(i).getPosition())) {
					node = edge.getStartNode();
			    } else {
					node = edge.getTargetNode();
				}
				spots.get(i).setTexture(TextureLibrary.parkingSlotTexture_NotOccupied);
				edge.addParking(spots.get(i));
				spots.get(i).setIdNode(node.getId());
				if(!EnvVar.edgesWithSpots.contains(edge)){
					EnvVar.edgesWithSpots.add(edge);
				}
			} else { //If parking doesn't have a node, try the next 20 closest points
				boolean find = false;
				Object[] nodesTab = getNearestNodesFrom(spots.get(i).getPosition(), NEIGHBOOR_NODE_NUMBER_EXTENDED);
				List<Node> nodesNearest = new ArrayList<>();
				for(int j = 0; j < nodesTab.length; j++){
					Node nodeTemp = (Node) nodesTab[j];
					nodesNearest.add(nodeTemp);
					EdgeOSM edgeTemp = (EdgeOSM) getNearestRoads(spots.get(i), nodesNearest);
					if(edgeTemp != null){
						spots.get(i).setTexture(TextureLibrary.parkingSlotTexture_NotOccupied);
						find = true;
						spots.get(i).setIdNode(nodeTemp.getId());
						for(int k = 0; k < edges.size(); k++){
							if(edges.get(k) == edgeTemp){
								((EdgeOSM)edgeTemp).addParking(spots.get(i));
								if(!EnvVar.edgesWithSpots.contains(edgeTemp)){
									EnvVar.edgesWithSpots.add(edgeTemp);
								}
							}
						}
						break;
					}
				}
				if(!find){
					spots.get(i).setTexture(TextureLibrary.parkingSlotTexture_Occupied);//TextureLibrary.parkingSlotTexture_Problem);
					spots.get(i).setFailed(true);
					failure ++;
				}
			}			
		}
		if(EnvVar.configFile.get("simulation_debug").equals("1")) {
			System.out.println("Available spots: " + (spots.size() - failure) + ".");
			System.out.println("Failure: " + failure + ".");
		}
		
		System.out.println("Time to process 'addParkingToRoad': " + (System.currentTimeMillis() - beginTime) + "ms.");
	}
	
}
