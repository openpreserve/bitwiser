<?php
exec('../python/bitwiser/LittleFlipper.py broken-graphic-large.jpg out.png');
header('Content-Type: image/png');
readfile('out.png');
exit;
?>
