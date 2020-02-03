package environment.city;

import gov.nasa.worldwind.render.SurfaceShape;

import java.util.Map;

import repast.simphony.space.gis.Geography;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Describes the current home of a micro agent.
 * @author Simon Pageaud
 *
 */
public class Home extends Building {
	
	public Home(Geography<Object> geography, String id, Map<String, String> attributes, SurfaceShape shape, Coordinate[] polygon){
		super(geography, id, attributes, shape, polygon);
	}
}
