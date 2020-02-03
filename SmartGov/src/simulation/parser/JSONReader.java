package simulation.parser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import repast.simphony.space.gis.Geography;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

import environment.city.Building;
import environment.city.BuildingType;
import environment.city.EnvVar;
import environment.city.Home;
import environment.city.WorkOffice;
import environment.city.parking.BlockFace;
import environment.city.parking.ParkingSpot;
import environment.graph.Arc;
import environment.graph.Node;
import environment.graph.Road;
import environment.graph.SinkNode;
import environment.graph.EdgeOSM;
import environment.graph.SourceNode;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfaceShape;

@SuppressWarnings("deprecation")
public abstract class JSONReader {

	static GeometryFactory geoFactory = new GeometryFactory();

	public static void parseOSMFiles(Geography<Object> geography, 
			String nodeFile, String buildingFile, String nodeBuildingFile, String roadFile,
			String edgeFile){
		long beginTime = System.currentTimeMillis();
		List<Integer>[] specialNodes = getSpawnNodes(edgeFile);
		EnvVar.nodes = parseNodeFileAndDeadEnd(nodeFile, specialNodes, geography);
		EnvVar.buildings = parseBuildingFile(buildingFile, nodeBuildingFile, geography);
		EnvVar.roads = parseRoadFile(roadFile);
		EnvVar.edgesOSM = readEdgesOSM(edgeFile, roadFile, EnvVar.nodes, geography);
		System.out.println("Time to process 'parseOSMFiles': " + (System.currentTimeMillis() - beginTime) + "ms.");
	}

	public static void parseBlockFaces(Geography<Object> geography,
			String blockfaceFile, String positionParkingFile){
		long beginTime = System.currentTimeMillis();
		EnvVar.blockfaces = readBlockFaces(blockfaceFile);
		EnvVar.parkingSpots = readParking(EnvVar.blockfaces, positionParkingFile, geography);
		System.out.println("Time to process 'parseBlockFaces': " + (System.currentTimeMillis() - beginTime) + "ms.");
	}	
	
	public static List<Node> parseNodeFileAndDeadEnd(String nodeFile, List<Integer>[] specialNodes, Geography<Object> geography){
		List<Node> nodes = new ArrayList<>();
		
		System.out.println("Number of source nodes: " + specialNodes[0].size() + ", number of sink nodes: " + specialNodes[1].size());
		int sinkNodesSize = specialNodes[1].size();
		int sourceNodesSize = specialNodes[0].size();
		int sinkNodeCounter = 0;
		int sourceNodeCounter = 0;
		
		JSONParser parser = new JSONParser();
		try{
			JSONObject object = (JSONObject) parser.parse(new FileReader(nodeFile));
			int index = 0;
			while(true){
				JSONObject currentNode = (JSONObject) object.get(String.valueOf(index));

				if(currentNode == null){
					break;
				}
				Node node = null;
				
				//We add null element to conserve: id = position in list
				if(specialNodes[0].contains(index)){
					node = new SourceNode(geography, String.valueOf(index), new Coordinate((double) currentNode.get("lon"), (double) currentNode.get("lat")));
					EnvVar.sourceNodes.add((SourceNode) node);
					sourceNodeCounter++;
					if(sinkNodeCounter < sinkNodesSize){
						EnvVar.sinkNodes.add(null);
					}
					
					//System.out.println("Spawn node: " + node.getId());
				} else if(specialNodes[1].contains(index)){
					node = new SinkNode(geography, String.valueOf(index), new Coordinate((double) currentNode.get("lon"), (double) currentNode.get("lat")));
					EnvVar.sinkNodes.add((SinkNode) node);
					sinkNodeCounter++;
					if(sourceNodeCounter < sourceNodesSize){
						EnvVar.sourceNodes.add(null);
					}
					//System.out.println("Despawn node: " + node.getId());
				} else {
					node = new Node(geography, String.valueOf(index), new Coordinate((double) currentNode.get("lon"), (double) currentNode.get("lat")));
					if(sinkNodeCounter < sinkNodesSize){
						EnvVar.sinkNodes.add(null);
					}
					if(sourceNodeCounter < sourceNodesSize){
						EnvVar.sourceNodes.add(null);
					}
				}
				//Node node = new Node(geography, String.valueOf(index), new Coordinate((double) currentNode.get("lon"), (double) currentNode.get("lat")));
				nodes.add(node);
				index++;
			}
		} catch (Exception e) {
			System.out.println("Error in Parsing Nodes: ");
			e.printStackTrace();
		}

		return nodes;
	}

