## Scythe: Synthesizing SQL queries from input output examples.

Scythe is a program synthesis tool that takes in input tables, an output table and user provided query description to synthesize a SQL query that will produce the output from these input tables.

### Running instruction:

Here are the steps to run the tool on an input / output example.

1. Download the jar file [SimpleSynthesizer.jar](https://github.com/Mestway/SimpleSynthesizer/tree/symbolic-cleaned/out/artifacts/SimpleSynthesizer_jar). (Its corresponding source code is in the branch symoblic-cleaned branch.)
2. Prepare an example file containing input tables, an output table, and a constraint with constant information. An example file should look like these ones in the folder  [/data](https://github.com/Mestway/Scythe/tree/symbolic-cleaned/data).
3. Run in command line with command:
```
java -jar SimpleSynthesizer.jar path/to/the/example/file SymbolicEnumerator 2
```
In this command, "path/to/the/example/file" refers to your example file, option "SymbolicEnuemrator" referes to the synthesizer used in synthesis, and the last argument "2" presents the maxinum level of join should a query consider is 2. (Though the query may terminates earlier if a feasible SQL query is found for the given example.) 

### Technical note:

The technical note of the project can be find at [scythe-tr.pdf](https://github.com/Mestway/Scythe/blob/symbolic-cleaned/scythe-tr.pdf).
