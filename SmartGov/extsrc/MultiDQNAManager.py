from QDNAgent import DQNAgent
import shutil


class MultiDQNAManager:
    """Doc To Do."""

    def __init__(self):
        """Constructor."""
        self.models = {}

    def add_model(self, 
        model_id, 
        state_size, 
        action_size, 
        memory_path, 
        model_path=None,
        callback_folder=None,
        id_creation=1):
        """A."""
        #self.models[model_id] = DQNAgent(state_size, action_size)
        self.models[model_id] = DQNAgent(state_size, action_size, model_id, memory_path, model_path, callback_folder, id_creation)
        if model_path is None:
            self.models[model_id].create_model()
        else:
            self.models[model_id].load_model(model_path)
        return True

    def save_model(self, model_id, path):
        """B."""
        self.models[model_id].save_model(path)
        return True

    def act(self, model_id, state):
        """C."""
        return self.models[model_id].act(state)

    def remember(self, model_id, previous_action, reward, state):
        """D."""
        self.models[model_id].remember_online(previous_action, reward,
                                              state)
        return True

    def remember_new_line(self, model_id, state):
        self.models[model_id].remember_fresh(state)
        return True

    def recall(self, model_id, state):
        """E'."""
        return self.models[model_id].recall_online(state)

    def replay(self, model_id, batch_size):
        return self.models[model_id].replay(batch_size)

    def predict(self, model_id, state):
        return self.models[model_id].predict_online(state)
