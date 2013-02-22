#!/usr/bin/env python
import os.path, math, string, sys
import Image, ImageDraw


def draw_bitmap(data, name):
    length = len(data)
    side = int(length**0.5)
    c = Image.new("RGB", (side,side))
    cd = ImageDraw.Draw(c)
    cd.point((0, 0), fill="red")
    for i in xrange(side):
        for j in xrange(side):
            val = ord(data[j+side*i]);
            cd.point((j,i), fill=(val,val,val))
    c.resize( (side*2,side*2) )
    c.save(name)

def main():
    from optparse import OptionParser, OptionGroup
    parser = OptionParser(
                usage = "%prog [options] infile [output]",
                version="%prog 0.1",
            )
    parser.add_option(
        "-s", "--size", action="store",
        type="int", dest="size", default=256,
        help="Image width in pixels."
    )
    
    options, args = parser.parse_args()
    if len(args) not in [1, 2]:
        parser.error("Please specify input and output file.")

    d = file(args[0]).read()
    if len(args) == 2:
        dst = args[1]
    else:
        base = os.path.basename(args[0])
        if "." in base:
            base, _ = base.rsplit(".", 1)
        dst = base + options.suffix + ".png"

    if os.path.exists(dst) and len(args) < 2:
        print >> sys.stderr, "Refusing to over-write '%s'. Specify explicitly if you really want to do this."%dst
        sys.exit(1)
    
    draw_bitmap(d, dst)
      
        
main()