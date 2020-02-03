package simulation.scenario;

import java.io.File;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

import environment.WorldObjectTexture;
import environment.city.Building;
import environment.city.EnvVar;
import environment.graph.Arc;
import environment.graph.SourceNode;
import microagent.MicroAgent;
import repast.simphony.context.Context;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import simulation.FilePath;
import simulation.GISComputation;
import simulation.parser.JSONReader;

/**
 * Primary components for scenario visualization.
 * @author Simon
 */
public abstract class ScenarioBasic {
	
	public ScenarioBasic() {
		parseConfigFile();
		EnvVar.init();
	}
	
	protected void parseConfigFile() {
		EnvVar.configFile = EnvVar.parseConfig("input" + File.separator + "config.ini");
	}
	
	/**
	 * Used to carry information when the simulation starts.
	 */
	protected List<String> additionnalInfo;

	protected String specificGISFolder;
	
	/**
	 * Add elements to context and instantiates agents.
	 * @param context
	 * @return Updated context used by SmartGov simulator
	 */
	public abstract Context<Object> loadWorld(Context<Object> context);
	
	public Context<Object> initContext(Context<Object> context){
		Geography<Object> geography = loadDisplay(context);
		loadFeatures(geography);
		populateContext(context, geography, addElementToContext());
		return context;
	}
	
	public abstract MicroAgent createAnAgentWithID(
			int id, 
			Geography<Object> geography, 
			SourceNode sourceNode);
	
	protected Geography<Object> loadDisplay(Context<Object> context){
		GeographyParameters<Object> geoParams = new GeographyParameters<>();
		return GeographyFactoryFinder.createGeographyFactory(null)
				.createGeography("geography", context, geoParams);
	}
	
	protected void populateContext(
			Context<Object> context, 
			Geography<Object> geography, 
			List<List<?>> elementsToBeAdded){
		for(int indexOfList = 0; indexOfList < elementsToBeAdded.size(); indexOfList++){
			long beginTime = System.currentTimeMillis();
			List<?> elements = elementsToBeAdded.get(indexOfList);
			if(!elements.isEmpty()) {
				for(int elementIndex = 0; elementIndex < elements.size(); elementIndex++){
					context.add(elements.get(elementIndex));
					moveElement(elements.get(elementIndex), geography);
				}
				System.out.println("Time to add " + elements.get(0).getClass().getSimpleName() + " to context: " + (System.currentTimeMillis() - beginTime) + "ms.");
			}
		}
	}
	
	protected void moveElement(Object object, Geography<Object> geography){
		if(object instanceof WorldObjectTexture){
			geography.move(object, EnvVar.GEOFACTORY.createPoint(((WorldObjectTexture)object).getPosition()));
		} else if(object instanceof Building){
			geography.move(object, EnvVar.GEOFACTORY.createPolygon(((Building)object).getPolygon()));
		} else if(object instanceof Arc){
			Geometry geom = (LineString) ((Arc)object).getPolyLine().getGeometryN(0);
			geography.move(object, geom);
		}
	}
	
	/**
	 * NodeFile, EdgeFile and RoadFile are required in order to have the minimum
	 * files to create an environment.
	 * BuildingFile and BuildingNodeFile are optional.
	 * @param geography
	 */
	protected void loadFeatures(Geography<Object> geography) {
		String nodeFile            = 	FilePath.GISFolder + specificGISFolder + File.separator + "nodes_for_roads.json";
		String edgeFile            = 	FilePath.GISFolder + specificGISFolder + File.separator + "edges.json";
		String roadFile            = 	FilePath.GISFolder + specificGISFolder + File.separator + "roads.json";
		String buildingFile        =  	FilePath.GISFolder + specificGISFolder + File.separator + "buildings.json";
		String buildingNodeFile    = 	FilePath.GISFolder + specificGISFolder + File.separator + "nodes_for_buildings.json";
		JSONReader.parseOSMFiles(geography, nodeFile, buildingFile, buildingNodeFile, roadFile, edgeFile);
		GISComputation.checkNodesInRoad();
	}
	
	public abstract List<List<?>> addElementToContext();
	
	public List<String> getAdditionnalInfo() {
		return additionnalInfo;
	}
	
	public String getGISFolder() {
		return specificGISFolder;
	}
	
}
