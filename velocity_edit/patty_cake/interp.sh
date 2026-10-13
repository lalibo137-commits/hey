#!/bin/sh
# usage: interp.sh VIDEO INDEX START DUR
ffmpeg -v error -y -ss "$3" -t "$4" -i "$1" -an -vf "scale=1280:960:flags=lanczos,minterpolate=fps=100:mi_mode=mci:mc_mode=aobmc:me_mode=bidir:vsbmc=1:scd=none" -c:v libx264 -preset veryfast -crf 12 "clip_$2.mp4"
