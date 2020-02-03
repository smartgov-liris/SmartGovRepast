package simulation;

import java.io.File;

/**
 * Store important filename during simulation
 * @author spageaud
 *
 */
public class FileName {

	public static final String AgentFile        = "Agents.json";
	public static final String PolicyAgentsFile = "PolicyAgents.json";
	public static final String MANAGER_FILE     = "manager_attributes.txt";
	public static final String INITIAL_STATE    = "initial_state.txt";
	
	//Manager files
	public static final String MANAGER_PARAMETERS_FILE    = "parameters.txt";
	public static final String MANAGER_STRUCTURES_AVERAGE = "structures_average.txt";
	public static final String MANAGER_BLOCKFACES         = "blockfaces_all.txt";
	public static final String MANAGER_ACTIONS            = "actions_all.txt";
	public static final String MANAGER_VOTES              = "votes_all.txt";
	public static final String MANAGER_LOGS               = "logs.txt";
	
	//GIS Folders
	public static final String GIS_BASIC_SPLIT   = "LA_basic_split"    + File.separator;
	public static final String GIS_BASIC_MERGE   = "LA_basic_merge"    + File.separator;
	public static final String GIS_SPLIT_MERGE   = "LA_splitmerge"     + File.separator;
	public static final String GIS_VISUALIZATION = "VisualizationTest" + File.separator;
	public static final String GIS_ENVIRONMENT   = "Analysis"          + File.separator;
	public static final String GIS_ROAD          = "Roads"             + File.separator;
	
	
}
