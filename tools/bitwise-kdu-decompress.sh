#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
export LD_LIBRARY_PATH=$DIR/KDU/Linux-x86-32
$DIR/KDU/Linux-x86-32/kdu_expand -i $1 -o $2.tif
mv $2.tif $2
