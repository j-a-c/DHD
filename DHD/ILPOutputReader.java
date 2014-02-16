package DHD;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses the result of SCIP ILP solver.
 */
class ILPOutputReader
{
    private static final String levelEnding = ILPGenerator.levelEnding;

    private File solutionFile;

    public ILPOutputReader(File solutionFile)
    {
        this.solutionFile = solutionFile; 
    }

    /** 
     * Returns the variable mappings for the solution to the ILP.
     */
    public Map<String, Integer> getResults()
    {
        Map<String, Integer> results = new HashMap<String, Integer>();

        BufferedReader input = null;
        String line;

        try
        {
            input = new BufferedReader(new FileReader(solutionFile));

            boolean objectiveFound = false;

            // Read the whole file.
            while ((line = input.readLine()) != null)
            {
                // If we find the objective value, we are ready to start
                // parsing the ILP solution.
                if (line.startsWith("objective value:"))
                {
                    objectiveFound = true;
                    continue;
                }

                // The values of the variables start on the line after the
                // objective function. We need to wait until we find the
                // objective value.
                if (objectiveFound)
                {
                    String[] lineObjs = line.split("\\s++");
                    int nameIndex = lineObjs[0].lastIndexOf(levelEnding);
                    // Penalties may also be in this file.
                    if (nameIndex == -1)
                        continue;
                    String name = lineObjs[0].substring(0, nameIndex);
                    results.put(name, Integer.parseInt(lineObjs[1]));
                }
            }
        }
        catch(IOException e)
        {
            System.err.println(e);
        }
        finally
        {
            try
            {
                if (input != null)
                    input.close();
            }
            catch (IOException e)
            {
                System.err.println(e);
            }
        }
        
        return results;
    }
}
