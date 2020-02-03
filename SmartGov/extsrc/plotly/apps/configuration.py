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

layout = [
    html.Div(
        children=html.Div([
            html.H5('Configuration : ')
        ])
    )
]