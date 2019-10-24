package com.vub.be;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.javafx.scene.control.behavior.OptionalBoolean;
import com.vub.be.runner.Runner;
import com.vub.be.utils.Node;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.cli.*;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

public class Main {

    public static ArrayList<String> getClasses(String path) throws IOException {
        File file = new File(path);
        ArrayList<String> classes = new ArrayList<>();

        if (file.exists()) {
            ZipFile zipFile = new ZipFile(file);

            Enumeration<? extends ZipEntry> entries =  zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry =  entries.nextElement();
                String entryName = entry.getName();

                if (!entry.isDirectory() && entryName.endsWith(".class")) {
                    if (!entryName.equals("module-info.class")) {
                        ClassParser classParser = new ClassParser(zipFile.getInputStream(entry), entryName);
                        JavaClass javaClass = classParser.parse();

                        if (javaClass.isPublic() || !(javaClass.isPrivate() & javaClass.isProtected())) {
                            String className = javaClass.getClassName();

                        if (className.contains("$"))
                            className = className.replaceAll("\\$", "\\.");

                        classes.add(className);
                        }
                    }
                }
            }
            zipFile.close();
        }
        return classes;
    }

    public static String mapMetadata(String property, Properties properties) {
        if (property.startsWith("$")) {
            String chunkedProperty = property.substring(2, property.length() - 1);
            return properties.getProperty(chunkedProperty);
        }
        return property;
    }

    public static ArrayList<String> pomDependencies(Stream<Path> pomLocations) {
        MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
        List<String> listLocations = pomLocations.map(Path::toString).collect(Collectors.toList());

        List<Node> pomsFiles = new ArrayList<>();
        List<String> parentsMetadata = new ArrayList<>();

        for (int i = 0; i < listLocations.size(); i ++) {
            String location = listLocations.get(i);

            try {
                InputStream inputStream = new FileInputStream(new File(location));
                Model model = xpp3Reader.read(inputStream);

                // Information about the parent
                Parent parent = model.getParent();
                String parentID = parent.getGroupId() + " " + parent.getArtifactId() + " " + parent.getVersion();
                parentsMetadata.add(parentID);

                Properties properties = model.getProperties();

                List<Dependency> dependenciesModel = model.getDependencies();
                // Dependencies in the DependencyManagment
                if (dependenciesModel.isEmpty())
                    dependenciesModel = model.getDependencyManagement().getDependencies();

                ArrayList<String> dependencies = new ArrayList<>();
                // Regular dependencies
                for (Dependency dependency: dependenciesModel) {
                    String groupId = dependency.getGroupId();
                    String artifactId = dependency.getArtifactId();
                    String version = dependency.getVersion();

                    if (properties.size() > 0) {
                        groupId = mapMetadata(groupId, properties);
                        artifactId = mapMetadata(artifactId, properties);
                        version = mapMetadata(version, properties);
                    }

                    String metadata = groupId + " " + artifactId + " " + version;
                    if (!metadata.contains("project.version"))
                        dependencies.add(metadata);
                }
                // Making objects with the information
                Node pomNode = new Node(location,
                        model.getGroupId(),
                        model.getArtifactId(),
                        model.getVersion(),
                        dependencies);
                pomsFiles.add(pomNode);

            } catch (Exception e) {
                System.out.println();
            }
        }

        // Relate POMs files in a project
        for (int i = 0; i < parentsMetadata.size(); i++) {
            String parent = parentsMetadata.get(i);
            for (int j = 0; j < pomsFiles.size(); j++) {
                Node pomTemp = pomsFiles.get(j);
                if (pomTemp.toString().equals(parent)) {
                    // This POM file is the parent
                    pomsFiles.get(i).setParent(pomTemp);
                }
            }
        }

        // Get a definitive list of dependencies
        ArrayList<String> listDependencies = new ArrayList<>();
        for (Node pomFile : pomsFiles) {
            listDependencies.addAll(pomFile.getDependencies());
        }

        return (ArrayList<String>) listDependencies.stream().distinct().collect(Collectors.toList());
    }

    public static Stream<Path> pomLocations(String projectPath) throws IOException {
        Path pathProject = Paths.get(projectPath);
        return Files.find(pathProject, Integer.MAX_VALUE, (path, basicFileAttributes) -> String.valueOf(path).endsWith("pom.xml"));
    }

    public static HashMap<String, ArrayList<String>> extractUsages(String project, String library, boolean mainLibrary) throws IOException {
        HashMap<String, ArrayList<String>> linesMap = new HashMap<>();

        // Extract the classes for a library
        ArrayList<String> classesLibrary = getClasses(library);

        Runner runner = new Runner(project,
                new String[]{library},
                classesLibrary);

        HashMap<String, ArrayList<Integer>> usages = runner.getUsagesLibraryLineNumber();
        for (Map.Entry entry : usages.entrySet()) {

            ArrayList<Integer> values = (ArrayList<Integer>) entry.getValue();
            ArrayList<String> valuesStr = new ArrayList<>();

            for (int value : values) {
                if (mainLibrary)
                    valuesStr.add(String.valueOf(value));
                else
                    valuesStr.add(value + "d");
            }
            linesMap.put((String) entry.getKey(), (ArrayList<String>) valuesStr.stream().distinct().collect(Collectors.toList()));
        }
        return linesMap;
    }

    public static void processDependencies(String project, String dependency) throws IOException, InterruptedException {
        System.out.println("Processing library: " + dependency);

        String[] dependencySplitted = dependency.split(" ");
        String groupId = dependencySplitted[0];
        String artifactId = dependencySplitted[1];
        String version = dependencySplitted[2];

        // Creating folders if ot doesnt exist
        File ghostFolder = new File("data/ghostProcessing");
        if (!ghostFolder.exists()) {
            ghostFolder.mkdirs();
        }

        // Writing a ghost POM file
        PrintWriter pw = new PrintWriter(new File(ghostFolder.getPath() + "/pom.xml"));
        String textBefore = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">" +
                "<modelVersion>4.0.0</modelVersion>\n" +
                "\n" +
                "  <groupId>com.vub.be</groupId>\n" +
                "  <artifactId>ghostProcessing</artifactId>\n" +
                "  <version>0.1</version>\n" +
                "\n  <dependencies>\n";

        String textAfter = "  </dependencies>\n\n</project>";
        String textDependency = String.format("    <dependency>\n" +
                        "      <groupId>%s</groupId>\n" +
                        "      <artifactId>%s</artifactId>\n" +
                        "      <version>%s</version>\n" +
                        "    </dependency>\n", groupId, artifactId, version);

        pw.write(textBefore);
        pw.write(textDependency);
        pw.write(textAfter);

        pw.close();

        // Getting the classpath of the ghost project
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "getClasspath.sh", "data/ghostProcessing");
        Process process = processBuilder.start();
        process.waitFor();

        // Read the file of the classPath
        File classPath = new File("data/ghostProcessing/cp.txt");
        if (classPath.exists()) {
            BufferedReader fileReader = new BufferedReader(new FileReader(classPath));
            String[] libraries = fileReader.readLine().split(":");
            HashMap<String, ArrayList<String>> usages;
            HashMap<String, ArrayList<String>> joinedUsages = new HashMap<>();

            // Extracting and joining usages
            for (int i = 0; i < libraries.length; i++) {
                if (i == 0) {
                    usages = extractUsages(project, libraries[i],true);
                    joinedUsages.putAll(usages);
                }
                else {
                    usages = extractUsages(project, libraries[i],false);
                    for (Map.Entry entry : usages.entrySet()) {
                        String key = (String) entry.getKey();
                        ArrayList<String> values = (ArrayList<String>) entry.getValue();

                        if (joinedUsages.containsKey(key)) {
                            ArrayList<String> newValues = joinedUsages.get(key);
                            newValues.addAll(values);
                            ArrayList<String> distinctValues = (ArrayList<String>) newValues.stream().distinct().collect(Collectors.toList());

                            joinedUsages.replace(key, distinctValues);
                        } else
                            joinedUsages.put(key, values);
                    }
                }
            }

            // Checking lines
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode arrayNode = mapper.createArrayNode();

            for (Map.Entry entry : joinedUsages.entrySet()) {
                String file = (String) entry.getKey();
                ArrayList<String> values = (ArrayList<String>) entry.getValue();

                ObjectNode classObject = mapper.createObjectNode();
                classObject.put("classFile", file);

                ArrayNode lines = mapper.createArrayNode();
                for (String value : values) {
                    ObjectNode numberObject = mapper.createObjectNode();
                    numberObject.put("number", value);

                    lines.add(numberObject);
                }
                classObject.putPOJO("lines", lines);
                arrayNode.add(classObject);
            }

            File usagesFolder = new File("data/usages");
            if (!usagesFolder.exists())
                usagesFolder.mkdir();

            String jsonFile = String.format("%s/%s.json", usagesFolder.getPath(), dependency.replaceAll(" ", "_"));
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(jsonFile), arrayNode);

            fileReader.close();
        } else {
            System.out.println("Classpath file does not exist");
        }
    }

    public static void main(String[] args) throws Exception {
        Options options = new Options();

        // Path of the project in analysis
        Option sourceInput = Option.builder("i")
                .longOpt("input")
                .required(true)
                .hasArg()
                .desc("Maven-based project in analysis")
                .build();

        // Optional library metadata information
        Option libraryAnalysis = Option.builder("l")
                .longOpt("library")
                .required(false)
                .hasArgs()
                .desc("Specific library to analyze")
                .build();

        options.addOption(sourceInput);
        options.addOption(libraryAnalysis);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }

        String projectName = cmd.getOptionValue("input");
        String[] library = cmd.getOptionValues("library");

        ArrayList<String> dependencies = pomDependencies(pomLocations(projectName));

        if (library == null) {
            for (String dependency : dependencies) {
                processDependencies(projectName, dependency);
            }
        } else {
            String dependency = String.join(" ", library);
            processDependencies(projectName, dependency);
        }
    }
}
