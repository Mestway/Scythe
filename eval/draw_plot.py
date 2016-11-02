import sys
import os

import matplotlib.pyplot as plt
import numpy as np

from matplotlib.ticker import EngFormatter
from matplotlib.ticker import FuncFormatter, MaxNLocator

# parse a log file to obtain running time
def parse_log_file(content):
	time = -1
	for l in content:
		if l.startswith("[[Synthesis Time]] "):
			time = float(l[len("[[Synthesis Time]] "):-2].strip())
	return time

threashold_list = [0, 1, 2, 5, 10, 20, 30, 60, 120, 240, 600]

def count_le(time_set, threashold):
	count = 0
	for t in time_set:
		if t < threashold:
			count += 1
	return count

def main():

	if len(sys.argv) < 2:
		print "[ERROR] Not enough arguments provided."
		print "usage: python draw_plot.py log_folder"
		sys.exit(-1)
	log_dir = sys.argv[1]
	files = [os.path.join(log_dir, f) for f in os.listdir(log_dir) if os.path.isfile(os.path.join(log_dir, f))]
	
	benchmark_time = []

	for fname in files:
		with open(fname) as f:
			synthesis_time = parse_log_file(f.readlines())
			benchmark_time.append(synthesis_time)
			#print fname, ":", synthesis_time 

	for threashold in threashold_list:
		print threashold, ":", count_le(benchmark_time, threashold)

	fig, ax = plt.subplots()

	wierd_scale = range(len(threashold_list));

	ys = wierd_scale
	xs = map(lambda x: count_le(benchmark_time, threashold_list[x]), wierd_scale)
	ax.plot(xs, ys)

	plt.yticks(wierd_scale, threashold_list)

	plt.show()

if __name__ == '__main__':
	main()
