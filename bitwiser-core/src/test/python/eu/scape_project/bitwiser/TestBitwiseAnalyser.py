"""
Created on Feb 15, 2012
"""
__author__ = 'Peter May (Peter.May@bl.uk)'
__license__ = 'Apache Software License, Version 2.0'
__version__ = '0.0.1'

import unittest
import os.path
import eu.scape_project.bitwiser.BitwiseAnalyser as BitwiseAnalyser

#===============================================================================
# Test Commands
#===============================================================================
CMD_CONVERT = "C:/Program Files/ImageMagick-6.7.3-Q16/convert"

RESOURCES_DIR  = "../../../../resources/"

TEMP_DIR    = "C:/Projects/SCAPE/BitWiser/Data/"
TMP_FILE    = os.path.join(TEMP_DIR, "tmp.chrome_32x32_lzw.tif")
CONV_FILE   = os.path.join(TEMP_DIR, "conv.jp2")

#===============================================================================
# Test Files
#===============================================================================
BYTE_FLIP_TEST  = os.path.join(RESOURCES_DIR, "byteflip.txt")
TEST1_FILE      = os.path.join(RESOURCES_DIR, "chrome_32x32_lzw.tif")
EMPTY_TIF       = os.path.join(RESOURCES_DIR, "empty.tif")

#===============================================================================
# Tests Tuples: (File, successful, erroneous)
#===============================================================================
TEST1 = (EMPTY_TIF, 0, 0)
TEST2 = (TEST1_FILE, 749, 1103)

class TestBitwiserAnalyser(unittest.TestCase):
    
    def testEmptyAnalyse(self):
        self.__runAnalyse(TEST1)
    
    def testAnalyse(self):
        self.__runAnalyse(TEST2)
    
    #------------------------------------------------------------------------------ 
        
    def assertFileExists(self, fpt):
        self.assertTrue(os.path.exists(fpt), "File does not exist: "+fpt)
        
    def __runAnalyse(self, test):
        (success, errors) = BitwiseAnalyser.analyse(test[0])
        
        #self.assertFileExists(TMP_FILE)
        #self.assertTrue(filecmp.cmp(test[0], TMP_FILE, shallow=False), "Copied file not equal")
        #self.assertFileExists(CONV_FILE)
        self.assertEqual(success, test[1])
        self.assertEqual(errors, test[2])
        
    

if __name__ == "__main__":
    #import sys;sys.argv = ['', 'Test.testName']
    unittest.main()