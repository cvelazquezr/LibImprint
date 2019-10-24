## LibImprint
The goal of this project is the exploration of Maven-based projects through the analysis of the libraries specified in
the configuration file e.g.(POM file). Basically, this project parse the POM(s) file(s) extracting the dependencies in
it. Later on, for each dependency computes the usages present in the source code files of the project. Finally, the tool
generates a JSON file for each dependency. This JSON file list the classes and line numbers for the class file where the 
dependency is used.

#### Tool setup
After cloning the project you should produce an artifact for the tool:
```
mvn package
```
In the `target` folder you should find two `jar` files with names like this:
```
libImprint-0.1.jar
libImprint-0.1-jar-with-dependencies.jar
```

#### Tool execution
To use the tool through the command line you can execute the second `jar` file:
```
java -jar target/libImprint-0.1-jar-with-dependencies.jar -i project/
```
Only Maven-based projects with POM files can be analyzed.

#### Output description
After running the tool in an specific project, you should have a folder `data` with the following structure:
```
data:
---- ghostprocessing
---- usages
```
Inside the `usages` folder are located JSON files for each dependency of the project. Each file is structured with the
class file path in the project and the number of lines where you can find the statement that use the dependent library.

Note that some line number ends with a letter `d` e.g. (`45d`). This means that for that line number (`45`), the tool
only could found a usage based on the dependencies of the library in analysis and not the library itself.