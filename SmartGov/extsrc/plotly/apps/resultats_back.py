import dash
import dash_core_components as dcc
import dash_html_components as html
from dash.dependencies import Input, Output
from dash_table_experiments import DataTable
import pandas as pd
import plotly.graph_objs as go
import numpy as np
import glob
import os

from app import app

path = '../../input/localLearner/'
def read_subfolders():
    values = []
    for folder in glob.glob(path+'*'):
        cut = os.path.split(path)[0] + '\\'
        values.append(folder.split(cut)[1])
    return values
values = read_subfolders()

layout = [
    html.Div(
        [
            html.H5('Choix de la simulation : '),
            dcc.Dropdown(
                id='subfolder',
                options=[
                    {'label': i, 'value': i} for i in values
                ],
                value='2606_10_ScenarioBasicSplit_4'),
            html.Button('Refresh choix simulations', id='refresh-dropdown'),
            dcc.Interval(id='interval_component'),
            dcc.RadioItems(id='set-time',
                value=10*1000,
                options=[
                    {'label': 'Every 10 second', 'value': 10*1000},
                    {'label': 'Every minute', 'value': 60*1000},
                    {'label': 'Off', 'value': 60*60*1000} # or just every hour
                ]),
            html.Hr(),
            html.Label('Gain par agent politique'),
            dcc.Graph(id='graph-gain'),
            html.Hr(),
            html.Label('Score de confiance par agent local'),
            dcc.Graph(id='graph-trust'),
            html.Hr(),
            html.Label('Histogramme cumulé des actions effectuées par les agents locaux'),
            dcc.Graph(id='graph-actions'),
            html.Hr()
        ]
    )
]

@app.callback(
    dash.dependencies.Output('interval_component', 'interval'),
    [dash.dependencies.Input('set-time', 'value')])
def update_interval(value):
    #https://github.com/plotly/dash-recipes/blob/master/toggle-interval.py
    return value

def get_policyagents(value):
    subfolder = value + '/'
    agents = []
    for full_filename in glob.glob(os.path.join(path + subfolder, '*.txt')):
        filename = os.path.basename(full_filename)
        if '_global.txt' in filename:
            agents.append(filename.split('_')[1])
    return agents

@app.callback(
    Output('subfolder', 'options'),
    [Input('refresh-dropdown', 'n_clicks')]
)
def refresh_dropdown_menu(value):
    values = read_subfolders()
    return [{'label': i, 'value': i} for i in values]

@app.callback(
    Output('graph-gain', 'figure'),
    [Input('interval_component', 'n_intervals'),
    Input('subfolder', 'value')]
)
def update_figure(n, value):
    print("coucou")
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
            mode = 'lines'#,
            #mode = 'lines',
            #line = dict(
            #    color = [agent],
            #    colorscale='Viridis'
            #    )
        ))
    traces.append(go.Scatter(
        x = [key for key,value in total_gain.items()],
        y = [total_gain[key] for key,value in total_gain.items()],
        name = 'Cumulative gain',
        mode = 'lines'#,
        #mode = 'lines',
        #line = dict(
        #    color = len(agents),
        #    colorscale='Viridis'
        #    )

    ))
    return {
        'data': traces
    }

def load_file(path):
    lines = []
    file = open(path, 'r')
    for line in file:
        line = line.rstrip('\n')
        lines.append(line)
    return lines

def find_all_agents(lines):
    #Need to read every lines since the number of agents might change several times during execution
    agents = []
    for line in lines:
        line_without_number = line.split(')')
        agents_answers = line_without_number[1].split(';')
        for agent in agents_answers:
            agent_str= agent.split(':')[0]
            if(agent_str != ''):
                if agent_str not in agents:
                    agents.append(agent_str)

    #line = lines[0]
    #agents = []
    #line_without_number = line.split(')')
    #agents_answers = line_without_number[1].split(';')
    #for agent in agents_answers:
    #    agent_str= agent.split(':')[0]
    #    if(agent_str != ''):
    #        agents.append(agent_str)
    return agents

def parse_trust_for_agent(agent_id, lines):
    agent_trust = []
    iteration_track = []
    agent_trust = []
    for line in lines:
        line_without_number = line.split(')')
        agents_answers = line_without_number[1].split(';')
        for agent in agents_answers:
            agent_str= agent.split(':')[0]
            if(agent_str == agent_id):
                agent_action = agent.split(':')[1].split('_')[0]
                agent_action_type = agent.split(':')[1].split('_')[1]
                #agent_trust.append(float(agent.split(':')[1].split('_')[2]))
                agent_trust = add_to_index(int(line_without_number[0]), float(agent.split(':')[1].split('_')[2]), agent_trust)
                #iteration_track.append(line_without_number[0])
                iteration_track = add_to_index(int(line_without_number[0]), line_without_number[0], iteration_track)
    return agent_trust, iteration_track

