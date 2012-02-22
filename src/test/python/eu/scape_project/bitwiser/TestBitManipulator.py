"""
Created on Feb 20, 2012
"""
__author__ = 'Peter May (Peter.May@bl.uk)'
__license__ = 'Apache Software License, Version 2.0'
__version__ = '0.0.1'

from io import FileIO
import os
import shutil
import unittest

import eu.scape_project.bitwiser.BitwiseAnalyser as BitwiseAnalyser

#===============================================================================
# Directories
#===============================================================================
RESOURCES_DIR   = "../../../../resources/"
TEMP_DIR        = "C:/Projects/SCAPE/BitWiser/Data/"

#===============================================================================
# Test Files
#===============================================================================
BYTE_FLIP_TEST  = os.path.join(RESOURCES_DIR, "byteflip.txt")   # 6 bytes

#===============================================================================
# Test Data
#===============================================================================
TEST_BYTE1 = 0x13

class TestBitManipulator(unittest.TestCase):

    def setUp(self):
        pass

    def tearDown(self):
        pass
    
#------------------------------------------------------------------------------ 

    
    def testBitRetrieval(self):
        expected = self.__bitList(TEST_BYTE1)
        for x in xrange(len(expected)):
            self.assertEqual(expected[x], BitwiseAnalyser.BitManipulator.getBitFromByteAt(TEST_BYTE1, x),
                             "Bits different at position: "+str(x))
            
    def testBitPositionTooLarge(self):
        self.assertRaises(IndexError, BitwiseAnalyser.BitManipulator.getBitFromByteAt, TEST_BYTE1, 9)
        
    def testBitPositionTooSmall(self):
        self.assertRaises(IndexError, BitwiseAnalyser.BitManipulator.getBitFromByteAt, TEST_BYTE1, -1)
            
    def testFlipBitsInSequence(self):
        """Test flipping of each bit in the test file."""
        for i in xrange(8):
            self.__flipBitAtIndex(BYTE_FLIP_TEST, i)
            
    def testFlipBitsPositionTooLarge(self):
        self.__flipBitAtIndex(BYTE_FLIP_TEST, 49)
        
    def testFlipBitsPositionTooSmall(self):
        self.__flipBitAtIndex(BYTE_FLIP_TEST, -1)
            
    def testFlipBytesInSequence(self):
        """Test flipping of each byte in sequence."""
        for i in xrange(6):
            self.__flipBytesAtIndex(BYTE_FLIP_TEST, i)

#------------------------------------------------------------------------------ 

    def __flipBitAtIndex(self, file, position):
        """Asserts the flip of the bit at the specified bit position in the file."""
        tmp = self.__copyFile(file)
        if 0<=position<(os.path.getsize(file)*8):
            # if a bit position within the file limits, assert the relevant bit has been flipped
            BitwiseAnalyser.BitManipulator.flipBitAt(tmp, position)
            self.assertFlippedBit(file, tmp, position)
        else:
            # if a bit position out of file size, assert that an IndexError is raised
            self.assertRaises(IndexError, BitwiseAnalyser.BitManipulator.flipBitAt, tmp, position)
        os.remove(tmp)      # clean up
    
    def __flipBytesAtIndex(self, file, position):
        """Asserts the XOR flip of the byte at the specified position in the file."""
        tmp = self.__copyFile(file)
        BitwiseAnalyser.BitManipulator.flipByteAt(tmp, position)
        self.assertFlippedByte(file, tmp, position)
        os.remove(tmp)      # clean up
        
    def __copyFile(self, file):
        self.assertFileExists(file)
        tmp = os.path.join(TEMP_DIR, os.path.basename(file)+".tmp")
        shutil.copyfile(file, tmp)
        self.assertFileExists(tmp)
        # Check that files are equal size
        self.assertEqual(os.path.getsize(file), os.path.getsize(tmp), "Files of different sizes")
        return tmp
    
    def __bitList(self, byte):
        return [int(c) for c in bin(byte)[2:].zfill(8)]

#------------------------------------------------------------------------------ 

    def assertFileExists(self, fpt):
        self.assertTrue(os.path.exists(fpt), "File does not exist: "+fpt)
    
    def assertFlippedBit(self, file_orig, file_modded, position):
        len_orig   = os.path.getsize(file_orig)
        len_modded = os.path.getsize(file_modded)
        self.assertEqual(len_orig, len_modded, "Files of different sizes")
        
        f_o = FileIO(file_orig, "r+b")
        f_m = FileIO(file_modded, "r+b")
        
        for i in xrange(len_orig):
            # read in a byte from each file and compare
            b_o = ord(f_o.read(1))
            b_m = ord(f_m.read(1))
            if i==(position/8):
                for m in xrange(8):
                    bit_m = BitwiseAnalyser.BitManipulator.getBitFromByteAt(b_m, m)
                    bit_o = BitwiseAnalyser.BitManipulator.getBitFromByteAt(b_o, m)
                    if m==(position%8):
                        self.assertNotEqual(bit_m, bit_o, "Bits are equal when the should be different at position: "+str(position))
                    else:
                        self.assertEqual(bit_m, bit_o, "Bits are incorrectly different at position "+str(i))
            else:
                self.assertEqual(b_o, b_m, "Bytes differ (when the shouldn't) at position "+str(i))
        f_o.close()
        f_m.close()
    
    def assertFlippedByte(self, file_orig, file_modded, position):
        len_orig   = os.path.getsize(file_orig)
        len_modded = os.path.getsize(file_modded)
        
        self.assertEqual(len_orig, len_modded, "Files of different sizes")
        
        f_o = FileIO(file_orig, "r+b")
        f_m = FileIO(file_modded, "r+b")
        
        for i in xrange(len_orig):
            # read in a byte from each file and compare
            b_o = ord(f_o.read(1))
            b_m = ord(f_m.read(1))
            if i==position:
                self.assertEqual(b_m, b_o^0xff, "Flipped bytes are actually equal at position "+str(i))
            else:
                self.assertEqual(b_o, b_m, "Bytes differ (when the shouldn't) at position "+str(i))
        f_o.close()
        f_m.close()

if __name__ == "__main__":
    #import sys;sys.argv = ['', 'Test.testName']
    unittest.main()