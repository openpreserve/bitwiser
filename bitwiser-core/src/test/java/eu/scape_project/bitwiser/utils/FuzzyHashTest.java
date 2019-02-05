/**
 * 
 */
package eu.scape_project.bitwiser.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Andrew Jackson <Andrew.Jackson@bl.uk>
 *
 */
public class FuzzyHashTest {

    @Test
    public void testComparison() {
        FuzzyHash fh1 = FuzzyHash.fromString(
                "96:Axd6wa9IrXjodTkt8yJksfGwr9+grnw8LoCQBkhrmsl7OnsCYBaAcIjz:c6wvodwt5AkHLoDOhrUnsbBaEjz,\"README.md\"");
        FuzzyHash fh2 = FuzzyHash.fromString(
                "96:qxd6wa9IrXjodTkt8yJksfGwr9+grnw8LoCQBkhrmsl7OnsCYBaI:q6wvodwt5AkHLoDOhrUnsbBaI,\"README-2.md\"");
        System.out.println("FH1: " + fh1.toString());
        System.out.println("FH2: " + fh2.toString());
        int comparison = FuzzyHash.compare(fh1, fh2);
        System.out.println("Comparison = " + comparison);
        assertEquals("Wrong comparisong result!", 94, comparison);
    }

    @Test
    public void testComparisonWithoutNames() {
        FuzzyHash fh1 = FuzzyHash.fromString(
                "96:Axd6wa9IrXjodTkt8yJksfGwr9+grnw8LoCQBkhrmsl7OnsCYBaAcIjz:c6wvodwt5AkHLoDOhrUnsbBaEjz");
        FuzzyHash fh2 = FuzzyHash.fromString(
                "96:qxd6wa9IrXjodTkt8yJksfGwr9+grnw8LoCQBkhrmsl7OnsCYBaI:q6wvodwt5AkHLoDOhrUnsbBaI");
        System.out.println("FH1: " + fh1.toString());
        System.out.println("FH2: " + fh2.toString());
        int comparison = FuzzyHash.compare(fh1, fh2);
        System.out.println("Comparison = " + comparison);
        assertEquals("Wrong comparisong result!", 94, comparison);
    }
}
