import sys
import os

onlyMeasureFirstStage = False
totalSize = False

# parse a log file to obtain running time
def parse_Slog_file(content):
	time = 999999
	size = 0

	sum_ast_node_cnt = 0
	sum_ascii_cnt = 0
	instance_cnt = 0

	for l in content:
		if l.startswith("[ASCII Cnt] "):
			sum_ascii_cnt += int(l[len("[ASCII Cnt]"):len(l)].strip())
			instance_cnt += 1
		if l.startswith("[ASTNodeCnt] "):
			sum_ast_node_cnt += int(l[len("[ASTNodeCnt]"):len(l)].strip())

	print float(sum_ast_node_cnt)/instance_cnt, float(sum_ascii_cnt)/instance_cnt
	return float(sum_ast_node_cnt)/instance_cnt, float(sum_ascii_cnt)/instance_cnt

def parse_log_dir(log_dir):
	print log_dir
	result = {}
	files = [os.path.join(log_dir, f) for f in os.listdir(log_dir) if (os.path.isfile(os.path.join(log_dir, f))) ]
	
	ast_cnt_sum = 0
	ascii_cnt_sum = 0
	cnt = 0

	for fname in files:
		if ("recent" in fname):
			with open(fname) as f:
				try:
					ast_cnt, ascii_cnt = parse_Slog_file(f.readlines())
					ast_cnt_sum += ast_cnt
					ascii_cnt_sum += ascii_cnt
					cnt += 1
				except:
					print f
	return float(ast_cnt_sum) / cnt, float(ascii_cnt_sum) / cnt

def main():
	
	if len(sys.argv) == 2:
		log_dir1 = sys.argv[1]
		print parse_log_dir(log_dir1)
	
if __name__ == '__main__':
	main()
