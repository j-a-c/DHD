package DHD;

import java.io.File;
import java.util.Set;

/**
 * @author Joshua A. Campbell
 * Driver for the DHD module.
 */
class Driver
{
    public static void main(String[] args)
    {
        // Check argument length.
        if (args.length != 2)
        {
            System.out.println("Usage: java Driver graphFile numLevels");
            return;
        }

        // Test if graph file exists.
        File graphFile = new File(args[0]);
        if (!graphFile.exists())
        {
            System.out.println("Specified graph does not exist.");
            return;
        }

        GraphReader reader = new GraphReader(graphFile);
        
        // Get the parts of the graph.
        Set<Node> nodes = reader.getNodes();
        Set<Edge> edges = reader.getEdges();
    
        // Initialize the LP generator.
        ILPGenerator gen = new ILPGenerator(nodes, edges, new LPFormatter(), 
                Integer.parseInt(args[1]));

        // Generate the ILP.
        String ilp = gen.generate();
        
        // TODO
        System.out.println(ilp);
    }
}
