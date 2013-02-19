#!/bin/bash
diff <(hexdump -C $1 ) <(hexdump -C $2 )
