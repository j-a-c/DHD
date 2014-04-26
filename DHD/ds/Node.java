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

    // The nodes which point to this node.
    private Set<Node> heads = new HashSet<Node>();
    // The nodes to which this node points to.
    private Set<Node> tails = new HashSet<Node>();

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
     * Marks that this node is part of an edge where this node is the tail and
     * the parameter node is the head.
     *
     * @param head A head of a directed edge containing this node.
     */
    public void addHead(Node head)
    {
        heads.add(head); 
    }

    /**
     * Marks that this node is part of an edge where this node is the head and
     * the parameter node is the tail.
     *
     * @param tail A tail of a directed edge containing this node.
     */
    public void addTail(Node tail)
    {
        tails.add(tail);
    }

    /**
     * Returns the name of the node.
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Returns the nodes which point to this node.
     */
    public Set<Node> getHeads()
    {
        return heads;
    }

    /**
     * Returns the nodes to which this node points to.
     */
    public Set<Node> getTails()
    {
        return tails;
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
