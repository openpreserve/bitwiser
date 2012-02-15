/**
 * 
 */
package eu.scape_project.bitwiser.utils;

import java.io.*;
import java.util.concurrent.Callable;

/**
 * Implements a Callable method to read from a specified {@link InputStream}.
 * Used specifically to read stdout/stderr from a {@link Process} in separate
 * threads so as not to block {@link Process#waitFor()} calls.
 * 
 * @author Peter May
 * @see BitwiseAnalyser#runCommand(File, File, long)
 */
public class StreamReader implements Callable<String> {
    InputStream is;
    
    /**
     * Construct a new {@link StreamReader} for the specified {@link InputStream}.
     * 
     * @param is	the {@link InputStream} to read
     */
    public StreamReader(InputStream is) {
        this.is = is;
    }
    
    /**
     * Builds a {@link String} from data received on the {@link InputStream} specified
     * in the constructor.
     * 
     * @return	String	sets a {@link String} result accessible via {@link Future#get()}.
     */
    public String call() throws Exception {
    	StringBuilder sb = new StringBuilder();
        String line = null;
        try {
        	BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException ioe) {
        	ioe.printStackTrace();  
        }
        return sb.toString();
    }
}