def add_to_index(index, value, array):
	if len(array) <= index:
		for newIndex in range(len(array),index+1):
			array.append("")
	array[index] = value
	return array

def merge_two_arrays(array1, array2):
    counter = 0
    if len(array1) > len(array2):
        for i in array2:
            if(i != ""):
                array1 = add_to_index(counter,i,array1)
                counter = counter + 1
        return array1
    else:
        for i in array1:
            if(i != ""):
                array2 = add_to_index(counter,i,array2)
                counter = counter + 1
        return array2

def merge_two_arrays(agent_trust1, agent_trust2, iterations1, iterations2):
    if len(agent_trust1) > len(agent_trust2):
        for i in iterations2:
            if(i != ""):
                value = int(i)
                agent_trust1 = add_to_index(value,agent_trust2[value],agent_trust1)
                iterations1  = add_to_index(value,value,iterations1)
        return agent_trust1, iterations1
    else:
        for i in iterations1:
            if(i != ""):
                value = int(i)
                agent_trust2 = add_to_index(value,agent_trust1[value],agent_trust2)
                iterations2  = add_to_index(value,value,iterations2)
        return agent_trust2, iterations2

def clean_values_in_array(array, value):
	return list(filter(lambda a: a != value, array))


@app.callback(
    Output('graph-trust', 'figure'),
    [Input('interval_component', 'n_intervals'),
    Input('subfolder', 'value')]
)
def update_figure(n, value):
    policyagents = get_policyagents(value)
    traces = []
    iteration_track = []
    dict_iterations_trust_per_agent = {} #[AgentID][][][]
    for policyagent in range(0, len(policyagents)):
        action_file = 'policyagent_'+str(policyagent)+'_actions.txt'
        filepath = path + value + '/' + action_file
        agents = find_all_agents(load_file(filepath))
        max_agents = len(agents)
        counter = 0
        for agent in agents:
            agent_trust, iteration_track = parse_trust_for_agent(agent, load_file(filepath))

            if agent in dict_iterations_trust_per_agent:
                update_trust, update_iterations = merge_two_arrays(agent_trust,
                    dict_iterations_trust_per_agent[agent][1][0],
                    iteration_track,
                    dict_iterations_trust_per_agent[agent][1][1])
                dict_iterations_trust_per_agent[agent][1][0] = update_trust
                dict_iterations_trust_per_agent[agent][1][1] = update_iterations
                #if dict_iterations_trust_per_agent[agent][1][1][0] > iteration_track[0]:
                #    iteration_temp = dict_iterations_trust_per_agent[agent][1][1]
                #    dict_iterations_trust_per_agent[agent][1][1] = iteration_track
                #    dict_iterations_trust_per_agent[agent][1][1] += iteration_temp
                #    trust_temp = dict_iterations_trust_per_agent[agent][1][0]
                #    dict_iterations_trust_per_agent[agent][1][0] = agent_trust
                #    dict_iterations_trust_per_agent[agent][1][0] += trust_temp
                #else:
                #    dict_iterations_trust_per_agent[agent][1][0] += agent_trust
                #    dict_iterations_trust_per_agent[agent][1][1] += iteration_track
            else:
                dict_iterations_trust_per_agent[agent] = agent, [agent_trust, iteration_track]

            #if agent in dict_iterations_trust_per_agent:
            #    if dict_iterations_trust_per_agent[agent][1][1][0] > iteration_track[0]:
            #        iteration_temp = dict_iterations_trust_per_agent[agent][1][1]
            #        dict_iterations_trust_per_agent[agent][1][1] = iteration_track
            #        dict_iterations_trust_per_agent[agent][1][1] += iteration_temp
            #        trust_temp = dict_iterations_trust_per_agent[agent][1][0]
            #        dict_iterations_trust_per_agent[agent][1][0] = agent_trust
            #        dict_iterations_trust_per_agent[agent][1][0] += trust_temp
            #    else:
            #        dict_iterations_trust_per_agent[agent][1][0] += agent_trust
            #        dict_iterations_trust_per_agent[agent][1][1] += iteration_track
            #else:
            #    dict_iterations_trust_per_agent[agent] = agent, [agent_trust, iteration_track]

    for element in dict_iterations_trust_per_agent.items():
        element[1][1][1] = clean_values_in_array(element[1][1][1],"")
        element[1][1][0] = clean_values_in_array(element[1][1][0],"")

    for element in dict_iterations_trust_per_agent.items():
        traces.append(go.Scatter(
            x = element[1][1][1], #Iteration
            y = element[1][1][0], #Trust score
            name = element[1][0]  #ID
        ))
    return {
        'data': traces
    }

