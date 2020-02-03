import glob, os

def get_policyagents(value, path):
    subfolder = value + '/'
    agents = []
    for full_filename in glob.glob(os.path.join(path + subfolder, '*.txt')):
        filename = os.path.basename(full_filename)
        if '_global.txt' in filename:
            agents.append(filename.split('_')[1])
    return agents

def gain_per_agent_per_iteration(value, path):
    gain_per_agent = {}
    agents = get_policyagents(value, path)
    for agent in range(0, len(agents)):
        file = open_file_for_specific(value, agent, 'global', path)
        gain_per_agent[agent] = {}
        for line in file:
            gain = float(line.split(')')[1].split(',')[0])
            iteration = int(line.split(')')[0])
            gain_per_agent[agent][iteration] = gain
    return gain_per_agent

def open_file_for_specific(value, agent, extension, path):
    subfolder = value + '/'
    action_file = 'policyagent_'+str(agent)+'_'+ extension + '.txt'
    return open(path + subfolder + action_file, 'r')

def cumulative_gain_per_iteration(gain_per_agent):
    total_gain = {}
    for agent in range(0, len(gain_per_agent)):
        for key, value in gain_per_agent[agent].items():
            if key in total_gain:
                total_gain[key] += value
            else:
                total_gain[key] = value
    return total_gain

def gain_per_agent_per_episode(value, path):
    gain_per_agent = {}
    agents = get_policyagents(value, path)
    iterations = track_episode_bis(value, path)
    for agent in range(0, len(agents)):
        file = open_file_for_specific(value, agent, 'global', path)
        #iterations = track_episode(value, path)
        gain_per_agent[agent] = {}
        cumulative_gain = 0
        for line in file:
            gain = float(line.split(')')[1].split(',')[0])
            iteration = int(line.split(')')[0])
            if iteration in iterations:
                gain_per_agent[agent][iterations[iteration]] = cumulative_gain + gain
                cumulative_gain = 0
            else:
                cumulative_gain += gain
    return gain_per_agent

def gain_per_agent_per_episode_2(value, path):
    counter = 1
    cumulative_gain = 0
    gain_per_agent_per_episode = {}
    agent_has_cumulative_gain = 0
    iterations = track_episode_3(value, path)
    gain_per_agent_per_iteration = gain_per_agent_per_iteration_2(value, path)
    for agent in gain_per_agent_per_iteration.keys():
        gain_per_agent_per_episode[agent] = {}
        for index in iterations.keys():
            for iteration in range(counter, iterations[index]):
                if iteration in gain_per_agent_per_iteration[agent]:
                    agent_has_cumulative_gain = 1
                    cumulative_gain += gain_per_agent_per_iteration[agent][iteration]
            if agent_has_cumulative_gain :
                gain_per_agent_per_episode[agent][index] = cumulative_gain
            #gain_per_agent_per_episode[agent][index] = cumulative_gain
            cumulative_gain = 0
            agent_has_cumulative_gain = 0
            counter = iterations[index]
        counter = 1
    print(gain_per_agent_per_episode)
    return gain_per_agent_per_episode

def gain_per_agent_per_iteration_2(value, path):
    gain_per_agent_per_iteration = {}
    agents = get_policyagents(value, path)
    for agent in range(0, len(agents)):
        gain_per_agent_per_iteration[agent] = {}
        file = open_file_for_specific(value, agent, 'global', path)
        for line in file:
            gain = float(line.split(')')[1].split(',')[0])
            iteration = int(line.split(')')[0])
            gain_per_agent_per_iteration[agent][iteration] = gain
    return gain_per_agent_per_iteration

def track_episode(value, path):
    iterations = {}
    iteration_counter = 0
    agents = get_policyagents(value, path)
    for agent in range(0, len(agents)):
        file = open_file_for_specific(value, agent, 'actions', path)
        for line in file:
            iteration = int(line.split(')')[0])
            if iteration != iteration_counter + 1:
                iterations[iteration_counter] = 'ok'
            elif iteration in iterations:
                iterations.pop(iteration, None)
            iteration_counter = iteration
    counter = 0
    keys = []
    #print(iterations.keys())
    for key,value in iterations.items():
        keys.append(key)
    keys = sorted(keys)
    for index in range(0, len(keys)):
        iterations[keys[index]] = index
    return iterations

def track_episode_3(value, path):
    iterations = {}
    agents = get_policyagents(value, path)
    for agent in range(0, len(agents)):
        file = open_file_for_specific(value, agent, 'actions', path)
        for line in file:
            iteration = int(line.split(')')[0])
            iterations[iteration] = iteration
    previous_key = 0
    episode_indexes = {}
    index = 0
    for key in iterations.keys():
        if(key != previous_key + 1):
            episode_indexes[index] = previous_key
            index += 1
        previous_key = key
    return episode_indexes

def track_episode_bis(value, path):
    iterations = {}
    agents = get_policyagents(value, path)
    for agent in range(0, len(agents)):
        file = open_file_for_specific(value, agent, 'actions', path)
        for line in file:
            iteration = int(line.split(')')[0])
            iterations[iteration] = iteration
    previous_key = 0
    episode_iteration = []
    episode_dict = {}
    episode_indexes = {}
    test = []
    test2 = []
    index = 0
    for key in iterations.keys():
        if(key != previous_key + 1):
            episode_iteration.append(previous_key + 1)
            episode_dict[previous_key] = index
            test.append(previous_key)
            episode_indexes[index] = previous_key
            index += 1
        previous_key = key
    #print(episode_iteration)
    #print(episode_dict)
    for element in range(1, len(test)):
        test2.append(test[element] - test[element -1])
    print(test)
    print(test2)
    print(episode_indexes)
    return episode_dict

def trust_score_per_iteration(value, path):
    score_per_agent = {}
    agents = get_policyagents(value, path)
    for agent in range(0, len(agents)):
        file = open_file_for_specific(value, agent, 'actions', path)
        score_per_agent[agent] = {}
        for line in file:
            local_agents = line.split(')')[1].split(';')
            cumulative_score = 0
            agent_counter = 0
            for local_agent in local_agents:
                if len(local_agent) > 3:
                    id_agent = local_agent.split(':')[0]
                    action = int(local_agent.split(':')[1].split('_')[0])
                    score = int(local_agent.split(':')[1].split('_')[2])
                    cumulative_score += score
                    agent_counter += 1
            iteration = int(line.split(')')[0])
            #print('Agent{}){}:{}'.format(agent, iteration, cumulative_score))
            score_per_agent[agent][iteration] = cumulative_score#/agent_counter
    return score_per_agent

def average_score_per_iteration(score_per_agent, number_of_agents):
    average_score_per_iteration = {}
    #number_of_agents = len(score_per_agent.keys())
    for agent in score_per_agent.items():
        #print(agent[0])
        #print(score_per_agent[agent[0]].keys())
        for iteration in score_per_agent[agent[0]].keys():
            if iteration in average_score_per_iteration:
                average_score_per_iteration[iteration] += (score_per_agent[agent[0]][iteration]/number_of_agents)
            else:
                average_score_per_iteration[iteration] = (score_per_agent[agent[0]][iteration]/number_of_agents)
    return average_score_per_iteration