package simulation.scenario;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.filefilter.DirectoryFileFilter;

import environment.city.EnvVar;
import environment.graph.Graph;
import policyagent.PolicyAction;
import repast.simphony.context.Context;
import repast.simphony.space.gis.Geography;
import simulation.FileName;
import simulation.FilePath;
import simulation.GISComputation;
import simulation.parser.JSONReader;
import smartGov.ClockSingleton;

/**
 * Scenario with environment and policy manager layer. 
 * Have a manager that regulates coupling of simulations.
 * @author Simon
 *
 */
public abstract class ScenarioSmartGov extends ScenarioLowerLayer {
	
	/**
	 * Used to store results in a specific folder.
	 */
	protected int simulationIndex = 1;
	
	public abstract Map<String, Number> getInfos();

	protected void createClockSingleton(Context<Object> context){
		ClockSingleton clock = ClockSingleton.getInstance();
		context.add(clock);
	}
	
	protected void loadFeatures(Geography<Object> geography) {
		String nodeFile            = 	FilePath.GISFolder + specificGISFolder + "nodes_for_roads.json";
		String edgeFile            = 	FilePath.GISFolder + specificGISFolder + "edges.json";
		String roadFile            = 	FilePath.GISFolder + specificGISFolder + "roads.json";
		String buildingFile        =  	FilePath.GISFolder + specificGISFolder + "buildings.json";
		String buildingNodeFile    = 	FilePath.GISFolder + specificGISFolder + "nodes_for_buildings.json";
		String positionParkingFile = 	FilePath.structuresFolder + "SensorPosition.json";
		String blockfaceFile       = 	FilePath.structuresFolder + "BlockfaceInfo.json";
		JSONReader.parseOSMFiles(geography, nodeFile, buildingFile, buildingNodeFile, roadFile, edgeFile);
		JSONReader.parseBlockFaces(geography, blockfaceFile, positionParkingFile);
		GISComputation.checkNodesInRoad();
	}
	
	protected void loadFeaturesForSpecificScenarios(Geography<Object> geography) {
		String positionParkingFile = 	FilePath.structuresFolder + "SensorPosition.json";
		String blockfaceFile       = 	FilePath.structuresFolder + "BlockfaceInfo.json";
		JSONReader.parseBlockFaces(geography, blockfaceFile, positionParkingFile);
		GISComputation.checkNodesInRoad();
	}
	
	/**
	 * Used to initiate KDTree.
	 */
	protected Graph createGraph() {
		Graph roadGraph = new Graph(EnvVar.nodes, EnvVar.arcs);
		roadGraph.addParkingToRoad(EnvVar.parkingSpots, EnvVar.edgesOSM, EnvVar.nodes);
		return roadGraph;
	}
	
	/**
	 * Create a specific folder using current date and increment folder 
	 * index using previous folders of the same date.
	 */
	public void createFolder() {
		ZoneId z = ZoneId.of("Europe/Paris");
		LocalDate ld = LocalDate.now(z);
		int dayOfMonth  = ld.getDayOfMonth();
		String day = (dayOfMonth < 10) ? ("0" + dayOfMonth) : String.valueOf(dayOfMonth);
		int monthOfYear = ld.getMonthValue();
		String month = (monthOfYear < 10) ? ("0" + monthOfYear) : String.valueOf(monthOfYear);
		int yearInt = ld.getYear();
		String year = String.valueOf(yearInt).substring(2);
		String date = day + month + year;
		
		File directory = new File(FilePath.localLearnerFolder);
		File[] subdirs = directory.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
		//if(subdirs.length != 0) {
			for (File dir : subdirs) {
				String[] dirDate = dir.getName().split("_");
				if(dirDate[0].equals(date)) {
					if(Integer.parseInt(dirDate[1]) >= simulationIndex) {
						simulationIndex = Integer.parseInt(dirDate[1]) + 1;
					}
				}
			}
		//}
		
		String scenarioID = Integer.parseInt(EnvVar.configFile.get("scenarioID")) > 0 ? "_" + EnvVar.configFile.get("scenarioID") : "";
		String dirName = "";
		if(EnvVar.configFile.get("simulation_debug").equals("0")) {
			dirName = date + "_" + simulationIndex + "_" + EnvVar.configFile.get("scenario") + scenarioID + File.separator;
		} else {
			dirName = date + "_" + simulationIndex + "_debug_" + EnvVar.configFile.get("scenario") + scenarioID + File.separator;
		}
		new File(FilePath.localLearnerFolder + dirName).mkdirs();
		FilePath.currentLocalLearnerFolder = FilePath.localLearnerFolder + dirName;
		
	}
	
	/**
	 * Stakeholder can specify policy actions to apply on the perimeter.
	 * @return
	 */
	protected List<PolicyAction> loadPolicyActions() {
		return new ArrayList<>();
	}
	
	/**
	 * Specify specific policy agent actions to give more control over perimeters.
	 * @return
	 */
	protected List<PolicyAction> loadSpecialPolicyActions() {
		return new ArrayList<>();
	}
	
	/**
	 * If the user specify folders to copy results of the previous simulation, then copy specified files to 
	 * the current local learner folder.
	 */
	protected void copyFiles() {
		if(!EnvVar.configFile.get("model_folder").equals("-")) {
			List<String> filesToCopy = new ArrayList<>();
			if(Integer.valueOf(EnvVar.configFile.get("validation")) == 1) {
				filesToCopy.add(FileName.AgentFile);
			} else {
				filesToCopy.add("_global.txt");
				filesToCopy.add("_actions.txt");
				filesToCopy.add("policyAgentActions.txt");
				filesToCopy.add(FileName.AgentFile);
				filesToCopy.add(FileName.MANAGER_FILE);
				filesToCopy.add(FileName.PolicyAgentsFile);
				filesToCopy.add(FileName.MANAGER_ACTIONS);
			}
			String folders = EnvVar.configFile.get("model_folder");
			for(String folder : folders.split(";")) {
				File sourceFolder = new File(FilePath.localLearnerFolder + folder + File.separator);
				for (final File fileEntry : sourceFolder.listFiles()) {
			        if (!fileEntry.isDirectory()) {
			            String filename = fileEntry.getName();
			            for(String stringToCheck : filesToCopy) {
			            	if(filename.contains(stringToCheck)) {
			            		Path sourceFile = Paths.get(FilePath.localLearnerFolder + EnvVar.configFile.get("model_folder") + File.separator + filename);
			            		Path destinationFolder = Paths.get(FilePath.currentLocalLearnerFolder + filename);
			            		try {
									Files.copy(sourceFile, destinationFolder, StandardCopyOption.REPLACE_EXISTING);
								} catch (IOException e) {
									e.printStackTrace();
								}
			            	}
			            }
			        }
			    }
			}
		}
	}

}
