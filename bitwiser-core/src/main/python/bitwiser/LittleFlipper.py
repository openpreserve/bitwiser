import BitwiseAnalyser
import argparse
import os
import shutil
import random


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Run the LittleFlipper')
    parser.add_argument('file', help='input file')
    parser.add_argument('out', help='output file')
    args = parser.parse_args()

    testfile = args.file
    outfile = args.out
    tmp_file = "tmp-flipped-"+os.path.basename(testfile)

    # create a temporary file for bit manipulation
    shutil.copyfile(testfile, tmp_file)

    # Length in bytes
    filelen = os.path.getsize(tmp_file)

    # Pick a random bit:
    i = int(filelen*8*random.random())

    # Flip it:
    BitwiseAnalyser.BitManipulator.flipBitAt(tmp_file, i)

    # Interpret:
    #os.system("convert -size 512x512 "+tmp_file+" "+outfile);
    os.system("convert "+tmp_file+" "+outfile);