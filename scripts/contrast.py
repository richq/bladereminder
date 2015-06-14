#!/usr/bin/env python
from __future__ import print_function
import sys
"""
for file in res/drawable/round_*
do
    echo $file
    python contrast.py $( grep $(awk -F'"' '/<solid/ {print $2}' $fil e |
    cut -d/ -f2) -w res/values/colors.xml |
    awk -F'"' '{print $3}' | sed 's/>#\(.*\)<.*/\1/g')
done
"""

def main(hexcolor):
    r = int(hexcolor[0:2], 16)
    g = int(hexcolor[2:4], 16)
    b = int(hexcolor[4:6], 16)
    yiq = ((r*299)+(g*587)+(b*114))/1000;
    if yiq >= 128:
        print(hexcolor, 'black')
    else:
        print(hexcolor, 'white')

if __name__ == '__main__':
    for c in sys.argv[1:]:
        main(c)
