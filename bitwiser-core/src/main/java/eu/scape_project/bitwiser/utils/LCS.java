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
	    for (int i = 0; i < a.length(); i++) {
	        for (int j = 0; j < b.length(); j++) {
	            if (a.charAt(i) == b.charAt(j)) {
	                lengths[i+1][j+1] = lengths[i][j] + 1;
	            } else {
	                lengths[i+1][j+1] =
	                    Math.max(lengths[i+1][j], lengths[i][j+1]);
	            }
	        }
	    }
	 
	    // Read the substring out from the matrix
	    StringBuffer sb = new StringBuffer();
	    for (int x = a.length(), y = b.length();
	         x != 0 && y != 0; ) {
	        if (lengths[x][y] == lengths[x-1][y]) {
	            x--;
	        } else if (lengths[x][y] == lengths[x][y-1]) {
	            y--;
	        } else {
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
