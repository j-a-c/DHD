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
    private Node from;
    private Node to;

    public Edge(Node from, Node to)
    {
        this.from = from;
        this.to = to;
    }

    public Node getFrom()
    {
        return this.from;
    }

    public Node getTo()
    {
        return this.to;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other instanceof Edge)
        {
            Edge e = (Edge) other;
            return this.from.equals(e.from) && this.to.equals(e.to);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return from.hashCode() & to.hashCode();
    }

    @Override 
    public String toString()
    {
        return this.from.toString() + "__" + this.to.toString();
    }


}
