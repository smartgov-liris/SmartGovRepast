package simulation.scenario.learningXP;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import policyagent.PolicyAction;
import simulation.FilePath;
import simulation.scenario.ScenarioDRL;

public class ScenarioSplitMergeRandom extends ScenarioDRL {

	public ScenarioSplitMergeRandom() {
		super();
		filename = "splitmergeRandom.json";
	}

	/**
	 * Case 1 : 2 personalities 1,3;2,4
	 * Case 2 :
	 * Case 3 : 2 personalities 1,2;3,4
	 * Case 4 : 1 personality
	 * Case 5 : 2 personalities 1;2
	 * Case 6 : 1 personality 
	 */
	@Override
	protected String loadGISFile() {
		switch (scenarioID) {
		case 1:
			return "four_blocks" + File.separator;
		case 2:
			return "four_blocks_all_connected" + File.separator;
		case 3:
			return "two_blocks" + File.separator;
		case 4:
			return "two_blocks_all_connected" + File.separator;
		default:
			return "";
		}
	}
	
	@Override
	protected Map<String, List<String>> createStructuresPerID() {
		String file = FilePath.perimeterFolder + filename;
		List<String> perimeterList = new ArrayList<>();
		
		/*
		 * Extract all structures of the same type for random policy agent allocation.
		 */
		JsonParser parser = new JsonParser();
		try {
			JsonObject structuresObject = (JsonObject) parser.parse(new FileReader(file));
			for(Entry<String, JsonElement> element : structuresObject.entrySet()) {
				JsonArray scenario = element.getValue().getAsJsonObject().getAsJsonArray("ScenarioID");
				for(int i = 0; i < scenario.size(); i++) {
					if(scenarioID == scenario.get(i).getAsInt()) {
						JsonObject perimeters = element.getValue().getAsJsonObject().getAsJsonObject("Perimeters");
						for(Entry<String, JsonElement> perimeter : perimeters.entrySet()) {
							JsonArray perimeterArray = perimeter.getValue().getAsJsonObject().get("blockface").getAsJsonArray();
							for(int j = 0; j < perimeterArray.size(); j++) {
								perimeterList.add(perimeterArray.get(j).getAsString());
							}
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Error in parsing perimeter for policy agents: ");
			e.printStackTrace();
		}
		
		return shuffle(perimeterList, 5);
	}
	
	protected Map<String, List<String>> shuffle(List<String> structures, int clusterNumber) {
		Map<String, List<String>> structuresIDsPerID = new HashMap<>();
		Random rnd = new Random();
		Collections.shuffle(structures, rnd);
		
		int sum = structures.size();
		
		List<Integer> sumList = new ArrayList<>();
		for(int i = 0; i < clusterNumber; i++) {
			sumList.add(0);
		}
		int groupID = 0;
		
		while(sumList(sumList) != sum) {
			sumList.set(groupID, rnd.nextInt(sum)); 
			groupID++;
			if(groupID == clusterNumber) {
				groupID = 0;
			}
		}
		
		for(int i = 0; i < sumList.size(); i++) {
			List<String> strings = new ArrayList<>();
			for(int j = 0; j < sumList.get(i); j++) {
				strings.add(structures.remove(0));
			}
			structuresIDsPerID.put(i+"", strings);
		}
		
		return structuresIDsPerID;
	}
	
	private int sumList(List<Integer> list) {
		int sum = 0;
		for(int i = 0; i < list.size(); i++) {
			sum += list.get(i);
		}
		return sum;
	}

	@Override
	protected List<PolicyAction> loadSpecialPolicyActions() {
		List<PolicyAction> policyActions = new ArrayList<>();
		policyActions.add(PolicyAction.MERGE);
		policyActions.add(PolicyAction.SPLIT);
		policyActions.add(PolicyAction.ROLLBACK);
		policyActions.add(PolicyAction.KEEP);
		return policyActions;
	}
	
}
