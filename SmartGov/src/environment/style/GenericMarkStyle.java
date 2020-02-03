package environment.style;

import environment.WorldObjectTexture;
import gov.nasa.worldwind.render.WWTexture;
import repast.simphony.visualization.gis3D.style.DefaultMarkStyle;

/**
 * Used by Repast Simphony to display dots.
 * @author Simon Pageaud
 *
 */
public class GenericMarkStyle extends DefaultMarkStyle<WorldObjectTexture> {

	@Override
	public WWTexture getTexture(WorldObjectTexture object, WWTexture texture) {
		return object.getTexture();
	}
	
}
