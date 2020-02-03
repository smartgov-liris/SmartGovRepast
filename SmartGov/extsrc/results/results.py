import glob, os, sys
import graphfunctions as gf
path = '../../input/localLearner/'
#value = '161118_8_ScenarioIQN_6'

def read_subfolders():
    values = []
    for folder in glob.glob(path+'*'):
        cut = os.path.split(path)[0] + '\\'
        values.append(folder.split(cut)[1])
    return values

def cumulative_graph(value):
    #gain_per_agent = gf.gain_per_agent_per_episode(value, path)
    gain_per_agent = gf.gain_per_agent_per_episode_2(value, path)
    return gf.cumulative_gain_per_iteration(gain_per_agent)
    #print([key for key,value in total_gain.items()])


def create_dat_file(name, extension, total_gain):
    file = name + '.' + extension
    dat_file = open(name + '.' + extension, 'w+')
    dat_file.write('E Cumul\n')
    if len(total_gain.keys()) > 1000 :
        modulo = 0
        for element in total_gain.items():
            if modulo == 3:
                dat_file.write(str(element[0]) + ' ' + str(total_gain[element[0]]) + '\n')
                modulo = 0
            else:
                modulo += 1
    else :
        for element in total_gain.items():
            #print(element[0])
            #print(total_gain[element[0]])# + ' ' + str(total_gain[element][1]))
            dat_file.write(str(element[0]) + ' ' + str(total_gain[element[0]]) + '\n')

#Display global cumulated payoff
#Need to have the average trust score per simulation
'''
Ici, on pourra donc comparer la différence entre le score de confiance moyen
dans le cadre de l'IQL par épisode et le score de confiance moyen dans le cadre du
CDQN, l'idée est de montrer que bien que l'agent maximise son immediate payoff, coordination
between agnostic agents allow for a better trust score
'''

def manager(begin, end, date, scenario, number_of_agents):
    for i in range(begin,end+1):
        value = (date + '_{}_' + scenario).format(i)
        create_dat_file(value + '_cumul','dat', cumulative_graph(value))
        #trust_per_agent = gf.trust_score_per_iteration(value, path)
        #average_score = gf.average_score_per_iteration(trust_per_agent, number_of_agents)
        #create_dat_file(value + '_score_per_iteration','dat', average_score)

def main():
    values = []
    #manager(12,14,'161118','ScenarioSplitMerge_4', 32)
    manager(11,17,'270619','ScenarioSplitMerge_1',32)
    manager(1,8,'280619','ScenarioSplitMerge_1',32)
    #print("BasicMerge")
    #manager(26,27,'161118','ScenarioBasicMerge_1', 17)
    #print("IQL")
    #manager(18,20,'161118','ScenarioIQN_1', 17)
    #value = '161118_12_ScenarioSplitMerge_4'
    #gf.track_episode_bis(value, path)
    #print('SplitMerge Regular')
    #manager(12,14,'161118','ScenarioSplitMerge_4', 32)
    #print('IQL Regular')
    #manager(3,6,'151118', 'ScenarioIQN_5',32)
    #for i in range(7, 12):
    #    value = '161118_{}_ScenarioIQN_6'.format(i)
    #    create_dat_file(value + '_cumul','dat', cumulative_graph(value))
    #    trust_per_agent = gf.trust_score_per_iteration(value, path)
    #    average_score = gf.average_score_per_iteration(trust_per_agent)
    #    create_dat_file(value + '_score_per_iteration','dat', average_score)

    #for i in range(2,10):
    #    value = '141118_{}_ScenarioSplitMerge_3'.format(i)
    #    create_dat_file(value + '_cumul','dat', cumulative_graph(value))
    #    trust_per_agent = gf.trust_score_per_iteration(value, path)
    #    average_score = gf.average_score_per_iteration(trust_per_agent)
    #    create_dat_file(value + '_score_per_iteration','dat', average_score)

main()