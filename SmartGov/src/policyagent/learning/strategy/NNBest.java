package policyagent.learning.strategy;

import java.io.File;
import java.util.List;

import environment.city.EnvVar;
import policyagent.PolicyAction;
import policyagent.Position;
import simulation.FilePath;
import simulation.manager.AbstractManager;
import simulation.socket.ClientCommunication;

/**
 * Deep Reinforcement Learning strategy using tensorflow and Keras.
 * @author Simon
 *
 */
public class NNBest extends Strategy {

	private static final String NNEXTENSION = ".hdf5";
	private static final String TEXTENTENSION = ".txt";

	/**
	 * Batch size to replay part of the experience
	 */
	private final int batchSize;

	/**
	 * Save every n calls of "chooseAction" where saveCounter is incremented.
	 */
	private static final int SAVE = 10;

	private Position lastPerception;
	private String id;
	private int saveCounter;
	private int experienceCounter;

	private String modelFileName;
	private String memoryFileName;
	private String modelFilePath;
	private String memoryFilePath;
	private String callbackFilePath;
	
	private boolean learning = true;
	private boolean validation = false;

	public NNBest(String id, int stateSize, int nbActions, List<String> labels, List<PolicyAction> policyActions) {
		super(policyActions);
		
		//Add callbacks folder to localLearner
		new File(FilePath.currentLocalLearnerFolder + "callbacks").mkdirs();

		this.id = id;
		String answer = "";

		modelFileName  = "model_deepLearner_"  + parseLabels(labels) + id + NNEXTENSION;
		memoryFileName = "memory_deepLearner_" + parseLabels(labels) + id + TEXTENTENSION;
		String memoryToCopyPath = "";
		String modelToCopyPath = "";
		
		if(Integer.parseInt(EnvVar.configFile.get("learning")) == 0) {
			learning = false;
			validation = true;
		}
		
		if(EnvVar.configFile.get("server_debug").equals("0")) {
			memoryFilePath = FilePath.currentLocalLearnerFolder + memoryFileName;
			modelFilePath  = FilePath.currentLocalLearnerFolder + modelFileName;
			memoryToCopyPath = FilePath.currentLocalLearnerFolder;
			modelToCopyPath = FilePath.currentLocalLearnerFolder;
			callbackFilePath = FilePath.currentLocalLearnerFolder + "callbacks" + File.separator;
		} else {
			//Debug mode need backpath for everything
			memoryFilePath = FilePath.backPath + FilePath.currentLocalLearnerFolder + memoryFileName;
			modelFilePath  = FilePath.backPath + FilePath.currentLocalLearnerFolder + modelFileName;
			memoryToCopyPath = FilePath.backPath + FilePath.currentLocalLearnerFolder;
			modelToCopyPath = FilePath.currentLocalLearnerFolder;
			callbackFilePath = FilePath.backPath + FilePath.currentLocalLearnerFolder + "callbacks" + File.separator;
		}

		batchSize = Integer.parseInt(EnvVar.configFile.get("batch_size"));
		if(EnvVar.configFile.get("initNN").equals("1")) {
			stillExploration = true;

			if(EnvVar.configFile.get("callbacks").equals("1")) {
				answer = ClientCommunication.communicationWithServer(
						"create_model," + 
								id + "," + 
								stateSize + "," + 
								nbActions + "," +
								memoryFilePath + "," +
								callbackFilePath);
			} else {
				answer = ClientCommunication.communicationWithServer(
						"create_model," + 
								id + "," + 
								stateSize + "," + 
								nbActions + "," +
								memoryFilePath);
			}
		} else if(EnvVar.configFile.get("initNN").equals("0")){
			/*/
			stillExploration = false;
			answer = ClientCommunication.communicationWithServer("load_model," + id + "," + stateSize + "," + nbActions + "," +
					memoryFilePath + "," +
					getModelFile());
			//*/
			if(learning) {
				answer = ClientCommunication.communicationWithServer(
						"copy_memory," + 
								getFileForExtension(".txt") + "," +
								memoryToCopyPath);
				answer = ClientCommunication.communicationWithServer(
						"load_model_with_memory," + 
								id + "," + 
								stateSize + "," + 
								nbActions + "," +
								memoryFilePath + "," +
								getFileForExtension(".hdf5"));
			} else if(validation) {
				answer = ClientCommunication.communicationWithServer(
						"copy_memory," + 
								getFileForExtension(".txt") + "," +
								memoryToCopyPath);
				answer = ClientCommunication.communicationWithServer(
						"copy_model," +
								getFileForExtension(".hdf5") + "," +
								modelToCopyPath);						
				answer = ClientCommunication.communicationWithServer(
						"load_only_model," + 
								id + "," + 
								stateSize + "," + 
								nbActions + "," +
								memoryFilePath + "," +
								modelFilePath);
			}
		} else {
			//Only load model
			stillExploration = false;
			answer = ClientCommunication.communicationWithServer("load_only_model," + id + "," + stateSize + "," + nbActions + "," +
					memoryFilePath + "," +
					modelFilePath);
		}
		if(answer.equals("1")) {
			System.out.println("Correctly create model for " + id + ".");
		} else if(answer.equals("2")) {
			System.out.println("Correctly load model for " + id + ".");
		}

		experienceCounter = 0;
		saveCounter = 0;
	}

