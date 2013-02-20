#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
$DIR/jhove-1.9/jhove $1 | grep -v "^ Date: " > $2
