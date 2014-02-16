package DHD;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Set;

/**
 * @author Joshua A. Campbell
 * Driver for the DHD module.
 * The driver stores some state information in tmp/__state.
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
 * manner is not guaranteed to produce an optimal solution.
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
    // The file containing the graph to parse.
    private static File graphFile = null;
    // The ILP solution to the previous iteration.
    private static File prevFile = null;
    // The number of levels in the hierarchical decomposition.
    private static int numLevels = -1;
    // The number of nodes to include this iteration.
    private static int numNodesThisIter = Integer.MAX_VALUE;


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

    public static void main(String[] args)
    {
        // Parse command line arguments.
        if (!parseArgs(args)) return;

        if (graphFile == null || numLevels == -1)
        {
            System.out.println("-i, -l  parameters are required.");
            return;
        }

        GraphReader reader = new GraphReader(graphFile);
        
        // Get the parts of the graph.
        Set<Node> nodes = reader.getNodes();
        Set<Edge> edges = reader.getEdges();

        // TODO form new edge and node set if necessary.

    
        // Initialize the LP generator.
        ILPGenerator gen = new ILPGenerator(nodes, edges, new LPFormatter(),
                numLevels);

        // Generate the ILP.
        String ilp = gen.generate();
        saveOutput(ilp, "tmp/temp.lp");
    }
}