	/**
	 * Use to get dead end in graph. Nodes with no incoming edges will be spawn nodes (agent creation) and nodes with no outcoming edges will be despawn nodes (agent destruction).
	 * @param edgeFile
	 * @return Array of id node list, where the first list contains spawn nodes and the second list contains despawn nodes
	 */
	public static List<Integer>[] getSpawnNodes(String edgeFile){
		long beginTime = System.currentTimeMillis();
		Map<Integer, Integer> incomingNodeNumber = new HashMap<>();
		Map<Integer, Integer> outcomingNodeNumber = new HashMap<>();
		List<Integer> despawnNodeId = new ArrayList<>();
		List<Integer> spawnNodeId = new ArrayList<>();
		
		@SuppressWarnings("unchecked")
		List<Integer>[] listOfNodeIds = new ArrayList[2];
		
		JSONParser parser = new JSONParser();
		try{
			JSONObject object = (JSONObject) parser.parse(new FileReader(edgeFile));
			int index = 0;
			while(true){
				JSONObject currentEdge = (JSONObject) object.get(String.valueOf(index));
				if(currentEdge == null){
					break;
				}
				if((boolean) currentEdge.get("backward")){
					int beginNodeId = Integer.valueOf(((Long) currentEdge.get("startNode")).intValue());
					int targetNodeId = Integer.valueOf(((Long) currentEdge.get("targetNode")).intValue());
					if(outcomingNodeNumber.containsKey(beginNodeId)){
						outcomingNodeNumber.put(beginNodeId, outcomingNodeNumber.get(beginNodeId) + 1);
					} else {
						outcomingNodeNumber.put(beginNodeId, 1);
					}
					if(incomingNodeNumber.containsKey(targetNodeId)){
						incomingNodeNumber.put(targetNodeId, incomingNodeNumber.get(targetNodeId) + 1);
					} else {
						incomingNodeNumber.put(targetNodeId, 1);
					}
					
				}
				index++;
			}
		} catch (Exception e){
			System.out.println("Error in getSpawnNodes: ");
			e.printStackTrace();
		}

		for(Entry<Integer, Integer> element : outcomingNodeNumber.entrySet()){
			//node got only one outcoming edge and no incoming edge
			if(element.getValue() >= 1 && !incomingNodeNumber.containsKey(element.getKey())){//get(element.getKey()) == 0){
				//create a spawn node
				spawnNodeId.add(element.getKey()); //add node id
				//System.out.println("Spawn node identified: " + element.getKey());
			}
		}
		for(Entry<Integer, Integer> element : incomingNodeNumber.entrySet()){
			//node got only one incoming edge and no outcoming edge
			if(element.getValue() >= 1 && !outcomingNodeNumber.containsKey(element.getKey())){//get(element.getKey()) == 0){
				//create a despawn node
				despawnNodeId.add(element.getKey()); //add node id
				//System.out.println("Despawn node identified: " + element.getKey());
			}
		}
		listOfNodeIds[0] = spawnNodeId;
		listOfNodeIds[1] = despawnNodeId;
		System.out.println("Time to process spawns node identification: " + (System.currentTimeMillis() - beginTime) + "ms.");
		return listOfNodeIds;
	}

