package environment.city.parking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import policyagent.ActionableByPolicyAgent;
import policyagent.Indicator;
import policyagent.PolicyAction;
import policyagent.Position;
import environment.Monetary;
import environment.Structure;
import environment.city.EnvVar;
import microagent.MicroAgent;
import microagent.properties.ParkProperties;

/**
 * A block-face is one side of a street between two consecutive features 
 * intersecting that street. The features can be other streets or boundaries of standard geographic areas.
 * @see <a href="http://www.statcan.gc.ca/pub/92-195-x/2011001/other-autre/bf-ci/def-eng.htm">Block Face definition</a>
 * @author Simon Pageaud
 *
 */
public class BlockFace implements Monetary, Structure, ActionableByPolicyAgent {
	
	public static final String     OCCUPATION =    "occupation";
	public static final String   SATISFACTION =  "satisfaction";
	public static final String       DISTANCE =      "distance";
	public static final String TIME_SEARCHING = "timeSearching";
	public static final String        UTILITY =       "utility";
	public static final String          PRICE =         "price";
	public static final String         PLACES =        "places";
	
	private int id;
	private int maxNumberPlaces;
	private int minNumberPlaces = 1;
	private int currentNumberPlaces; //Number of spots before the start of the simulation
	private int currentAvailablePlaces; //Number of spots during the simulation
	private List<String> parkingId;
	private List<ParkingSpot> parkingSpots;
	private double price;
	
	public static double distanceValueWhenNoAgentParked = 250.0;
	public static double timeValueWhenNoAgentParked = 150.0;
	
	private Indicator parkingPrice;
	
	private boolean failed = false;
	
	private String tag = "";
	
	private double lastGain    = 0.0;
	private double highestGain = 0.0;
	
	public BlockFace(int id, int numberPlaces){
		this.id = id;
		this.currentNumberPlaces = numberPlaces;
		this.maxNumberPlaces = numberPlaces;
		this.parkingSpots = new ArrayList<>();
		
		this.price = (double) EnvVar.params.getDouble("initial_price"); //arbitrary value
		
		this.parkingPrice = new Indicator(EnvVar.PRICE, String.valueOf(id), getPrice());
	}
	
	@Override
	public double getPrice() {
		return price;
	}

	@Override
	public void updatePrice(double priceVariation) {
		this.price += priceVariation;
		if(this.price < 0.5){
			this.price = 0.5;
		} else if(this.price > 4.){
			this.price = 4.;
		} else {
			this.parkingPrice.updateValue(priceVariation);
		}
		
	}
	
	public void updateSpots(int spotVariation) {
		currentNumberPlaces = Math.min(Math.max(currentNumberPlaces + spotVariation, minNumberPlaces), maxNumberPlaces);
		currentAvailablePlaces = currentNumberPlaces;
		enableSpots();
	}
	
	private void allSpotsUnavailable() {
		for(int i = 0; i < parkingSpots.size(); i++) {
			if(!parkingSpots.get(i).isFailed()) {
				parkingSpots.get(i).setUnavailable(true);
			}
		}
	}
	
	public void enableSpots() {
		allSpotsUnavailable();
		int counter = 0;
		for(int i = 0; i < parkingSpots.size(); i++) {
			if(counter == currentNumberPlaces) {
				break;
			}
			if(!parkingSpots.get(i).isFailed()) {
				parkingSpots.get(i).setUnavailable(false);
				counter++;
			}
		}
	}
	
	public void enableAllSpots() {
		for(int i = 0; i < parkingSpots.size(); i++) {
			parkingSpots.get(i).setUnavailable(false);
		}
	}
	
	public void setPrice(double price) {
		this.price = price;
		this.parkingPrice.setValue(price);
	}
	
	public void setParkingId(List<String> parkingId) {
		this.parkingId = parkingId;
	}
	
	public List<ParkingSpot> getParkingSpots() {
		return parkingSpots;
	}
	
	public int getCorretSpots(){
		int correctSpotCounter = 0;
		for(int i = 0; i < parkingSpots.size(); i++){
			if(!parkingSpots.get(i).isFailed()){
				correctSpotCounter++;
			}
		}
		return correctSpotCounter;
	}
	
	public void setParkings(List<ParkingSpot> parkings) {
		this.parkingSpots = parkings;
	}
	
	public void addParking(ParkingSpot parking){
		this.parkingSpots.add(parking);
	}
	
	public List<String> getParkingId() {
		return parkingId;
	}
	
	public int getOccupancy(){
		int occupiedPlaces = 0;
		for(int i = 0; i < parkingSpots.size(); i++){
			if(parkingSpots.get(i).isOccupied()){
				occupiedPlaces++;
			}
		}
		return occupiedPlaces;
	}
	
	public int getId() {
		return id;
	}
	
	public void setFailed(boolean failed) {
		this.failed = failed;
	}
	
	public boolean isFailed() {
		return failed;
	}
	
	public String getTag() {
		return tag;
	}

	@Override
	public String getID() {
		return String.valueOf(id);
	}

	@Override
	public String getClassName() {
		return "BlockFace";
	}
	
