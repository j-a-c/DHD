"""
Given the graph file, compares the HIDEN method and the dynamic method.
Outputs a graph comparing the penalties and the cumulative times.
Runs for each test TRIALS number of times, for itersPerTrial iterations.
Joshua A. Campbell
"""

from math import ceil
from subprocess import call

import os
import matplotlib.pyplot as plt
import time

"""
 Configurable Parameters
"""

# Name of the graph file
graph ="sf_75_2_1"
#graph="sf_50_2_1"
orig_graph = graph

# The number of levels in the hierarchy.
levels = 17

# The ILP solution file root name.
# This will be appended with the counter.
sol_root ="tmp/out.sol"

# The ILP solution file.
sol_file = "tmp/out.sol"

# Output for the solution to the mutated graph.
mut_sol_file ="tmp/mut_sol.sol"

# The number of edges to mutate per iteration.
mut_per = 0.025

# Then maximum amount of levels a node can change when being re-ranked.
#leveld = 3
leveld = levels

# The neighborhood size to choose from when selecting nodes to dynamically re-rank.
hood_size = 1

# The number of trials. (How many times should we run the perturbation program).
trials = 10

# The number of iterations to run the pertubation for.
# This does not include the 0th iteration, where the dynamic solver
# uses HIDEN's output as its start state.
itersPerTrial = 50

# Output files for the timining values.
dtime = "dhd_time.txt"
htime = "hid_time.txt"

# Output files for the penalty values.
dpenalty = "dhd_pen.txt"
hpenalty = "hid_pen.txt"

# Output files for the graphs.
penaltyOutputGraph = orig_graph + "_trials_" + str(trials) + "_mut_" + str(mut_per) + "_ld_" + str(leveld) + "_penalty.png"
timeOutputGraph = orig_graph + "_trials_" + str(trials) + "_mut_" + str(mut_per) + "_ld_" + str(leveld) + "_time.png"


#-----------------
# Non-Configurable Parameters
#-----------------

# Trial counter.
trialCounter = 1

# The counter per trial.
counter = 1

# The name of the file the mutated graph to store the mutated graph.
mutatedGraph = graph + ".mutate." + str(counter)


#-----------------
# Job starts here!
#-----------------

# Delete previous run's ILP files.
print "Setting up temporary workspace."
call(['rm', '-rf', 'tmp'])
call(['mkdir', '-p', 'tmp'])

# Delete previous data.
call(['rm', hpenalty])
call(['rm', dpenalty])
call(['rm', dtime])
call(['rm', htime])

#-----
# Trial iterations.
#-----

penaltyAvg = []
timeAvg = []

# Set up empty arrays.
for i in range(itersPerTrial+1):
    penaltyAvg.append(0)
    timeAvg.append(0)

