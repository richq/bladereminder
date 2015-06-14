#!/usr/bin/env python

import csv
import sys

a = csv.reader(file(sys.argv[1]))
idx = 1
for line in a:
    date, day, cnt, comment = line
    if not date.startswith('2'):
        continue
    if cnt == '-' or not cnt:
        continue
    print 'INSERT INTO "shaves" VALUES(%s, "%s", %s, "%s");' % (idx, date, cnt, comment)
    idx = idx + 1
