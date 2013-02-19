#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
python $DIR/jp2StructCheck-master/jp2StructCheck.py $1 > $2
