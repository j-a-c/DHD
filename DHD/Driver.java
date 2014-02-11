package DHD;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
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
        
        PrintWriter out = null;
        try
        {
            out = new PrintWriter("graph.lp");
            out.write(ilp);
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Unable to write ILP to file. Outputint to STOUT.");
            System.out.println(ilp);
        }
        finally
        {
            if (out != null)
                out.close();
        }
    }
}
