package environment.city;

import environment.graph.Node;
import gov.nasa.worldwind.render.SurfaceShape;

import java.util.ArrayList;
import java.util.Map;

import repast.simphony.space.gis.Geography;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Describes the work place of a micro agent.
 * @author Simon Pageaud
 *
 */
public class WorkOffice extends Building {
	
	public WorkOffice(Geography<Object> geography, String id, Map<String, String> attributes, SurfaceShape shape, Coordinate[] polygon){
		super(geography, id, attributes, shape, polygon);
		closestNodesWithSpots = new ArrayList<Node>();
	}
	
}
