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
package eu.scape_project.bitwiser.utils;

/**
 * @author AnJackson
 *
 */
public class LCS {

	/**
	 * Longest common substring algorithm
	 * @param a
	 * @param b
	 * @return
	 */
	public static String lcs(String a, String b) {
		// Create match matrix, which Java auto-initializes to zeros
	    int[][] lengths = new int[a.length()+1][b.length()+1];
	    
	    // Fill up the matrix:
	    for (int i = 0; i < a.length(); i++)
	        for (int j = 0; j < b.length(); j++)
	            if (a.charAt(i) == b.charAt(j))
	                lengths[i+1][j+1] = lengths[i][j] + 1;
	            else
	                lengths[i+1][j+1] =
	                    Math.max(lengths[i+1][j], lengths[i][j+1]);
	 
	    // Read the substring out from the matrix
	    StringBuffer sb = new StringBuffer();
	    for (int x = a.length(), y = b.length();
	         x != 0 && y != 0; ) {
	        if (lengths[x][y] == lengths[x-1][y])
	            x--;
	        else if (lengths[x][y] == lengths[x][y-1])
	            y--;
	        else {
	            assert a.charAt(x-1) == b.charAt(y-1);
	            sb.append(a.charAt(x-1));
	            x--;
	            y--;
	        }
	    }
	 
	    // Reverse and pass-back:
	    return sb.reverse().toString();
	}
	
	/**
	 * A simple example of use.
	 * 
	 * @param args
	 */
	public static void main( String[] args ) {
		System.out.println("LCS: " + lcs("computer","internet"));
		System.out.println("LCS: " + lcs("computer","input"));
		System.out.println("LCS: " + lcs("computer","zzzaaakk"));
	}
}
