package DHD;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import DHD.ds.*;
import DHD.graph.*;
import DHD.ilp.*;
import DHD.logger.*;

/**
 * @author Joshua A. Campbell
 *
 * Partially solves a graph hierarchy given a previous vertex ranking, the new
 * graph to solve, and the edges that have changed since the previous ranking.
 *
 * This version uses a smarter selection in order to minimize the amount of
 * constraints that we add.
 *
 * Usage:
 *  java -cp DHD.jar DHD.PartialSolver -i graphFile -p prevRanking -d newEdges
 *      -k size -l levels
 *
 *  The format for the edge diff file should be:
 */
public class SmartPartialSolver
{
    private static File inputFile = null;
    private static File prevRankingFile = null;
    private static File prevGraphFile = null;
    private static int neighborhoodSize = -1;
    private static int levelChange = -1;
    private static int numLevels = -1;

    // We do not allow instantiation of this class.
    private SmartPartialSolver(){}

    /** 
     * Parses the input arguments.
     *
     * @param args The arguments to parse.
     *
     * @return Returns true if the arguments were successfully parsed.
     */
    private static boolean parseArgs(String[] args)
    {
        // Print a usage message if there are no arguments.
        if (args.length == 0)
        {
            System.err.println("Usage: java -cp DHD.jar DHD.PartialSolver [params]");
            System.err.println("\t-i: The input graph file. (required)");
            System.err.println("\t-p: The ranking file for the previous graph. (required)");
            System.err.println("\t-d: The previous graph file. (required)");
            System.err.println("\t-k: The neighborhood size to consider. (required)");
            System.err.println("\t-c: The max levels a dynamic node can move up or down the hierarchy. (required)");
            System.err.println("\t-l: The number of levels in the original graph.");
            return false;
        }

        // Check to make sure that we have a parameter for each flag.
        if (args.length % 2 != 0)
        {
            System.err.println("Each flag must have an argument.");
            System.err.println(Arrays.toString(args));
            return false;
        }

        // Since all of our arguments are preceeded by flags, we will swithc on
        // the flag.
        for (int index = 0; index < args.length; index += 2)
        {
            String param = args[index + 1];

            switch (args[index])
            {
                case "-i":
                    inputFile = new File(param);
                    if (!inputFile.exists())
                    {
                        System.err.println("File does not exist: " +  param);
                        return false;
                    }
                    break;
                case "-p":
                    prevRankingFile = new File(param);
                    if (!prevRankingFile.exists())
                    {
                        System.err.println("File does not exist: " +  param);
                        return false;
                    }
                    break;
                case "-d":
                    prevGraphFile = new File(param);
                    if (!prevGraphFile.exists())
                    {
                        System.err.println("File does not exist: " +  param);
                        return false;
                    }
                    break;
                case "-k":
                    neighborhoodSize = Integer.parseInt(param);
                    break;
                case "-c":
                    levelChange = Integer.parseInt(param);
                    break;
                case "-l":
                    numLevels = Integer.parseInt(param);
                    break;
                default:
                    System.err.println("Illegal flag: " + args[index]);
                    return false;
            }
        }

        // One final check to ensure that all the parameters have been set. 
        if (inputFile == null || prevRankingFile == null || prevGraphFile == null)
            return false;
        if (neighborhoodSize == -1 || levelChange == -1 || numLevels == -1)
            return false;

        return true;
    }

    /**
     * Reads the rankings (state file) in order to produce a map of the rankings.
     */
    public static Map<String,Integer> readStateFile()
    {
        Map<String,Integer> rankings = new HashMap<String,Integer>();

        String line;
        String[] lineObjs;
        try (BufferedReader input = new BufferedReader(new FileReader(prevRankingFile)))
        {
            // Read the whole file.
            while ((line = input.readLine()) != null)
            {
                lineObjs = line.split("\\s++"); 
                
                rankings.put(lineObjs[0], Integer.parseInt(lineObjs[1]));
            }

            input.close();
        }
        catch(IOException e)
        {
            System.err.println(e);
        }

        return rankings;
    }

