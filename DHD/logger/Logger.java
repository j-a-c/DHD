package DHD.logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * A simple logging class. This class is not meant to be threadsafe.
 */
public class Logger
{

    private PrintWriter output;

    public Logger(String loc)
    {
        try
        {
            this.output = new PrintWriter(
                    new BufferedWriter(new FileWriter(loc, true)));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public void log(String msg)
    {
        output.println(msg);
        output.flush();
    }
}
