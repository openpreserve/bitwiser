"""
Brute-force bitwise analysis of transformations and analysers.

Created on Feb 15, 2012
"""

__author__ = 'Peter May (Peter.May@bl.uk), Andrew Jackson (Andrew.Jackson@bl.uk)'
__license__ = 'Apache Software License, Version 2.0'
__version__ = '0.0.2'

from io import FileIO
import argparse
import datetime
import os
import shutil
import struct
import subprocess
import signal
import sys
import tempfile
import hashlib
import time
from string import Template

CMD_CONVERT = "/home/anj/bitwiser/tools/bitwise-convert-to-png.sh"
CMD_FID = "/home/anj/bitwiser/tools/bitwise-file.sh"
CMD_EXV = "/home/anj/bitwiser/tools/bitwise-exiftool-v.sh"
CMD_JSC = "/home/anj/bitwiser/tools/bitwise-jp2StructCheck.sh"
CMD_JHO = "/home/anj/bitwiser/tools/bitwise-jhove.sh"
CMD_JHV = "/home/anj/bitwiser/tools/bitwise-jhove-valid.sh"
CMD_JHV_14 = "/home/anj/bitwiser/tools/bitwise-jhove-1.4-valid.sh"
CMD_JPL = "/home/anj/bitwiser/tools/bitwise-jpylyzer.sh"
CMD_JPV = "/home/anj/bitwiser/tools/bitwise-jpylyzer-valid.sh"
CMD_OJD = "/home/anj/bitwiser/tools/bitwise-openjpeg-decompress.sh"
CMD_KDD = "/home/anj/bitwiser/tools/bitwise-kdu-decompress.sh"
CMD = CMD_CONVERT

OUTFREQ = 100

#===============================================================================
# s.substitute(who='tim', what='kung pao')
#
# identify -verbose src/test/resources/chrome_32x32_lzw.tif > identify.out
# jpylyzer
# jp2structCheck
# file
# TIKA? VSLOW
# DROID? VVSLOW
#===============================================================================


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
        """Exposes bits from the specified file as a Generator"""
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
    
    @staticmethod
    def setBitOfByteAt(byte, bit, position):
        if not 0<=position<8:
            raise IndexError("Position "+str(position)+" is out of range")
        return byte|(bit<<(7-position))

def md5sum(filename):
    md5 = hashlib.md5()
    with open(filename,'rb') as f: 
        for chunk in iter(lambda: f.read(128*md5.block_size), b''): 
             md5.update(chunk)
    return md5.hexdigest()

