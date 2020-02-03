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
            html.Div(
                [
                    html.H5('Choix de la simulation : '),
                    dcc.Dropdown(
                        id='subfolder',
                        options=[
                            {'label': i, 'value': i} for i in values
                        ],
                        value='2606_10_ScenarioBasicSplit_4'
                    ),
                    html.Hr(),
                ],
                className="tab_header",
            ),
            
            html.Div(
                [
                    html.Label('Gain par agent politique'),
                    dcc.Graph(id='graph-gain'),
                ],                
                className="tab_body",
            )
        ]
    )
]

def get_policyagents(value):
    subfolder = value + '/'
    agents = []
    for full_filename in glob.glob(os.path.join(path + subfolder, '*.txt')):
        filename = os.path.basename(full_filename)
        if '_global.txt' in filename:
            agents.append(filename.split('_')[1])
    return agents

@app.callback(
    Output('graph-gain', 'figure'),
    [Input('subfolder', 'value')]
)
def update_figure(value):
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