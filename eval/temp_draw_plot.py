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
visable_ticks = [5, 10, 60, 120, 180, 300, 450, 600]
threashold_list = []
for i in range(0, 610, 10):
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
		#		continue
			synthesis_time = parse_log_file(f.readlines())
			print fname, synthesis_time
			benchmark_time.append(synthesis_time)
			#print fname, ":", synthesis_time 

	#for threashold in threashold_list:
	#	print threashold, ":", count_le(benchmark_time, threashold)

	return benchmark_time

def draw_plot(benchmark_time1, benchmark_time2):

	wierd_scale = range(len(threashold_list));

	percentage_tick = []
	for i in range(0, 193, 10):
		percentage_tick.append(i)
	percentage_tick.append(193)
	percentage_tick_label = []
	for i in range(0, 193, 10):
		percentage_tick_label.append(str(i))
	percentage_tick.append(193)


	fig, ax = plt.subplots()

	xs = wierd_scale
	ys1 = map(lambda x: (count_le(benchmark_time1, threashold_list[x])), wierd_scale)
	ys2 = map(lambda x: (count_le(benchmark_time2, threashold_list[x])), wierd_scale)

	print ys1
	print ys2

	scythe, = ax.plot(xs, ys1, 'g', label="Scythe", linewidth=1.8)
	enum, = ax.plot(xs, ys2, '--', label="Enum", linewidth=1.8)

	plt.legend([scythe, enum], ['Scythe', 'Enum'], 'lower right')
	plt.xticks(wierd_scale, threashold_list)
	plt.yticks(percentage_tick, percentage_tick_label)

	ax.axhline(y=0.5,xmin=0,xmax=3,c="grey",linewidth=0.5,zorder=0)

	make_invisible = True
	if (make_invisible):
			xticks = ax.xaxis.get_major_ticks()
			for i in range(0, 61):
				if (not (i * 10) in visable_ticks):
					xticks[i].label1.set_visible(False)
			yticks = ax.yaxis.get_major_ticks()
			#	yticks[-1].label1.set_visible(False)
      #xticks[-1].label1.set_visible(False)

	font = FontProperties()
	font.set_family('sans-serif')
	font.set_weight("bold")
	font.set_size('large')
	#plt.figtext(0.04, 0.5, 'Solved Cases', horizontalalignment='center', rotation='vertical', fontproperties=font) 
	plt.figtext(0.5, 0.01, 'Time (seconds)', horizontalalignment='center', fontproperties=font) 

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
	print count_le(benchmark_time2, 10), count_le(benchmark_time1, 10)
	print count_le(benchmark_time2, 630), count_le(benchmark_time1, 630)
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
