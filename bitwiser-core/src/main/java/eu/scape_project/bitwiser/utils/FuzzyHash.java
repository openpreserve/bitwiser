package eu.scape_project.bitwiser.utils;

import org.apache.commons.lang.StringUtils;

public class FuzzyHash { 
	/** the blocksize used by the program, */
	protected int blocksize;
	/** the hash for this blocksize */
	protected String hash;
	/** the hash for twice the blocksize, */
	protected String hash2;
	/** the filename. */
	protected String filename = null;
	
	protected FuzzyHash() {
	}

	protected FuzzyHash(int blockSize, String string, String string2) {
		this.blocksize = blockSize;
		this.hash = string;
		this.hash2 = string2;
		this.clean();
	}
	
	private void clean() {
		// The raw output can contain NULLs, strip them out:
		this.hash = hash.replaceAll("\0", "");
		this.hash2 = hash2.replaceAll("\0", "");		
	}
	
	/**
	 * @return the blocksize
	 */
	public int getBlocksize() {
		return blocksize;
	}

	/**
	 * @return the hash
	 */
	public String getHash() {
		this.clean();
		return hash;
	}

	/**
	 * @return the hash2
	 */
	public String getHash2() {
		this.clean();
		return hash2;
	}

	/**
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		this.clean();
		String str =  this.blocksize + ":" + this.hash + ":" + this.hash2;
		if( this.filename != null ) {
			str += ",\""+this.filename+"\"";
		}
		return str;
	}
	
    /**
     * 
     * @param fhs
     * @return
     */
    public static FuzzyHash fromString(String fhs) {
        if (StringUtils.isBlank(fhs))
            return null;
        String[] parts = fhs.split("[:,]+", 4);
        if (parts.length != 3 && parts.length != 4)
            return null;
        FuzzyHash fh = new FuzzyHash(Integer.parseInt(parts[0]), parts[1],
                parts[2]);
        if (parts.length == 4)
            fh.filename = parts[3].replaceAll("^\"", "").replaceAll("\"$", "");
        return fh;
    }

    /**
     * 
     * @param h
     * @param h2
     * @return
     */
    public static int compare(FuzzyHash h, FuzzyHash h2) {
        return SSDeep.fuzzyCompare(h, h2);
    }

    /**
     * 
     * @param h1
     * @param h2
     * @return
     */
    public static int compare(String h1, String h2) {
        FuzzyHash fh1 = FuzzyHash.fromString(h1);
        FuzzyHash fh2 = FuzzyHash.fromString(h2);
        if (fh1 == null || fh2 == null) {
            return 0;
        }
        return FuzzyHash.compare(fh1, fh2);
    }

}