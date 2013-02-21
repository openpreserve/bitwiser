#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
$DIR/jhove-1.4/jhove $1 | grep -v "^ Date: " | head -10 > $2
