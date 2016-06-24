import os
import matplotlib.mlab as mlab
import matplotlib.pyplot as plt
import numpy as np


# parsing all log files
timeL = []
for fn in os.listdir("."):
    if fn.endswith(".log"):
        fileId = fn[0: int(fn.index("."))]
    f = open(fn, 'r')
    content = f.readlines()
    found = False
    for l in content:
        if (l.startswith("[[Synthesizer finished]] time: ")):
            time = l[len("[[Synthesizer finished]] time: "): -1].strip()
            found = True
            x = time.split(":")
            seconds = (float(x[0])) * 3600 + (float(x[1])) * 60 + (float(x[2])) + (float(x[3])) * 0.001
            timeL.append(float(seconds))
    if (not found):
        timeL.append(600.)

print timeL

fig, ax = plt.subplots()

index = np.arange(len(timeL))
bar_width = 0.4

opacity = 0.4
error_config = {
    'ecolor': '0.3'
}

rects1 = plt.bar(index, timeL, bar_width,
    alpha = opacity,
    color = 'b',
    error_kw = error_config,
    label = 'Men')
plt.ylabel('Synthesis Time / seconds')
plt.show()