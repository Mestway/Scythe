doalarm () { perl -e 'alarm shift; exec @ARGV' "$@"; } # define a helper function

for i in $(ls data/benchmarks/);do
	echo Running $i;
	doalarm 600 java -Xmx4096m -jar out/artifacts/SimpleSynthesizer_jar/SimpleSynthesizer.jar "data/benchmarks/"$i CanonicalEnumeratorOnTheFly 2 > "evaluation_log/"$i".log";
done 
