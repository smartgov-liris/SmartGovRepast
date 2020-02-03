package simulation.scenario.lowlayer;

import java.util.ArrayList;
import java.util.List;

import environment.city.EnvVar;
import environment.graph.SourceNode;
import microagent.MicroAgent;
import repast.simphony.context.Context;
import repast.simphony.space.gis.Geography;
import simulation.FileName;
import simulation.scenario.ScenarioBasic;

/**
 * Specific Scenario with the minimum functions to start visualization of
 * SmartGov environment without human agents.
 * Create an environment representation based on files in VisualizationTest
 * folder.
 * @author Simon
 *
 */
public class ScenarioVisualization extends ScenarioBasic {
	
	public ScenarioVisualization() {
		specificGISFolder = FileName.GIS_VISUALIZATION;
	}

	@Override
	public Context<Object> loadWorld(Context<Object> context) {
		return initContext(context);
	}

	@Override
	public List<List<?>> addElementToContext() {
		List<List<?>> elementsToBeAdded = new ArrayList<>();
		elementsToBeAdded.add(EnvVar.nodes);
		elementsToBeAdded.add(EnvVar.buildings);
		elementsToBeAdded.add(EnvVar.edgesOSM);
		return elementsToBeAdded;
	}

	/**
	 * Not used in Visualization.
	 */
	@Override
	public MicroAgent createAnAgentWithID(int id, Geography<Object> geography, SourceNode sourceNode) {
		return null;
	}

}
