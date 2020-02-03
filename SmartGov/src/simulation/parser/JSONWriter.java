package simulation.parser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.json.simple.JsonObject;

import environment.Structure;
import microagent.AbstractMicroAgent;
import microagent.AbstractProperties;
import microagent.MicroAgent;
import policyagent.Perimeter;
import policyagent.PolicyAgent;

public class JSONWriter {

	public static void writeAgents(List<AbstractMicroAgent<?>> agents, String fileName){
		JsonObject agentListJSON = new JsonObject();
		
		for(int agentIndex = 0; agentIndex < agents.size(); agentIndex++){
			JsonObject currentAgentToJSON = new JsonObject();
			MicroAgent currentAgent = (MicroAgent) agents.get(agentIndex);
			currentAgentToJSON.put("ID", currentAgent.getId());
			AbstractProperties properties = currentAgent.getProperties();
			for(Entry<String, Object> attribute : properties.getAttributesOfAgent().entrySet()){
				currentAgentToJSON.put(attribute.getKey(), attribute.getValue());
			}
			agentListJSON.put(String.valueOf(agentIndex), currentAgentToJSON);
		}
		writeFile(fileName, agentListJSON);
	}
	
	public static void writeFile(String fileName, JsonObject nodeJSONObj){
		try {
			try {
				Path pathToFile = Paths.get(fileName);
				Files.createDirectories(pathToFile.getParent());
				Files.createFile(pathToFile);
			} catch(FileAlreadyExistsException e) {
				
			}
			
			FileWriter fileWriter = new FileWriter(fileName);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.write(nodeJSONObj.toJson());
			bufferedWriter.flush();
			bufferedWriter.close();
			fileWriter.close();
		} catch (IOException e){
			e.printStackTrace();
		}
	}

	public static void writePolicyAgents(String currentLocalLearnerFolder, String policyagentsfile, List<PolicyAgent> policyAgents) {
		JsonObject policyAgentJSON = new JsonObject();
		int counter = 0;
		for(int policyAgentIndex = 0; policyAgentIndex < policyAgents.size(); policyAgentIndex++){
			if(policyAgents.get(policyAgentIndex) != null) {
				counter++;
				JsonObject currentPolicyAgentToJSON = new JsonObject();
				PolicyAgent policyAgent = (PolicyAgent) policyAgents.get(policyAgentIndex);
				currentPolicyAgentToJSON.put("ID", policyAgent.getId());
				List<String> actions = new ArrayList<>();
				for(int i = 0; i < policyAgent.getActions().size(); i++) {
					actions.add(policyAgent.getActions().get(i).name());
				}
				if(!policyAgent.getSpecialActions().isEmpty()) {
					List<String> specialActions = new ArrayList<>();
					for(int i = 0; i < policyAgent.getSpecialActions().size(); i++) {
						specialActions.add(policyAgent.getSpecialActions().get(i).name());
					}
					currentPolicyAgentToJSON.put("special_actions", specialActions);
				}
				currentPolicyAgentToJSON.put("actions", actions);
				currentPolicyAgentToJSON.put("perimeter", writePerimeter(policyAgent.getPerimeter()));
				policyAgentJSON.put(String.valueOf(policyAgentIndex), currentPolicyAgentToJSON);
			}
		}
		JsonObject infos = new JsonObject();
		infos.put("PolicyAgentNumber", counter);
		policyAgentJSON.put("Info", infos);
		writeFile(currentLocalLearnerFolder + policyagentsfile, policyAgentJSON);
	}
	
	private static JsonObject writePerimeter(Perimeter perimeter) {
		JsonObject perimeterJson = new JsonObject();
		for(Structure structure : perimeter.getStructures()) {
			perimeterJson.put(structure.getID(), structure.getClass().getSimpleName());
		}
		return perimeterJson;
	}
	
}
