package DHD;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * @author Joshua A. Campbell
 * Driver for the DHD module.
 * The driver stores some state information in tmp/__state.
 * The state file should be deleted in between runs.
 *
 *
 * The Driver uses the input arguments to formulate an ILP to find the optimal
 * hierarchical decomposition of the given graph. 
 * 
 * Below is an example use case for finding the hierarchical decomposition for
 * the graph located in file 'graph' using 4 levels.
 *
 *  java Driver -i graph -l 4 
 *  -- ILP solver solves problem formulation. --
 *
 * 
 * Since solving an ILP is time
 * consuming, the Driver can also attempt to solve the hierarchical
 * decomposition in 'chunks'. Solving the hierarchical decomposition in this
 * manner is not guaranteed to produce an optimal solution. The results from
 * previous iterations will be treated as constants in the next iteration.
 * 
 * Below is an example use case for finding the hierarchical decomposition for
 * the graph located in file 'graph' using 4 levels. In the first iteration,
 * the Driver is told to use only 25 nodes from the original graph. In the
 * second iteration, the Driver is told to use the remaining 12 nodes of the
 * graph in combination with the solution file for the previous iteration.
 *
 * java Driver -i graph -l 4 -n 25
 *  -- ILP solver solves problem formulation. --
 * java Driver -i graph -l 4 -n 12 -p out-1.solution
 * -- ILP solver solves problem formulation. --
 *
 */
class Driver
{
    private static final int DEFAULT = -1;

    // The file containing the graph to parse.
    private static File graphFile = null;
    // The ILP solution to the previous iteration.
    private static File prevFile = null;
    // The number of levels in the hierarchical decomposition.
    private static int numLevels = DEFAULT;
    // The number of nodes to include this iteration.
    private static int numNodesThisIter = DEFAULT;

    // The 
    private static final String statePath = "tmp" + File.separator + "__state";


    /**
     * Parses the arguments supplied to the driver. The function initializes
     * the following arguments (if the flags are specified):
     *      graphFile, prevFile, numLevels, numNodesThisIter
     *
     * @param args The arguments to parse.
     *
     * @return Returns true if the arguments were successfully parsed.
     */
    private static boolean parseArgs(String[] args)
    {
        // Check argument length.
        if (args.length < 4)
        {
            System.out.println("Usage: java Driver -i graphFile -l numLevels -n numNodes -p prevIter");
            System.out.println("\t-i : The input graph file. (required)");
            System.out.println("\t-l : The number of levels in the hierarchical decomposition. (required)");
            System.out.println("\t-n : The number of nodes to select from the orginal graph. (optional)");
            System.out.println("\t-p : The output from the previous iteration. (optional)");
            return false;
        }

        // Check to make sure we have a parameter for each flag.
        if (args.length % 2 != 0)
        {
            System.out.println("Each flag must have an argument.");
            return false;
        }

        // Since all of our arguments are preceeded by flags, we will switch on
        // the flag.
        for (int index = 0; index < args.length; index += 2)
        {
            // The parameter corresponding to this file.
            String param = args[index + 1];

            switch (args[index])
            {
                // Input file argument.
                case "-i" :
                    // Test if file exists.
                    graphFile = new File(param);
                    if (!graphFile.exists())
                    {
                        System.err.println("File does not exist: " + param);
                        return false;
                    }
                    break;
                // Argument for the number of levels in the hierarchy.
                case "-l" :
                    numLevels = Integer.parseInt(param);
                    break;
                // Argument for the number of nodes this iteration.
                case "-n" :
                    numNodesThisIter = Integer.parseInt(param);
                    break;
                // Previous iteration file argument.
                case "-p" :
                    // Test if file exists.
                    prevFile = new File(param);
                    if (!prevFile.exists())
                    {
                        System.err.println("File does not exist: " + param);
                        return false;
                    }
                    break;
                default :
                    System.err.println("Illegal flag: " + args[index]);
                    return false;
            }
        }

        return true;
    }


