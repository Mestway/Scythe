import sys
import os


totalSize = True

# parse a log file to obtain running time
def parse_Clog_file(content):
	time = 999999
	size = 0
	for l in content:
		if l.startswith("[[Synthesis Time]] "):
			time = float(l[len("[[Synthesis Time]] "):-2].strip())
		if totalSize:
			if l.startswith("[Sum Size of the tables] "):
				size += float(l[len("[Sum Size of the tables] "):-1].strip())
		else:
			if l.startswith("[Total Number of Intermediate Result] "):
				size += float(l[len("[Total Number of Intermediate Result] "):-1].strip())
	if time > 630:
		return None
	return size

def parse_Slog_file(content):
	time = 999999
	size = 0
	for l in content:
		if l.startswith("[[Synthesis Time]] "):
			time = float(l[len("[[Synthesis Time]] "):-2].strip())
		if totalSize:
			if l.startswith("[SummaryTableTotalSize]"):
				size += float(l[len("[SummaryTableTotalSize]"):-1].strip())
		else:
			if l.startswith("[SummaryTableNumber] "):
				size += float(l[len("[SummaryTableNumber] "):-1].strip())
	if time > 630:
		return None
	return size

def parse_log_dir(log_dir):
	print log_dir
	result = {}
	files = [os.path.join(log_dir, f) for f in os.listdir(log_dir) if (os.path.isfile(os.path.join(log_dir, f))) ]
	for fname in files:
		with open(fname) as f:
			if "S" in log_dir:
				size = parse_Slog_file(f.readlines())
			elif "C" in log_dir:
				size = parse_Clog_file(f.readlines())
			result[os.path.basename(fname)] = size
			#print fname, ":", synthesis_time 
	return result

def find_reduction_rate(log_dir):
	print log_dir
	result = {}
	files = [os.path.join(log_dir, f) for f in os.listdir(log_dir) if (os.path.isfile(os.path.join(log_dir, f))) ]
	sum_avg_red_rate = 0
	max_red_rate = 0
	min_red_rate = 999999
	avg_red_count = 0
	for fname in files:
		with open(fname) as f:
			sum_reduction_rate = 0
			cnt = 0
			for l in f.readlines():
				if l.startswith("[Filter Reduction Rate] "):
					sum_reduction_rate += float(l[len("[Filter Reduction Rate] "):-1].strip())
					cnt += 1

			print sum_reduction_rate
			print "~",cnt 
			if sum_reduction_rate < 0:
				print "~~~",sum_reduction_rate
				continue
			if min_red_rate > sum_reduction_rate / cnt:
				min_red_rate = sum_reduction_rate / cnt
			if max_red_rate < sum_reduction_rate / cnt:
				max_red_rate = sum_reduction_rate / cnt
			if sum_avg_red_rate < 0:
				continue
			sum_avg_red_rate += sum_reduction_rate / cnt
			avg_red_count += 1
			#print fname, ":", synthesis_time 
	print "Max:", max_red_rate
	print "Min:", min_red_rate
	print "Avg:", sum_avg_red_rate / avg_red_count
	return sum_avg_red_rate / avg_red_count

def main():

	if len(sys.argv) == 3:
		print "[ERROR] Not enough arguments provided."
		sys.exit(-1)
		log_dir1 = sys.argv[1]
		log_dir2 = sys.argv[2]

		dict1 = parse_log_dir(log_dir1)
		dict2 = parse_log_dir(log_dir2)

		for key in dict1:
			v1 = dict1[key]
			v2 = dict2[key]
			if (v1 != None and v2 != None):
				print v1 / v2

	elif len(sys.argv) == 2:
		log_dir1 = sys.argv[1]
		print find_reduction_rate(log_dir1)

	
if __name__ == '__main__':
	main()
