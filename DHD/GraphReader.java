package DHD;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
     *
     * @param graphFile The graph file to parse.
     * @param directed True if the graph is directed.
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
        edges = new HashSet<Edge>();

        // Temporary node data structure used to add neighbors to nodes.
        Map<String, Node> tempNodes = new HashMap<String, Node>();

        BufferedReader input = null;
        String line;
        String[] lineObjs;
        try
        {
            if (graphFile == null)
                System.out.println("graphFile is null.");
             if (tempNodes== null)
                System.out.println("tempNodes is null.");


            input = new BufferedReader(new FileReader(graphFile));

            // Read the whole file.
            while ((line = input.readLine()) != null)
            {
                lineObjs = line.split("\\s++"); 
                
                Node a = new Node(lineObjs[0]);
                Node b = new Node(lineObjs[1]);

                // Add nodes to the temporary data structure.
                if (!tempNodes.containsKey(a.getName()))
                    tempNodes.put(a.getName(), a);
                if (!tempNodes.containsKey(b.getName()))
                    tempNodes.put(b.getName(), b);

                edges.add(new Edge(a,b));

                // Update the neighbors in the temporary data structure.
                a = tempNodes.get(a.getName());
                b = tempNodes.get(b.getName());
                a.addNeighbor(b);
                b.addNeighbor(a);
            }

            // Add the temporary nodes to the final node data structure.
            nodes = new HashSet<Node>(tempNodes.values());
        }
        catch(IOException e)
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
        return new HashSet<Node>(nodes);
    }

    /**
     * Returns the edges that were read from the graph file.
     * Lazy-reads the graph.
     */
    public Set<Edge> getEdges()
    {
        if (edges == null)
            parseGraph();
        return new HashSet<Edge>(edges);
    }


}