while trialCounter <= trials:

    # Inner loop counter.
    counter = 1

    # Generate a new graph each trial.
    call([os.curdir + '/new_graph', graph])
    # SAve a copy of this graph for later.
    call(['mkdir', '-p', 'tmp/trial'+str(trialCounter)+'/'])
    call(['cp', graph, 'tmp/trial'+str(trialCounter)+'/'+graph])

    # The number of nodes in the graph. Also, count the number of edges
    # If using a custom graph format, update this!
    nodes = set()
    total_edges = 0
    for line in open(graph):
        total_edges += 1
        for node in line.split():
            nodes.add(node)
    total_nodes = len(nodes)

    # The number of perturbations each iteration.
    numPerturb = int(ceil(total_edges * mut_per / 2))

    # Run HIDEN to calculate the inital optimial hierarchy.
    print "Calculating Initial Hierarchy."
    call(['java', '-jar', 'DHD.jar', '-i', graph, '-l', str(levels), '-n',
        str(total_nodes)])
    call([os.curdir + '/scip_script', sol_file])

    call(['java', '-jar', 'DHD.jar', '-f', sol_file])

    # Save HIDEN's state for DHD to use (only for the first round).
    call(['cp', 'tmp/__state', 'tmp/__mstate'])
    # Save the original state since we will be running the program multiple time.
    call(['cp', 'tmp/__state', 'tmp/__ostate'])

    print "Mutating original graph."
    # Mutate original graph.
    call(['java', '-cp', 'DHD.jar', 'DHD.Mutator', '-i', orig_graph, '-o',
        mutatedGraph, '-n', str(numPerturb)])

    # Set the original graph state.
    call(['cp', 'tmp/__ostate', 'tmp/__mstate'])
    call(['cp', 'tmp/__ostate', 'tmp/__state'])

    prev_graph = orig_graph

    # Used to keep track of the cumulative times.
    dtimeTemp = 0.0
    htimeTemp = 0.0

    # Perturbation iterations.
    while counter <= itersPerTrial:

        # Initialize array if we are on the first trial.
        if trialCounter == 1:
            penaltyAvg[counter] = 0
            timeAvg[counter] = 0

        print "Starting: Iteration:", counter, '/', itersPerTrial, 'Trial:', trialCounter, '/', trials
        # Make the new directories.
        call(['mkdir', '-p',
            'tmp/trial'+str(trialCounter)+'/iter'+str(counter)+'/'])

        # Dynamically solve the mutated graph.
        # We use the state file from HIDEN's output.
        print "Calculating DHD."
        call(['java', '-cp', 'DHD.jar', 'DHD.PartialSolver', '-i',
            mutatedGraph, '-p', 'tmp/__mstate', '-d', prev_graph, '-k',
            str(hood_size), '-c', str(leveld), '-l', str(levels)])
        # Saving timing values for command. These values are decimal, so we pipe through bc.
        call([os.curdir + '/scip_script', mut_sol_file])
        dtimeTemp += float(open('tmp/time').readline())
        call(['rm', 'tmp/time'])
        call(['java', '-jar', 'DHD.jar', '-f', mut_sol_file])
        # Save state for next iteration.
        call(['cp', 'tmp/__state', 'tmp/__mstate'])
        call(['cp', mut_sol_file,
            'tmp/trial'+str(trialCounter)+'/iter'+str(counter)+'/dhd_state'])
        # Save penalty. These values will be integer, but we will pipe through bc in case of rounding errors (like 6.0000001)
        f = open(mut_sol_file)
        f.readline(); # Skip the first line.
        dpenaltyTemp = int(round(float(f.readline().split()[2])))


        # Run HIDEN on the mutated graph.
        print "Calculating HIDEN."
        call(['java', '-jar', 'DHD.jar', '-i', mutatedGraph, '-l', str(levels),
            '-n', str(total_nodes)])
        # Save timing values for command. These values are decimal, so we pipe through bc.
        call([os.curdir + '/scip_script', sol_file])
        htimeTemp += float(open('tmp/time').readline())
        call(['rm', 'tmp/time'])
        call(['cp', sol_file,
            'tmp/trial'+str(trialCounter)+'/iter'+str(counter)+'/hid_state'])
        call(['java', '-jar', 'DHD.jar', '-f', sol_file])
        # Save penalty. These values will be integer, but we will pipe through bc in case of rounding errors (like 6.0000001)
        f = open(sol_file)
        f.readline() # Skip the first line
        hpenaltyTemp = int(round(float(f.readline().split()[2])))

        # Set up variables for next round.
        prev_graph = mutatedGraph
        mutatedGraph = graph + ".mutate." + str(counter)

        # Mutate the graph.
        print "Mutating graph."
        call(['java', '-cp', 'DHD.jar', 'DHD.Mutator', '-i', prev_graph, '-o',
            mutatedGraph, '-n', str(numPerturb)])

        # Update values
        if hpenaltyTemp != 0:
            penaltyAvg[counter] += (1.0*dpenaltyTemp/hpenaltyTemp)
        else:
            penaltyAvg[counter] += dpenaltyTemp
        timeAvg[counter] += (1.0*dtimeTemp/htimeTemp)

        counter += 1


    # Increment trial counter.
    trialCounter += 1

#-----
# Plot the data.
#-----

# Update the averages.
for i in range(len(penaltyAvg)):
    penaltyAvg[i] = 1.0 * penaltyAvg[i] / trials
    timeAvg[i] = 1.0 * timeAvg[i] / trials

print "Creating final graphs."

# Penalty data
plt.figure()
plt.plot(range(len(penaltyAvg)), penaltyAvg, 'ro')
plt.xlabel('Iteration #')
plt.ylabel('sum(DHD)/sum(Hiden) penalties')
plt.savefig(penaltyOutputGraph)

# Time data
plt.figure()
plt.plot(range(len(penaltyAvg)), timeAvg, 'ro')
plt.xlabel('Iteration #')
plt.ylabel('sum(DHD)/sum(Hiden) cumulative run time per iteration')
plt.savefig(timeOutputGraph)


#-----
# Cleanup
#-----

# Delete the mutated graphs.
#call(os.curdir + '/clean_up')
