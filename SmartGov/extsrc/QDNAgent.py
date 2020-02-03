from keras.models import load_model, Sequential
from keras.layers import Dense
from keras.optimizers import Adam
from keras.callbacks import ModelCheckpoint, CSVLogger
import numpy as np
import random
import datetime
from shutil import copyfile
from LossHistory import LossHistory

class DQNAgent:
    """Deep Q-Learning agent."""

    def __init__(self,
        state_size,
        action_size,
        id_model,
        memory_path,
        model_path=None,
        callback_folder=None,
        id_creation=1):
        """Constructor."""
        self.state_size = state_size
        self.action_size = action_size
        self.gamma = 0.95    # discount rate
        self.epsilon = 1.0  # exploration rate
        self.epsilon_min = 0.01
        self.epsilon_decay = 0.995 #0.995
        self.learning_rate = 0.01 #0.01
        self.memory_path = memory_path
        self.memory = []
        self.id_model = id_model
        self.callbacks_list = None
        self.now = datetime.datetime.now()
        if model_path is None:
            open(self.memory_path, 'w').close()
        elif id_creation == 2:
            open(self.memory_path, 'w').close()
            #No exploration
            self.epsilon = 0.0
        else:
            file = open(self.memory_path, 'r')
            for line in file:
                self.memory.append(line)
            self.epsilon = self.epsilon_min
        if callback_folder is not None:
            self.callback_folder = callback_folder
            #history = LossHistory()
            csv_logger = CSVLogger(self.callback_folder+'training_'+str(self.id_model)+'.csv', append=True)
            #loss_filepath = self.callback_folder+"loss-{epoch:02d}-{loss:.2f}_"+str(id_model)+'_'+str(self.now.hour)+str(self.now.minute)+str(self.now.second)+".hdf5"
            #acc_filepath= self.callback_folder+"acc-{epoch:02d}-{acc:.2f}_"+str(id_model)+'_'+str(self.now.hour)+str(self.now.minute)+str(self.now.second)+".hdf5"
            #loss_checkpoint = ModelCheckpoint(loss_filepath, monitor='loss', verbose=0, save_best_only=False, save_weights_only=False, mode='auto', period=1)
            #acc_checkpoint = ModelCheckpoint(acc_filepath, monitor='acc', verbose=0, save_best_only=False, save_weights_only=False, mode='auto', period=1)
            #self.callbacks_list=[loss_checkpoint, acc_checkpoint]
            self.callbacks_list=[csv_logger]

    def create_model(self):
        """Create a model."""
        model = Sequential()
        model.add(Dense(20, input_dim=self.state_size, activation='sigmoid'))
        model.add(Dense(self.action_size, activation='linear'))

        model.compile(loss='mse', optimizer=Adam(lr=self.learning_rate), metrics=['accuracy'])
        self.model = model

    def load_memory_extern(self, memory_path, memory_to_clone):
        copyfile(memory_to_clone, memory_path)
        file = open(self.memory_path, 'r')
        for line in file:
            self.memory.append(line)

    def load_model(self, path):
        """Load the model from path."""
        self.model = load_model(path)

    def save_model(self, path):
        """Save the model into path."""
        self.model.save(path)

    def remember(self, previous_action, reward,
                 state, path):
        """Save the current state into a file."""
        file = open(path, "a")
        file.write( str(previous_action)                       + ',')
        file.write( str(reward)                                + ',')
        file.write( (' ').join(str(s) for s in state)          + '\n')
        file.write( (' ').join(str(s) for s in state)          + ',')

    def remember(self, line, state, path):
        """Save the current state into a file."""
        file = open(path, "a")
        file.write( str(line) )

    def remember_online(self, previous_action, reward,
                        state):
        """Save the current state into memory."""
        # Complete the previous memory line
        line = self.memory[-1]
        line += str(previous_action)              + ','
        line += str(reward)                       + ','
        line += (' ').join(str(s) for s in state) + '\n'
        self.memory[-1] = line
        # Create a new line with the current state.
        self.memory.append((' ').join(str(s) for s in state) + ',')
        self.remember(
            line,
            state,
            self.memory_path
            )

    def remember_fresh(self, state):
        #line = self.memory[-1]
        #line = (' ').join(str(s) for s in state) + ','
        #Overwrite previous entry
        if len(self.memory) == 0:
            self.memory.append((' ').join(str(s) for s in state) + ',')
        else:
            self.memory[-1] = (' ').join(str(s) for s in state) + ','
        #self.memory.append((' ').join(str(s) for s in state) + ',')
        #self.remember(
        #    line,
        #    state,
        #    self.memory_path)

    def recall(self, path):
        """Recall previous state."""
        file = open(path, "r")
        line = file.readlines()[-1]
        line = line.split(',')
        state = []
        for i in line[0].split(' '):
            state.append(float(i))
        return state

    def append_state_to_memory(self, state):
        self.memory.append((' ').join(str(s) for s in state) + ',')

    def recall_online(self, state):
        try:
            line = self.memory[-1]
            line = line.split(',')
            previous_state = []
            for i in line[0].split(' '):
                previous_state.append(float(i))
            return previous_state
        except IndexError:
            self.append_state_to_memory(state)
            return state

    def act(self, state):
        """Chose an action to perform."""
        if np.random.rand() <= self.epsilon:
            #print("Random action")
            return random.randrange(self.action_size), '0'
        #print("Predicted action")
        act_values = self.model.predict(self.list_to_nparray(state))
        return np.argmax(act_values[0]), '1'  # returns action

    def replay(self, batch_size):
        minibatch = random.sample(self.memory[0:-1], int(batch_size))
        X = []
        Y = []
        for line in minibatch:
            new_line = line.split(",")
            state = [float(e) for e in new_line[0].split(' ')]
            state_formated = self.list_to_nparray(state)
            action = int(new_line[1])
            reward = float(new_line[2])
            next_state = [float(e) for e in new_line[3].split(' ')]
            next_state = self.list_to_nparray(next_state)
            target = reward + self.gamma * \
                    np.amax(self.model.predict(next_state))#[0])
            target_f = self.model.predict(state_formated)
            target_f[0][action] = target
            X.append(state)
            Y.append(target_f)
        X = np.asarray(X)
        Y = np.asarray(Y)
        Y = Y.reshape((len(Y), self.action_size))
        if self.callbacks_list is None:
            self.model.fit(X, Y, batch_size=int(batch_size), epochs=10, verbose=0)
        else:
            self.model.fit(X, Y, batch_size=int(batch_size), epochs=10, verbose=0, callbacks=self.callbacks_list)

        if self.epsilon > self.epsilon_min:
            self.epsilon *= self.epsilon_decay
            #print("Current epsilon value {}".format(self.epsilon))

        #X = []
        #Y = []
        #for line in self.memory[0:-1]:
        #    new_line = line.split(",")
        #    state = [float(e) for e in new_line[0].split(' ')]
        #    state_formated = self.list_to_nparray(state)
        #    action = int(new_line[1])
        #    reward = float(new_line[2])
        #    next_state = [float(e) for e in new_line[3].split(' ')]
        #    next_state = self.list_to_nparray(next_state)
        #    target = reward + self.gamma * np.amax(self.model.predict(next_state))#[0])
        #    target_f = self.model.predict(state_formated)
        #    target_f[0][action] = target
        #    X.append(state)
        #    Y.append(target_f)
        #X = np.asarray(X)
        #Y = np.asarray(Y)
        #Y = Y.reshape((len(Y), self.action_size))
        #self.model.fit(X, Y, batch_size=int(batch_size), epochs=10, verbose=0)
        #if self.epsilon > self.epsilon_min:
        #    self.epsilon *= self.epsilon_decay

        #minibatch = random.sample(self.memory[0:-1], int(batch_size))
        #for line in minibatch:
        #    new_line = line.split(",")
        #    state = [float(e) for e in new_line[0].split(' ')]
        #    state_formated = self.list_to_nparray(state)
        #    action = int(new_line[1])
        #    reward = float(new_line[2])
        #    next_state = [float(e) for e in new_line[3].split(' ')]
        #    next_state = self.list_to_nparray(next_state)
        #    target = reward + self.gamma * \
        #            np.amax(self.model.predict(next_state))#[0])
        #    target_f = self.model.predict(state_formated)
        #    target_f[0][action] = target
        #    self.model.fit(state_formated, target_f, epochs=1, verbose=0)
        #if self.epsilon > self.epsilon_min:
        #    self.epsilon *= self.epsilon_decay

    def predict_online(self, state):
        print(self.act(state))
        return self.model.predict(self.list_to_nparray(state))

    def list_to_nparray(self, state):
        state = np.asarray(state)
        return state.reshape((1, self.state_size))
