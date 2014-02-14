/**
 * 
 */
package eu.scape_project.bitwiser;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import eu.scape_project.bitwiser.utils.Entropy;
import eu.scape_project.bitwiser.utils.StreamReader;

/**
 * Brute force bitwise analysis of transformations and analysers.
 * 
 * Compressed images appear to be rather good for spotting damage. 
 * Need to align with Volker's work. 
 * e.g. Small but silent damage is worse that major damage that is easy to spot.
 * Byte-wise versus bit-wise analysis modes? 
 * Byte-flips easily produce nonsense values (e.g. negative integers).
 * Bit-flips are more thorough and produces more accurate results.
 * Compare TIFFs, compressed, commented, etc. 
 * Collect massive data-map showing exit codes etc for each data point?
 * Of course, this is also testing the tools ability to cope with difficult input.
 * 
 * Metrics, format robustness and tool coverage.
 *  - Sensitivity: Fraction of bit-flips that raise warnings. 
 *  - Robustness: Fraction of bit-flips raise warnings are are repaired accurately/wrongly.
 *  - Coverage: Fraction of bit-flips that change the resulting file (including no-output).
 *  - Transmission: Bitwise difference in output file compared to the input file. ???
 *  - Fuzzing: Fraction of bit-flips that cause the process to flip out/hang/asplode.
 *  
 *  Largely superceded by the Python version.
 * 
 * 
 * @author anj
 *
 */
public class BitwiseAnalyser {
    
    enum PROCESS { 
        CLEAN,
        WARNING,
        ERROR
    }
    enum OUTPUT {
        SAME,
        DIFFERENT,
        NONE
    }
    
    private static final String CMD_CONVERT = "C:/Program Files/ImageMagick-6.8.8-Q16/convert";
    private static final String DATA_DIR	= "C:/Projects/SCAPE/BitWiser/Data/";

    public static void main(String [] args) throws IOException {
    	String inFile   = null;
    	String tFile    = null;
    	String outFile  = null;
    	String ext		= null;
    	
    	if (args.length!=2){
    		ext		= args[0].substring(args[0].lastIndexOf('.'));
    		inFile  = args[0];
    		tFile   = inFile.substring(0, inFile.lastIndexOf('.'))+".tmp"+ext;
    		outFile = inFile.substring(0, inFile.lastIndexOf('.'))+".jp2";
    		
    		process(inFile, tFile, outFile);
    	} else {
    		System.out.println("Usage: java BitwiseAnalyser <file path>");
    	}
    }
    
    private static void process(String inFile, String tFile, String outFile) throws IOException {
    	if(inFile==null || tFile==null || outFile==null ){
    		return;
    	}
    	
        File sourceFile = new File(inFile);
        File tempFile = new File(tFile);
        File outputFile = new File(outFile);
        copy(sourceFile,tempFile);
        
        // Entropy Calc:
        Entropy ent = new Entropy();
        System.out.println("Starting entropy calc...");
        ent.calculate(tempFile, false, false, false, false);
        
        // Start munging...
        System.out.println("Start bit-flipping...");
        RandomAccessFile rf = new RandomAccessFile(tempFile, "rws");
        String truth = runCommand(tempFile, outputFile);
        System.out.println("Truth is "+truth);
        String result = "";
        int clears = 0;
        int errors = 0;
        int warnings = 0;
        int count = 0;
        long len = tempFile.length();
        System.out.println("File length: "+len);
        for( long pos = 0; pos < tempFile.length(); pos++ ) {
        	count++;
       		System.out.println("Completed: "+(100*count/len)+"%");
            flipByteAt(rf,pos);
            result = runCommand(tempFile, outputFile);
            if( ! result.equals(truth) )
                System.out.println("Flipped byte "+pos+" : "+result);
            if( result.equals(truth) ) {
                clears++;
            }
            // To do - add flipBitAt as an alternative (and default):
            flipByteAt(rf,pos);
        }
        System.out.println("Clears: "+clears+"/"+tempFile.length());
    }
    
    static void flipByteAt(RandomAccessFile rf, long pos ) throws IOException {
        rf.seek(pos);
        byte b = rf.readByte();
        b = (byte) (b ^ 0xff);
        rf.seek(pos);
        rf.write(b);
    }

    /**
     * 
     * @param rf the file
     * @param bytepos byte position in file, 0 - len-1
     * @param bitpos bit position in file, 0 - 7
     * @throws IOException
     */
    static void flipBitAt(RandomAccessFile rf, long bytepos, int bitpos ) throws IOException {
        rf.seek(bytepos);
        byte b = rf.readByte();
        b = (byte) (b ^ (1<<(7-bitpos)));
        rf.seek(bytepos);
        rf.write(b);
    }
    
    static String runCommand( File tempFile, File outputFile ) throws IOException {
        outputFile.delete();
        String[] commands = new String[]{CMD_CONVERT, tempFile.getAbsolutePath(), outputFile.getAbsolutePath() };
                
        ProcessBuilder pb = new ProcessBuilder(commands);
        // Do this?
        pb.redirectErrorStream(true);
        Process child = pb.start();
        
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<String> ftr_stdout = executor.submit(new StreamReader(child.getInputStream()));
        Future<String> ftr_stderr = executor.submit(new StreamReader(child.getErrorStream()));
        
        try {
            child.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
        	// clear InterruptedException - is this still needed?
        	Thread.interrupted();
        }
        
        String stdout = "";
        String stderr = "";
        try {
			stdout = ftr_stdout.get();
			stderr = ftr_stderr.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        int exitCode = child.exitValue();
        boolean exists = outputFile.exists();
        
        // Shutdown the executor service so that program terminates
        executor.shutdown();
        
        if( exists ) {
            if(exitCode != 0 || stdout.length() > 1 || stderr.length() > 1) return "WARNING:"+exitCode+":"+stdout+"::"+stderr;
            return "CLEAR";
        } else {
            return "ERROR:"+exitCode+":"+stdout+"::"+stderr;
        }
    }
    
    static void copy(File src, File dst) throws IOException {
    	InputStream in = null;
    	OutputStream out = null;
    	
    	try{
    		in = new FileInputStream(src);
    		out = new FileOutputStream(dst);
    	
			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) >= 0) {
				if( len > 0 ) out.write(buf, 0, len);
			}
			out.flush();
    	} finally {
    		close(in);
    		close(out);
    	}
	}
    
    private static void close(Closeable c) {
        if (c==null) {
        	return; 
        }
        
        try {
            c.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
     }
}