	@Override
	public Position getLocalPerformances(List<String> labels) {
		Position averageValues = new Position();
		for(int i = 0; i < labels.size(); i++) {
			if(labels.get(i).equals("utility")) {
				double sumOfValues = 0.0;
				int counter = 0;
				for(int j = 0; j < parkingSpots.size(); j++) {
					if(parkingSpots.get(j).isOccupied()){
						counter++;
						MicroAgent agent = (MicroAgent)EnvVar.agents.get(parkingSpots.get(j).getIdOfAgentAsInt());
						ParkProperties properties = (ParkProperties)agent.getProperties();
						sumOfValues += properties.getPreviousUtilityWhenParked();
					}
				}
				if(counter > 0) {
					averageValues.addCoordinate(sumOfValues/counter);
				} else {
					averageValues.addCoordinate(0.0);
				}
			} else if(labels.get(i).equals("satisfaction")) {
				double sumOfValues = 0.0;
				int counter = 0;
				for(int j = 0; j < parkingSpots.size(); j++) {
					if(parkingSpots.get(j).isOccupied()){
						counter++;
						MicroAgent agent = (MicroAgent)EnvVar.agents.get(parkingSpots.get(j).getIdOfAgentAsInt());
						ParkProperties properties = (ParkProperties)agent.getProperties();
						sumOfValues += properties.getPreviousSatisfaction();
					}
				}
				if(counter > 0) {
					averageValues.addCoordinate(sumOfValues/counter);
				} else {
					averageValues.addCoordinate(0.0);
				}
			} else if(labels.get(i).equals("distance")) {
				double sumOfValues = 0.0;
				int counter = 0;
				for(int j = 0; j < parkingSpots.size(); j++) {
					if(parkingSpots.get(j).isOccupied()){
						counter++;
						MicroAgent agent = (MicroAgent)EnvVar.agents.get(parkingSpots.get(j).getIdOfAgentAsInt());
						ParkProperties properties = (ParkProperties)agent.getProperties();
						sumOfValues += properties.getDistanceWork().getValue();
					}
				}
				if(counter > 0) {
					averageValues.addCoordinate(sumOfValues/counter);
				} else {
					averageValues.addCoordinate(distanceValueWhenNoAgentParked);
				}
			} else if(labels.get(i).equals("timesearching")) {
				double sumOfValues = 0.0;
				int counter = 0;
				for(int j = 0; j < parkingSpots.size(); j++) {
					if(parkingSpots.get(j).isOccupied()){
						counter++;
						MicroAgent agent = (MicroAgent)EnvVar.agents.get(parkingSpots.get(j).getIdOfAgentAsInt());
						ParkProperties properties = (ParkProperties)agent.getProperties();
						sumOfValues += properties.getTimeSearching().getValue();
					}
				}
				if(counter > 0) {
					averageValues.addCoordinate(sumOfValues/counter);
				} else {
					averageValues.addCoordinate(timeValueWhenNoAgentParked);
				}
			} else if(labels.get(i).equals("occupation")) {
				double counter = 0.0;
				int totalPlaces = 0;
				for(int j = 0; j < parkingSpots.size(); j++) {
					if(parkingSpots.get(j).isOccupied()){
						counter++;
					}
					if(!parkingSpots.get(j).isFailed()){
						totalPlaces++;
					}
				}
				if(counter > 0) {
					averageValues.addCoordinate(counter/totalPlaces);
				} else {
					averageValues.addCoordinate(0.0);
				}
			} else if(labels.get(i).equals("numberOfPlaces")) {
				averageValues.addCoordinate(currentNumberPlaces);
			} else if(labels.get(i).equals("reward")) {
				String computationType = "computeReward";
				switch (computationType) {
					case "computeReward":
						computeReward(averageValues);
						break;
					case "computeRewardEqualPayoff":
						computeRewardEqualPayoff(averageValues);
						break;
					case "computeRewardSlow":
						computeRewardSlow(averageValues);
						break;
					case "computeRewardWithStand":
						computeRewardWithStand(averageValues);
						break;
					default:
						computeReward(averageValues);
						break;
				}
			} else if(labels.get(i).equals("price")) {
				averageValues.addCoordinate(price);
			} else if(labels.get(i).equals("gain")) {
				int counter = 0;
				for(int j = 0; j < parkingSpots.size(); j++) {
					if(parkingSpots.get(j).isOccupied()){
						counter++;
					}
				}
				if(counter*price <= 0.0) {
					averageValues.addCoordinate(0.0);
				} else {
					averageValues.addCoordinate(counter*price);
				}
			}
		}		
		return averageValues;
	}
	
	public Map<String, Integer> agentPerUtilityParked(){
		Map<String, Integer> parkedAgentsPerUtility = new HashMap<String, Integer>();
		for(ParkingSpot spot : parkingSpots) {
			if(spot.getIdOfAgentAsInt() != -1 && spot.isOccupied()) {
				MicroAgent agent = (MicroAgent)EnvVar.agents.get(spot.getIdOfAgentAsInt());
				String utility = ((ParkProperties)agent.getProperties()).getUtility().getProfileName();
				if(parkedAgentsPerUtility.containsKey(utility)) {
					parkedAgentsPerUtility.put(utility, parkedAgentsPerUtility.get(utility) + 1);
				} else {
					parkedAgentsPerUtility.put(utility, 1);
				}
			}
		}
		return parkedAgentsPerUtility;
	}
	
