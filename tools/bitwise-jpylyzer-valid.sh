#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
$DIR/openplanets-jpylyzer-4abf899/jpylyzer.py $1 | xmllint --format - | grep isValidJP2 > $2
