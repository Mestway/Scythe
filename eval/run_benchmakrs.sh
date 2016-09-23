# folder name
date=`date +%Y%m%d%H%M%S`
folder="log_"$date"/"

mkdir $folder

# get all benchmark files in the dir
for i in $(find ../data/benchmarks -type f);do
	echo Running $i;
    bname=$(basename $i);
    java -Xmx4096m -jar ../out/artifacts/Scythe_jar/Scythe.jar $i StagedEnumerator > $folder$bname".log";  
done 
