package DHD.ilp;

/**
 * @author Joshua A. Campbell
 */
public abstract class ILPGenerator
{
    // Ending identifiers for the variables generated for the ILP.
    public static final String penaltyEnding = "__p";
    public static final String levelEnding = "__t";

    // Returns the generated ILP.
    public abstract String generate();

}
