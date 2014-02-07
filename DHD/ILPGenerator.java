package DHD;

import java.util.Set;

/**
 * @author Joshua A. Campbell
 *
 * Generates an ILP that to find the hierarchy in the graph.
 */
class ILPGenerator
{
    private Set<Node> nodes;
    private Set<Edge> edges;
    private LPFormatter formatter;
    private int numLevels;

    /**
     * @param nodes The nodes in the graph.
     * @param edges The edges in the graph.
     * @param formatter The formatter for the resulting ILP.
     * @param numLevels The number of levels in the hierarchy.
     */
    public ILPGenerator(Set<Node> nodes, Set<Edge> edges, 
            LPFormatter formatter, int numLevels)
    {
        this.nodes = nodes;
        this.edges = edges;
        this.formatter = formatter;
        this.numLevels = numLevels;
    }

    public String generate()
    {
        // TODO
        
        return this.formatter.toString();
    }
}
