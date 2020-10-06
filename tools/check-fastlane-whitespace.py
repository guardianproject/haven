#!/usr/bin/env python3

import glob
import os

os.chdir(os.path.join(os.path.dirname(__file__), '../fastlane/android'))
for f in glob.glob('metadata/*/*.txt') +  glob.glob('metadata/*/*/*.txt'):
    if os.path.getsize(f) == 0:
        os.remove(f)
        continue

    with open(f) as fp:
        data = fp.read()
    with open(f, 'w') as fp:
        fp.write(data.rstrip())
        fp.write('\n')
