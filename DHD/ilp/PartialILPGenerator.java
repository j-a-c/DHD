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
 * Generates an ILP that to find the hierarchy when only certain nodes in the
 * hierarchy are allowed to be changed. This is the first attempt at dynamic
 * hierarchical decomposition.
 */
public class PartialILPGenerator extends ILPGenerator
{
    private Set<Edge> edges;
    private Set<Node> unmodifiedNodes;
    private Set<Node> modifiedNodes;
    private Map<String,Integer> rankings;
    private CplexLPFormatter formatter;
    private int levelChange;
    private int numLevels;


    /**
     * @param edges The edges in the graph.
     * @param unmodifiedNodes The nodes in the graph whose level ranking will
     * remain constant.
     * @param modifiedNodes The nodes in the graph whose level will be allowed
     * to deviate by + or - levelChange.
     * @param formatter The formatter for the resulting ILP.
     * @param levelChange The amount of levels a dynamic node can move up or
     * down the hierarchy.
     * @param numLevels The number of levels in the original graph.
     */
    public PartialILPGenerator(Set<Edge> edges, Set<Node> unmodifiedNodes,
            Set<Node> modifiedNodes, Map<String,Integer> rankings,
            CplexLPFormatter formatter, int levelChange, int numLevels)
    {
        this.edges = edges;
        this.unmodifiedNodes = unmodifiedNodes;
        this.modifiedNodes = modifiedNodes;
        this.rankings = rankings;
        this.formatter = formatter;
        this.levelChange = levelChange;
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
     * A different constaint will be used depending on whether this node was
     * unmodified or not:
     *  modified (Node n):
     *      lBound = level(n) - levelChange LT 0 ? 0 : level(n) -levelChange
     *      uBound = level(n) + levelChange GT numLevels-1 ? numLevels-1 :
     *          level(n) + levelChange
     *      lBound LTE newRank(n) LTE uBound
     *  unmodified:
     *      level(n) LTE newRank(n) LTE level(n)
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

        // Now we add the level constraints.
        for (String levelVar : levelVars)
        {
            // Add integer constraints.
            formatter.addIntegerVar(levelVar);

            // Check if this node must be a constant level.
            int endingIndex = levelVar.lastIndexOf(levelEnding);
            // The key will be the original node name.
            String key = levelVar.substring(0, endingIndex);

            // This node was unmodified nodes.
            if (unmodifiedNodes.contains(new Node(key)))
            {
                int level = rankings.get(key);
                formatter.addBound(level, level, levelVar);
            }
            else // This node was modified.
            {
                // Calculate the lower bound.
                int negOffset = rankings.get(key) - levelChange;
                int lBound = negOffset < 0 ? 0 : negOffset;

                // Calculate the upper bound.
                int posOffSet = rankings.get(key) + levelChange;
                int uBound = (posOffSet > (numLevels-1)) ? (numLevels-1) : posOffSet;

                formatter.addBound(lBound, uBound, levelVar);
            }
        }
        
        return this.formatter.toString();
    }
}
