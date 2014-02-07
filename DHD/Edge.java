package DHD;

/**
 * @author Joshua A. Campbell
 *
 * Represents an edge.
 */
class Edge
{
    /*
     * Given the directed edge:
     * A --> B
     * from = A, to = B.
     * Given an undirected, the order should not matter.
     */
    private String from;
    private String to;

    public Edge(String from, String to)
    {
        this.from = from;
        this.to = to;
    }

}
