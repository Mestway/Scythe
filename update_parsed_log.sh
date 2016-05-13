scp -r clwang@recycle.cs.washington.edu:./enumeration_test/log ./temp-log
cd temp-log
python parse_log.py > ../log_summary.txt
cd ..
git add log_summary.txt
git commit -m "log_summary_updated"
git push -u origin symbolic
rm -r temp-log
