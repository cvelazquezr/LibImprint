package com.vub.be.runner;

import com.vub.be.builder.Builder;
import com.vub.be.configuration.DefaultConfiguration;

import java.util.ArrayList;
import java.util.HashMap;

public class Runner {
    HashMap<String, ArrayList<String>> usagesLibraryCode;
    HashMap<String, ArrayList<Integer>> usagesLibraryLineNumber;

    public Runner(String projectPath, String [] classPath, ArrayList<String> classes) {
        run(projectPath, classPath, classes);
    }

    private void run(String projectPath, String []classPath, ArrayList<String> classes) {
        DefaultConfiguration extractorConfiguration = new DefaultConfiguration();
        LibraryUsage libraryUsage = new LibraryUsage(classes);
        extractorConfiguration.usageExamplePredicate = libraryUsage;

        Builder builder = new Builder(extractorConfiguration);
        builder.build(new String[]{projectPath}, classPath);

        this.usagesLibraryCode = libraryUsage.getUsagesCodes();
        this.usagesLibraryLineNumber = libraryUsage.getUsagesLineNumbers();
    }

    public HashMap<String, ArrayList<String>> getUsagesLibraryCode() {
        return usagesLibraryCode;
    }

    public HashMap<String, ArrayList<Integer>> getUsagesLibraryLineNumber() {
        return usagesLibraryLineNumber;
    }
}