	@Override
	public PolicyAction chooseAction() {
		String answer = "";
		double reward = lastPerception.getCoordinates().get(lastPerception.getCoordinates().size() - 1);
		Position currentState = new Position();
		for(int i = 0; i < lastPerception.getCoordinates().size() - 1; i++) {
			currentState.addCoordinate(lastPerception.getCoordinates().get(i));
		}
		//*/
		if(learning) {
			replayModelMessage();
		}
		//*/
		if(!EnvVar.manager.isRecentlyReset()) {
			answer = ClientCommunication.communicationWithServer(
					"next_step," + 
							id + "," + 
							lastAction.getIndex() + "," +
							currentState + "," +
							reward);
			lastAnswer = answer;
		} else {
			//*/ Submit special message to not call recall function of agent
			answer = ClientCommunication.communicationWithServer(
					"replay_and_predict," + 
							id + "," + 
							currentState);
			lastAnswer = answer;
		}
		try {
			if(answer.split("_")[2].equals("0")) {
				stillExploration = true;
			} else {
				stillExploration = false;
			}
		} catch (NullPointerException|ArrayIndexOutOfBoundsException e) {
			System.out.println(answer);
			e.printStackTrace();
		}
		if(learning) {
			saveModelMessage();
		}

		return parseActionFromRequest(answer);
	}

	public void setLastPerception(Position lastPerception) {
		this.lastPerception = lastPerception;
	}

	private String parseLabels(List<String> labels) {
		String str = "";
		for(int i = 0; i < labels.size(); i++) {
			if(!labels.get(i).equals("reward")) {
				str += labels.get(i).substring(0, 1);
				str += "_";
			}
		}
		return str;
	}

	private PolicyAction parseActionFromRequest(String answer) {
		String[] splitAnswer = answer.split("_");
		PolicyAction action = policyActions.get(Integer.parseInt(splitAnswer[1]));
		lastPredictedAction = action;
		return action;
	}

	public void saveModelMessage() {
		if(saveCounter == SAVE) {
			saveCounter = 0;
			String saveMessage = ClientCommunication.communicationWithServer(
					"save_model," + 
							id + "," + 
							modelFilePath);
			if(EnvVar.configFile.get("server_debug").equals("1")) {
				System.out.println(saveMessage);
			}
		} else {
			saveCounter++;
		}
	}

	public void replayModelMessage() {
		if(!AbstractManager.validationPhase) {
			if(experienceCounter > (batchSize +10)) {
				String replayMessage = ClientCommunication.communicationWithServer(
						"replay_model," + 
								id + "," + 
								batchSize);
				if(EnvVar.configFile.get("server_debug").equals("1")) {
					System.out.println(replayMessage);
				}
			}
			experienceCounter++;
		}
	}

	/**
	 * Load memory with '.txt' extension and model with '.hdf5'.
	 * @param extension
	 * @return
	 */
	private String getFileForExtension(String extension) {
		String folders = EnvVar.configFile.get("model_folder");
		for(String folder : folders.split(",")) {
			File path = new File(FilePath.localLearnerFolder + folder);
			for (final File fileEntry : path.listFiles()) {
				if (!fileEntry.isDirectory()) {
					String filename = fileEntry.getName();
					if(filename.contains("_" + id + extension)) {
						if(EnvVar.configFile.get("server_debug").equals("0")) {
							return FilePath.localLearnerFolder + folder + "\\" + filename;
						} else {
							return FilePath.backPath + FilePath.localLearnerFolder + folder + "\\" + filename;
						}

					}
				}
			}
		}
		return "";
	}

}
