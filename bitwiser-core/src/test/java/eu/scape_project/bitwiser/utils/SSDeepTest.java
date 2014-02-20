package eu.scape_project.bitwiser.utils;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SSDeepTest {
	
	byte[] b2 = "Hello World how are you today...\n".getBytes();
	byte[] b3 = "Hello World how are you today? Well, I trust?".getBytes();

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testFuzzy_hash_file() throws IOException {
		SSDeep ssd = new SSDeep();
		FuzzyHash h = ssd.fuzzyHashFile(new File("src/test/resources/ssdeep/lorem.txt"));
		String expected = "48:98fXPWJi6+VvUjy6xO1/nPK2bW4zw4zBW5t9TeP4maXLPgtAGNbHV7dF2X4/IhBp:98fXPieUOkW/PjzBA4wR6DNbH1dF2I/Y,\"src/test/resources/ssdeep/lorem.txt\"";
		String result = h.toString();
		assertEquals("File-based ssdeep failed!", expected, result );
		FuzzyHash h2 = ssd.fuzzyHashFile(new File("src/test/resources/ssdeep/lorem-cut.txt"));
		System.out.println("Similarity: "+ssd.fuzzyCompare(h,h2)+"%" );
	}

	@Test
	public void testFuzzy_hash_buf() {
		SSDeep ssd = new SSDeep();
		FuzzyHash h = ssd.fuzzyHashBuf(b2);
		String expected = "3:aAVFUrPgbn:aAvgIn";
		String result = h.toString();
		assertEquals("Buffer-based ssdeep failed!", expected, result );
		// Now, again, with shorter string:
		FuzzyHash h2 = ssd.fuzzyHashBuf(b3, b3.length);
		System.out.println("Similarity: "+ssd.fuzzyCompare(h,h2)+"%" );
	}

}
