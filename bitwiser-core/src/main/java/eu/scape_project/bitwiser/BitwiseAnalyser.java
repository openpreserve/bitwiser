/**
 * Copyright (C) 2010 - 2013 SCAPE Consortium <office@scape-project.eu>
 *
 * This file is part of Bitwiser.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 
 */
package eu.scape_project.bitwiser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    
    private static final String CMD_CONVERT = "C:/Program Files/ImageMagick-6.7.3-Q16/convert";
    private static final String DATA_DIR	= "C:/Projects/SCAPE/BitWiser/Data/";

    public static void main(String [] args) throws IOException {
        File sourceFile = new File(DATA_DIR+"02c440f.tif");		//"/home/anj/Desktop/jp2-tests/32x32-lzw.tif");
        File tempFile = new File(DATA_DIR+"02c440f.tmp.tif");	//"/home/anj/Desktop/jp2-tests/32x32.tmp.tif");
        File outputFile = new File(DATA_DIR+"02c440f.jp2");		//"/home/anj/Desktop/jp2-tests/32x32.tmp.jp2");
        copy(sourceFile,tempFile);
        
        // Entropy Calc:
        Entropy ent = new Entropy();
        System.out.println("Starting entropy calc...");
        ent.calculate(tempFile, false, false, false, false);
        //System.exit(0);
        
        // Start munging...
        System.out.println("Start bit-flipping...");
        RandomAccessFile rf = new RandomAccessFile(tempFile, "rws");
        String truth = runCommand(tempFile, outputFile);
        System.out.println("Truth is "+truth);
        String result = "";
        int clears = 0;
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
//        String[] commands = new String[]{"file", tempFile.getAbsolutePath() };
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
     InputStream in = new FileInputStream(src);
     OutputStream out = new FileOutputStream(dst);

     // Transfer bytes from in to out
     byte[] buf = new byte[1024];
     int len;
     while ((len = in.read(buf)) >= 0) {
         if( len > 0 ) out.write(buf, 0, len);
     }
     in.close();
     out.flush();
     out.close();
    }
}
