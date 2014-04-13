package DHD.graph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Set;

import DHD.ds.*;

/**
 * @author Joshua A. Campbell
 *
 * Outputs the graph in the default format.
 * The file will consist of edges:
 *  E1  E2
 *  E3  E1
 *  ...
 *  For each edge that appears in the graph.
 */
public class DefaultGraphWriter extends GraphWriter
{
    // The edges of the graph.
    private Set<Edge> edges;
    // The output file to write to.
    private File outputFile;

    public DefaultGraphWriter(Set<Edge> edges, File outputFile)
    {
        this.edges = edges;
        this.outputFile = outputFile;
    }
    
    @Override
    public void write()
    {
        try (PrintWriter writer = new PrintWriter(outputFile))
        {
            for (Edge edge : edges)
                writer.println(edge.getFrom() + " " + edge.getTo());

            writer.close();
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Unable to write graph to file in DefaultGraphWriter.write()");
            System.err.println(e);
        }
    }
}
