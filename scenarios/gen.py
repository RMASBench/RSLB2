#!/usr/bin/env python

import sys
import random

maps = {
    'paris': [
            52232, 44376, 48293, 64614, 19357, 32134, 45104, 52352,
            35932, 51370, 27415, 63803, 52107, 44900, 50962, 20820,
            53053, 24887, 61658, 49476, 36306, 53189, 29928, 28444,
            47800, 64915, 24660, 23963, 63861, 41726, 63873, 39083,
            64546, 38547, 64256, 55238, 47041
    ],
}

def main(args):
    mapname, fires, agents = args[0], int(args[1]), int(args[2])
    points = maps[mapname]

    print '<?xml version="1.0" encoding="UTF-8"?>'
    print ''
    print '<scenario:scenario xmlns:scenario="urn:roborescue:map:scenario">'
    print ''
    print '\t<!-- The fire station (do *not* remove this) -->'
    print '\t<scenario:firestation scenario:location="%d"/>' % random.choice(points)
    print ''

    print '\t<!-- The initial fires -->'
    for fire in random.sample(points, fires):
        print '\t<scenario:fire scenario:location="%d"/>' % fire
    print ''

    print '\t<!-- The fire fighters -->'
    for agent in random.sample(points, agents):
        print '\t<scenario:firebrigade scenario:location="%d"/>' % agent
    print ''

    print '</scenario:scenario>'

if __name__ == '__main__':
    main(sys.argv[1:])