	public int getCurrentNumberPlaces() {
		return currentNumberPlaces;
	}
	
	public int getMaxNumberPlaces() {
		return maxNumberPlaces;
	}
	
	public int getMinNumberPlaces() {
		return minNumberPlaces;
	}
	
	public void setMaxNumberPlaces(int maxNumberPlaces) {
		this.maxNumberPlaces = maxNumberPlaces;
	}
	
	public void setCurrentNumberPlaces(int currentNumberPlaces) {
		this.currentNumberPlaces = currentNumberPlaces;
		currentAvailablePlaces = currentNumberPlaces;
		enableSpots();
	}

	@Override
	public List<PolicyAction> getAvailablePolicyActions() {
		List<PolicyAction> policies = new ArrayList<>();
		policies.add(PolicyAction.INCREASE_PRICES);
		policies.add(PolicyAction.DECREASE_PRICES);
		policies.add(PolicyAction.DO_NOTHING);
		policies.add(PolicyAction.INCREASE_PLACES);
		policies.add(PolicyAction.DECREASE_PLACES);
		return policies;
	}

	@Override
	public void doPolicyAction(PolicyAction policyAction) {
		switch (policyAction) {
		case INCREASE_PRICES:
			updatePrice(0.5);
			break;

		case DECREASE_PRICES:
			updatePrice(-0.5);
			break;

		case INCREASE_PLACES:
			updateSpots(1);
			break;


		case DECREASE_PLACES:
			updateSpots(-1);
			break;

		default:
			break;
		}
		
	}
	
	/**
	 * Payoff is price * occupation
	 * Several ways to compute rewards:
	 * <br> - r = {-1,1} with counter*price > lastGain (computeReward)
	 * <br> - r = {-1,1} with counter*price >= lastGain (computeRewardWithStand)
	 * <br> - r = {-1,0,1} (computeRewardSlow)
	 * <br> - r = payoff (computeRewardEqualPayoff)
	 * @param averageValues
	 */
	private void computeReward(Position averageValues) {
		int counter = 0;
		for(int j = 0; j < parkingSpots.size(); j++) {
			if(parkingSpots.get(j).isOccupied()){
				counter++;
			}
		}		
		if(counter*price <= 0.0) {
			averageValues.addCoordinate(-1.0);
			lastGain = 0.0;
		} else {
			if(counter*price >= highestGain) {
				highestGain = counter*price;
				averageValues.addCoordinate(1.0);
			} else if(counter*price > lastGain) { 
				averageValues.addCoordinate(1.0);
			} else {
				averageValues.addCoordinate(-1.0);
			}
			lastGain = counter*price;
		}
	}
	
	private void computeRewardEqualPayoff(Position averageValues) {
		int counter = 0;
		for(int j = 0; j < parkingSpots.size(); j++) {
			if(parkingSpots.get(j).isOccupied()){
				counter++;
			}
		}
		averageValues.addCoordinate(price*counter);
	}
	
	private void computeRewardWithStand(Position averageValues) {
		int counter = 0;
		for(int j = 0; j < parkingSpots.size(); j++) {
			if(parkingSpots.get(j).isOccupied()){
				counter++;
			}
		}		
		if(counter*price <= 0.0) {
			averageValues.addCoordinate(-1.0);
			lastGain = 0.0;
		} else {
			if(counter*price >= highestGain) {
				highestGain = counter*price;
				averageValues.addCoordinate(1.0);
			} else if(counter*price >= lastGain) {
				averageValues.addCoordinate(1.0);
			} else {
				averageValues.addCoordinate(-1.0);
			}
			lastGain = counter*price;
		}
	}
	
	private void computeRewardSlow(Position averageValues) {
		int counter = 0;
		for(int j = 0; j < parkingSpots.size(); j++) {
			if(parkingSpots.get(j).isOccupied()){
				counter++;
			}
		}		
		if(counter*price <= 0.0) {
			averageValues.addCoordinate(-1.0);
			lastGain = 0.0;
		} else {
			if(counter*price >= highestGain) {
				highestGain = counter*price;
				averageValues.addCoordinate(1.0);
			} else if(counter*price > lastGain) { 
				averageValues.addCoordinate(1.0);
			} else if(counter*price == lastGain) {
				averageValues.addCoordinate(0.0);
			} else {
				averageValues.addCoordinate(-1.0);
			}
			lastGain = counter*price;
		}
	}
	
	/**
	 * Used when an agent ENTER or LEAVE a spot of the current blockface.
	 * @param modification either 1 or -1
	 */
	public void updateAvailability(int modification) {
		currentAvailablePlaces = Math.max(Math.min(currentAvailablePlaces + modification, currentNumberPlaces), 0);
	}

}