    /**
     * Saves the output to the file at the given location.
     *
     * @param output The string to output.
     * @param loc The location to save the output to.
     */
    private static void saveOutput(String output, String loc)
    {
        PrintWriter outWriter = null;
        try
        {
            outWriter = new PrintWriter(loc);
            outWriter.write(output);
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Unable to write ILP to file. Outputting to STOUT.");
            System.out.println(output);
        }
        finally
        {
            if (outWriter != null)
                outWriter.close();
        }

    }

    /**
     * Populates the data structure with data about the previous run.
     * This is necessary since some ILP solver do not output some variables if
     * their solution is 0.
     *
     * The state file contains one "edge level" per line.
     */
    public static void readStateFile(Set<Node> prevNodes, Map<String,Integer> prevNodeLevels)
    {
        BufferedReader input = null;
        String line;
        String[] lineObjs;
        try
        {

            input = new BufferedReader(new FileReader(new File(statePath)));

            // Read the whole file.
            while ((line = input.readLine()) != null)
            {
                lineObjs = line.split("\\s++"); 
                
                prevNodes.add(new Node(lineObjs[0]));
                prevNodeLevels.put(lineObjs[0], Integer.parseInt(lineObjs[1]));
            }
        }
        catch(IOException e)
        {
            System.err.println(e);
        }
        finally
        {
            try
            {
                if (input != null)
                    input.close();
            }
            catch (IOException e)
            {
                System.err.println(e);
            }
        }
    }

