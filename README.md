Bitwiser
========

This is a small suite of tools used to perform bitwise analysis of data and processes related to digital preservation.

Tools
-----
There are Python and Java tools, both held under ```bitwiser-core/src/main```.


### Python Bitwise Analysis Tools ###

The best and most recent version of the main tools for bitwise analysis are written in Python. The BitwiseAnalyser is designed to run a given command (those in the ```tools``` directory) on every possible bitwise permutation of an input file, and record whether and how the given process responds to the bit-level modification. There is also an additional Python tool to aid in the visualisation of those results.

More information about these tools, and about the kind of results that can be generated using them, see:

 * [Understanding Tools & Formats Via Bitwise Analysis](http://anjackson.github.io/keeping-codes/experiments/Understanding%20Tools%20and%20Formats%20Via%20Bitwise%20Analysis.html)
 * [Improvements to Jpylyzer based on that analysis.](https://github.com/openplanets/jpylyzer/issues/31)


### Java Tools ###

The Java codebase contains an older version of the bitwise analysis tools, but these have been largely superseded by the Python implementation. However, apart from that, the Java codebase contains utilities when performing low-level analysis of bitstreams, in particular:

* An Entropy class, based on the [ent command source code](http://www.fourmilab.ch/random/), that calculates the Shannon entropy of a bitstream.
* An SSDeep class, based on the [ssdeep source code](http://ssdeep.sourceforge.net/), that can calculate the [ssdeep fuzzy hash](http://www.forensicswiki.org/wiki/Ssdeep) of a bitstream.

Ideas
-----
* http://stackoverflow.com/questions/492751/tools-to-help-reverse-engineer-binary-file-formats
* tupni-ccs08.pdf
* Follow up on http://www.openplanetsfoundation.org/comment/428#comment-428

Branches
--------
* master - Main release branch.
* python - Old python development branch, now folded into master.

Support
-------
These tools were initially developed as part of the SCAPE Project: http://www.scape-project.eu/
