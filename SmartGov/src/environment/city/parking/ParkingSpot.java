package environment.city.parking;

import java.util.ArrayList;
import java.util.List;

import repast.simphony.space.gis.Geography;

import com.vividsolutions.jts.geom.Coordinate;

import environment.MicroAgentAction;
import environment.ActionableByMicroAgent;
import environment.Perceivable;
import environment.WorldObjectTexture;
import environment.city.EnvVar;
import environment.style.TextureLibrary;
import gov.nasa.worldwind.render.WWTexture;
import microagent.AbstractMicroAgentBody;
import microagent.perception.Perception;

/**
 * ParkingSpot is an atomic element of a parking or block face. It only knows its
 * status witch is occupied or not.
 * @author Simon Pageaud
 *
 */
public class ParkingSpot extends WorldObjectTexture implements ActionableByMicroAgent, Perceivable {

	private boolean occupied;
	private int idOfAgentParked = -1;
	private BlockFace blockface;
	private String idNode;
	private Coordinate projectionOnEdge;
	
	//If no roads come near this spot
	private boolean failed = false;
	private boolean unavailable = false;
	
	public ParkingSpot(Geography<Object> geography, Coordinate position, boolean isOccupied, String id) {
		super(geography, position, id);
		this.occupied = isOccupied;
		applyTexture();
	}
	
	@Override
	public WWTexture getTexture() {
		if(isUnavailable()){
			return TextureLibrary.parkingSlotTexture_Unavailable;
		} else {
			if(occupied){
				return TextureLibrary.parkingSlotTexture_Occupied;
			} else {
				return TextureLibrary.parkingSlotTexture_NotOccupied;
			}
		}
	}
	
	public void applyTexture(){
		if(isUnavailable()){
			this.texture = TextureLibrary.parkingSlotTexture_Unavailable;
		} else {
			if(occupied){
				this.texture = TextureLibrary.parkingSlotTexture_Occupied;
			} else {
				this.texture = TextureLibrary.parkingSlotTexture_NotOccupied;
			}
		}
	}

	@Override
	public List<MicroAgentAction> getAvailableActions() {
		List<MicroAgentAction> availableActions = new ArrayList<>();
		if(occupied){
			availableActions.add(MicroAgentAction.LEAVE);
		} else {
			availableActions.add(MicroAgentAction.ENTER);
		}
		return availableActions;
	}

	@Override
	public void doMicroAgentAction(MicroAgentAction action) {
		if(action.equals(MicroAgentAction.ENTER)){
			occupied = true;
			this.texture = TextureLibrary.parkingSlotTexture_Occupied;
			
			//Remove this parking from kdTree (Not used in learning)
			if(EnvVar.kdtreeWithSpots.search(this.getPositionInDouble()) != null){
				EnvVar.kdtreeWithSpots.delete(this.getPositionInDouble());
			}
			blockface.updateAvailability(-1);
			
		} else if(action.equals(MicroAgentAction.LEAVE)){
			occupied = false;
			this.texture = TextureLibrary.parkingSlotTexture_NotOccupied;
			blockface.updateAvailability(1);
		}
	}

	@Override
	public Perception perceivedObject(AbstractMicroAgentBody agentBodyAbstract) {
		return new Perception(this);
	}
	
	public boolean isOccupied() {
		return occupied;
	}
	
	public BlockFace getBlockface() {
		return blockface;
	}
	
	public void setBlockface(BlockFace blockface) {
		this.blockface = blockface;
	}
	
	public double getPrice(){
		return this.blockface.getPrice();
	}
	
	public void setIdNode(String idNode) {
		this.idNode = idNode;
	}
	
	public String getIdNode() {
		return idNode;
	}
	
	public void setOccupied(boolean isOccupied) {
		this.occupied = isOccupied;
		blockface.updateAvailability(isOccupied == true ? -1 : 1);
	}
	
	public Coordinate getProjectionOnEdge() {
		return projectionOnEdge;
	}
	
	public void setProjectionOnEdge(Coordinate projectionOnEdge) {
		this.projectionOnEdge = projectionOnEdge;
	}
	
	public void setFailed(boolean failed) {
		this.failed = failed;
	}
	
	public boolean isFailed() {
		return failed;
	}
	
	public boolean isUnavailable() {
		return unavailable;
	}
	
	public void setUnavailable(boolean unavailable) {
		this.unavailable = unavailable;
	}
	
	public void setIdOfAgentParked(int idOfAgentParked) {
		this.idOfAgentParked = idOfAgentParked;
	}
	
	public int getIdOfAgentAsInt() {
		return idOfAgentParked;
	}
	
	public String getBlockFaceTag() {
		return blockface.getTag();
	}

}
