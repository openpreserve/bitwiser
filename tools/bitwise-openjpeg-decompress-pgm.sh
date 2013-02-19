#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
j2k_to_image -i $1 -o $2.pgm
mv $2.pgm $2
