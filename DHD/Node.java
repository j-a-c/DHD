package DHD;

/**
 * @author Joshua A. Campbell
 *
 * Represents a node in a graph.
 */
class Node
{
    // The name of this node. Should be unique in a graph.
    private String name;

    public Node(String name)
    {
        this.name = name;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other instanceof Node)
            return this.name.equals(((Node)other).name);
        return false;
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public String toString()
    {
        return name;
    }

}
