package DHD;

import java.io.File;
import java.util.List;
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
 */
class GraphReader
{
    // The file containing the graph to be processed.
    private File graphFile;

    // Holds the nodes and edges in this graph.
    private Set<Node> nodes = null;
    private List<Edge> edges = null;

    /**
     * Constructs a graph reader using the specified file as the input graph.
     */
    public GraphReader(File graphFile)
    {
        this.graphFile = graphFile;
    }

    public Set<Node> getNodes()
    {
        if (nodes == null)
        {
            // TODO
        }
        
        return nodes;
    }


}
