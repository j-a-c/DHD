"""
    Creates graphs that show the avg increase in penalty between HIDEN and DHD
    per iteration.
"""

import matplotlib.pyplot as plt
import os


# The current directory we are in.
currDir = os.curdir

for directory in os.listdir(currDir):
    if os.path.isdir(directory):
        values = [0.0 for i in range(50)]

        # Get the tmp directory.
        tempPath = os.path.join(currDir, directory, 'tmp')

        trials = [i+1 for i in range(10)]

        for trial in trials:
            trialPath = os.path.join(tempPath, 'trial' + str(trial))

            iters = [i+1 for i in range(50)]

            for iteration in iters:
                iterPath = os.path.join(trialPath, 'iter' + str(iteration))

                hidenPath = os.path.join(iterPath, 'hid_state')
                dhdPath = os.path.join(iterPath, 'dhd_state')

                hidenFile = open(hidenPath)
                # Skip the first line.
                hidenFile.readline()
                optimal = float(hidenFile.readline().split()[2])
                hidenFile.close()

                dhdFile = open(dhdPath)
                # Skip the first line.
                dhdFile.readline()
                dhdvalue = float(dhdFile.readline().split()[2])
                dhdFile.close()

                values[iteration-1] += 1.0*(dhdvalue - optimal)

        # Calculate the average.
        values = [val/10 for val in values]


        xvals = [i+1 for i in range(50)]

        outputgraph = os.path.join(directory, 'pen_graph')

        # Generate the graphs.
        # Penalty data
        plt.figure()
        plt.plot( xvals, values, 'ro')
        plt.xlabel('Iteration #')
        plt.ylabel('avg (dhd - hiden) penalties')
        plt.savefig(outputgraph)