    /**
     * Reads the previous solution file. This updates the data structures with
     * the optimal solutions found by the ILP solver.
     */
    public static void readPrevFile(Map<String,Integer> prevNodeLevels)
    {
        ILPOutputReader reader = new ILPOutputReader(prevFile); 

        for (Map.Entry<String, Integer> entry : reader.getResults().entrySet())
        {
            prevNodeLevels.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Save the current state of the hierarchical decomposition. We need to
     * save state in case we need to perform multiple iterations to solve the
     * hierarchical decomposition.
     */
    public static void saveState(Map<String,Integer> prev, Set<Node> curr)
    {
        PrintWriter outWriter = null;
        try
        {
            outWriter = new PrintWriter(statePath);

            for (Map.Entry<String,Integer> entry : prev.entrySet())
                outWriter.println(entry.getKey() + " " + entry.getValue());

            // We write 0 because this node level has not been assigned yet. It
            // should be updated next iteration as the ILP solution file is read.
            for (Node node : curr)
                outWriter.println(node.getName() + " " + 0);

        }
        catch (FileNotFoundException e)
        {
            System.out.println("Unable to write state file!");
            System.err.println(e);
        }
        finally
        {
            if (outWriter != null)
                outWriter.close();
        }

    }

    /**
     * Driver will start execution here.
     */
    public static void main(String[] args)
    {
        // Parse command line arguments.
        if (!parseArgs(args)) return;

        if (graphFile == null || numLevels == DEFAULT)
        {
            System.out.println("-i, -l  parameters are required.");
            return;
        }

        // We will use the following two data structures to determine which
        // nodes has been used in previous iterations.
        // The nodes already used in the previous iterations.
        Set<Node> prevNodes = new HashSet<Node>();
        // The levels corresponding to the nodes used in the previous
        // iterations.
        HashMap<String, Integer> prevNodeLevels = new HashMap<String, Integer>();

        // If the previous file flag has been specified, then we need to parse
        // information for the output of the ILP solver.
        if (prevFile != null)
        {
            readStateFile(prevNodes, prevNodeLevels);
            readPrevFile(prevNodeLevels);
        }

        // Reader to read the input graph file.
        GraphReader reader = new GraphReader(graphFile);
        
        // Get the parts of the graph.
        // These two data structures now contain the complete graph.
        Set<Node> nodes = reader.getNodes();
        Set<Edge> edges = reader.getEdges();
        // Copy nodes.
        Set<Node> temp = reader.getNodes();
        temp.retainAll(prevNodes);
        prevNodes = temp;

        // Form new edge and node set if necessary.
        if (numNodesThisIter != DEFAULT)
        {
            Random random = new Random();

            // Temporary data structure to hold our current selection of nodes.
            // These two structures should only hold nodes we have not used in
            // previous iterations.
            Set<Node> selectedNodes = new HashSet<Node>();
            Set<Edge> selectedEdges = new HashSet<Edge>();

            
            // We remove all nodes that we cannot use in this iteration to give
            // us a stop condition. This now contains all the nodes that we
            // have not used in previous iterations.
            Set<Node> unusedNodes = new HashSet<Node>();
            unusedNodes.addAll(nodes);
            unusedNodes.removeAll(prevNodes);

            // A list of all the unused nodes.
            ArrayList<Node> unusedNodesList;

            // The set of neighbors that we are allowed to choose from on our
            // random walk of the graph.
            Set<Node> neighborSet = new HashSet<Node>();
            // Initialize neighborSet with the neighbors of all the
            // previously visited nodes.
            for (Node prev : prevNodes)
                neighborSet.addAll(prev.getNeighbors());
            neighborSet.removeAll(prevNodes);


            // TODO Delete debug output.
            System.out.println("Prev Nodes:");
            for (Node n : prevNodes)
                System.out.println(n);
            System.out.println();
            System.out.println("Starting neighbor set:");
            for (Node n : neighborSet)
                System.out.println(n);
            System.out.println();


            // If necessary, select an inital neighbor.
            // This will be necessary during the first iteration.
            if (neighborSet.isEmpty())
            {
                 unusedNodesList = new ArrayList<Node>(unusedNodes);

                int startIndex = random.nextInt(unusedNodesList.size());
                Node randomNode = unusedNodesList.get(startIndex);
                
                // This node is no longer unused.
                unusedNodes.remove(randomNode);

                // Add the random node to our current set.
                selectedNodes.add(randomNode);

                // Add the nodes neighbor's to the neighbor set.
                neighborSet.addAll(randomNode.getNeighbors());
                // Remove neighbors we have already visited.
                neighborSet.retainAll(unusedNodes);

                numNodesThisIter--;
            }

            unusedNodesList = new ArrayList<Node>(neighborSet);

            // TODO Smart stop if numNodesThisIter > nodes.size()
            // Random walk the graph to select new nodes for next node selection.
            while (numNodesThisIter > 0 && !unusedNodesList.isEmpty())
            {
                // Select a node from the unused nodes.
                int startIndex = random.nextInt(unusedNodesList.size());
                Node randomNode = unusedNodesList.get(startIndex);

                // This node is no longer unused.
                unusedNodes.remove(randomNode);
                unusedNodesList.remove(randomNode);

                // Add the random node to our current set.
                selectedNodes.add(randomNode);

                // Add the nodes neighbor's to the neighbor set.
                neighborSet.addAll(randomNode.getNeighbors());
                // Remove neighbors we have already visited and update our list
                // if necessary.
                neighborSet.retainAll(unusedNodes);

                unusedNodesList = new ArrayList<Node>(neighborSet);

                numNodesThisIter--;
            }

            // Add all edges connecting all the new nodes and connecting
            // all the new nodes to the old nodes to the edge set.
            for (Edge edge : edges)
            {
                if (selectedNodes.contains(edge.getFrom()) && selectedNodes.contains(edge.getTo()))
                    selectedEdges.add(edge);
                else if (selectedNodes.contains(edge.getFrom()) && prevNodes.contains(edge.getTo()))
                    selectedEdges.add(edge);
                else if (prevNodes.contains(edge.getFrom()) && selectedNodes.contains(edge.getTo()))
                    selectedEdges.add(edge);
            }
            
            // Finalize our node and edge selection.
            nodes = selectedNodes;
            edges = selectedEdges;
        }

        // Write state file.
        saveState(prevNodeLevels, nodes);


        // TODO Delete debug output.
        System.out.println("Choosing Nodes:");
        for (Node n : nodes)
            System.out.println(n);
        System.out.println();
        System.out.println("Using edges:");
        for (Edge e : edges)
            System.out.println(e);
        System.out.println();


        // Initialize the LP generator.
        ILPGenerator gen = new ILPGenerator(nodes, edges, prevNodeLevels, new LPFormatter(),
                numLevels);

        // Generate the ILP.
        String ilp = gen.generate();
        saveOutput(ilp, "tmp/temp.lp");
    }
}
