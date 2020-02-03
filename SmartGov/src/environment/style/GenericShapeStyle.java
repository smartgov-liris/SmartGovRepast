package environment.style;

import java.awt.Color;

import environment.WorldObjectShape;
import gov.nasa.worldwind.render.SurfaceShape;
import repast.simphony.visualization.gis3D.style.SurfaceShapeStyle;

/**
 * Used by Repast Simphony to display shapes by specifying color, line opacity and so on.
 * @author Simon Pageaud
 *
 */
public class GenericShapeStyle implements SurfaceShapeStyle<WorldObjectShape> {

	@Override
	public SurfaceShape getSurfaceShape(WorldObjectShape object,
			SurfaceShape shape) {
		return object.getShape();
	}

	@Override
	public Color getFillColor(WorldObjectShape obj) {
		return obj.getFillColor();
	}

	@Override
	public double getFillOpacity(WorldObjectShape obj) {
		return obj.getFillOpacity();
	}

	@Override
	public Color getLineColor(WorldObjectShape obj) {
		return obj.getLineColor();
	}

	@Override
	public double getLineOpacity(WorldObjectShape obj) {
		return obj.getLineOpacity();
	}

	@Override
	public double getLineWidth(WorldObjectShape obj) {
		return obj.getLineWidth();
	}

}
