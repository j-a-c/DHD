package DHD;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    /**
     * Returns the ILP that will find hierarchy in the graph.
     *
     * M = numLevels
     *
     * For directed edge (n_i, n_j):
     *  p_i_j = {0,1}
     *
     *  t_j - t_i - M * p_i_j GTE -M
     *  t_j - t_i - M * p_i_j LTE -1
     *
     * min: sum(p_k)
     */
    public String generate()
    {
        // Set objective type to minimize.
        formatter.setObjectiveType(-1);

        // Coefficients for the constraints.
        List<Integer> coefs = new ArrayList<Integer>();
        coefs.add(1);
        coefs.add(-1);
        coefs.add(-1 * this.numLevels);

        // Holds the unique vars we will create that represent the levels.
        Set<String> levelVars = new HashSet<String>();

        // TODO
        for (Edge edge : edges)
        {
            // Name the penalty variable
            String p_i_j = edge.toString() + "__p";
            // Name the level variables.
            String t_j = edge.getTo().toString() + "__t";
            String t_i = edge.getFrom().toString() + "__t";

            // Store the level variables.
            levelVars.add(t_j);
            levelVars.add(t_i);

            // Variables for the constraint.
            List<String> vars = new ArrayList<String>();
            vars.add(t_j);
            vars.add(t_i);
            vars.add(p_i_j);

            // Generate two constraints.
            formatter.addConstraint(coefs, vars, 1, -this.numLevels);
            formatter.addConstraint(coefs, vars, -1, -1);
            
            // Add binary constraints.
            formatter.addBinaryVar(p_i_j);
            
            // Add penalty to objective function.
            formatter.addToObjective(1, p_i_j);
        }

        for (String level : levelVars)
        {
            // Add integer constraints.
            formatter.addIntegerVar(level);
            
            // Add integer bounds.
            formatter.addBound(0, this.numLevels-1, level);
        }
        
        return this.formatter.toString();
    }
}
