# folder name
date=`date +%Y%m%d%H%M%S`
folder="log_"$date"/"

mkdir $folder

if [ "$#" -ne 1 ]; then
    echo "usage: run_benchmarks.sh enumerator_name"
    # Will exit with status of last command.
    exit
fi

echo "[Synthesis Start]"$1

# StagedEnumerator // CanonicalEnumeratorOnTheFly
enumerator=$1

# get all benchmark files in the dir
for i in $(find ../data/benchmarks -type f);do
	echo Running $i;
    #bname=$(basename $i);
    #java -Xmx4096m -jar ../out/artifacts/Scythe_jar/Scythe.jar $i StagedEnumerator > $folder$bname".log";  
done 
