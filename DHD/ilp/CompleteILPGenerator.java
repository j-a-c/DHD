package DHD.ilp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import DHD.ds.*;

/**
 * @author Joshua A. Campbell
 *
 * Generates an ILP that to find the hierarchy in a complete graph.
 */
public class CompleteILPGenerator extends ILPGenerator
{
    private Set<Edge> edges;
    private Map<String, Integer> prevNodes;
    private CplexLPFormatter formatter;
    private int numLevels;

    /**
     * @param edges The edges in the graph.
     * @param prevNodes Nodes that should have a constant level in the graph.
     * @param formatter The formatter for the resulting ILP.
     * @param numLevels The number of levels in the hierarchy.
     */
    public CompleteILPGenerator(Set<Edge> edges, Map<String,Integer> prevNodes,
            CplexLPFormatter formatter, int numLevels)
    {
        this.edges = edges;
        this.prevNodes = prevNodes;
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
     *  A different constaint will be generated depending on whether the node
     *  was used in a previous computation or not:
     *  prevNodes (Node n):
     *      level(n) LTE newRank(n) LTE level(n)
     *  others (Node n):
     *      0 LTE newRank(n) LTE numLevels-1
     *
     * min: sum(p_k)
     */
    @Override
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

        for (Edge edge : edges)
        {
            // Name the penalty variable
            String p_i_j = edge.toString() + penaltyEnding;
            // Name the level variables.
            String t_j = edge.getTo().toString() + levelEnding;
            String t_i = edge.getFrom().toString() + levelEnding;

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

        for (String levelVar : levelVars)
        {
            // Add integer constraints.
            formatter.addIntegerVar(levelVar);

            // Check if this node must be a constant level.
            int endingIndex = levelVar.lastIndexOf(levelEnding);
            // The key will be the original node name.
            String key = levelVar.substring(0, endingIndex);
            if (prevNodes.containsKey(key))
            {
                int level = prevNodes.get(key);
                formatter.addBound(level, level, levelVar);
            }
            else
            {
                formatter.addBound(0, this.numLevels-1, levelVar);
            }
        }
        
        return this.formatter.toString();
    }
}
