#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
export LD_LIBRARY_PATH=$DIR/openjpeg-2.0.0-Linux-i386/lib
$DIR/openjpeg-2.0.0-Linux-i386/bin/opj_decompress -i $1 -o $2.png
mv $2.png $2