	public static List<EdgeOSM> readEdgesOSM(String edgeFile, String roadFile, List<Node> nodes, Geography<Object> geography){
		List<Arc> edges = new ArrayList<>();
		List<EdgeOSM> edgesOSM = new ArrayList<>();

		JSONParser parser = new JSONParser();
		try{
			JSONObject object = (JSONObject) parser.parse(new FileReader(edgeFile));
			int index = 0;
			while(true){
				JSONObject currentEdge = (JSONObject) object.get(String.valueOf(index));
				if(currentEdge == null){
					break;
				}

				Node beginNode = nodes.get(Integer.valueOf(((Long) currentEdge.get("startNode")).intValue()));
				Node targetNode = nodes.get(Integer.valueOf(((Long) currentEdge.get("targetNode")).intValue()));

				Coordinate[] coordinates1 = new Coordinate[2];
				coordinates1[0] = beginNode.getPosition();
				coordinates1[1] = targetNode.getPosition();
				MultiLineString roadLine = createMultiLineString(coordinates1);
				int lanes = 0;
				try {
					lanes = Integer.valueOf(((Long) currentEdge.get("lanes")).intValue());
				} catch (Exception e) {
					lanes = 0;
				}
				String type = (String) currentEdge.get("highway");
				
				
				/*
				 * Bug here, TODO know why, disable this and every road become twoway instead of oneway
				 */
				//*/
				if((boolean) currentEdge.get("backward")){
					EdgeOSM edgeOSM = new EdgeOSM(
							geography,
							String.valueOf(index),

							beginNode,
							targetNode,
							(double) currentEdge.get("distance"),
							(boolean) currentEdge.get("backward"),
							(boolean) currentEdge.get("forward"),
							roadLine, 
							lanes,
							type);

					edgesOSM.add(edgeOSM);

					Arc edge = new Arc(
							geography,
							String.valueOf(index),
							EnvVar.roads.get(Integer.valueOf(((Long) currentEdge.get("road")).intValue())),
							beginNode,
							targetNode,
							(double) currentEdge.get("distance"),
							(boolean) currentEdge.get("backward"),
							(boolean) currentEdge.get("forward"),
							roadLine);

					beginNode.addAOutcomingArc(edgeOSM);
					targetNode.addAIncomingArc(edgeOSM);
					edges.add(edge);					
				}
				//*/
				index++;
			}

		} catch (Exception e) {
			System.out.println("Error in Read Edges: ");
			e.printStackTrace();
		}
		return edgesOSM;
	}

	public static List<BlockFace> readBlockFaces(String blockfaceFile){
		List<BlockFace> blockfaces = new ArrayList<>();

		JSONParser parser = new JSONParser();
		try{
			Object object = parser.parse(new FileReader(blockfaceFile));
			JSONObject blockFaceJson = (JSONObject) object;
			JSONArray blockfaceArray = (JSONArray) blockFaceJson.get("value");

			int id = 1;
			BlockFace blockface = null;
			int index = 0;
			while(index < blockfaceArray.size()){
				JSONObject currentBlockFace = (JSONObject) blockfaceArray.get(index);
				id = (int) (long) currentBlockFace.get("BlockfaceId");
				int numberPlaces = (int) (long) currentBlockFace.get("NumParkingPlaces");
				blockface = new BlockFace(id, numberPlaces);
				if((int) (long) currentBlockFace.get("NumParkingPlaces") == 1){
					List<String> aggregateIds = new ArrayList<>();
					aggregateIds.add((String) currentBlockFace.get("AggregateId"));
					blockface.setParkingId(aggregateIds);
					blockfaces.add(blockface);
					index ++;
				} else {
					List<String> aggregateIds = new ArrayList<>();
					for(int i = 0; i < (int) (long) currentBlockFace.get("NumParkingPlaces"); i ++){
						currentBlockFace = (JSONObject) blockfaceArray.get(index + i);
						aggregateIds.add((String) currentBlockFace.get("AggregateId"));
					}
					index += (int) (long) currentBlockFace.get("NumParkingPlaces");
					blockface.setParkingId(aggregateIds);
					blockfaces.add(blockface);
				}
			}

		} catch (Exception e){
			e.printStackTrace();
		}
		return blockfaces;
	}

