#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
j2k_to_image -i $1 -o $2.bmp
mv $2.bmp $2
