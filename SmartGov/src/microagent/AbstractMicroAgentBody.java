package microagent;

import environment.MicroAgentAction;
import environment.Perceivable;
import environment.WorldObjectTexture;
import environment.style.TextureLibrary;
import gov.nasa.worldwind.render.WWTexture;
import microagent.perception.Perception;
import repast.simphony.space.gis.Geography;
import simulation.Vector2D;

/**
 * Abstract class for agent body. 
 * @author Simon Pageaud
 *
 */
public abstract class AbstractMicroAgentBody extends WorldObjectTexture implements Perceivable {

	protected int agentID;
	
	protected Perceivable objectToPerceive;
	protected Vector2D direction;
	protected WWTexture baseTexture;
	
	public AbstractMicroAgentBody(int agentID, String id, Geography<Object> geography) {
		super(geography, id);
		init(agentID);
	}

	public Perceivable getObjectToPerceive() {
		return objectToPerceive;
	}

	public void setObjectToPerceive(Perceivable objectToPerceive) {
		this.objectToPerceive = objectToPerceive;
	}
	
	public Vector2D getDirection() {
		return direction;
	}

	public void setDirection(Vector2D direction) {
		this.direction = direction;
		applyRotation();
	}

	//If has a direction, apply the rotation
	//Applied every tick
	public void setTexture(WWTexture texture) {
		this.baseTexture = texture;
		applyRotation();
	}
	
	public void applyRotation(){
		double angle = -TextureLibrary.xVector.angleInRad(direction);
		if(direction.y >= 0){
			angle = 2*Math.PI - TextureLibrary.xVector.angleInRad(direction);
		}
		int angleIndex = (int) Math.round(angle/TextureLibrary.DELTA_ORIENTATION);
		if(angleIndex < 0){
			System.out.println("angle: " + angle + ", angleIndex: " + angleIndex + ", direction: " + direction);
		}
		this.texture = getCurrentTextureRotated(baseTexture, angleIndex%TextureLibrary.NUMBER_OF_ORIENTATION);
	}
	
	/**
	 * Select the texture based on agent state (to be improved)
	 * @param baseTexture
	 * @param angleIndex
	 * @return
	 */
	public WWTexture getCurrentTextureRotated(WWTexture baseTexture, int angleIndex){
		if(baseTexture == TextureLibrary.agentBodyAwareness){
			return TextureLibrary.agentBodyAwarenessPack.get(angleIndex);
		} else if(baseTexture == TextureLibrary.agentBodyWander){
			return TextureLibrary.agentBodyWanderPack.get(angleIndex);
		} else if(baseTexture == TextureLibrary.agentBodyMovingToSpot){
			return TextureLibrary.agentBodyMovingToSpotPack.get(angleIndex);
		} else if(baseTexture == TextureLibrary.agentBodyCloseToDestination){
			return TextureLibrary.agentBodyCloseToDestinationPack.get(angleIndex);
		} else if(baseTexture == TextureLibrary.agentBodyParked){
			return TextureLibrary.agentBodyParkedPack.get(angleIndex);
		} else if(baseTexture == TextureLibrary.agentBodyTexture){
			return TextureLibrary.agentBodyTexturePack.get(angleIndex);
		} else {
			return baseTexture;
		}
	}
	
	public abstract void init(int agentID);
	
	public abstract Perception getPerception();
	
	public abstract void doAction(MicroAgentAction action);
	
}