@app.callback(
    Output('graph-actions', 'figure'),
    [Input('interval_component', 'n_intervals'),
    Input('subfolder', 'value')]
)
def update_figure(n, value):
    action_number = 5
    policyagents = get_policyagents(value)
    dict_iterations_sum_per_action = {}
    traces = []
    for policyagent in range(0, len(policyagents)):
        action_file = 'policyagent_'+str(policyagent)+'_actions.txt'
        filepath = path + value + '/' + action_file
        answers = load_file(filepath)
        for line in answers:
            iteration = line.split(')')[0]
            blockfaces_answers = line.split(')')[1].split(';')
            for blockface in blockfaces_answers:
                if blockface.strip():
                    action = int(blockface.split(':')[1].split('_')[0])
                    if iteration in dict_iterations_sum_per_action:
                        dict_iterations_sum_per_action[iteration][1][0][action] += 1
                    else:
                        dict_iterations_sum_per_action[iteration] = iteration, np.zeros((1,action_number))
                        dict_iterations_sum_per_action[iteration][1][0][action] += 1
    for i in range(0, action_number):
        traces.append(go.Bar(
            x = [key for key,value in dict_iterations_sum_per_action.items()],
            y = [dict_iterations_sum_per_action[key][1][0][i] for key,value in dict_iterations_sum_per_action.items()],
            name = i
        ))
    return {
        'data': traces,
        'layout' : go.Layout(barmode = 'stack')
    }

def count_all_local_agents(value, policyagents):
    agents = 0
    last_iteration = -1
    for policyagent in range(0, len(policyagents)):
        action_file = 'policyagent_'+str(policyagent)+'_actions.txt'
        filepath = path + value + '/' + action_file
        answers = load_file(filepath)
        line = answers[0]
        iteration = line.split(')')[0]
        if last_iteration == -1:
            last_iteration = iteration
            agents += len(line.split(')')[1].split(';'))
        else:
            if last_iteration == iteration:
                agents += len(line.split(')')[1].split(';'))        
    return agents

#@app.callback(
#    Output('graph-actions-local-agents', 'figure'),
#    [Input('interval_component', 'n_intervals'),
#    Input('subfolder', 'value')]
#)
#def update_figure(n, value):
#    policyagents = get_policyagents(value)
#    agent_number = count_all_local_agents(value, policyagents)
#    dict_iterations_action_per_agent = {}
#    traces = []
#    local_agents = []
#    for policyagent in range(0, len(policyagents)):
#        action_file = 'policyagent_'+str(policyagent)+'_actions.txt'
#        filepath = path + value + '/' + action_file
#        answers = load_file(filepath)
#        for line in answers:
#            iteration = line.split(')')[0]
#            blockfaces_answers = line.split(')')[1].split(';')
#            for blockface in blockfaces_answers:
#                if blockface.strip():
#                    agent  = blockface.split(':')[0]
#                    if agent not in local_agents:
#                        local_agents.append(agent)
#                    action = int(blockface.split(':')[1].split('_')[0])
#                    if iteration in dict_iterations_action_per_agent:
#                        dict_iterations_action_per_agent[iteration][1][0].append(agent)
#                        dict_iterations_action_per_agent[iteration][1][1].append(action)
#                    else:
#                        dict_iterations_action_per_agent[iteration] = iteration, [[agent], [action]]
#    for i in range(0, len(local_agents)):
#        traces.append(go.Bar(
#            x = [key for key,value in dict_iterations_action_per_agent.items()],
#            y = [dict_iterations_action_per_agent[key][1][1][i] for key,value in dict_iterations_action_per_agent.items()],
#            name = local_agents[i]
#        ))
#    return {
#        'data': traces,
#        'layout' : go.Layout(barmode = 'stack')
#    }

@app.callback(
    Output('content', 'children'),
    [Input('subfolder', 'value')]
)
def display_config(value):
    parameters = 'parameters.txt'
    filepath = path + value + '/' + parameters
    lines = load_file(filepath)
    return html.Div([
        html.Div(id='config_output'),
        DataTable(
            id='datatable',
            rows=[{'Key' : line} for line in lines]
        )
    ])