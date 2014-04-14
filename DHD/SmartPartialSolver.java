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


    /**
     * Returns true if we must check this node to achieve a better hierarchy.
     * This is basically the smart selection algorithm. This function is to be
     * used on the modifications (added or deleted edges), not the neighbors of
     * the affected nodes.
     *
     * @param from The node the edge is coming from.
     * @param to The node the edge is going to.
     * @param rankings The previous rankings of the nodes.
     * @param added True if this edge is being added, false if the edge is
     * being deleted.
     *
     * @return True if we need to check this node, false otherwise.
     */
    private static boolean mustCheckModification(Node from, Node to, Map<String,Integer> rankings, boolean added)
    {
        int fromRank = rankings.get(from.getName());
        int toRank = rankings.get(to.getName());

        if (added) // This edge is being added.
        {
            if (fromRank > toRank)
                return false;
            else
                return true;
        }
        else // This edge is being deleted.
        {
            if (fromRank < toRank)
                return true;
            else return false;
        }
    }

    /**
     * Returns true if we must check this node to achieve a better hierarchy.
     * This function is to be used on the neighbors of the nodes that were
     * added or deleted initially.
     *
     * @param from The node the edge is coming from.
     * @param to The node the edge is going to.
     * @param tail The tail in the original edge.
     * @param rankings The previous rankings of the nodes.
     * @param added True if this edge is being added, false if the edge is
     * being deleted.
     * @param first True if the from node is part of the edge being inserted or
     * deleted.
     *
     * @return True if we need to check this node, false otherwise.
     */
    private static boolean mustCheckNeighbor(Node from, Node to, Node tail, Map<String,Integer> rankings, boolean added, boolean first)
    {
        int tailRank = rankings.get(tail.getName());

        if (added) // This edge is being added.
        {
            if (first) 
            {
                int headRank = rankings.get(from.getName());
                int toRank = rankings.get(to.getName());

                if (headRank == tailRank && headRank < toRank)
                    return true;
                else if (headRank < tailRank && toRank > headRank)
                    return true;
                else
                    return false;
            }
            else
            {
                int headRank = rankings.get(to.getName());
                int toRank = rankings.get(from.getName());

                if (headRank == tailRank && toRank < headRank)
                    return false;
                else if (headRank < tailRank && toRank < headRank)
                    return false;
                else
                    return true;

            }
        }
        else // This edge is being deleted.
        {
            if (first)
            {
                int headRank = rankings.get(from.getName());
                int toRank = rankings.get(to.getName());

                if (headRank == tailRank && toRank <= headRank)
                    return true;
                else if (headRank > tailRank && toRank > headRank)
                    return true;
                else
                    return false;
            }
            else
            {
                int headRank = rankings.get(to.getName());
                int toRank = rankings.get(from.getName());

                if (headRank == tailRank && toRank < headRank)
                    return true;
                else if (headRank > tailRank && toRank <= headRank)
                    return true;
                else 
                    return false;
            }
        }
    }



    /**
     * Execution will begin here.
     */
    public static void main(String[] args)
    {
    
        // Parse arguments.
        if (!parseArgs(args)) return;

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
        // Nodes that we can ignore.
        Set<Node> ignoreNodes = new HashSet<Node>();

        // Determine which nodes were modified.
        Set<Edge> modifiedEdges = new HashSet<Edge>();
        // Edges that were added.
        currEdges.removeAll(prevEdges);
        // Only check edges that we must.
        for (Edge edge : currEdges)
        {
            // See if we need to consider this edge at all.
            if (mustCheckModification(edge.getFrom(), edge.getTo(), rankings, true))
            {
                for (Node n : edge.getFrom().getNeighbors())
                {
                    // The edge is pointing to this node.
                    if (currReader.getEdges().contains(new Edge(n, edge.getFrom())))
                    {
                        // See if we must check this neighbor.
                        if (mustCheckNeighbor(n, edge.getFrom(), edge.getTo(), rankings, true, false))
                        {
                            if (modifiedNodes.add(edge.getFrom()))
                                nodesToCheck.add(edge.getFrom());
                            if (modifiedNodes.add(n))
                                nodesToCheck.add(n);
                        }
                        else // We can ignore this neighbor.
                        {
                            ignoreNodes.add(n);
                        }
                    }
                    else // The edge is pointing from this node.
                    {
                         // See if we must check this neighbor.
                        if (mustCheckNeighbor(edge.getFrom(), n, edge.getTo(), rankings, true, true))
                        {
                            if (modifiedNodes.add(edge.getFrom()))
                                nodesToCheck.add(edge.getFrom());
                            if (modifiedNodes.add(n))
                                nodesToCheck.add(n);
                        }
                        else // We can ignore this neighbor.
                        {
                            ignoreNodes.add(n);
                        }
                    }
                }
            }
            else // We can ignore this edge.
            {
                ignoreNodes.add(edge.getFrom());
                ignoreNodes.add(edge.getTo());
            }
        }
        
        // Edges that were removed.
        // We need to refresh the current edges.
        currEdges = currReader.getEdges();
        prevEdges.removeAll(currEdges);
        // Only check edges that we must.
        for (Edge edge : prevEdges)
        {
            // See if we need to consider this edge at all.
            if (mustCheckModification(edge.getFrom(), edge.getTo(), rankings, false))
            {
                for (Node n : edge.getFrom().getNeighbors())
                {
                    // The edge is pointing to this node.
                    if (currReader.getEdges().contains(new Edge(n, edge.getFrom())))
                    {
                        // See if we must check this neighbor.
                        if (mustCheckNeighbor(n, edge.getFrom(), edge.getTo(), rankings, false, false))
                        {
                            if (modifiedNodes.add(edge.getFrom()))
                                nodesToCheck.add(edge.getFrom());
                            if (modifiedNodes.add(n))
                                nodesToCheck.add(n);
                        }
                        else // We can ignore this neighbor.
                        {
                            ignoreNodes.add(n);
                        }
                    }
                    else // The edge is pointing from this node.
                    {
                         // See if we must check this neighbor.
                        if (mustCheckNeighbor(edge.getFrom(), n, edge.getTo(), rankings, false, true))
                        {
                            if (modifiedNodes.add(edge.getFrom()))
                                nodesToCheck.add(edge.getFrom());
                            if (modifiedNodes.add(n))
                                nodesToCheck.add(n);
                        }
                        else // We can ignore this neighbor.
                        {
                            ignoreNodes.add(n);
                        }
                    }
                }
            }
            else // We can ignore this edge.
            {
                ignoreNodes.add(edge.getFrom());
                ignoreNodes.add(edge.getTo());
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

            // Iterate over all nodes we need to check...
            for (Node node : nodesToCheck)
            {
                // And check their neighbors....
                for (Node neighbor : node.getNeighbors()) 
                {
                    // If we have not already visited this neighbor or if this
                    // neighbor is being ignored on purpose.
                    if (modifiedNodes.add(neighbor) || ignoreNodes.contains(neighbor))
                    {
                        // Add it to the nodes we will visit next iteration.
                        newNodesToCheck.add(neighbor);
                    }
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
    }

}
