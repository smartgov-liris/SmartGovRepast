def update_figure(n, value):
    subfolder = value + '/'
    agents = get_policyagents(value)
    traces = []
    total_gain = {}
    iteration_counter = 0
    for agent in range(0, len(agents)):
        action_file = 'policyagent_'+str(agent)+'_global.txt'
        file = open(path + subfolder + action_file, 'r')
        iteration_track = []
        reward_track = []
        counter = 0
        for line in file:
            if(line == line.split(')')[0]):
                #Old version of global file
                gain = float(line.split(',')[0])
                counter += 1
                reward_track.append(gain)
                iteration_track.append(counter)
                if counter in total_gain:
                    total_gain[counter] += gain
                else:
                    total_gain[counter] = gain
            else:
            #for line in file:
                gain = float(line.split(')')[1].split(',')[0])
                iteration = int(line.split(')')[0])
                #Remove lines when interruption in lines
                if iteration != iteration_counter + 1:
                    reward_track.append("")
                    iteration_track.append("")
                else:
                    reward_track.append(gain)
                    iteration_track.append(iteration)
                if iteration in total_gain:
                    total_gain[iteration] += gain
                else:
                    total_gain[iteration] = gain
                iteration_counter = iteration
        traces.append(go.Scatter(
            x = iteration_track,
            y = reward_track,
            name = agent,
            mode = 'lines'
        ))
    traces.append(go.Scatter(
        x = [key for key,value in total_gain.items()],
        y = [total_gain[key] for key,value in total_gain.items()],
        name = 'Cumulative gain',
        mode = 'lines'

    ))
    return {
        'data': traces
    }

def gain_per_agent_per_iteration(value):
    gain_per_agent = {}
    agents = get_policyagents(value)
    for agent in range(0, len(agents)):
        file = open_file_for_specific(value, agent, 'global')
        gain_per_agent[agent] = {}
        for line in file:
            gain = float(line.split(')')[1].split(',')[0])
            iteration = int(line.split(')')[0])
            gain_per_agent[agent][iteration] = gain
    return gain_per_agent

def open_file_for_specific(value, agent, extension):
    subfolder = value + '/'
    action_file = 'policyagent_'+str(agent)+'_'+ extension + '.txt'
    return open(path + subfolder + action_file, 'r')

def update_figure(n, value):
    gain_per_agent = gain_per_agent_per_iteration(value)
    traces = []
    for agent in range(0,len(gain_per_agent)):
        traces.append(go.Scatter(
            x = [key for key,value in gain_per_agent[agent].items()],
            y = [gain_per_agent[agent][key] for key,value in gain_per_agent[agent].items()],
            name = agent,
            mode = 'lines'
        ))
    total_gain = cumulative_gain_per_iteration(gain_per_agent)
    traces.append(go.Scatter(
        x = [key for key,value in total_gain.items()],
        y = [total_gain[key] for key,value in total_gain.items()],
        name = 'Cumulative gain',
        mode = 'lines'

    ))
    return {
        'data': traces
    }

def cumulative_gain_per_iteration(gain_per_agent):
    total_gain = {}
    for agent in range(0, len(gain_per_agent)):
        for key, value in gain_per_agent[agent].items():
            if key in total_gain:
                total_gain[key] += value
            else:
                total_gain[key] = value
    return total_gain

def gain_per_agent_per_episode(value):
    gain_per_agent = {}
    agents = get_policyagents(value)
    for agent in range(0, len(agents)):
        file = open_file_for_specific(value, agent, 'global')
        iterations = track_episode()
        gain_per_agent[agent] = {}
        cumulative_gain = 0
        for line in file:
            gain = float(line.split(')')[1].split(',')[0])
            iteration = int(line.split(')')[0])
            if iteration in iterations:
                gain_per_agent[agent][iteration] = cumulative_gain
                cumulative_gain = 0
            else:
                cumulative_gain += gain
    return gain_per_agent

def track_episode():
    iterations = {}
    agents = get_policyagents(value)
    for agent in range(0, len(agents)):
        file = open_file_for_specific(value, agent, 'actions')
        for line in file:
            iteration = int(line.split(')')[0])
            iterations[iteration] = 'ok'
    return iterations