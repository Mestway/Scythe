import os

path = "."

# parsing all log files
for fn in os.listdir("."):
	log = []
	if fn.endswith(".log"):
        f = open(fn, 'r')
        content = f.readlines()
        i = 0
        log_entry = {}
        log_entry["log_name"] = f.name
        stages = []
        while i < len(content):
            line = content[i]

            # extract the running time
            if line.startswith("[[Synthesizer finished]] time:"):
                time = line[len("[[Synthesizer finished]] time:") + 1:-1]
                log_entry["time"] = time
            # extract stage information
            elif line.startswith("[Stage"):
				stage_no = line[len("[Stage ")]
				stage_table_count = -1
				stage_query_count = -1
				i += 1
				while (len(content[i]) - len(content[i].lstrip()) > 0):
					if (content[i].lstrip().startswith("Queries generated")):
						stage_query_count = content[i].lstrip()[len("Queries generated: "): -1]
					if (content[i].lstrip().startswith("Tables generated")):
						stage_table_count = content[i].lstrip()[len("Tables generated: "): -1]
					if (content[i].lstrip().startswith("Total Table by now")):
						total_table_enumerated = content[i].lstrip()[len("Total Table by now: "): -1]
					i += 1
				stage_entry = {}
				stage_entry["id"] = stage_no
				stage_entry["table_count"] = stage_table_count
				stage_entry["query_count"] = stage_query_count
				stage_entry["total_table_by_now"] = total_table_enumerated
				stages.append(stage_entry)
				i -= 1
			# extract final table information
			elif (line.startswith("[Runner up Table Count")):
				runner_up_table = line[len("[Runner up Table Count] "):-1]
				log_entry["runner_up"] = runner_up_table
			elif (line.startswith("Total Tree Count")):
				total_table_tree_count = line[len("Total Tree Count: "):-1]
				log_entry["total_table_tree"] = (total_table_tree_count)
			elif (line.startswith("Total Query Count")):
				total_query_count = line[len("Total Query Count: "):-1]
				log_entry["total_query_count"] = (total_query_count)
			i += 1
		log_entry["stages"] = stages
		log.append(log_entry)
		print log_entry
