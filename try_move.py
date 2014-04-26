from subprocess import call

import collections
import os
import random


log = open("try_log", 'w')


# Used to select which test to call.
currTest = 0

# The number of tests to run.
numIters = 1000

# Deleting an edge is just the reverse off (orig then add).
# If this is true, then we are assuming that the previous level graph is the
# original graph. Else, we are assuming that the previous level graph is the
# current graph (this equates to deleting an edge).
addLast = True

# Statistics we are going to keep track of.
headStats = [[0,0,0] for i in range(6)]
tailStats = [[0,0,0] for i in range(6)]

for iteration in range(numIters):

    print "Starting iteration", iteration

    graph = "move_graph"
    newgraph = "new_move_graph"
    statefile = "tmp/__state"
    levels = 9

    # The ILP solution file.
    sol_file = "tmp/out.sol"

    # Generate a random graph
    call([os.curdir + '/new_graph_move', graph])

    # The number of nodes in the graph. Also, count the number of edges
    # If using a custom graph format, update this!
    nodes = set()
    edges = set()
    total_edges = 0
    for line in open(graph):
        total_edges += 1
        edge_nodes = line.split()
        for node in edge_nodes:
            nodes.add(node)
        edges.add( (edge_nodes[0], edge_nodes[1]) )
    total_nodes = len(nodes)

    # We wil convert the set of nodes to a list so we can index it when
    # selecting a random edge.
    nodeList = [node for node in nodes]



    # Find HIDEN rankings
    print "Calculating HIDEN Hierarchy for the original graph."
    call(['java', '-jar', 'DHD.jar', '-i', graph, '-l', str(levels), '-n',
        str(total_nodes)])
    call([os.curdir + '/scip_script', sol_file])

    call(['java', '-jar', 'DHD.jar', '-f', sol_file])



    # Keep track of the original levels.
    origLevels = collections.defaultdict(list)
    # Map levels to nodes in that level.
    origLevelsReverse = collections.defaultdict(list)
    for line in open(statefile):
        objs = line.split()
        origLevels[objs[0]] = objs[1]
        origLevelsReverse[int(objs[1])].append(objs[0])

    # The nodes we have chosen to add an edge between.
    headNode = ''
    tailNode = ''



    # Add an edge between two nodes at the name level.
    if currTest == 0:
        levelIndex = random.randint(0, levels - 1)
        while(True):
            numNodesInLevel = len(origLevelsReverse[levelIndex])

            # There have to be at least two nodes in this level.
            if numNodesInLevel < 2:
                levelIndex = (levelIndex + 1) % levels
                continue

            # We have found a level with at least two nodes. Now we need to
            # select two nodes at random from this level.
            firstNodeIndex = random.randint(0, numNodesInLevel - 1)
            secondNodeIndex = random.randint(0, numNodesInLevel - 1)
            # We don't want to select the same node twice!
            while firstNodeIndex == secondNodeIndex:
                secondNodeIndex = random.randint(0, numNodesInLevel - 1)

            headNode = origLevelsReverse[levelIndex][firstNodeIndex]
            tailNode = origLevelsReverse[levelIndex][secondNodeIndex]
            break
    else:
        # Select two random levels.
        level1 = 0
        level2 = 0
        while (True):
            level1 = random.randint(0, levels - 1)
            level2 = random.randint(0, levels - 1)

            if level1 == level2:
                continue

            if (len(origLevelsReverse[level1]) != 0) and (len(origLevelsReverse[level2]) != 0):
                break

        # For ease, we will max level1 > level2.
        if level1 < level2:
            temp = level1
            level1 = level2
            level2 = temp

        # Add an edge from a node to a higher level to a node at a lower level.
        if currTest == 1:
            headNode = origLevelsReverse[level1][random.randint(0, len(origLevelsReverse[level1]) - 1)]
            tailNode = origLevelsReverse[level2][random.randint(0, len(origLevelsReverse[level2]) - 1)]
        # Add an edge from a node at a lower level to a node at a higher level.
        elif currTest == 2:
            headNode = origLevelsReverse[level2][random.randint(0, len(origLevelsReverse[level2]) - 1)]
            tailNode = origLevelsReverse[level1][random.randint(0, len(origLevelsReverse[level1]) - 1)]

    # Write to the log file.
    log.write("Adding edge " + headNode + " - " + tailNode + '\n')
    # Read the old graph and write the new graph. We add the new
    # edge. Also, we construct the neighbor map so we can mark the
    # movement later.
    old = open(graph)
    new = open(newgraph, "w")
    # Copy all the old edges.
    for line in old:
        new.write(line)
    # Write the new edge.
    new.write(headNode + " " + tailNode)
    # Close the files.
    new.close()
    old.close()



    # Calculate the new hierarchy. 
    print "Calculating HIDEN Hierarchy for modified graph."
    call(['java', '-jar', 'DHD.jar', '-i', newgraph, '-l', str(levels), '-n',
        str(total_nodes)])
    call([os.curdir + '/scip_script', sol_file])

    call(['java', '-jar', 'DHD.jar', '-f', sol_file])


    # Keep track of the new levels.
    newLevels = collections.defaultdict(list)
    for line in open(statefile):
        objs = line.split()
        newLevels[objs[0]] = objs[1]

    # Write the unmodified graph.
    for line in open(graph):
        log.write(line)
    log.write('----\n')

    for node in nodes:
        if addLast:
            log.write(node + ' ' + str(origLevels[node]) + ' ' + str(newLevels[node]) + '\n')
        else:
            log.write(node + ' ' + str(newLevels[node]) + ' ' + str(origLevels[node]) + '\n')


    UP = 0
    DOWN = 1
    SAME = 2

    # TODO Keep track of the statistics.
    if addLast:
        headOld = origLevels[headNode]
        headNew = newLevels[headNode]
        tailOld = origLevels[tailNode]
        tailNew = newLevels[tailNode]

        if headOld == headNew:
            headStats[currTest][SAME] += 1
        elif headOld > headNew:
            headStats[currTest][DOWN] += 1
        else:
            headStats[currTest][UP] += 1


        if tailOld == tailNew:
            tailStats[currTest][SAME] += 1
        elif tailOld > tailNew:
            tailStats[currTest][DOWN] += 1
        else:
            tailStats[currTest][UP] += 1

    else:
        headOld = newLevels[headNode]
        headNew = origLevels[headNode]
        tailOld = newLevels[tailNode]
        tailNew = origLevels[tailNode]

        if headOld == headNew:
            headStats[currTest + 3][SAME] += 1
        elif headOld > headNew:
            headStats[currTest + 3][DOWN] += 1
        else:
            headStats[currTest + 3][UP] += 1


        if tailOld == tailNew:
            tailStats[currTest + 3][SAME] += 1
        elif tailOld > tailNew:
            tailStats[currTest + 3][DOWN] += 1
        else:
            tailStats[currTest + 3][UP] += 1


    # Write a separator in the log file.
    log.write("===\n")

    # Increment the test instance.
    currTest = (currTest + 1) % 3

    # Switch to the deletion case if necessary.
    if currTest == 0:
        addLast = not addLast


# TODO Print the stats.
print '0: Add =, 1: Add >, 2: Add <, 3: Del =, 4: Del >, 5: Del <'

print "Head stats: UP, DOWN, SAME"
for i in range(6):
    print i, ':', headStats[i]

print "Tail stats"
for i in range(6):
    print i, ':', tailStats[i]
