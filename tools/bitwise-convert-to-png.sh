#!/bin/bash
convert $1 -define png:exclude-chunk=date $2.png
mv $2.png $2
