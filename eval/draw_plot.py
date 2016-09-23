import sys
import os

# parse a log file to obtain running time
def parse_log_file(content):
	time = -1
	for l in content:
		if l.startswith("[[Synthesizer finished]] seconds: "):
			time = float(l[len("[[Synthesizer finished]] seconds: "):-1].strip())
	return time

def main():
	if len(sys.argv) < 2:
		print "[ERROR] Not enough arguments provided."
		print "usage: python draw_plot.py log_folder"
		sys.exit(-1)
	log_dir = sys.argv[1]
	files = [os.path.join(log_dir, f) for f in os.listdir(log_dir) if os.path.isfile(os.path.join(log_dir, f))]
	for fname in files:
		with open(fname) as f:
			print fname, ":", parse_log_file(f.readlines())

if __name__ == '__main__':
	main()
