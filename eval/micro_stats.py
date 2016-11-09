import sys
import os

# parse a log file to obtain running time
def parse_Clog_file(content):
	time = 999999
	size = 0
	for l in content:
		if l.startswith("[[Synthesis Time]] "):
			time = float(l[len("[[Synthesis Time]] "):-2].strip())
		if l.startswith("[Sum Size of the tables] "):
			size += float(l[len("[Sum Size of the tables] "):-1].strip())
	if time > 630:
		return None
	return size

def parse_Slog_file(content):
	time = 999999
	size = 0
	for l in content:
		if l.startswith("[[Synthesis Time]] "):
			time = float(l[len("[[Synthesis Time]] "):-2].strip())
		if l.startswith("[SummaryTableTotalSize]"):
			size += float(l[len("[SummaryTableTotalSize]"):-1].strip())
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

def main():

	if len(sys.argv) < 2:
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

if __name__ == '__main__':
	main()
