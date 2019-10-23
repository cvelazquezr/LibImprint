package com.vub.be.utils;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class Node {
    private Node parent;
    private String location;
    private String groupId;
    private String artifactId;
    private String version;
    private ArrayList<String> dependencies;

    public Node(String location, String groupId, String artifactId, String version, ArrayList<String> dependencies) {
        this.location = location;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.dependencies = dependencies;
    }

    private ArrayList<String> concatenateArrays(ArrayList<String> first, ArrayList<String> second)  {
        ArrayList<String> operationResult = new ArrayList<>(first);
        operationResult.addAll(second);

        ArrayList<String> finalList = new ArrayList<>();
        for (int i = 0; i < operationResult.size(); i++) {
            String[] arraySplitted = operationResult.get(i).split(" ");
            String groupId = arraySplitted[0];
            String artifactId = arraySplitted[1];
            String version = arraySplitted[2];

            for (int j = 0; j < operationResult.size(); j++) {
                if (i != j) {
                    String[] arraySplitted2 = operationResult.get(j).split(" ");
                    String groupId2 = arraySplitted2[0];
                    String artifactId2 = arraySplitted2[1];
                    String version2 = arraySplitted2[2];

                    if ((groupId + " " + artifactId).equals(groupId2 + " " + artifactId2) && !version.equals(version2)) {
                        if (version.equals("null"))
                            version = version2;
                    }
                }
            }
            finalList.add(groupId + " " + artifactId + " " + version);
        }

        return (ArrayList<String>) finalList.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return getGroupId() + " " + getArtifactId() + " " + getVersion();
    }

    public void setParent(Node parent) {
        this.parent = parent;
        setDependencies(parent.getDependencies());

        if (getGroupId() == null) {
            setGroupId(parent.getGroupId());
        }

        if (getArtifactId() == null) {
            setArtifactId(parent.getArtifactId());
        }

        if (getVersion() == null) {
            setVersion(parent.getVersion());
        }
    }

    private void setDependencies(ArrayList<String> dependencies) {
        this.dependencies = concatenateArrays(this.dependencies, dependencies);
    }

    public String getLocation() {
        return location;
    }

    public String getGroupId() {
        return groupId;
    }

    private void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    private void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    private void setVersion(String version) {
        this.version = version;
    }

    public ArrayList<String> getDependencies() {
        return dependencies;
    }
}
