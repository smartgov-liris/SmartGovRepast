package microagent;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import simulation.HumanIndicator;

@SuppressWarnings("deprecation")
public abstract class AbstractUtility<P extends AbstractProperties> {

	protected static String MINIMUM = "min";
	protected static String MAXIMUM = "max";
	protected static String WEIGHT = "weight";
	protected static String COEFA = "normCoefA";
	protected static String COEFB = "normCoefB";
	protected static String TYPE = "TYPE";
	protected static String TEXT = "TEXT";
	protected static String CATEGORIE = "CATEGORIE";
	protected static String MODE = "MODE";
	
	protected Map<String, Map<String, Double>> indicatorsCoefs;
	protected Map<String, Double> areaCoefs;
	protected Map<String, HumanIndicator> indicators;
	
	protected String profileName;
	
	public AbstractUtility(Map<String, String> jsonFiles, String profile){
		indicatorsCoefs = new HashMap<>();
		areaCoefs = new HashMap<>();
		readAreaCoefsFromJson(null, profile);
		indicators = new HashMap<>();
		for(Entry<String, String> file : jsonFiles.entrySet()){
			readJson(file.getValue(), profile, file.getKey());
		}
	}
	
	public AbstractUtility(Map<String, String> jsonFiles, String profile, String areaCoefFile){
		indicatorsCoefs = new HashMap<>();
		areaCoefs = new HashMap<>();
		readAreaCoefsFromJson(areaCoefFile, profile);
		indicators = new HashMap<>();
		for(Entry<String, String> file : jsonFiles.entrySet()){
			readJson(file.getValue(), profile, file.getKey());
		}
		profileName = profile;
	}
	
	protected double getScoreFor(String indicatorName, double value){
		Map<String, Double> values = indicatorsCoefs.get(indicatorName);
		double score = values.get(COEFA)*value + values.get(COEFB); //Linear function
		if(score > 1){
			return 1;
		} else if(score < 0){
			return 0;
		} else {
			return score;
		}
	}
	
	public abstract double getScore(Object objectToScore, P properties);
	
	public double getScore(String mode, Object objectToScore, P properties){
		double normalIndScoreSum = 0.0;
		double normalIndCoefSum = 0.0;
		double bonusIndScoreSum = 0.0;
		double bonusIndCoefSum = 0.0;
		for(Entry<String, HumanIndicator> indicator : indicators.entrySet()){
			if(indicator.getValue().getMode().equals(mode)){
				if(indicator.getValue().getType().equals("normal")){
					normalIndScoreSum += getScoreFor(indicator.getKey(), parseCategorieFromIndicator(objectToScore, properties, indicator.getValue())) * getValuesOf(indicator.getKey()).get(WEIGHT);
					normalIndCoefSum += getValuesOf(indicator.getKey()).get(WEIGHT);
				} else if (indicator.getValue().getType().equals("bonus")){
					bonusIndScoreSum += getScoreFor(indicator.getKey(), parseCategorieFromIndicator(objectToScore, properties, indicator.getValue())) * getValuesOf(indicator.getKey()).get(WEIGHT);
					bonusIndCoefSum += getValuesOf(indicator.getKey()).get(WEIGHT);
				}
			}
		}
		return Double.min((normalIndScoreSum / normalIndCoefSum) + (bonusIndScoreSum / (normalIndCoefSum + bonusIndCoefSum)), 1.0);
	}
	
	public double getScore(String mode, Object objectToScore, P properties, String tag){
		double normalIndScoreSum = 0.0;
		double normalIndCoefSum = 0.0;
		double bonusIndScoreSum = 0.0;
		double bonusIndCoefSum = 0.0;
		if(tag == null || tag.equals("")) {
			tag = "Default";
		}
		for(Entry<String, HumanIndicator> indicator : indicators.entrySet()){
			if(indicator.getValue().getMode().equals(mode)){
				if(indicator.getValue().getType().equals("normal")){
					normalIndScoreSum += getScoreFor(indicator.getKey(), parseCategorieFromIndicator(objectToScore, properties, indicator.getValue())) * getValuesOf(indicator.getKey()).get(WEIGHT) * areaCoefs.get(tag);
					normalIndCoefSum += getValuesOf(indicator.getKey()).get(WEIGHT);
				} else if (indicator.getValue().getType().equals("bonus")){
					bonusIndScoreSum += getScoreFor(indicator.getKey(), parseCategorieFromIndicator(objectToScore, properties, indicator.getValue())) * getValuesOf(indicator.getKey()).get(WEIGHT) * areaCoefs.get(tag);
					bonusIndCoefSum += getValuesOf(indicator.getKey()).get(WEIGHT);
				}
			}
		}
		return Double.min((normalIndScoreSum / normalIndCoefSum) + (bonusIndScoreSum / (normalIndCoefSum + bonusIndCoefSum)), 1.0);
	}
	
	public abstract double getSatisfaction(Object objectToScore, P properties);
	
	public double getSatisfaction(String mode, Object objectToScore, P properties){
		double normalIndScoreSum = 0.0;
		double normalIndCoefSum = 0.0;
		double bonusIndScoreSum = 0.0;
		double bonusIndCoefSum = 0.0;
		for(Entry<String, HumanIndicator> indicator : indicators.entrySet()){
			if(indicator.getValue().getMode().equals(mode)){
				if(indicator.getValue().getType().equals("normal")){
					normalIndScoreSum += getScoreFor(indicator.getKey(), parseCategorieFromIndicator(objectToScore, properties, indicator.getValue())) * getValuesOf(indicator.getKey()).get(WEIGHT);
					normalIndCoefSum += getValuesOf(indicator.getKey()).get(WEIGHT);
				} else if (indicator.getValue().getType().equals("bonus")){
					bonusIndScoreSum += getScoreFor(indicator.getKey(), parseCategorieFromIndicator(objectToScore, properties, indicator.getValue())) * getValuesOf(indicator.getKey()).get(WEIGHT);
					bonusIndCoefSum += getValuesOf(indicator.getKey()).get(WEIGHT);
				}
			}
		}
		return Double.max((normalIndScoreSum / normalIndCoefSum) - (bonusIndScoreSum / (normalIndCoefSum + bonusIndCoefSum)), 0.0);
	}
	
