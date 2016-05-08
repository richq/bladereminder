#!/usr/bin/env python

import csv
import sys

def main():
    a = csv.reader(open(sys.argv[1]))
    idx = 1
    razors = ['']
    for line in a:
        date, day, cnt, comment, razor = line
        if not date.startswith('2'):
            continue
        if cnt == '-' or not cnt:
            continue
        razorid = 1
        if razor in razors:
            razorid = razors.index(razor)
        print('INSERT INTO "shaves" VALUES(%s, "%s", %s, "%s", "%s");' % (idx, date, cnt, comment, razorid))
        idx = idx + 1

if __name__ == '__main__':
    main()
