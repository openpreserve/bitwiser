/* ssdeep
   Copyright (C) 2006 ManTech International Corporation

   $Id: fuzzy.c 97 2010-03-19 15:10:06Z jessekornblum $

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

   The code in this file, and this file only, is based on SpamSum, part 
   of the Samba project: 
         http://www.samba.org/ftp/unpacked/junkcode/spamsum/

   Because of where this file came from, any program that contains it
   must be licensed under the terms of the General Public License (GPL).
   See the file COPYING for details. The author's original comments
   about licensing are below:



  this is a checksum routine that is specifically designed for spam. 
  Copyright Andrew Tridgell <tridge@samba.org> 2002

  This code is released under the GNU General Public License version 2
  or later.  Alteratively, you may also use this code under the terms
  of the Perl Artistic license.

  If you wish to distribute this code under the terms of a different
  free software license then please ask me. If there is a good reason
  then I will probably say yes.
  
*/
package eu.scape_project.bitwiser.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;

/**
 * SSDeep
 *
 * <p>
 * A Java version of the ssdeep algorithm, based on the fuzzy.c source 
 * code, taken from version 2.6 of the ssdeep package.
 * 
 * <p>
 * Transliteration/port to Java from C by...
 * 
 * @author Andrew Jackson <Andrew.Jackson@bl.uk>
 *
 */
public class SSDeep {
	
	/// Length of an individual fuzzy hash signature component
	public static final int SPAMSUM_LENGTH = 64;
	
	/// The longest possible length for a fuzzy hash signature (without the filename)
	public static final int FUZZY_MAX_RESULT = (SPAMSUM_LENGTH + (SPAMSUM_LENGTH/2 + 20));

	
	public static final int MIN_BLOCKSIZE  = 3;
	public static final int ROLLING_WINDOW = 7;

	public static final int HASH_PRIME     = 0x01000193;
	public static final int HASH_INIT      = 0x28021967;

	// Our input buffer when reading files to hash
	public static final int BUFFER_SIZE  = 8192;

	static class roll_state_class {
	  int[] window = new int[ROLLING_WINDOW];
	  int h1, h2, h3;
	  int n;
	}
	private static roll_state_class rollState = new roll_state_class();


	/*
	  a rolling hash, based on the Adler checksum. By using a rolling hash
	  we can perform auto resynchronisation after inserts/deletes

	  internally, h1 is the sum of the bytes in the window and h2
	  is the sum of the bytes times the index

	  h3 is a shift/xor based rolling hash, and is mostly needed to ensure that
	  we can cope with large blocksize values
	*/
	static int rollHash(int c)
	{
		
	  rollState.h2 -= rollState.h1;
	  //roll_state.h2 = roll_state.h2 & 0x7fffffff
	  rollState.h2 += ROLLING_WINDOW * c;
	  //roll_state.h2 = roll_state.h2 & 0x7fffffff
	  
	  rollState.h1 += c;
	  //roll_state.h1 = roll_state.h1 & 0x7fffffff
	  rollState.h1 -= rollState.window[(rollState.n % ROLLING_WINDOW)];
	  //roll_state.h1 = roll_state.h1 & 0x7fffffff
	  
	  rollState.window[rollState.n % ROLLING_WINDOW] = (char)c;
	  rollState.n = (rollState.n+1)%ROLLING_WINDOW;
	  
	  /* The original spamsum AND'ed this value with 0xFFFFFFFF which
	     in theory should have no effect. This AND has been removed 
	     for performance (jk) */
	  rollState.h3 = (rollState.h3 << 5);
	  rollState.h3 ^= c;
	  //roll_state.h3 = roll_state.h3 & 0x7FFFFFFF
	  //if( roll_state.h3 > 0xEFFFFFFF ) roll_state.h3 -= 0xEFFFFFFF
	  
	  long result = ((rollState.h1 + rollState.h2 + rollState.h3));//&0x7FFFFFFF
	  
	  return (int) result;
	}

	/*
	  reset the state of the rolling hash and return the initial rolling hash value
	*/
	static void rollReset()
	{	
		  rollState.h1 = 0;
		  rollState.h2 = 0;
		  rollState.h3 = 0;
		  rollState.n = 0;
		  Arrays.fill(rollState.window,(char)0);
	}

	/* a simple non-rolling hash, based on the FNV hash */
	static int sumHash(int c, int h)
	{
	  h *= HASH_PRIME;
	  //h = h & 0xFFFFFFFF
	  h ^= c;
	  //h = h & 0xFFFFFFFF
	  return h;
	}

