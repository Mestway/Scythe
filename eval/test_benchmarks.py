import subprocess32
import datetime
import sys
import time
import os

logging = True
timeout = 600

scythe = os.path.join("..", "out", "artifacts", "Scythe_jar", "Scythe.jar")

if __name__ == "__main__":


	data_dir = os.path.join("..", "data")

	benchmark_dir_list = ["dev_set", "recent_posts", "top_rated_posts", "sqlsynthesizer"]
	log_dir = os.path.join("log", "log_" + datetime.datetime.fromtimestamp(time.time()).strftime('%Y%m%d_%H%M'))

	if logging and not os.path.exists(log_dir):
		os.makedirs(log_dir)

	for benchmark_dir_suffix in benchmark_dir_list:

		if not "sqlsynthesizer" in benchmark_dir_suffix:
			continue

		benchmark_dir = os.path.join(data_dir, benchmark_dir_suffix)

		if not os.path.exists(benchmark_dir):
			print "[[ERROR]] benchmark directory not exists: " + benchmark_dir 

		files = [os.path.join(benchmark_dir, name) for name in os.listdir(benchmark_dir)
																if os.path.isfile(os.path.join(benchmark_dir, name))]

		for f in files:
			if (f.endswith("X")):
				continue

			print "[[Running]] " + f

			if logging:
				log_file = os.path.join(log_dir, benchmark_dir_suffix + "__" + os.path.basename(f) + ".log")
				output = open(log_file, "w")
				try:
					subprocess32.call(['java', '-jar', scythe, f,'StagedEnumerator'], stdout=output, timeout=timeout)
				except:
					print "  [FAIL] timeout"
			else:
				try:
					FNULL = open(os.devnull, 'w')
					subprocess32.call(['java', '-jar', scythe, f,'StagedEnumerator'], stdout=FNULL, timeout=timeout)
				except:
					print "  [FAIL] timeout"