def analyse(testfile, outfile, byteflipping=False):
    """Run the convert command on the specified input test file.
    
       If True, byteflipping indicates that whole bytes should be flipped,
       rather than the default individual bits.
       
    """
    
    # Store the absolute path of the test file
    testfile = os.path.abspath(testfile)
    outfile = os.path.abspath(outfile)
    
    # create a temporary folder to run in, and cd into it:
    tmp_dir = tempfile.mkdtemp()
    saved_path = os.getcwd()
    os.chdir(tmp_dir)
    
    # create a temporary file for bit manipulation
    tmp_file = os.path.basename(testfile)
    shutil.copyfile(testfile, tmp_file)
    
    # Reset the file timestamps to avoid the output changing if they are included.
    file_ts = os.path.getmtime(testfile)
    os.utime(tmp_file,(file_ts, file_ts))

    # create the specified output file
    rout_file = file(outfile, 'wb')

    
    # run command on original to get desired output for comparison
    out_file = outfile+"-initial"
    expected = __runCommand(CMD, tmp_file, out_file)
    print "EXPECTED:",expected.exitcode
    print "EXPECTED:",expected.stdout
    print "EXPECTED:",expected.stderr
    expected_md5 = md5sum(out_file)
    print "EXPECTED_MD5:",expected_md5

    # stats
    clear = 0
    error = 0
    out_unchanged = 0
    out_changed = 0
    out_none = 0
    
    # open temporary file and flip bits/bytes
    out_file = "out-test"
    outbyte = 0
    bit = 0
    filelen = os.path.getsize(tmp_file) if byteflipping else 8*os.path.getsize(tmp_file)
    for i in xrange(filelen):
        # Flip bit/byte
        BitManipulator.flipAt(tmp_file, i, byteflipping)
        # Reset the file timestamps to avoid the output changing if they are included.
        os.utime(tmp_file,(file_ts, file_ts))
        # Run the program again:
        output = __runCommand(CMD, tmp_file, out_file)
        
        # Check and clean up:
        if output.exitcode==expected.exitcode:
            clear+=1
        else:
            error+=1
        # Is there a file, and is it the same as before?
        if os.path.exists(out_file):
            md5 = md5sum(out_file)
            if md5 == expected_md5:
                out_unchanged+=1
                outbyte = BitManipulator.setBitOfByteAt(outbyte, 1, bit)
            else:
                out_changed+=1
            if not i%OUTFREQ:
                shutil.copyfile(tmp_file,"%s-in-%d"%(outfile,i))
                shutil.copyfile(out_file,"%s-out-%d"%(outfile,i))
            os.remove(out_file)
        else:
            out_none+=1

        # Write the result bitmap out:
        if bit==7:
            rout_file.write("%c"%outbyte)
            rout_file.flush()
            outbyte = 0
            bit = 0
        else:
            # increment bit counter
            bit += 1

        # Flip the bit(s) back
        BitManipulator.flipAt(tmp_file, i, byteflipping)

        # Report percentage complete:        
        if not i%OUTFREQ:
            print "Completed (%d/%d): %d%%"%(i+1,filelen,(100*(i+1)/filelen))
            print clear, error, out_none, out_unchanged, out_changed

    # close file
    rout_file.close()

    # clear up
    if os.path.exists(tmp_file):
        os.remove(tmp_file)
    shutil.rmtree(tmp_dir)
    
    # chdir back:
    os.chdir(saved_path)
    
    # and return
    return (clear, error, out_none, out_unchanged, out_changed)

def __runCommand(command, inputfile, outputfile):
    """Runs the specified command on the specified input file.
    
       returns: Output object
       
    """
    cmd = [command,inputfile,outputfile]

    timeout = 5 # 5 second timeout
    
    #print sys.platform
    # See http://www.activestate.com/blog/2007/11/supressing-windows-error-report-messagebox-subprocess-and-ctypes
    # and http://stackoverflow.com/questions/5069224/handling-subprocess-crash-in-windows
    subprocess_flags = 0
    if sys.platform.startswith("win"):
        import ctypes
        SEM_NOGPFAULTERRORBOX = 0x0002  # From http://msdn.microsoft.com/en-us/library/ms680621(VS100).aspx
        ctypes.windll.kernel32.SetErrorMode(SEM_NOGPFAULTERRORBOX) #@UndefinedVariable
        subprocess_flags = 0x8000000    # win32con.CREATE_NO_WINDOW

    start = datetime.datetime.now()
    process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, 
                               creationflags=subprocess_flags, 
                               preexec_fn=os.setsid )
    
    while process.poll() is None:
        time.sleep(0.1)
        now = datetime.datetime.now()
        if (now-start).seconds>timeout:
#            process.kill()
            os.killpg(process.pid, signal.SIGTERM)
#            pid = process.pid
#            os.waitpid(pid)
            # Also copy off the input time that caused the hang:
            shutil.copyfile(inputfile,"%s-killed-%d"%(inputfile,process.pid)) 
            return Output(100001,"","")

    exitcode = process.wait()
    output = process.communicate()

    # print output[0], output[1]
        
    return Output(exitcode, output[0], output[1])



if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Run the Bitwise Analyser over the specified command')
    parser.add_argument('file', help='example input file to test with')
    parser.add_argument('out', help='output file to write results to')
    parser.add_argument('--bytes', action='store_true', help='use byte-level flipping, rather than bit flipping')
    
    args = parser.parse_args()
    start_time = time.time()
    results = analyse(args.file, args.out, args.bytes)
    elapsed_time = time.time() - start_time
    print "Results compared to original file execution:"
    print " # mods causing expected exit code:  ",results[0]
    print " # mods causing unexpected exit code:",results[1]
    print " # mods causing no output:           ",results[2]
    print " # mods causing identical output:    ",results[3]
    print " # mods causing changed output:      ",results[4]
    print "Elapsed time: ",elapsed_time
    