	private class ss_context {
		FuzzyHash ret;
		  char[] p;
	  long total_chars;
	  int h, h2, h3;
	  int j, k;
	  int block_size;
	  char[] ret2 = new char[SPAMSUM_LENGTH/2 + 1];
	}


	static boolean ssInit(ss_context ctx, File handle)
	{
	  if ( ctx == null ) {
		  return true;
	  }

	  ctx.ret = new FuzzyHash();

	  if (handle != null) {
		  ctx.total_chars = handle.length();
	  }

	  ctx.block_size = MIN_BLOCKSIZE;
	  while (ctx.block_size * SPAMSUM_LENGTH < ctx.total_chars) {
	    ctx.block_size = ctx.block_size * 2;
	  }

	  return false;
	}

	static char[] b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

	static void ssEngine(ss_context ctx, 
			      byte[] buffer, 
			      int bufferSize)
	{
	  if (null == ctx || null == buffer) {
	    return;
	  }

	  for ( int i = 0 ; i < bufferSize ; ++i)
	  {

	    /* 
	       at each character we update the rolling hash and
	       the normal hash. When the rolling hash hits the
	       reset value then we emit the normal hash as a
	       element of the signature and reset both hashes
	    */
		  
	    ctx.h  = rollHash(buffer[i]);// & 0x7FFFFFFF;
	    ctx.h2 = sumHash(buffer[i], ctx.h2);// & 0x7FFFFFFF;
	    ctx.h3 = sumHash(buffer[i], ctx.h3);// & 0x7FFFFFFF;
	    
	    if (((0xFFFFFFFFl & ctx.h) % ctx.block_size) == (ctx.block_size-1)) {
	      /* we have hit a reset point. We now emit a
		 hash which is based on all chacaters in the
		 piece of the message between the last reset
		 point and this one */
	      ctx.p[ctx.j] = b64[(int)((ctx.h2&0xFFFF) % 64)];
	      
	      if (ctx.j < SPAMSUM_LENGTH-1) {
		/* we can have a problem with the tail
		   overflowing. The easiest way to
		   cope with this is to only reset the
		   second hash if we have room for
		   more characters in our
		   signature. This has the effect of
		   combining the last few pieces of
		   the message into a single piece */

		ctx.h2 = HASH_INIT;
		(ctx.j)++;
	      }
	    }
	    
	    /* this produces a second signature with a block size
	       of block_size*2. By producing dual signatures in
	       this way the effect of small changes in the message
	       size near a block size boundary is greatly reduced. */
	    if (((0xFFFFFFFFl & ctx.h) % (ctx.block_size*2)) == ((ctx.block_size*2)-1)) {
	      ctx.ret2[ctx.k] = b64[(int) (ctx.h3&0xFFFF % 64)];
	      if (ctx.k < SPAMSUM_LENGTH/2-1) {
		ctx.h3 = HASH_INIT;
		(ctx.k)++;
	      }
	    }
	  }
	}

	static boolean ssUpdate(ss_context ctx, File handle) throws IOException
	{
	  int bytesRead = 0;
	  byte[] buffer; 

	  if (null == ctx || null == handle) {
	    return true;
	  }

	  buffer = new byte[BUFFER_SIZE];

	  ctx.ret.blocksize = ctx.block_size;
	  // ctx.p = ctx.ret + strlen(ctx.ret)  
	  ctx.p = new char[SPAMSUM_LENGTH];
	  
	  //memset(ctx.p, 0, SPAMSUM_LENGTH+1)
	  Arrays.fill(ctx.p, (char)0 );
	  //memset(ctx.ret2, 0, sizeof(ctx.ret2.length))
	  Arrays.fill(ctx.ret2, (char)0 );
	  
	  ctx.k  = ctx.j  = 0;
	  ctx.h3 = ctx.h2 = HASH_INIT;
	  ctx.h  = 0;
	  rollReset();

	  FileInputStream in = new FileInputStream(handle);
	  // while ((bytes_read = fread(buffer,sizeof(byte),BUFFER_SIZE,handle)) > 0)
	  while (in.available() > 0 )
	  {
		  bytesRead = in.read(buffer);
	      ssEngine(ctx,buffer,bytesRead);
	  }
	  in.close();

	  if (ctx.h != 0) 
	  {
	    ctx.p[ctx.j] = b64[(int) ((ctx.h2 & 0xFFFF) % 64)];
	    ctx.ret2[ctx.k] = b64[(int) ((ctx.h3 &0xFFFF) % 64)];
	  }
	  
	  ctx.ret.hash = new String(ctx.p);
	  ctx.ret.hash2 = new String(ctx.ret2);

	  return false;
	}

