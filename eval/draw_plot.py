import sys
import os

import matplotlib.pyplot as plt
import numpy as np

import matplotlib.patches as mpatches

from matplotlib.ticker import EngFormatter
from matplotlib.ticker import FuncFormatter, MaxNLocator
from matplotlib.font_manager import FontProperties


# parse a log file to obtain running time
def parse_log_file(content):
	time = 999999
	for l in content:
		if l.startswith("[[Synthesis Time]] "):
			time = float(l[len("[[Synthesis Time]] "):-2].strip())
	return time

#threashold_list = [0, 1, 2, 5, 10, 20, 30, 60, 120, 240, 600]
#threashold_list = [0, 30, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330, 360]
visable_ticks = [5, 10, 30, 60, 120, 180, 300, 450, 600]
threashold_list = []
for i in range(0, 630, 10):
	threashold_list.append(i)
#threashold_list.append(120)
#threashold_list.append(300)
#threashold_list.append(600)

def count_le(time_set, threashold):
	count = 0
	for t in time_set:
		if t < threashold:
			count += 1
	return count

def collect_time(log_dir):
	files = [os.path.join(log_dir, f) for f in os.listdir(log_dir) if os.path.isfile(os.path.join(log_dir, f))]
	
	benchmark_time = []

	for fname in files:
		with open(fname) as f:
			#if not "sql" in fname:
			#	continue
			synthesis_time = parse_log_file(f.readlines())
			print fname, synthesis_time
			benchmark_time.append(synthesis_time)
			#print fname, ":", synthesis_time 

	#for threashold in threashold_list:
	#	print threashold, ":", count_le(benchmark_time, threashold)

	return benchmark_time

def draw_plot(benchmark_time1, benchmark_time2):

	wierd_scale = range(len(threashold_list));

	fig, ax = plt.subplots()

	ys = wierd_scale
	xs1 = map(lambda x: count_le(benchmark_time1, threashold_list[x]), wierd_scale)
	xs2 = map(lambda x: count_le(benchmark_time2, threashold_list[x]), wierd_scale)

	scythe, = ax.plot(xs1, ys, 'g', label="Scythe", linewidth=1.5)
	enum, = ax.plot(xs2, ys, '--', label="Enum", linewidth=1.5)

	plt.legend([scythe, enum], ['Scythe', 'Enum'], 'upper left')
	plt.yticks(wierd_scale, threashold_list)

	make_invisible = True
	if (make_invisible):
			yticks = ax.yaxis.get_major_ticks()
			for i in range(0, 63):
				if (not (i * 10) in visable_ticks):
					yticks[i].label1.set_visible(False)
      #xticks[-1].label1.set_visible(False)

	font = FontProperties()
	font.set_family('sans-serif')
	font.set_weight("bold")
	font.set_size('large')
	plt.figtext(0.04, 0.5, 'Time (seconds)', horizontalalignment='center', rotation='vertical', fontproperties=font) 
	plt.figtext(0.5, 0.01, 'Number of Solved Benchmarks', horizontalalignment='center', fontproperties=font) 

	plt.show()

def main():

	if len(sys.argv) < 3:
		print "[ERROR] Not enough arguments provided."
		print "usage: python draw_plot.py log_folder"
		sys.exit(-1)
	log_dir1 = sys.argv[1]
	log_dir2 = sys.argv[2]

	benchmark_time1 = collect_time(log_dir1)
	benchmark_time2 = collect_time(log_dir2)

	draw_plot(benchmark_time1, benchmark_time2)

	sum_speedup = 0
	cnt = 0
	max_speedup = 0
	min_speedup = 99999

	print len(benchmark_time2), len(benchmark_time1)
	for i in range(0, len(benchmark_time1)):
		if benchmark_time1[i] <= 630 and benchmark_time2[i] <= 630:
			speedup = benchmark_time2[i] / benchmark_time1[i]
			sum_speedup += speedup
			if max_speedup < speedup:
				max_speedup = speedup
			if min_speedup > speedup:
				min_speedup = speedup
			cnt += 1
	print "average speedup", (sum_speedup / cnt)
	print "min speedup", min_speedup
	print "max speedup", max_speedup

if __name__ == '__main__':
	main()
