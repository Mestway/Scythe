grep "Error occ" *.log | cut -c 1-3 | awk '{print $1  " "}'