	/**
	 * 
	 * @param handle
	 * @return
	 * @throws IOException
	 */
	public FuzzyHash fuzzyHashFile(File handle) throws IOException
	{
	  ss_context ctx;  
	  boolean done = false;
	  
	  if (null == handle) {
	    return null;
	  }
	  
	  ctx = new ss_context();

	  ssInit(ctx, handle);
	  
	  ctx.ret.filename = handle.getPath();

	  while (!done)
	  {
	    ssUpdate(ctx,handle);

	    // our blocksize guess may have been way off - repeat if necessary
	    if (ctx.block_size > MIN_BLOCKSIZE && ctx.j < SPAMSUM_LENGTH/2) {
	      ctx.block_size = ctx.block_size / 2;
	    } else {
	      done = true;
	    }
	  }

	  return ctx.ret;
	}

	/**
	 * 
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public FuzzyHash fuzzyHashFilename(String filename) throws IOException
	{
		
	  if (null == filename) {
	    return null;
	  }

	  File handle = new File(filename);//,"rb");
	  if (!handle.exists()) {
	    return null;
	  }

	  return fuzzyHashFile(handle);
	}


	public FuzzyHash fuzzyHashBuf(byte[] buf, int bufLen)
	{
	  ss_context ctx = new ss_context();
	  boolean done = false;

	  if (buf == null) {
	    return null;
	  }

	  ctx.total_chars = bufLen;
	  ssInit(ctx, null);

	  while (!done)
	  {
		  ctx.p = new char[SPAMSUM_LENGTH+1]; // TODO Duplication!
	    
	    ctx.k  = ctx.j  = 0;
	    ctx.h3 = ctx.h2 = HASH_INIT;
	    ctx.h  = 0;
	    rollReset();

	    ssEngine(ctx,buf,bufLen);

	    /* our blocksize guess may have been way off - repeat if necessary */
	    if (ctx.block_size > MIN_BLOCKSIZE && ctx.j < SPAMSUM_LENGTH/2) {
	      ctx.block_size = ctx.block_size / 2;
	    } else {
	      done = true;
	    }

