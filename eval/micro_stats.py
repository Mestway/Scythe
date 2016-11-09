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

def update_avg_min_max_cnt(triple, v):
	if v < 0:
		return triple
	triple[0] += v
	if triple[1] > v:
		triple[1] = v
	if triple[2] < v:
		triple[2] = v
	triple[3] += 1
	return triple

def find_reduction_rate(log_dir):
	print log_dir
	result = {}
	files = [os.path.join(log_dir, f) for f in os.listdir(log_dir) if (os.path.isfile(os.path.join(log_dir, f))) ]
	sum_avg_red_rate = 0
	max_red_rate = 0
	min_red_rate = 999999
	avg_red_count = 0

	filter_red = [0,9999999,0,0]
	bw_ratio = [0,9999999,0,0]
	abstract_search_prune = [0,9999999,0,0]

	for fname in files:
		with open(fname) as f:

			#predicate redcution rate
			sum_reduction_rate = 0
			cnt = 0

			# abstract search effectiveness
			abstractSearchPrunedCountSum = 0
			abscnt = 0

			# backward search prune effectiveness
			backwardPrunedEffectiveness = 0
			bwcnt = 0

			for l in f.readlines():
				if l.startswith("[Filter Reduction Rate] "):
					sum_reduction_rate += float(l[len("[Filter Reduction Rate] "):-1].strip())
					cnt += 1
				elif l.startswith("[CFilter Reduction Rate] "):
					sum_reduction_rate += float(l[len("[CFilter Reduction Rate] "):-1].strip())
					cnt += 1
				elif l.startswith("[AbstractSearchPrunedCount]"):
					abstractSearchPrunedCountSum += float(l[len("[AbstractSearchPrunedCount] "):-1].strip())
					abscnt += 1
				elif l.startswith("[Backward Prune Effectiveness]"):
					backwardPrunedEffectiveness = float(l[len("[Backward Prune Effectiveness] "):-1].strip())
					bwcnt += 1

			avgpred = sum_reduction_rate / cnt
			avgbw = backwardPrunedEffectiveness / bwcnt
			avgabstr = abstractSearchPrunedCountSum / abscnt

			filter_red = update_avg_min_max_cnt(filter_red, avgpred)
			bw_ratio = update_avg_min_max_cnt(bw_ratio, avgbw)
			abstract_search_prune = update_avg_min_max_cnt(abstract_search_prune, avgabstr)

			#if avgpred < 0:
		#		continue
		#	if min_red_rate > avgpred:
	#			min_red_rate = avgpred
#			if max_red_rate < avgpred:
			#	max_red_rate = avgpred
			#sum_avg_red_rate += sum_reduction_rate / cnt
			#avg_red_count += 1


			#print fname, ":", synthesis_time 
	#print "Max:", max_red_rate
	#print "Min:", min_red_rate
	#print "Avg:", sum_avg_red_rate / avg_red_count

	print "Filter Reduction: ", filter_red, filter_red[0]/filter_red[3]
	print "Backward Prune Ratio: ", bw_ratio, bw_ratio[0]/bw_ratio[3]
	print "Abstract Search Prune Ratio: ", abstract_search_prune, abstract_search_prune[0]/abstract_search_prune[3]

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
