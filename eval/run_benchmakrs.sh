date=`date +%Y%m%d%H%M%S`
folder="log_"$date"/"

mkdir $folder

for i in $(ls ../data/benchmarks/);do
	echo Running $i;
	java -Xmx4096m -jar ../out/artifacts/Scythe_jar/Scythe.jar "../data/benchmarks/"$i StagedEnumerator 2 > $folder$i".log";  
done 