    /**
     * Saves the output to the file at the given location.
     *
     * @param output The string to output.
     * @param loc The location to save the output to.
     */
    private static void saveOutput(String output, String loc)
    {
        try (PrintWriter outWriter = new PrintWriter(loc))
        {
            outWriter.write(output);

            outWriter.close();
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Unable to write ILP to file. Outputting to STOUT.");
            System.err.println(output);
        }

    }

    private static int UP = 0;
    private static int DOWN = 1;


    /**
     * Execution will begin here.
     */
    public static void main(String[] args)
    {
        // Parse arguments.
        if (!parseArgs(args)) return;

        // A logger for debug.
        Logger logger = new Logger("tmp/smartLog");

        // Get the edges from the previous graph.
        GraphReader prevReader = new DefaultGraphReader(prevGraphFile);
        Set<Edge> prevEdges = prevReader.getEdges();
        
        // Get the edges from the current graph.
        GraphReader currReader = new DefaultGraphReader(inputFile);
        Set<Edge> currEdges = currReader.getEdges();

        // The previous node rankings.
        Map<String,Integer> rankings = readStateFile();

        // Now we need to find all the nodes that were affected. This is
        // because we impose constraints on the nodes, not the edges. We will
        // also calculate the neighborhood sets at the name time.
        Set<Node> modifiedNodes = new HashSet<Node>();
        List<Node> nodesToCheck = new ArrayList<Node>();

        // Mark the movement of nodes.
        Map<Node, HashSet<Integer>> movements = new HashMap<Node, HashSet<Integer>>();

        // Determine which nodes were modified.
        Set<Edge> modifiedEdges = new HashSet<Edge>();
        // Edges that were added.
        currEdges.removeAll(prevEdges);
        // Only check edges that we must.
        for (Edge edge : currEdges)
        {
            Node fromNode = edge.getFrom();
            Node toNode = edge.getTo();
            int fromRank = rankings.get(fromNode.getName());
            int toRank = rankings.get(toNode.getName());

            if (fromRank <= toRank)
            {
                if (movements.get(fromNode) == null)
                    movements.put(fromNode, new HashSet<Integer>());
                if (movements.get(toNode) == null)
                    movements.put(toNode, new HashSet<Integer>());

                movements.get(fromNode).add(UP);
                movements.get(toNode).add(DOWN);

                nodesToCheck.addAll(fromNode.getNeighbors());
                nodesToCheck.addAll(toNode.getNeighbors());

                modifiedNodes.add(fromNode);
                modifiedNodes.add(toNode); 

                //logger.log(fromNode + " (head of added edge) " + fromRank + ", tail rank (" + toNode + "):" + toRank);
            }
        }
        
        // Edges that were removed.
        // We need to refresh the current edges.
        currEdges = currReader.getEdges();
        prevEdges.removeAll(currEdges);
        // Only check edges that we must.
        for (Edge edge : prevEdges)
        {
            Node fromNode = edge.getFrom();
            Node toNode = edge.getTo();
            int fromRank = rankings.get(fromNode.getName());
            int toRank = rankings.get(toNode.getName());

            if (fromRank > toRank) // Used to be >=
            {
                if (movements.get(fromNode) == null)
                    movements.put(fromNode, new HashSet<Integer>());
                if (movements.get(toNode) == null)
                    movements.put(toNode, new HashSet<Integer>());

                movements.get(fromNode).add(DOWN);
                movements.get(toNode).add(UP);

                nodesToCheck.addAll(fromNode.getNeighbors());
                nodesToCheck.addAll(toNode.getNeighbors());

                modifiedNodes.add(fromNode);
                modifiedNodes.add(toNode);

                //logger.log(fromNode + " (head of deleted edge) " + fromRank + ", tail rank (" + toNode + "):" + toRank);

            }
        }

        // Each iteration we will check k-th neighbors (nodes that have a path
        // of at most k edges from themselves to the original nodes in
        // nodesToCheck.
        while (neighborhoodSize > 0)
        {
            // We want to keep the 'new nodes to check' and the 'old nodes to
            // check' separate.
            List<Node> newNodesToCheck = new ArrayList<Node>();

            boolean checkNeighbors = false;

            // Iterate over all nodes we need to check...
            for (Node nodeToCheck : nodesToCheck)
            {
                int currNodeRank = rankings.get(nodeToCheck.getName());

                // Check the nodes that point to this node.
                for (Node head : nodeToCheck.getHeads())
                {
                    int currHeadRank = rankings.get(head.getName());

                    // If rank(head) >= rank(current node) and the head is
                    // moving.
                    if (currHeadRank >= currNodeRank 
                            && movements.get(head) != null)
                    {

                        // if rank(head) == rank(current node) and the head is
                        // moving up.
                        if ((currHeadRank == currNodeRank) && (movements.get(head).contains(UP)))
                        {
                            if (movements.get(nodeToCheck) == null)
                                movements.put(nodeToCheck, new HashSet<Integer>());

                            movements.get(nodeToCheck).add(UP);
                            
                            checkNeighbors = true;
                        }

                        if ((currHeadRank >= currNodeRank) && (movements.get(head).contains(DOWN)))
                        {
                            if (movements.get(nodeToCheck) == null)
                                movements.put(nodeToCheck, new HashSet<Integer>());

                            movements.get(nodeToCheck).add(DOWN);
                        
                            checkNeighbors = true;
                        }
                    }

                }

                // Check the nodes that this node points to.
                for (Node tail : nodeToCheck.getTails())
                {
                    int currTailRank = rankings.get(tail.getName());

                    // If rank(tail) > rank (current node) and the tail is moving.
                    if ((currTailRank > currNodeRank) && (movements.get(tail) != null)) 
                    {
                        if (movements.get(tail).contains(UP))
                        {
                            if (movements.get(nodeToCheck) == null)
                                movements.put(nodeToCheck, new HashSet<Integer>());

                            movements.get(nodeToCheck).add(UP);
                        
                            checkNeighbors = true;
                        }

                        if (movements.get(tail).contains(DOWN))
                        {
                            if (movements.get(nodeToCheck) == null)
                                movements.put(nodeToCheck, new HashSet<Integer>());

                            movements.get(nodeToCheck).add(DOWN);
                            
                            checkNeighbors = true;
                        }
                    }
                }

                if (checkNeighbors)
                {
                    modifiedNodes.add(nodeToCheck);
                    newNodesToCheck.addAll(nodeToCheck.getNeighbors());
                }

            }
        
            // Update the new nodes to check.
            nodesToCheck = newNodesToCheck;
        
            neighborhoodSize--;
        }

        // At this point we now have the k-th neighbors and the nodes that were
        // originally modified in the graph in the set modifiedNodes. All other
        // nodes should be constants in the ILP we form. The ranking of the
        // nodes in modifiedNodes should be allowed to change by +levelChange
        // or -levelChange.

        // These are the nodes whose level will remain constant.
        Set<Node> unmodifiedNodes = prevReader.getNodes();
        unmodifiedNodes.removeAll(modifiedNodes);

        PartialILPGenerator generator = new PartialILPGenerator(currEdges, 
                unmodifiedNodes, modifiedNodes, rankings, 
                new CplexLPFormatter(), levelChange, numLevels);

        // Generate and save the ILP formulation.
        String ilp = generator.generate();
        saveOutput(ilp, "tmp/temp.lp");

        logger.log("# Modified nodes: " + modifiedNodes.size() + 
                " Total nodes:" + (unmodifiedNodes.size() + modifiedNodes.size()));
        logger.log("=====");
    }

}
