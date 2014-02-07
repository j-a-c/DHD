package DHD;

import java.io.File;

/**
 * @author Joshua A. Campbell
 * Driver for the DHD module.
 */
class Driver
{
    public static void main(String[] args)
    {
        // Check argument length.
        if (args.length != 1)
            System.out.println("Usage: java Driver graph");

        // Test if graph file exists.
        File graphFile = new File(args[0]);
        if (!graphFile.exists())
        {
            System.out.println("Specified graph does not exist.");
            return;
        }

        GraphReader reader = new GraphReader(graphFile);
    }
}
