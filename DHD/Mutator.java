package DHD;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import DHD.ds.*;
import DHD.graph.*;

/**
 * @author Joshua A. Campbell
 *
 * Driver for the mutating function of the DHD module.
 * This driver will iteratively pick two random edges (i,j) and (u,v) such that
 * i != j != u != v and replace them with edges (i,v) and (u,j).
 *
 * Usage:
 *  java -cp DHD.jar DHD.Mutator -i graph -o output -n numEdges
 *
 *  @param i The input graph.
 *  @param o The output file.
 *  @param n The number of edges to mutate.
 */
public class Mutator
{
    private static File inputFile = null;
    private static File outputFile = null;
    private static int numEdges = -1;


    // We do not allow instantiation of a driver.
    private Mutator(){}

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
            System.err.println("Usage: java -cp DHD.jar DHD.Mutator -i input -o output -n numEdges");
            System.err.println("\t-i: The input graph file. (required)");
            System.err.println("\t-o: the output for the mutated graph. (required)");
            System.err.println("\t-n: The number of edges to mutate. (required)");
            return false;
        }

        // Check to make sure that we have a parameter for each flag.
        if (args.length % 2 != 0)
        {
            System.err.println("Each flag must have an argument.");
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
                case "-o":
                    outputFile = new File(param);
                    break;
                case "-n":
                    numEdges = Integer.parseInt(param);
                    break;
                default:
                    System.err.println("Illegal flag: " + args[index]);
                    return false;
            }
        }

        // One final check to ensure that all the parameters have been set. 
        if (inputFile == null || outputFile == null || numEdges == -1)
            return false;

        return true;
    }

    /**
     * This class will start execuation here.
     */
    public static void main(String[] args)
    {
        // Parse the command line arguments.
        if (!parseArgs(args)) return;

        // Read the original graph and get the nodes and edges.
        GraphReader reader = new DefaultGraphReader(inputFile);
        Set<Node> origNodes = reader.getNodes();
        Set<Edge> origEdges = reader.getEdges();

        // Create a new copy of the edges that we can easily modify.
        ArrayList<Edge> newEdges = new ArrayList<Edge>(origEdges);

        int totalEdges = origEdges.size();
        // Mutate some of the edges in the graph until we have satisfied the
        // edge requirement.
        while(numEdges > 0)
        {
            Random random = new Random();

            int index1 = random.nextInt(totalEdges);
            int index2 = random.nextInt(totalEdges);
            
            Edge edge1 = newEdges.get(index1);
            Edge edge2 = newEdges.get(index2);

            // Enforce that given two edges (i,j) and (u,v), i != j != u != v.
            if (edge1.getFrom() == edge2.getFrom())
                continue;
            if (edge1.getTo() == edge2.getTo())
                continue;
            if (edge1.getFrom() == edge2.getTo())
                continue;
            if (edge1.getTo() == edge2.getFrom())
                continue;

            // Remove the old edges from the graph.
            newEdges.remove(edge1);
            newEdges.remove(edge2);

            // Form the new edges.
            Edge newEdge1 = new Edge(edge1.getFrom(), edge2.getTo());
            Edge newEdge2 = new Edge(edge2.getFrom(), edge1.getTo());

            // Make sure the new edges don't exist already.
            if (newEdges.contains(newEdge1) || newEdges.contains(newEdge2))
                continue;

            // Add the two new edges to the graph.
            newEdges.add(newEdge1);
            newEdges.add(newEdge2);

            System.out.println("Removing: ");
            System.out.println("\t" + edge1);
            System.out.println("\t" +  edge2);

            System.out.println("Adding: ");
            System.out.println("\t" + newEdge1);
            System.out.println("\t" +  newEdge2);


            numEdges--;
        }

        // Output the mutated graph.
        GraphWriter writer = new DefaultGraphWriter(new HashSet<Edge>(newEdges), outputFile);
        writer.write();
    }

}
