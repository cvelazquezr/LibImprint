package com.vub.be.builder;

import com.vub.be.configuration.DefaultConfiguration;
import com.vub.be.utils.Utils;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;

public class Builder {
    private final DefaultConfiguration configuration;

    public Builder(DefaultConfiguration configuration) {
        this.configuration = configuration;
    }

    public void build(String[] sourcePaths, String[] classpaths) {
        for (String sourcePath : sourcePaths) {
            buildBatch(sourcePath, classpaths);
        }
    }

    private void buildBatch(String path, String[] classpaths) {
        processJavaFiles(new File(path), classpaths);
    }

    private void processJavaFiles(File file, String[] classpaths) {
        if (file.isDirectory()) {
            for (File sub : file.listFiles())
                processJavaFiles(sub, classpaths);
        } else if (file.isFile() && file.getName().endsWith(".java")) {
            String sourceCode = Utils.readStringFromFile(file.getAbsolutePath());
            processCU(sourceCode, file.getAbsolutePath(), file.getName(), classpaths);
        }
    }

    private void processCU(String sourceCode, String path, String name, String[] classpaths) {
        CompilationUnit cu = (CompilationUnit) Utils.parseSource(sourceCode, path, name, classpaths);
        configuration.usageExamplePredicate.matches(path, cu);
    }
}
