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
package eu.scape_project.bitwiser.utils;

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

	protected FuzzyHash(int block_size, String string, String string2) {
		this.blocksize = block_size;
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
	
}