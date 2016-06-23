# Import the os module, for the os.walk function
import os
import matplotlib.mlab as mlab
import matplotlib.pyplot as plt
import numpy as np

 
# parsing all log files
time = []
for fn in os.listdir("."):
    if fn.endswith(".log"):
        fileId = fn[0:int(fn.index("."))]
        f = open(fn, 'r')
        content = f.readlines()
        for l in content:
            if (l.startswith("[[Synthesizer finished]] seconds: ")):
                seconds = l[len("[[Synthesizer finished]] seconds: "):-1].strip()
                if (float(seconds) < 10):
	                time.append(float(seconds)) 

print time

n_groups = 5

means_men = (20, 35, 30, 35, 27)

means_women = (25, 32, 34, 20, 25)
std_women = (3, 5, 2, 3, 3)

fig, ax = plt.subplots()

index = np.arange(len(time))
bar_width = 0.4

opacity = 0.4
error_config = {'ecolor': '0.3'}

rects1 = plt.bar(index, time, bar_width,
                 alpha=opacity,
                 color='b',
                 error_kw=error_config,
                 label='Men')
plt.ylabel('Synthesis Time / seconds')
plt.show()
