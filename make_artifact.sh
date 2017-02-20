if [ -d "artifact" ]; then
    rm -r artifact
fi

if [ -f "artifact.tar" ]; then
    rm artifact.tar
fi

mkdir artifact

# copy data 
mkdir artifact/data
cp -R data/dev_set artifact/data/dev_set
cp -R data/recent_posts artifact/data/recent_posts
cp -R data/top_rated_posts artifact/data/top_rated_posts
cp -R data/sqlsynthesizer artifact/data/sqlsynthesizer

# copy artifact
cp out/artifacts/Scythe_jar/Scythe.jar artifact/

# copy scripts
mkdir artifact/eval_scripts
cp eval/*.py artifact/eval_scripts
# those two scripts are not necessary for artifact
rm artifact/eval_scripts/analyze_log.py
rm artifact/eval_scripts/temp_draw_plot.py
cp -R eval/log/Clog_20161108_0027/ artifact/eval_scripts/Enum_log 
cp -R eval/log/Slog_20161109_0118/ artifact/eval_scripts/Scythe_log 

# compress
tar -czf artifact.tar.gz artifact/
rm -r artifact
