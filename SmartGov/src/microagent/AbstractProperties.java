package microagent;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import simulation.FileName;
import simulation.FilePath;

/**
 * Stores attributes of an agent : home, workoffice, parameters
 * @author Simon Pageaud
 *
 */
@SuppressWarnings("deprecation")
public abstract class AbstractProperties {

	public static Map<String, Double> parseBehaviorFile(String filename){
		Map<String, Double> attributes = new HashMap<String, Double>();

		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(filename));
			try{
				StringBuilder sb = new StringBuilder();
				String line = br.readLine();

				while(line != null){
					String[] values = line.split("=");
					attributes.put(values[0], Double.valueOf(values[1]));
					sb.append(line);
					sb.append(System.lineSeparator());
					line = br.readLine();

				}
			} catch (Exception e){

			} finally {
				try {
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		return attributes;
	}

	public Map<String, Object> parseAttributesForId(int id){
		//String fileName = FilePath.currentAgentDetailsFolder + FileName.AgentFile;
		String fileName = FilePath.currentLocalLearnerFolder + FileName.AgentFile;
		Map<String, Object> attributes = new HashMap<>();

		JSONParser parser = new JSONParser();
		try{
			JSONObject object = (JSONObject) parser.parse(new FileReader(fileName));
			JSONObject currentAgent = (JSONObject) object.get(String.valueOf(id));
			for(Object key : currentAgent.keySet()){
				attributes.put((String)key, currentAgent.get((String)key));
			}
		} catch (Exception e) {
			System.out.println("Error in Parse Attributes: ");
			e.printStackTrace();
		}
		return attributes;
	}

	public Object getSpecificAttributeForId(int id, String attribute){
		return parseAttributesForId(id).get(attribute);
	}

	public static void updateFile(String filename, Map<String, Double> attributes){
		List<String> newLines = new ArrayList<>();
		try {
			for(String line : Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8)){
				String[] values = line.split("=");
				if(attributes.containsKey(values[0])){
					newLines.add(values[0]+"="+attributes.get(values[0]));
				} else {
					newLines.add(line);
				}
			}
			Files.write(Paths.get(filename), newLines, StandardCharsets.UTF_8);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	protected abstract void init();

	public abstract void resetProperties(int id);

	/**
	 * Use this to store specific attributes of the agent.
	 * @return Map of attributes that will be stored
	 */
	public abstract Map<String, Object> getAttributesOfAgent();

	public abstract double computeScore(Object objectToScore);

	public abstract double computeSatisfaction(Object objectToScore);

}
