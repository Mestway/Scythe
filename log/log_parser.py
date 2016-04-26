import os
from tabulate import tabulate

path = "."

log = []

# parsing all log files
for fn in os.listdir("."):
  if fn.endswith(".log"):
    f = open(fn, 'r')
    content = f.readlines()
    i = 0
    log_entry = {}
    log_entry["log_name"] = f.name
    log_entry["finished"] = False
    log_entry["fail_to_find"] = False
    log_entry["last_table_number"] = "-"
    log_entry["avg_table_size"] = "-"
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
            while (i < len(content) and len(content[i]) - len(content[i].lstrip()) > 0):
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
        elif (line.startswith("[Enumeration Finished")):
            log_entry["finished"] = True
            if "Does not find a query" in line:
                log_entry["fail_to_find"] = True
        elif (line.startswith("[Runner up Table Count")):
            runner_up_table = line[len("[Runner up Table Count] "):-1]
            log_entry["runner_up"] = runner_up_table
        elif (line.startswith("Total Tree Count")):
            total_table_tree_count = line[len("Total Tree Count: "):-1]
            log_entry["total_table_tree"] = (total_table_tree_count)
        elif (line.startswith("Total Query Count")):
            total_query_count = line[len("Total Query Count: "):-1]
            log_entry["total_query_count"] = (total_query_count)
        elif (line.startswith("We have ")):
            log_entry["avg_table_size"] = line.split()[-1][:line.split()[-1].index(".")]
            log_entry["last_table_number"] = line.split()[2]
        i += 1

    log_entry["stages"] = stages
    log.append(log_entry)

table_header = ["id", "status", "time", "table_tree / queries", "runner up", "table sz/cnt" ,"stage_1", "stage_2", "stage_3", "stage_4"]
tabular_log = []
for log_item in log:
    row = []
    row.append(log_item["log_name"])

    if log_item["finished"]:
        if (log_item["fail_to_find"]):
            row.append("fail to find")
            if ("time" in log_item):
                row.append(log_item["time"])
            else:
                row.append("---")
            row.append("0 / 0")
            row.append("0")
        else:
            row.append("succeed")
            if ("time" in log_item):
                row.append(log_item["time"])
            else:
                row.append("---")
            row.append(log_item["total_table_tree"] + " / " + log_item["total_query_count"])
            row.append(log_item["runner_up"])
    else:
        row.append("memory exceed")
        row.append("---")
        row.append("---")
        row.append("---")

    row.append(log_item["last_table_number"] + " / " + log_item["avg_table_size"])

    for i in range(4):
        if i >= len(log_item["stages"]):
            row.append("---")
        else:
            row.append(log_item["stages"][i]["table_count"] + " / " + log_item["stages"][i]["query_count"])

    tabular_log.append(row)

#print tabulate(tabular_log, table_header)

x = ""
for s in table_header:
    x = x + s + ", "
print x[:-2]
for l in tabular_log:
    x = ""
    for s in l:
        x = x + s + ", "
    print x[:-2]
