package DHD.graph;

import java.util.Set;

import DHD.ds.*;

/**
 * Abstract class for reading graphs.
 */
public abstract class GraphReader
{

    /**
     * Returns the nodes of the graph.
     */
    public abstract Set<Node> getNodes();

    /**
     * Returns the edges of the graph.
     */
    public abstract Set<Edge> getEdges();
}
