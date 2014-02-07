package DHD;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Joshua A. Campbell
 *
 * Reads an input file where the graph is specified as edge pairs.
 * Ex:
 * a b
 * a c
 * b d
 *
 * This class was not meant to be thread-safe.
 *
 * Nodes and edges must be unique.
 */
class GraphReader
{
    // The file containing the graph to be processed.
    private File graphFile;

    // Holds the nodes and edges in this graph.
    private Set<Node> nodes = null;
    private Set<Edge> edges = null;

    /**
     * Constructs a graph reader using the specified file as the input graph.
     */
    public GraphReader(File graphFile)
    {
        this.graphFile = graphFile;
    }

    /**
     * Parses the input graph file in order to file out the node and edge data
     * structures.
     */
    private void parseGraph()
    {
        nodes = new HashSet<Node>();
        edges = new HashSet<Edge>();

        BufferedReader input = null;
        String line;
        String[] lineObjs;
        try
        {
            input = new BufferedReader(new FileReader(graphFile));

            while ((line = input.readLine()) != null)
            {
                lineObjs = line.split("\\s++"); 
                
                Node a = new Node(lineObjs[0]);
                Node b = new Node(lineObjs[1]);

                nodes.add(a);
                nodes.add(b);
                edges.add(new Edge(a,b));
            }
        }
        catch(Exception e)
        {
            System.out.println(e);
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
                System.out.println(e);
            }
        }
    }

    /**
     * Returns the nodes that were read from the graph file.
     * Lazy-reads the graph.
     */
    public Set<Node> getNodes()
    {
        if (nodes == null)
            parseGraph();
        return nodes;
    }

    /**
     * Returns the edges that were read from the graph file.
     * Lazy-reads the graph.
     */
    public Set<Edge> getEdges()
    {
        if (edges == null)
            parseGraph();
        return edges;
    }


}
