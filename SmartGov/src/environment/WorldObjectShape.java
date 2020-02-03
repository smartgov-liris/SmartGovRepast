package environment;

import java.awt.Color;

import gov.nasa.worldwind.render.SurfaceShape;
import repast.simphony.space.gis.Geography;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * WorldObjectShape is the abstract class for any WorldObject in the
 * simulation represented by a polyline. (e.g.: a building, a road, ...)
 * @see WorldObject
 * @author Simon Pageaud
 *
 */
public abstract class WorldObjectShape extends WorldObject {

	protected SurfaceShape shape;
	
	public WorldObjectShape(Geography<Object> geography, String id) {
		super(geography, id);
	}
	
	public WorldObjectShape(Geography<Object> geography, Coordinate position, String id) {
		super(geography, position, id);
	}
	
	public SurfaceShape getShape() {
		return this.shape;
	}
	
	public Color getFillColor() {
		return Color.WHITE;
	}

	public double getFillOpacity() {
		return 0.25;
	}

	public Color getLineColor() {
		return Color.BLACK;
	}

	public double getLineOpacity() {
		return 0.75;
	}

	public double getLineWidth() {
		return 0.75;
	}

}