	    if (ctx.h != 0) {
			ctx.p[ctx.j] = b64[(int) ((ctx.h2&0xFFFF) % 64)];
			ctx.ret2[ctx.k] = b64[(int) ((ctx.h3&0xFFFF) % 64)];
	    }
	    
	  }


	  ctx.ret = new FuzzyHash(ctx.block_size, String.valueOf(ctx.p), String.valueOf(ctx.ret2));

	  return ctx.ret;
	}

	/**
	 * 
	 * @param buf
	 * @return
	 */
	public FuzzyHash fuzzyHashBuf(byte[] buf) {
		return this.fuzzyHashBuf(buf, buf.length);
	}

	/* 
	   we only accept a match if we have at least one common substring in
	   the signature of length ROLLING_WINDOW. This dramatically drops the
	   false positive rate for low score thresholds while having
	   negligable affect on the rate of spam detection.

	   return 1 if the two strings do have a common substring, 0 otherwise
	*/
	static int hasCommonSubstring(char[] s1, char[] s2)
	{
	  int i;
	  long[] hashes = new long[SPAMSUM_LENGTH];
	  
	  /* there are many possible algorithms for common substring
	     detection. In this case I am re-using the rolling hash code
	     to act as a filter for possible substring matches */
	  
	  rollReset();
	  
	  /* first compute the windowed rolling hash at each offset in
	     the first string */
	  for (i=0;i < s1.length;i++) 
	  {
	    hashes[i] = rollHash((char)s1[i]);
	  }
	  
	  rollReset();
	  
	  /* now for each offset in the second string compute the
	     rolling hash and compare it to all of the rolling hashes
	     for the first string. If one matches then we have a
	     candidate substring match. We then confirm that match with
	     a direct string comparison */
	  /*for (i=0;i < s2.length;i++) {
	    long h = roll_hash((char)s2[i]);
	    if (i < ROLLING_WINDOW-1) continue;
	    for (j=ROLLING_WINDOW-1;j<num_hashes;j++) 
	    {
	      if (hashes[j] != 0 && hashes[j] == h) 
	      {
		// we have a potential match - confirm it
	   	//FIXME
		if (strlen(s2+i-(ROLLING_WINDOW-1)) >= ROLLING_WINDOW && 
		    strncmp(s2+i-(ROLLING_WINDOW-1), 
			    s1+j-(ROLLING_WINDOW-1), 
			    ROLLING_WINDOW) == 0) 
		{
		  return 1;
		}
		
	      }
	    }
	  }*/
	  
	  return 0;
	}


	// eliminate sequences of longer than 3 identical characters. These
	// sequences contain very little information so they tend to just bias
	// the result unfairly
	static char[] eliminateSequences(String string)
	{
		char[] str = string.toCharArray();
	  StringBuffer ret = new StringBuffer();
	  
	  // Do not include repeats:
	  for (int i=3;i<str.length;i++) {
	    if (str[i] != str[i-1] ||
		    str[i] != str[i-2] ||
		    str[i] != str[i-3]) {
	      ret.append(str[i]);
	    }
	  }
	  
	  return ret.toString().toCharArray();
	}

	/*
	  this is the low level string scoring algorithm. It takes two strings
	  and scores them on a scale of 0-100 where 0 is a terrible match and
	  100 is a great match. The block_size is used to cope with very small
	  messages.
	*/
	static int scoreStrings(char[] s1, char[] s2, int blockSize)
	{
	  int score = 0;
	  int len1, len2;
	  
	  len1 = s1.length;
	  len2 = s2.length;
	  
	  if (len1 > SPAMSUM_LENGTH || len2 > SPAMSUM_LENGTH) {
	    /* not a real spamsum signature? */
	    return 0;
	  }
	  
	  /* the two strings must have a common substring of length
	     ROLLING_WINDOW to be candidates */
	  /*
	  if (has_common_substring(s1, s2) == 0) {
	    return 0;
	  }
	  */
	  
	  /* compute the edit distance between the two strings. The edit distance gives
	     us a pretty good idea of how closely related the two strings are */
	  score = StringUtils.getLevenshteinDistance(new String(s1), new String(s2));
	 
	  /* scale the edit distance by the lengths of the two
	     strings. This changes the score to be a measure of the
	     proportion of the message that has changed rather than an
	     absolute quantity. It also copes with the variability of
	     the string lengths. */
	  score = (score * SPAMSUM_LENGTH) / (len1 + len2);
	  
	  /* at this stage the score occurs roughly on a 0-64 scale,
	   * with 0 being a good match and 64 being a complete
	   * mismatch */
	  
	  /* rescale to a 0-100 scale (friendlier to humans) */
	  score = (100 * score) / 64;
	  
	  /* it is possible to get a score above 100 here, but it is a
	     really terrible match */
	  if (score >= 100) {
		  return 0;
	  }
	  
	  /* now re-scale on a 0-100 scale with 0 being a poor match and
	     100 being a excellent match. */
	  score = 100 - score;

	  //  printf ("len1: %"PRIu32"  len2: %"PRIu32"\n", len1, len2);
	  
	  /* when the blocksize is small we don't want to exaggerate the match size */
	  if (score > blockSize/MIN_BLOCKSIZE * Math.min(len1, len2)) {
	    score = blockSize/MIN_BLOCKSIZE * Math.min(len1, len2);
	  }
	  return score;
	}

	/*
	  given two spamsum strings return a value indicating the degree to which they match.
	*/
    static int fuzzyCompare(FuzzyHash fh1, FuzzyHash fh2)
	{
	  int score = 0;
	  char[] s11, s12;
	  char[] s21, s22;
	  
	  // if the blocksizes don't match then we are comparing
	  // apples to oranges. This isn't an 'error' per se. We could
	  // have two valid signatures, but they can't be compared. 
	  if (fh1.blocksize != fh2.blocksize && 
	      fh1.blocksize != fh2.blocksize*2 &&
	      fh2.blocksize != fh1.blocksize*2) {
	    return 0;
	  }
	  
	  // there is very little information content is sequences of
	  // the same character like 'LLLLL'. Eliminate any sequences
	  // longer than 3. This is especially important when combined
	  // with the has_common_substring() test below. 
	  s11 = eliminateSequences(fh1.hash);
	  s21 = eliminateSequences(fh2.hash);
	  
	  s12 = eliminateSequences(fh1.hash2);
	  s22 = eliminateSequences(fh2.hash2);
	  
	  // each signature has a string for two block sizes. We now
	  // choose how to combine the two block sizes. We checked above
	  // that they have at least one block size in common 
	  if (fh1.blocksize == fh2.blocksize) {
	    int score1, score2;
	    score1 = scoreStrings(s11, s21, fh1.blocksize);
	    score2 = scoreStrings(s12, s22, fh2.blocksize);

	    score = Math.min(score1, score2);
	  } else if (fh1.blocksize == fh2.blocksize*2) {

	    score = scoreStrings(s11, s22, fh1.blocksize);
	  } else {

	    score = scoreStrings(s12, s21, fh2.blocksize);
	  }
	  
	  return (int)score;
	}

}
