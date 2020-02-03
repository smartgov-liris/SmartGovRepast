package simulation.manager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import environment.city.EnvVar;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import simulation.FileName;
import simulation.FilePath;
import simulation.parser.FilesManagement;
import smartGov.ClockSingleton;

/**
 * Abstract management of simulation. Manages local and global indicators, clock, agent population in simulation and policy agents.
 * Saves policies using indicators.
 * @author Simon
 *
 */
public abstract class AbstractManager {

	//Number of simulations per action and per scenario
	public static int NUMBER_OF_ITERATIONS_BEFORE_APPLYING_POLICIES; //NumberOfSimulation
	public static int NUMBER_OF_ITERATIONS_BEFORE_RESTART;
	public static int NUMBER_OF_ITERATIONS_BEFORE_SAVE;
	public static int TOTAL_NUMBER_OF_SIMULATIONS;
	public static int NUMBER_OF_SIMULATIONS_BEFORE_RESTART;
	
	//Time Format
	public static String TIME_FORMAT = "dd-MM-yy-HHmm";

	protected LocalTime pauseSimulationAt;
	protected DateTime startSimulationAt;
	protected int timeToReset;
	protected Context<Object> context;

	protected int iterationCounter;
	protected int restartCounter;
	
	protected long beginTime;
	protected Instant currentInstant;
	public static final SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");

	//TimeStamp for file name
	public static String timeStamp;
	
	//Policy indexes
	protected int currentSetOfSimulationsIndex;
	protected int currentPolicyActionIndex;
	protected int pathwayIndex;
	protected int currentTrialIndex;
	protected int localEvaluationIndex;
	protected int currentSimulationIndex;
	protected int currentIteration; //This number should never be set to 0
	protected boolean currentlyExperimenting;
	
	protected boolean justReset = false;
	
	protected boolean recentlyReset = false;
	
	/**
	 * During this phase, apply an iterative price policy and create initial classes using clustering methods.
	 */
	public static boolean observationPhase = false;
	
	/**
	 * During this phase, apply an action to clustered classes and adapt if needed the current representation.
	 */
	public static boolean learningPhase = false;
	
	/**
	 * During this phase, apply an action based on the perception closest cluster.
	 */
	public static boolean validationPhase = false;

	protected int indexOfAction;
	
	public AbstractManager(){
		readManagerCounters();
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		currentInstant = Instant.now();
		beginTime = System.currentTimeMillis();
		init();
	}

	/**
	 * Live method, called every tick. All manager activity is in here.
	 */
	@ScheduledMethod(start = 1, interval = 1)
	public abstract void live();

	protected void resetClock(){
		ClockSingleton.resetSingleton();
		ClockSingleton.getInstance(startSimulationAt);
	}
	
	protected void parseConfigFile(Map<String, String> configFile){
		NUMBER_OF_ITERATIONS_BEFORE_APPLYING_POLICIES = Integer.parseInt(EnvVar.configFile.get("simulations_per_action"));
		NUMBER_OF_ITERATIONS_BEFORE_RESTART = NUMBER_OF_ITERATIONS_BEFORE_APPLYING_POLICIES * Integer.parseInt(EnvVar.configFile.get("actions_per_scenario"));
		NUMBER_OF_ITERATIONS_BEFORE_SAVE = NUMBER_OF_ITERATIONS_BEFORE_APPLYING_POLICIES;
		TOTAL_NUMBER_OF_SIMULATIONS = Integer.parseInt(EnvVar.configFile.get("total_number_of_simulations"));
		NUMBER_OF_SIMULATIONS_BEFORE_RESTART = Integer.parseInt(EnvVar.configFile.get("simulation_before_restart"));
	}

	protected abstract void clearAgents();

	protected abstract void resetStateOfSimulation();
	
	protected abstract void init();
	
	protected String createTimeStamp(){
		DateFormat dateFormat = new SimpleDateFormat(TIME_FORMAT);
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = new Date(System.currentTimeMillis());
		return dateFormat.format(date);
	}
	
	public int getLocalEvaluationIndex() {
		return localEvaluationIndex;
	}
	
	public int getCurrentTrialIndex() {
		return currentTrialIndex;
	}
	
	public int getIndexOfAction() {
		return indexOfAction;
	}
	
	public void setCurrentTrialIndex(int currentTrialIndex) {
		this.currentTrialIndex = currentTrialIndex;
	}
	
	public boolean isJustReset() {
		return justReset;
	}
	
	public void setJustReset(boolean justReset) {
		this.justReset = justReset;
	}
	
	public boolean isRecentlyReset() {
		return recentlyReset;
	}
	
	public void setRecentlyReset(boolean recentlyReset) {
		this.recentlyReset = recentlyReset;
	}
	
	protected abstract void saveConfigOfSimulation(List<String> additionnalInfo);
	
	public int getCurrentIteration() {
		return currentIteration;
	}
	
	public void setCurrentIteration(int currentIteration) {
		this.currentIteration = currentIteration;
	}
	
	protected void saveManagerCounters() {
		List<String> lines = new ArrayList<>();
		lines.add("iteration:"+currentIteration);
		FilesManagement.writeToFile(FilePath.currentLocalLearnerFolder, FileName.MANAGER_FILE, lines);
	}
	
	protected void readManagerCounters() {
		try {
			List<String> lines = FilesManagement.readFile(FilePath.currentLocalLearnerFolder, FileName.MANAGER_FILE);
			for(String line : lines) {
				String[] splits = line.split(":");
				if(splits[0].equals("iteration")) {
					currentIteration = Integer.parseInt(splits[1]);
				}
			}
			System.out.println("Load simulation previous iteration counter: " + currentIteration);
		} catch (Exception e) {
			System.out.println("No file.");
			currentIteration = 0;
		}
		
	}
	
	public boolean getCurrentlyExperimenting() {
		return currentlyExperimenting;
	}
	
	public void setCurrentlyExperimenting(boolean currentlyExperimenting) {
		this.currentlyExperimenting = currentlyExperimenting;
	}
	
}
