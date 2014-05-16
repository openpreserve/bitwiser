<?php
$parent_dir = dirname($_SERVER['SCRIPT_NAME']);
exec('python '.$parent_dir.'/../python/bitwiser/LittleFlipper.py '.$parent_dir.'/broken-graphic-large.jpg '.$parent_dir.'/out.png');
header('Content-Type: image/png');
readfile($parent_dir.'/out.png');
exit;
?>
