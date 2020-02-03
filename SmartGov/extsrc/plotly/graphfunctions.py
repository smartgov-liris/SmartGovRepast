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

def open_file_for_specific(value, agent, extension, path):
    subfolder = value + '/'
    action_file = 'policyagent_'+str(agent)+'_'+ extension + '.txt'
    return open(path + subfolder + action_file, 'r')

def cumulative_gain_per_iteration(gain_per_agent):
    total_gain = {}
    #for agent in gain_per_agent:
    for agent in range(0, len(gain_per_agent)):
        for key, value in gain_per_agent[agent].items():
            if key in total_gain:
                total_gain[key] += value
            else:
                total_gain[key] = value
    return total_gain

def gain_per_agent_per_episode(value, path):
    counter = 1
    cumulative_gain = 0
    gain_per_agent_per_episode = {}
    agent_has_cumulative_gain = 0
    iterations = track_episode_3(value, path)
    gain_per_agent_per_iterations = gain_per_agent_per_iteration(value, path)
    for agent in gain_per_agent_per_iterations.keys():
        gain_per_agent_per_episode[agent] = {}
        for index in iterations.keys():
            for iteration in range(counter, iterations[index]):
                if iteration in gain_per_agent_per_iterations[agent]:
                    agent_has_cumulative_gain = 1
                    cumulative_gain += gain_per_agent_per_iterations[agent][iteration]
            if agent_has_cumulative_gain :
                gain_per_agent_per_episode[agent][index] = cumulative_gain
            #gain_per_agent_per_episode[agent][index] = cumulative_gain
            cumulative_gain = 0
            agent_has_cumulative_gain = 0
            counter = iterations[index]
        counter = 1
    return gain_per_agent_per_episode

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

def track_episode(value, path):
    iterations = {}
    iteration_counter = 0
    agents = get_policyagents(value, path)
    for agent in agents:
    #for agent in range(0, len(agents)):
        file = open_file_for_specific(value, agent, 'actions', path)
        for line in file:
            iteration = int(line.split(')')[0])
            if iteration != iteration_counter + 1:
                iterations[iteration_counter] = 'ok'
            iteration_counter = iteration
    counter = 0
    keys = []
    for key,value in iterations.items():
        keys.append(key)
    keys = sorted(keys)
    for index in range(0, len(keys)):
        iterations[keys[index]] = index
    return iterations

def trust_score_per_iteration(value, path):
    score_per_agent = {}
    agents = get_policyagents(value, path)
    for agent in agents:
    #for agent in range(0, len(agents)):
        file = open_file_for_specific(value, agent, 'actions', path)
        score_per_agent[agent] = {}
        for line in file:
            local_agents = line.split(')')[1].split(';')
            cumulative_score = 0
            for local_agent in local_agents:
                if local_agent != '':
                    id_agent = local_agent.split(':')[0]
                    action = int(local_agent.split(':')[1].split('_')[0])
                    score = float(local_agent.split(':')[1].split('_')[2])
                    cumulative_score += score
            iteration = int(line.split(')')[0])
            score_per_agent[agent][iteration] = cumulative_score
    return score_per_agent
