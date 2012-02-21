"""
Brute force bitwise analysis of transformations and analysers.

Created on Feb 15, 2012
"""

__author__ = 'Peter May (Peter.May@bl.uk)'
__license__ = 'Apache Software License, Version 2.0'
__version__ = '0.0.1'


from io import FileIO
import argparse
import os
import shutil
import struct
import subprocess

CMD_CONVERT = "C:/Program Files/ImageMagick-6.7.3-Q16/convert"

TEMP_DIR = "C:/Projects/SCAPE/BitWiser/Data/"
CONV_FILE   = os.path.join(TEMP_DIR, "conv.jp2")

class Output:
    def __init__(self, exitcode, stdout, stderr):
        self.exitcode = exitcode
        self.stdout   = stdout
        self.stderr   = stderr
        
class BitManipulator(object):
    
    @staticmethod
    def flipAt(inputfile, position, byteflipping=False):
        if byteflipping:
            BitManipulator.flipByteAt(inputfile, position)
        else:
            BitManipulator.flipBitAt(inputfile, position)
            
    
    @staticmethod
    def flipByteAt(inputfile, position):
        """Flips the bits for the byte at the specified position in the input file."""
        f = FileIO(inputfile, "r+")
        f.seek(position)
        byte = ord(f.read(1))
        f.seek(-1, 1)   # go back 1 byte from current position
        f.write(struct.pack("B", byte^0xFF))    # read in the byte and XOR it
        f.close()
        
    @staticmethod
    def flipBitAt(inputfile, position):
        """Flips the bit at the specified position in the input file."""
        if not 0<=position<(8*os.path.getsize(inputfile)):
            raise IndexError("Position "+str(position)+" is out of range")
        
        f = FileIO(inputfile, "r+")
        f.seek(position/8)
        byte = ord(f.read(1))
        f.seek(-1, 1)   # go back 1 byte from the current position
        bitnum = position%8
        f.write(struct.pack("B", byte^(1<<(7-bitnum))))
        f.close()
        
    @staticmethod
    def bits(file):
        """Generator of bits in the specified file"""
        bytes = (ord(b) for b in file.read())
        for b in bytes:
            for i in xrange(8):
                yield (b>>i)&1
                
    @staticmethod
    def getBitFromByteAt(byte, position):
        """Returns the bit at the specified position"""
        if not 0<=position<8:
            raise IndexError("Position "+str(position)+" is out of range")
        return (byte>>(7-position))&1
    

def analyse(testfile, byteflipping=False):
    """Run the convert command on the specified input test file.
    
       If True, byteflipping indicates that whole bytes should be flipped,
       rather than the default individual bits.
       
    """
    
    # create a temporary file for bit manipulation
    tmp_file = os.path.join(TEMP_DIR, "tmp."+os.path.basename(testfile))
    shutil.copyfile(testfile, tmp_file)
    
    # run command on original to get desired output for comparison
    expected = __runCommand(CMD_CONVERT, tmp_file, CONV_FILE)
    
    # stats
    clear = 0
    error = 0
    
    # open temporary file and flip bits/bytes
    filelen = os.path.getsize(tmp_file) if byteflipping else 8*os.path.getsize(tmp_file)
    for i in xrange(filelen):
        print "Completed (%d/%d): %d%%"%(i,filelen,(100*i/filelen))
        BitManipulator.flipAt(tmp_file, i, byteflipping)    # flip bit/byte
        output = __runCommand(CMD_CONVERT, tmp_file, CONV_FILE)
        BitManipulator.flipAt(tmp_file, i, byteflipping)    # return to normality
        
        if output.exitcode==0:
            clear+=1
        else:
            error+=1

    # clear up
    os.remove(tmp_file)
    os.remove(CONV_FILE)
    # and return
    return (clear, error)

def __runCommand(command, inputfile, outputfile):
    """Runs the specified command on the specified input file.
    
       returns: Output object
       
    """
    
    process = subprocess.Popen([command, inputfile, outputfile], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    exitcode = process.wait()
    output = process.communicate()
    return Output(exitcode, output[0], output[1])



if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Run the Bitwise Analyser over the specified command')
    parser.add_argument('file', help='example input file to test with')
    parser.add_argument('--bytes', action='store_true', help='use byte-level flipping, rather than bit flipping')
    
    args = parser.parse_args()
    results = analyse(args.file, args.bytes)
    print "Results compared to original file execution:"
    print " #Byte mods causing same output as original:",results[0]
    print " #Byte mods causing different outputs:      ",results[1]