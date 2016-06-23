for i in $(ls data/benchmarks/);do
	echo Running $i;
	java -Xmx4096m -jar out/artifacts/SimpleSynthesizer_jar/SimpleSynthesizer.jar "data/benchmarks/"$i SymbolicEnumerator 2 > "evaluation_log/"$i".log";  
done 