	public static List<ParkingSpot> readParking(List<BlockFace> blockfaces, String parkingFile, Geography<Object> geography){
		List<ParkingSpot> parkings = new ArrayList<>();
		List<ParkingSpot> usedParkings = new ArrayList<>();

		JSONParser parser = new JSONParser();
		try{
			Object object = parser.parse(new FileReader(parkingFile));
			JSONObject json = (JSONObject) object;
			JSONArray jsonArray = (JSONArray) json.get("value");

			int index = 0;
			while(index < jsonArray.size()){
				JSONObject currentObject = (JSONObject) jsonArray.get(index);
				String id = (String) currentObject.get("SpaceNumber");
				Coordinate coords = new Coordinate(
						(double) currentObject.get("Longitude"), 
						(double) currentObject.get("Latitude"));
				parkings.add(new ParkingSpot(geography, coords, false, id));
				index ++;
			}
			for(int i = 0; i < blockfaces.size(); i++){
				for(int j = 0; j < blockfaces.get(i).getParkingId().size(); j++){
					for(int k = 0; k < parkings.size(); k++){
						if(blockfaces.get(i).getParkingId().get(j).equals(parkings.get(k).getId())){
							blockfaces.get(i).addParking(parkings.get(k));
							parkings.get(k).setBlockface(blockfaces.get(i));
							usedParkings.add(parkings.get(k));
						}
					}
				}
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return usedParkings;
	}	

	private static MultiLineString createMultiLineString(Coordinate[] coordinates){
		CoordinateArraySequence coords = new CoordinateArraySequence(coordinates);
		GeometryFactory geoFactory = new GeometryFactory();
		LineString line = new LineString(coords, geoFactory);
		LineString[] lines = new LineString[1];
		lines[0] = line;
		return new MultiLineString(lines, geoFactory);
	}

	private static SurfaceShape createSurfaceShape(Coordinate[] coordinates, double elevation){
		List<Position> positions = new ArrayList<>();
		for(int i = 0; i < coordinates.length; i++){
			positions.add(new Position(Angle.fromDegrees(coordinates[i].x), Angle.fromDegrees(coordinates[i].y), elevation));
		}
		return new SurfacePolygon(positions);
	}

	private static Coordinate[] convertNodeIDtoCoordinate(String[] nodesID, List<Coordinate> coords){
		Coordinate[] coordinates = new Coordinate[nodesID.length];
		for(int i = 0; i < nodesID.length; i++){
			int index = Integer.valueOf(nodesID[i]);
			coordinates[i] = coords.get(index);
		}
		return coordinates;
	}

	private static List<String> getNodesId(String[] nodesId){
		List<String> nodesIds = new ArrayList<>();
		for(int i = 0; i < nodesId.length; i ++){
			nodesIds.add(nodesId[i]);
		}
		return nodesIds;
	}

	public static List<Building> parseBuildingFile(String buildingFile, String nodeBuildingFile, Geography<Object> geography){
		List<Building> buildings = new ArrayList<>();
		List<Coordinate> coordinatesNodes = new ArrayList<>();

		//Parse building nodes
		JSONParser parserNode = new JSONParser();
		try{
			JSONObject object = (JSONObject) parserNode.parse(new FileReader(nodeBuildingFile));
			int index = 0;
			while(true){
				JSONObject currentNode = (JSONObject) object.get(String.valueOf(index));

				if(currentNode == null){
					break;
				}
				Coordinate coordinate = new Coordinate((double) currentNode.get("lon"), (double) currentNode.get("lat"));
				coordinatesNodes.add(coordinate);
				index++;
			}
		} catch (FileNotFoundException e) {
			System.out.println("No nodeBuildingFile found. No building will be load.");
			return new ArrayList<>();
		} catch (Exception e){
			System.out.println("Error in parsing building file: ");
			e.printStackTrace();
		}

		//Parse buildings
		JSONParser parserBuilding = new JSONParser();
		try{
			JSONObject object = (JSONObject) parserBuilding.parse(new FileReader(buildingFile));

			int index = 0;
			int idBuilding = 0;
			List<String> amenities_activities = Arrays.asList("bar","pub","restaurant","biergarten","cafe","fast_food","food_court","ice_cream","pub","restaurant",
					"college","kindergarten","library","public_bookcase","school","music_school","driving_school","language_school","university", "fuel", "bicycle_rental",
					"bus_station","car_rental","taxi","car_wash","ferry_terminal", "atm","bank","bureau_de_change", "baby_hatch","clinic", "dentist","doctors",
					"hospital","nursing_home","pharmacy","social_facility","veterinary","arts_centre","brothel","casino","cinema","community_centre","fountain",
					"gambling","nightclub","planetarium","social_centre","stripclub","studio","swingerclub","theatre", "animal_boarding", "animal_shelter","courthouse",
					"coworking_space","crematorium","dive_centre","dojo","embassy","fire_station","gym","internet_cafe","marketplace","police","post_office","townhall");
			List<String> building_activities = Arrays.asList("stable", "shop", "kiosk", "garages", "hangar", "stable", "cowshed", "digester",
					"commercial","office","industrial","retail","warehouse","port","cathedral","chapel","church","mosque","temple","synagogue","shrine",
					"civic","hospital","school","stadium","train_station","transportation","university","public");
			List<String> leisure_activities = Arrays.asList("adult_gaming_centre","amusement_arcade","beach_resort","dance","hackerspace","ice_rink","pitch",
					"sports_centre","stadium","summer_camp","swimming_area","water_park","dog_park", "bird_hide", "bandstand", "firepit", "fishing", "garden",
					"golf_course", "marina", "miniature_golf", "nature_reserve", "park", "playground", "slipway", "track", "wildlife_hide", "swimming_pool");
			
			while(true){
				JSONObject currentBuilding = (JSONObject) object.get(String.valueOf(index));
				Map<String, String> attributes = new HashMap<>();
				Coordinate[] coords = null;
				if(currentBuilding == null){
					break;
				}

				int nodeLength = 0;
				for(Iterator<?> iterator = currentBuilding.keySet().iterator(); iterator.hasNext();){
					String key = (String) iterator.next();
					if(key.equalsIgnoreCase("nodes")){
						String[] nodes = ((String)currentBuilding.get(key)).split(" ");
						nodeLength = nodes.length;
						//Convert nodesID to coordinates
						coords = convertNodeIDtoCoordinate(nodes, coordinatesNodes);
						if(nodeLength == 1){
							
							String[] nodesTemp = new String[4];
							nodesTemp[0] = nodes[0];
							nodesTemp[1] = nodes[0];
							nodesTemp[2] = nodes[0];
							nodesTemp[3] = nodes[0];
							coords = convertNodeIDtoCoordinate(nodesTemp, coordinatesNodes);
						} else if (nodes[0] != nodes[nodeLength - 1]){
							//Need to check if nodes form a closed linestring to avoid IllegalArgumentException when loading context
							String[] nodesTemp = new String[nodeLength + 1];
							for(int j = 0; j < nodeLength; j++){
								nodesTemp[j] = nodes[j];
							}
							nodesTemp[nodeLength] = nodes[0];
							coords = convertNodeIDtoCoordinate(nodesTemp, coordinatesNodes);
						}
						
						//System.out.println("Size of coords: " + coords.length);
					} else {
						attributes.put(key, (String)currentBuilding.get(key));
					}
				}
				//Create shape
				double elevation = 0.0;
				SurfaceShape surfaceShape = createSurfaceShape(coords, elevation);

				Building building = null;
				BuildingType type = null;
				
				
				if(coords.length<=1) // Les nodes seuls ne peuvent pas representer une residence
					type = BuildingType.WORKOFFICE;
				else if(attributes.containsKey("shop") && attributes.get("shop") != ""){
					type = BuildingType.WORKOFFICE;
				}
				else if(attributes.containsKey("leisure") && attributes.get("leisure") != "" && leisure_activities.contains(attributes.get("leisure"))){
					type =  BuildingType.WORKOFFICE;
				}
				else if (attributes.containsKey("amenity") && attributes.get("amenity") != "" && amenities_activities.contains(attributes.get("amenity"))){
						type =  BuildingType.WORKOFFICE;
				}
				else if(attributes.containsKey("landuse") && attributes.get("landuse") != ""){
					if(attributes.get("landuse").equalsIgnoreCase("residential"))
						type= BuildingType.HOME;
					else
						type =  BuildingType.WORKOFFICE;
				}
				else if(attributes.containsKey("building") && attributes.get("building") != ""){	
					if (building_activities.contains(attributes.get("building")))
						type =  BuildingType.WORKOFFICE;
					else type = BuildingType.HOME;
				} else {
					//Default case
					type = BuildingType.MIXED;
				}
				if(type!=null){
					switch (type) {
						case WORKOFFICE:
							building = new WorkOffice(geography, String.valueOf(idBuilding), attributes, surfaceShape, coords);
							EnvVar.offices.add((WorkOffice) building);
							break;
						case HOME:
							building = new Home(geography, String.valueOf(idBuilding), attributes, surfaceShape, coords);
							EnvVar.homes.add((Home) building);
							break;
						default:
							building = new Building(geography, String.valueOf(idBuilding), attributes, surfaceShape, coords);
							break;
					}
				
					building.setType(type);
					buildings.add(building);
					idBuilding++;
				} else {
					buildings.add(building);
				}
				index++;
			}

		} catch (Exception e){
			System.out.println("Error in parsing building file: ");
			e.printStackTrace();
		}
		/*
		for(int i = 0; i < buildings.size(); i++){
			Building building = buildings.get(i);
			context.add(building);
			geography.move(building, geoFactory.createPolygon(building.getPolygon()));//(buildings.get(i).getPosition()));
		}
		 */
		return buildings;
	}

	public static List<Road> parseRoadFile(String roadFile){
		List<Road> roads = new ArrayList<>();

		JSONParser parserNode = new JSONParser();
		try{
			JSONObject object = (JSONObject) parserNode.parse(new FileReader(roadFile));
			int index = 0;
			while(true){
				JSONObject currentRoad = (JSONObject) object.get(String.valueOf(index));

				if(currentRoad == null){
					break;
				}
				Map<String, String> attributes = new HashMap<>();
				List<String> nodesId = new ArrayList<>();
				for(Iterator<?> iterator = currentRoad.keySet().iterator(); iterator.hasNext();){
					String key = (String) iterator.next();
					if(key.equalsIgnoreCase("nodes")){
						String[] nodes = ((String)currentRoad.get(key)).split(" ");
						nodesId = getNodesId(nodes);
					} else {
						attributes.put(key, (String)currentRoad.get(key));
					}
				}
				Road road = new Road(String.valueOf(index), attributes, nodesId);
				roads.add(road);
				index++;
			}
		} catch (Exception e){
			System.out.println("Error in parsing road file: ");
			e.printStackTrace();
		}

		return roads;
	}
	
	/**********************************
	 * MANON
	 *********************************/
	
	public static Map<String, String> parseAgentFile(int id, String agentFile) {
		Map<String, String> attributes = new HashMap<>();
		JsonParser parserAgents = new JsonParser();
		try{
			JsonObject agentsJSON = (JsonObject) parserAgents.parse(new FileReader(agentFile));

			int index = 0;
			
			while(index < agentsJSON.size()){
				if(index == id) {
					JsonObject currentAgent = (JsonObject) agentsJSON.get(String.valueOf(index));
					
					if(currentAgent == null){
						break;
					}
					for(Entry<String, JsonElement> element : currentAgent.entrySet()) {
						attributes.put(element.getKey(), element.getValue().getAsString());
					}
					break;
				}
				index++;
			}
			
		} catch (Exception e){
			System.out.println("Error in parsing building file: ");
			e.printStackTrace();
		}
		return attributes;
	}
	
}
