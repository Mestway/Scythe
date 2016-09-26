## Scythe: Synthesizing SQL queries from input output examples.

Scythe is a program synthesis tool that synthesizes sql queries from input-output examples. Scythe takes in 1) input examples: a set of input tables, 2) an output table and 3) description of which constants or aggregators to be used, and returns a list of SQL queries synthesized from these inputs.

### Running instruction:

Here are the steps to run the tool on an input-output example.

1. Download the jar file [Scythe.jar](https://github.com/Mestway/Scythe/tree/refined/out/artifacts/Scythe_jar).
2. Prepare an example file containing input tables, an output table, and a constraint with constant information. An example file should look like these ones in the folder  [/data](https://github.com/Mestway/Scythe/tree/refined/data).
3. Run in command line with command:
```
java -jar Scythe.jar path/to/the/example/file StagedEnumerator
```
In this command, "path/to/the/example/file" refers to your example file, option "StagedEnumerator" referes to the synthesizer used in synthesis.

### Technical note:

The technical note of the project can be find at [scythe-tr.pdf](https://github.com/Mestway/Scythe/blob/symbolic-cleaned/scythe-tr.pdf).
