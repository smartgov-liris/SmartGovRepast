#!/usr/bin/env python

"""Server."""
import socket
from MultiDQNAManager import MultiDQNAManager
from argparse import ArgumentParser
import shutil

parser = ArgumentParser()
parser.add_argument("-p", "--port", help="specify port for server execution", default=15555, type=int)
args = parser.parse_args()
print('Port:{}'.format(args.port))
socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
socket.bind(('', args.port))

manager = MultiDQNAManager()

while True:
    socket.listen(5)
    client, address = socket.accept()
    #print("{} connected".format( address ))

    response = client.recv(255).decode()
    #print(response)
    if response != "":
        response = response.split(',')
        print("{} connected".format( address ) + " [" + response[1] + "]" +
            " with \'" + response[0] + "\' message")
        if response[0] == "next_step":
            model_id = response[1]
            previous_action = int(response[2])
            state = [float(i) for i in response[3].split(' ')]
            reward = float(response[4])
            previous_state = manager.recall(model_id, state)
            action, msg = manager.act(model_id, state)
            print("Suggested action: "+str(action)+", type: " + msg)
            manager.remember(model_id, previous_action, reward, state)
            client.send((str(model_id)+'_'+str(action)+'_'+str(msg) + str('\r\n')).encode())
        elif response[0] == "create_model":
            manager.add_model(
                model_id=response[1],
                state_size=int(response[2]),
                action_size=int(response[3]),
                memory_path=response[4],
                callback_folder=response[5])
            client.send(str('1\r\n').encode())
        elif response[0] == "load_model":
            manager.add_model(
                model_id=response[1],
                state_size=int(response[2]),
                action_size=int(response[3]),
                memory_path=response[4],
                model_path=response[5])
            client.send(str('2\r\n').encode())
        elif response[0] == "load_only_model":
            manager.add_model(
                model_id=response[1],
                state_size=int(response[2]),
                action_size=int(response[3]),
                memory_path=response[4],
                model_path=response[5],
                id_creation=2)
            client.send(str('2\r\n').encode())
        elif response[0] == "copy_memory":
            shutil.copy(response[1], response[2])
            client.send(str('2\r\n').encode())
        elif response[0] == "copy_model":
            shutil.copy(response[1], response[2])
            client.send(str('2\r\n').encode())
        elif response[0] == "load_model_with_memory":
            manager.add_model(
                model_id=response[1],
                state_size=int(response[2]),
                action_size=int(response[3]),
                memory_path=response[4],
                model_path=response[5])
            client.send(str('2\r\n').encode())
        elif response[0] == "save_model":
            manager.save_model(
                model_id=response[1],
                path=response[2])
            client.send(str('save\r\n').encode())
        elif response[0] == "replay_model":
            manager.replay(
                model_id=response[1],
                batch_size=int(response[2]))
            client.send(str('replay\r\n').encode())
        elif response[0] == "replay_and_predict":
            model_id = response[1]
            state = [float(i) for i in response[2].split(' ')]
            action, msg = manager.act(model_id, state)
            print("Suggested action: "+str(action)+", type: " + msg)
            manager.remember_new_line(model_id, state)
            client.send((str(model_id)+'_'+str(action)+'_'+str(msg) + str('\r\n')).encode())
        elif response[0] == "reset_rewards":
            state = [float(i) for i in response[2].split(' ')]
            manager.remember_new_line(
                model_id=response[1],
                state=state)
            client.send(str('reset_rewards\r\n').encode())
        elif response[0] == "reset_simulation":
            model_id = response[1]
            previous_action = int(response[2])
            state = [float(i) for i in response[3].split(' ')]
            reward = float(response[4])
            manager.remember(model_id, previous_action, reward, state)
            #Add a boolean to display a simulation reset
            client.send((str(model_id)+'_'+str(previous_action)+'_'+str(1) + str('\r\n')).encode())
    else:
        client.send(('error' + str('\r\n')).encode())

print("Close")
client.close()
socket.close()