	protected double getNormaliseValueBetween(double minimum, double maximum, double value){
		return (value - minimum)/(maximum - minimum);
	}
	
	protected double normCoefA(double minimum, double maximum, double nominalValue, double maxValue){
		return (maximum - minimum)/(nominalValue - maxValue);
	}
	
	protected double normCoefB(double minimum, double nominalValue, double maxValue){
		return (maxValue - minimum)/(nominalValue - maxValue);
	}
	
	protected double slope(double highUtilityBound, double lowUtilityBound){
		if(highUtilityBound == lowUtilityBound){
			return 1;
		}
		return (1/(highUtilityBound-lowUtilityBound));
	}
	
	protected double plug(double slope, double highUtilityBound){
		return (1-slope*highUtilityBound);
	}
	
	protected void readJson(String jsonFile, String profile, String indicatorName){
		JSONParser parser = new JSONParser();
		try{
			Object object = parser.parse(new FileReader(jsonFile));
			JSONObject currentJson = (JSONObject) object;
			JSONObject profileJson = (JSONObject) currentJson.get(profile);
			JSONObject infoJson = (JSONObject) currentJson.get("Info");
			
			Map<String, Double> values = new HashMap<>();
			values.put(MINIMUM, ((Number)infoJson.get("MIN")).doubleValue());
			values.put(MAXIMUM, ((Number)infoJson.get("MAX")).doubleValue());
			values.put(WEIGHT, ((Number)profileJson.get("weight")).doubleValue());
			
			Map<String, String> attributes = new HashMap<>();
			attributes.put(TYPE, (String) infoJson.get(TYPE));
			attributes.put(TEXT, (String) infoJson.get(TEXT));
			attributes.put(MODE, (String) infoJson.get(MODE));
			attributes.put(CATEGORIE, (String) infoJson.get(CATEGORIE));
			
			JSONObject intervalsJson = (JSONObject) profileJson.get("intervals");
			int index = 0;
			List<Double> bornesList = new ArrayList<>();
			List<Integer> utilities = new ArrayList<>();
			double maxUtility = Double.MIN_VALUE;
			double minUtility = Double.MAX_VALUE;
			int indexOfMaxUtility = -1;
			int indexOfMinUtility = -1;
			while(true){
				JSONObject currentInterval = (JSONObject) intervalsJson.get(String.valueOf(index));
				if(currentInterval == null){
					break;
				}
				bornesList.add(((Number)currentInterval.get("borne")).doubleValue());
				utilities.add(((Number)currentInterval.get("utility")).intValue());
				if(maxUtility < ((Number)currentInterval.get("utility")).doubleValue()){
					maxUtility = ((Number)currentInterval.get("utility")).doubleValue();
					indexOfMaxUtility = index;
				}
				if(minUtility > ((Number)currentInterval.get("utility")).doubleValue()){
					minUtility = ((Number)currentInterval.get("utility")).doubleValue();
					indexOfMinUtility = index;
				}
				index++;
			}
			if(bornesList.size() == 1){
				values.put(COEFA, slope(bornesList.get(0), bornesList.get(0)));
				values.put(COEFB, plug(values.get(COEFA), bornesList.get(0)));
			} else {
				values.put(COEFA, slope(bornesList.get(indexOfMaxUtility), bornesList.get(indexOfMinUtility)));
				values.put(COEFB, plug(values.get(COEFA), bornesList.get(indexOfMaxUtility)));
			}
			indicatorsCoefs.put(indicatorName, values);
			indicators.put(indicatorName, new HumanIndicator(indicatorName, values, attributes) );
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Read specific weights for tagged zone in the environment
	 * @param jsonFile Coefficient for tagged zone, if no file is provided the coefficients are set to default
	 * @param profile
	 */
	protected void readAreaCoefsFromJson(String jsonFile, String profile) {
		JSONParser parser = new JSONParser();
		if(jsonFile == null) {
			areaCoefs.put("Default", 1.0);
		} else {
			try{
				Object object = parser.parse(new FileReader(jsonFile));
				JSONObject currentJson = (JSONObject) object;
				for(Object area : currentJson.keySet()) {
					String areaID = (String)area;
					JSONObject currentArea = (JSONObject) currentJson.get(areaID);
					if(currentArea == null) {
						break;
					}
					parseArea(currentArea, profile, areaID);
				}
			} catch (Exception e){
				areaCoefs.put("Default", 1.0);
				e.printStackTrace();
			}
		}
	}
	
	private void parseArea(JSONObject currentArea, String profile, String areaID) {
		JSONObject currentProfile = (JSONObject) currentArea.get(profile);
		areaCoefs.put(areaID, ((Number)currentProfile.get("weight")).doubleValue());
	}
	
	public String getProfileName() {
		return profileName;
	}
	
	protected Map<String, Double> getValuesOf(String name){
		return indicators.get(name).getCoefficients();
	}
	
	protected abstract double parseCategorieFromIndicator(Object objectToScore, P properties, HumanIndicator indicator);
	
	protected abstract Object parseTextFromIndicator(Object objectToScore, P properties, String text) throws NullPointerException;
	
}
