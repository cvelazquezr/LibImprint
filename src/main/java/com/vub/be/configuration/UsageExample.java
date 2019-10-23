package com.vub.be.configuration;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public interface UsageExample {
    static UsageExample allUsageExamples() {
        return new UsageExample() {
            public boolean matches(String sourceFilePath, CompilationUnit cu) {
                return true;
            }

            public boolean matches(String sourceFilePath, MethodDeclaration methodDeclaration) {
                return true;
            }
        };
    }

    boolean matches(String sourcePath, CompilationUnit cu);

    boolean matches(String sourceFilePath, MethodDeclaration methodDeclaration);
}
