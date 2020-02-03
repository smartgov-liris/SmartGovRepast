package microagent.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.joda.time.LocalTime;

import com.vividsolutions.jts.geom.Coordinate;

import environment.city.EnvVar;
import environment.city.WorkOffice;
import environment.city.parking.ParkingSpot;
import environment.graph.SourceNode;
import microagent.AbstractUtility;
import policyagent.Indicator;
import simulation.GISComputation;

public class ParkProperties extends Properties {
	
	private AbstractUtility<ParkProperties> utility;
	
	private Map<String, Object> infos;
	private Map<String, Double> attributesD;
	private Map<String, Boolean> booleans;
	
	private WorkOffice workOffice;
	private SourceNode sourceNode;
	
	private Indicator timeSearching;
	private Indicator distanceWork;
	
	private boolean alreadyChecked = false;

	private ParkingSpot parkingSpot = null;
	private boolean parked = false;
	
	//Utility
	private double satisfaction;
	private double utilityWhenParked;
	
	//Park
	private int timeToPark = 5;
	private int currentTimeToPark = 0;
	
	//Historic
	private List<Double> previousPrice;
	private List<Double> previousDistance;
	private List<Double> previousTime;
	private List<Double> previousSatisfaction;
	private List<Double> previousBusSatisfaction;
	private List<Double> previousCommuteTime;
	private List<Double> totalSatisfaction;
	private List<Double> utilitiesWhenParked;
	
	private double[][] probaStates;
	private int currentState; //0 car, 1 bus
	
	private Map<String, Integer> numberOfRoadsCrossed = new HashMap<>(); //Store roads crossed
	
	public ParkProperties(){
		super();
		init();
		attributesD = new HashMap<>();
	}

	public ParkProperties(String filename){
		this();
		attributesD = parseBehaviorFile(filename);
	}
	
	public ParkProperties(String filename, LocalTime start, LocalTime end){
		this(filename);
	}
	
	public void setParkingSpot(ParkingSpot parkingSpot) {
		this.parkingSpot = parkingSpot;
	}
	public ParkingSpot getParkingSpot() {
		return parkingSpot;
	}
	public WorkOffice getWorkOffice() {
		return workOffice;
	}
	public Indicator getDistanceWork() {
		return distanceWork;
	}
	public Indicator getTimeSearching() {
		return timeSearching;
	}
	public void setWorkOffice(WorkOffice workOffice) {
		this.workOffice = workOffice;
	}
	public Map<String, Double> getAttributesD() {
		return attributesD;
	}
	public boolean isParked() {
		return parked;
	}
	public void setParked(boolean parked) {
		this.parked = parked;
	}
	public void setUtility(AbstractUtility<ParkProperties> utility) {
		this.utility = utility;
	}
	public AbstractUtility<ParkProperties> getUtility() {
		return utility;
	}
	public double getSatisfaction() {
		return satisfaction;
	}
	public void setSatisfaction(double satisfaction) {
		this.satisfaction = satisfaction;
	}
	public List<Double> getListOfPreviousSatisfaction(){
		return previousSatisfaction;
	}
	public void reinitSatisfactions(){
		this.previousSatisfaction = new ArrayList<Double>();
	}
	public double getPreviousSatisfaction(){
		return previousSatisfaction.get(previousSatisfaction.size() - 1);
	}
	public double getPreviousUtilityWhenParked() {
		return utilitiesWhenParked.get(utilitiesWhenParked.size() - 1);
	}
	public double getPreviousBusSatisfaction(){
		return previousBusSatisfaction.get(previousBusSatisfaction.size() - 1);
	}
	public Map<String, Integer> getNumberOfRoadsCrossed() {
		return numberOfRoadsCrossed;
	}
	public Map<String, Object> getInfos() {
		return infos;
	}
	public int getCurrentState() {
		return currentState;
	}
	public void setCurrentState(int currentState) {
		this.currentState = currentState;
	}
	public void setUtilityWhenParked(double utilityWhenParked) {
		this.utilityWhenParked = utilityWhenParked;
	}
	
	public boolean isAlreadyChecked() {
		return alreadyChecked;
	}
	public SourceNode getSourceNode() {
		return sourceNode;
	}
	public void setSourceNode(SourceNode sourceNode) {
		this.sourceNode = sourceNode;
	}
	
	public void setAlreadyChecked(boolean alreadyChecked) {
		this.alreadyChecked = alreadyChecked;
	}
	
	@Override
	public void resetProperties(int id) {
		parkingSpot = null;
		parked = false;
		distanceWork.setValue(1000.0);
		timeSearching.setValue(0.0);
		this.numberOfRoadsCrossed = new HashMap<>();
		if(id == -1){
			Random rnd = new Random();
			this.workOffice = EnvVar.offices.get(rnd.nextInt(EnvVar.offices.size()));
			worldObjects.put("endNode", EnvVar.nodes.get(this.workOffice.getClosestNodeId()));
		} else {
			this.workOffice = (WorkOffice) EnvVar.buildings.get(Integer.valueOf((String) getSpecificAttributeForId(id, "Office")));
			this.sourceNode = (SourceNode) EnvVar.sourceNodes.get(Integer.valueOf((String) getSpecificAttributeForId(id, "SourceNode")));
			worldObjects.put("endNode", EnvVar.nodes.get(this.workOffice.getClosestNodeId()));
		}
	}
	
