package DHD;

import java.lang.StringBuilder;
import java.util.List;

/**
 * @author Joshua A. Campbell
 *
 * Generates an ouput file that follows the CPLEX LP file format.
 */
class LPFormatter
{
    /*
     * Some string constants.
     */
    // Used to delimite fields for readability.
    private static final String SPACE = " ";
    // Colon.
    private static final String COLON = ":";
    // Type of the objective function.
    private static final String MAX = "Maximize";
    private static final String MIN = "Minimize";
    // Preceds the objective function.
    private static final String OBJHEADER = "obj:";
    // Constraint header.
    private static final String CONSHEADER = "Subject To";
    // The prefix for each constraint.
    private static final String CONSPREFIX = "c";
    // Bounds header
    private static final String BOUNDSHEADER = "Bounds";
    // Header for integer-only constraint.
    private static final String GENERAL = "General";
    // Header for binary Constraint.
    private static final String BINARY = "Binary";
    // Plus.
    private static final String PLUS = "+";
    // New line.
    private static final String NEWLINE = "\n";
    // Less than or equal to.
    private static final String LTE = "<=";
    // Greater than or equal to.
    private static final String GTE = ">=";
    // Equal to.
    private static final String EQ = "=";
    // Terminates the file.
    private static final String END = "End";

    // Holds the type ob the objective function. Maximize by default.
    private String objectiveType = MAX;
    // Holds the current objective function.
    private StringBuilder objectiveFunction = new StringBuilder();
    // Constraint functions for the problem.
    private StringBuilder constraints = new StringBuilder();
    // Bounds on variables.
    private StringBuilder bounds = new StringBuilder();
    // Holds the variables that have been constrained to be integers.
    private StringBuilder generalVars = new StringBuilder();
    // Holds the variables that have been constrained to be binary.
    private StringBuilder binaryVars = new StringBuilder();

    // The number of constraints that have been added so far.
    private int numConstraints = 0;
    
    public LPFormatter()
    {
    
    }

    /**
     * Sets the type (max/min) of the objective function.
     * A positive number indicates max, anything else indicates min.
     *
     * @param type The type of the objective function (+ = max, !+ = min).
     */
    public void setObjectiveType(int type)
    {
        if (type >= 0)
            objectiveType = MAX;
        else 
            objectiveType = MIN;
    }

    /**
     * Adds the variable will the given coefficients to the object function.
     * Ex: addToObjective(-1, "x")
     * Will result in -1x if the object function is empty.
     *
     * @param coef The coefficient of the variable.
     * @param var The variable to add.
     */
    public void addToObjective(int coef, String var)
    {
        if (objectiveFunction.length() == 0)
        {
            objectiveFunction.append(SPACE);
            objectiveFunction.append(OBJHEADER);
        }
        else 
        {
            objectiveFunction.append(SPACE);
            objectiveFunction.append(PLUS);
        }

        if (coef != 1)
        {
            objectiveFunction.append(SPACE);
            objectiveFunction.append(coef);
        }

        objectiveFunction.append(SPACE);
        objectiveFunction.append(var);
    }

    /**
     * Adds a new constraint to the list of constraints.
     * The coefficients list and the variables list must be the same size.
     *
     * @param coefs The coefficients of the constraint function.
     * @param vars The variables corresponding to the coefficients in the
     * constraint function.
     * @param eq Is this contraint <= (negative number), = (0), or >= (positive
     * number).
     * @param bound The bound on the contraint.
     */
    public void addConstraint(List<Integer> coefs, List<String> vars, 
            int eq, int bound)
    {
        if (coefs.size() != vars.size())
            return;

        if (constraints.length() != 0)
            constraints.append(NEWLINE);

        constraints.append(SPACE);
        constraints.append(CONSPREFIX);
        constraints.append(++numConstraints);
        constraints.append(COLON);

        for (int index = 0; index < vars.size(); index++)
        {
            constraints.append(SPACE);

            if (index != 0)
            {
                constraints.append(PLUS);
                constraints.append(SPACE);
            }

            constraints.append(coefs.get(index));
            constraints.append(SPACE);
            constraints.append(vars.get(index));
        }

        constraints.append(SPACE);
        if (eq == 0)
            constraints.append(EQ);
        else if (eq > 0)
            constraints.append(GTE);
        else constraints.append(LTE);

        constraints.append(SPACE);

        constraints.append(bound);
    }

    /**
     * Adds the specified variable to the list of variables that must be
     * integers.
     *
     * @param var The variable that must be integer.
     */
    public void addIntegerVar(String var)
    {
        if (generalVars.length() != 0)
            generalVars.append(NEWLINE);

        generalVars.append(SPACE);
        generalVars.append(var);
    }

    /**
     * Adds the specified variable to the list of variables that must be binary.
     *
     * @param var The variable that must be binary.
     */
    public void addBinaryVar(String var)
    {
        if (binaryVars.length() != 0)
            binaryVars.append(NEWLINE);

        binaryVars.append(SPACE);
        binaryVars.append(var);
    }

    /**
     * Add a new bound on a variable.
     *
     * @param lower The lower bound.
     * @param upper The upper bound.
     * @param var The variable to be bounded.
     *
     * Ex: lower <= var <= upper.
     */
    public void addBound(int lower, int upper, String var)
    {
        if (bounds.length() != 0)
            bounds.append(NEWLINE);
        
        bounds.append(SPACE);
        bounds.append(lower);
        bounds.append(SPACE);
        bounds.append(LTE);
        bounds.append(SPACE);
        bounds.append(var);
        bounds.append(SPACE);
        bounds.append(LTE);
        bounds.append(SPACE);
        bounds.append(upper);
    }

    /**
     * Returns the representation of the LP formed so far.
     */
    @Override
    public String toString()
    {
        StringBuilder lp = new StringBuilder();
        
        // Add objective function and type.
        if (objectiveFunction.length() != 0)
        {
                lp.append(objectiveType.toString());
                lp.append(NEWLINE);
                lp.append(objectiveFunction.toString());
                lp.append(NEWLINE);
        }

        // Add contraints.
        if (constraints.length() != 0)
        {
            lp.append(CONSHEADER);
            lp.append(NEWLINE);
            lp.append(constraints.toString());
            lp.append(NEWLINE);
        }

        // Add bounds.
        if (bounds.length() != 0)
        {
            lp.append(BOUNDSHEADER);
            lp.append(NEWLINE);
            lp.append(bounds.toString());
            lp.append(NEWLINE);
        }

        // Add integer constraints.
        if (generalVars.length() != 0)
        {
            lp.append(GENERAL);
            lp.append(NEWLINE);
            lp.append(generalVars.toString());
            lp.append(NEWLINE);
        }

        // Add binary constraints.
        if (binaryVars.length() != 0)
        {
            lp.append(BINARY);
            lp.append(NEWLINE);
            lp.append(binaryVars.toString());
            lp.append(NEWLINE);
        }

        // End the lp specification.
        lp.append(END);

        return lp.toString();
    }
}
