import subprocess
import datetime
import sys
import time
import os


if __name__ == "__main__":

	benchmark_dir_list = ["../data/dev_set", "../data/recent_posts", "../data/top_rated_posts", "../data/sqlsynthesizer"]
	log_dir = "./log/log_" + datetime.datetime.fromtimestamp(time.time()).strftime('%Y%m%d_%H%M')

	print log_dir

	if not os.path.exists(log_dir):
		os.makedirs(log_dir)

	for benchmark_dir in benchmark_dir_list:

		if not os.path.exists(benchmark_dir):
			print "[[ERROR]] benchmark directory not exists: " + benchmark_dir 

		files = [os.path.join(benchmark_dir, name) for name in os.listdir(benchmark_dir)
																if os.path.isfile(os.path.join(benchmark_dir, name))]

		for f in files:
			if (f.endswith("X")):
				continue

			log_file = os.path.join(log_dir, benchmark_dir.split("/")[-1] + "__" + f.split("/")[-1] + ".log")
			
			print "Running: " + f
			output = open(log_file, "w")
			subprocess.call(['java', '-jar', '../out/artifacts/Scythe_jar/Scythe.jar', f,'StagedEnumerator'], stdout=output)