	public void setProbaStates(String type){
		if(type.equals("bus")){
			currentState = 1;
			probaStates[0][0] = 1 - probaStates[1][0]; //Car to bus
			probaStates[1][0] = 0; //Bus to car
		} else if (type.equals("car")){
			currentState = 0;
			probaStates[0][0] = 1 - probaStates[1][0]; //Car to bus
			probaStates[1][0] = 1; //Bus to car
		} else {
			currentState = 0;
			probaStates[0][0] = 1 - probaStates[1][0]; //Car to bus
			probaStates[1][0] = 0.5; //Bus to car
		}
		
	}
	
	public int getCurrentTimeToPark() {
		return currentTimeToPark;
	}
	
	public void setCurrentTimeToPark() {
		this.currentTimeToPark++;
	}
	
	public int getTimeToPark() {
		return timeToPark;
	}

	@Override
	public double computeScore(Object objectToScore) {
		return this.utility.getScore(objectToScore, this);
	}
	
	@Override
	public double computeSatisfaction(Object objectToScore) {
		return this.utility.getSatisfaction(objectToScore, this);
	}
	
	public double getLastSatisfaction(){
		return totalSatisfaction.get(totalSatisfaction.size() - 1);
	}
	
	@Override
	public Map<String, Object> getAttributesOfAgent() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("Office", getWorkOffice().getId());
		attributes.put("SourceNode", getSourceNode().getId());
		for(Entry<String, Object> information : infos.entrySet()){
			attributes.put(information.getKey(), information.getValue());
		}
		return attributes;
	}
	
	public void setInfos(){
		infos = new HashMap<>();
		infos.put("ZipCode", null);
		infos.put("AverageCommuteTime", null);
	}
	
	@Override
	protected void init() {
		properties   = new HashMap<>();
		worldObjects = new HashMap<>();
		worldObjects.put("beginNode", null);
		worldObjects.put("endNode", null);
		worldObjects.put("office", null);
		worldObjects.put("home", null);
		properties.put("timeSearching", null);
		properties.put("distanceWork", null);
		
		setInfos();
		
		this.timeSearching = new Indicator(EnvVar.SEARCHING_TIME, 0.0);
		this.distanceWork = new Indicator(EnvVar.DISTANCE_FROM_WORK, 1000.0);
		clearHistoric();
		previousBusSatisfaction.add(0.5);
		probaStates = new double[2][2];
	}
	
	public void updateHistoric(Coordinate agentPosition){
		if(agentPosition != null){ //Agent took the car
			if(this.getParkingSpot() != null && isParked()){
				this.previousPrice.add(this.getParkingSpot().getPrice());
				this.previousDistance.add(GISComputation.GPS2Meter(agentPosition, this.getParkingSpot().getPosition()));
				this.previousTime.add(timeSearching.getValue());
				this.previousSatisfaction.add(satisfaction);
				this.previousCommuteTime.add(timeSearching.getValue());// + (Double)infos.get("AverageCommuteTime"));
				this.totalSatisfaction.add(satisfaction);
				this.utilitiesWhenParked.add(utilityWhenParked);
			} else {
				//System.out.println("Did not find a suitable parking spot.");
				this.previousPrice.add(Double.NaN); //Did not find a solution
				this.previousDistance.add(1000.0);//Double.NaN); //Did not find a solution
				this.previousTime.add(600.0);//Double.NaN);
				this.previousSatisfaction.add(0.0); //No satisfaction because agent did not park
				this.totalSatisfaction.add(0.0);
				this.utilitiesWhenParked.add(0.0);
			}
		} else { //Agent took the bus
			this.previousPrice.add(2.0);
			this.previousDistance.add(GISComputation.GPS2Meter(new Coordinate(34.0343, -118.2497), workOffice.getPosition()));
			//Average speed for pedestrians: 4km/h -> 1.11 m/s
			this.previousTime.add(GISComputation.GPS2Meter(new Coordinate(34.0343, -118.2497), workOffice.getPosition())*1.11 + (Double)infos.get("AverageCommuteTime"));
			this.previousSatisfaction.add(utility.getSatisfaction(null, this));
			this.totalSatisfaction.add(utility.getSatisfaction(null, this));
		}
		
	}
	
	public void clearHistoric(){
		previousPrice           = new ArrayList<>();
		previousDistance        = new ArrayList<>();
		previousTime            = new ArrayList<>();
		previousSatisfaction    = new ArrayList<>();
		previousBusSatisfaction = new ArrayList<>();
		previousCommuteTime     = new ArrayList<>();
		totalSatisfaction       = new ArrayList<>();
		utilitiesWhenParked       = new ArrayList<>();
	}

	public double getAverageOfSatisfaction(List<Double> historicSatisfaction, int windowSize, List<Double> discountFactors) {
		if(historicSatisfaction.size() < windowSize){
			//System.out.println("Historic too small...");
			return 1.0;
		} else {
			double average = 0.0;
			double sumOfDiscountFactors = 0.0;
			for(int i = 0; i < windowSize; i++){
				average += historicSatisfaction.get(i) * discountFactors.get(i);
				sumOfDiscountFactors += discountFactors.get(i);
			}
			//System.out.println("Current score: " + average / sumOfDiscountFactors);
			return average / sumOfDiscountFactors;
		}
	}
	
	public boolean changeCurrentState(double score, double lowBound, double highBound){
		if(score > lowBound && score <= highBound){
			return false;
		} else {
			double probaToChange = Double.min(- score/lowBound + 1.2, 1); //Linear function
			//System.out.println("Probability to change modality: " + probaToChange);
			Random rnd = new Random();
			return rnd.nextDouble() <= probaToChange ? true : false;
		}
	}
	
	public Map<String, Boolean> getBooleans() {
		return booleans;
	}

}