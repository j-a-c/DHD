package DHD.ds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Joshua A. Campbell
 *
 * Represents a node in a graph.
 */
public class Node
{
    // The name of this node. Should be unique in a graph.
    private String name;
    // The neighbors of this node.
    private Map<String, Node> neighbors = new HashMap<String, Node>();

    public Node(String name)
    {
        this.name = name;
    }

    /**
     * Add a new neighbors of the current node. Neighbors are considered in
     * this undirected sense. If (A,B) is a directed edge, the A is a neighbor
     * of B and B is a neighbor of A.
     */
    public void addNeighbor(Node neighbor)
    {
        String neighName = neighbor.getName();
        if (!neighbors.containsKey(neighName))
            neighbors.put(neighName, neighbor);
    }

    /**
     * Returns the name of the node.
     */
    public String getName()
    {
        return this.name;
    }


    /**
     * Returns a set containing the neighbors of this node.
     */
    public Set<Node> getNeighbors()
    {
        return new HashSet<Node>(neighbors.values());
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
        return getName();
    }

}
