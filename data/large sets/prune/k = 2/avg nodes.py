"""
    Creates graphs that show the avg nodes used by the pruning method.
"""

import matplotlib.pyplot as plt
import os


# The current directory we are in.
currDir = os.curdir

for directory in os.listdir(currDir):
    if os.path.isdir(directory):
        values = [0.0 for i in range(50)]

        # Get the tmp directory.
        logPath = os.path.join(currDir, directory, 'tmp', 'smartLog')

        # Our iteration starts at 0 because that is the array starting index.
        iteration = 0
        maxIter = 50

        # Each Iteration is separated by a line containing '====='
        skip = True
        for line in open(logPath):
            skip = not skip
            if skip:
                continue
            numNodes = line.split()[3]

            iteration = (iteration + 1) % maxIter

            values[iteration] += float(numNodes)


        # Calculate the average.
        values = [val/10 for val in values]


        xvals = [i+1 for i in range(50)]

        outputgraph = os.path.join(directory, 'nodes_inc')

        # Generate the graphs.
        # Penalty data
        plt.figure()
        plt.plot( xvals, values, 'ro')
        plt.xlabel('Iteration #')
        plt.ylabel('avg nodes included')
        plt.savefig(outputgraph)
