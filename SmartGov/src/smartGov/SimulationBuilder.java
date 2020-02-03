package smartGov;

import javax.swing.JPanel;

import environment.city.EnvVar;
import environment.style.TextureLibrary;
import repast.simphony.ui.RSApplication;
import repast.simphony.context.Context;
import repast.simphony.dataLoader.ContextBuilder;
import simulation.FilePath;
import simulation.SimulationTool;
import simulation.gui.UserPanel;

/**
 * Main class of SmartGov. Loads a scenario and the clock singleton and GUI.
 * @author Simon
 *
 */
public class SimulationBuilder implements ContextBuilder<Object>{

	public static final String ID_NAME = "SmartGov";
	
	/**
	 * Configuration File with parameters for simulations.
	 */
	public static String configFile = FilePath.inputFolder + "config.ini";

	/**
	 * This function is called each time Repast start a simulation. Every variables need to be cleared before a new simulation to avoid bad garbage collection.
	 */
	@Override
	public Context<Object> build(Context<Object> context) {
		long beginTime = System.currentTimeMillis();
		
		context.setId(ID_NAME);
		System.out.println("Building: " + ID_NAME);

		refreshUserPanel();
		
		refreshClock();
		
		EnvVar.clear();
		EnvVar.configFile = EnvVar.parseConfig(configFile);
		
		TextureLibrary.createTexturesAndColors(EnvVar.configFile.get("color_type"));
		
		EnvVar.scenario = EnvVar.loadScenarioNames(EnvVar.configFile.get("scenario"));
		System.out.println("Loading scenario " + EnvVar.scenario.getClass().getSimpleName());
		context = EnvVar.scenario.loadWorld(context);
		System.out.println("Time to process simulation creation: " + (System.currentTimeMillis() - beginTime) + " ms.");
		SimulationTool.BEGIN_SIMULATION_TIME = System.currentTimeMillis();
		return context;
	}
	
	/**
	 * Add custom user panel to track agent informations.
	 */
	private JPanel addCustomPanel(){
		UserPanel panel = new UserPanel();
		return panel.createPanel();
	}
	
	/**
	 * Reset User Panel if one already exists.
	 */
	private void refreshUserPanel(){
		JPanel userPanel = addCustomPanel();
		if(!RSApplication.getRSApplicationInstance().hasCustomUserPanelDefined()){
			RSApplication.getRSApplicationInstance().addCustomUserPanel(userPanel);
		} else {
			RSApplication.getRSApplicationInstance().removeCustomUserPanel();
			RSApplication.getRSApplicationInstance().addCustomUserPanel(userPanel);
		}
	}
	
	/**
	 * Reset clock if one already exists.
	 */
	private void refreshClock(){
		if(ClockSingleton.getInstance() != null){
			ClockSingleton.resetSingleton();
		}
	}

}
