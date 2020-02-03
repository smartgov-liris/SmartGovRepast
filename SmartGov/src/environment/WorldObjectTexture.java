package environment;

import environment.style.TextureLibrary;
import gov.nasa.worldwind.render.WWTexture;
import repast.simphony.space.gis.Geography;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * WorldObjectTexture is the abstract class for any WorldObject that
 * is represent by a dot in the simulation (e.g.: a parking spot, a road junction
 * an agent, ...).
 * @see WorldObject
 * @author Simon Pageaud
 *
 */
public abstract class WorldObjectTexture extends WorldObject {
	
	protected WWTexture texture;
	
	public WorldObjectTexture(Geography<Object> geography, String id) {
		super(geography, id);
		this.texture = TextureLibrary.defaultTexture;
	}
	
	public WorldObjectTexture(Geography<Object> geography, Coordinate position, String id) {
		super(geography, position, id);
		this.texture = TextureLibrary.defaultTexture;
	}

	/**
	 * The method used to display a specific WWTexture. By default, this method
	 * returns a red square.
	 * @see TextureLibrary
	 * @return WWTexture stores in TextureLibrary.
	 */
	public WWTexture getTexture() {
		return texture;
	}
	
	public void setTexture(WWTexture texture) {
		this.texture = texture;	
	}
}
