package com.vub.be.runner;

import com.vub.be.configuration.UsageExample;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashMap;


public class LibraryUsage implements UsageExample {
    private ArrayList<String> classes;
    private String sourceFilePath;
    private ArrayList<String> usages;
    private ArrayList<Integer> lineNumbers;
    private ArrayList<String> filesUsages;
    private CompilationUnit cuCopy;

    public LibraryUsage(ArrayList<String> classes) {
        this.classes  = classes;
        this.usages = new ArrayList<>();
        this.lineNumbers = new ArrayList<>();
        this.filesUsages = new ArrayList<>();
        this.sourceFilePath = "";
    }

    public boolean matches(String sourceFilePath, CompilationUnit cu) {
        this.sourceFilePath = sourceFilePath;
        this.cuCopy = cu;
        return matches(cu);
    }

    public boolean matches(String sourceFilePath, MethodDeclaration methodDeclaration) {
        this.sourceFilePath = sourceFilePath;
        return matches(methodDeclaration);
    }

    private boolean matches(ASTNode node) {
        node.accept(new ASTVisitor(false) {
            @Override
            public boolean visit(VariableDeclarationStatement node) {
                String variableType = node.getType().toString();

                if (endsWithLibrary(variableType)) {
                    usages.add(node.toString());

                    lineNumbers.add(cuCopy.getLineNumber(node.getStartPosition()));
                    filesUsages.add(sourceFilePath);
                }

                return super.visit(node);
            }

            @Override
            public boolean visit(MethodInvocation node) {

                if (node.resolveMethodBinding() == null) {
                    return super.visit(node);
                }

                boolean declaredByApi = isDeclaredByApiClass(node.resolveMethodBinding());
                String qualifiedName = node.resolveMethodBinding().getDeclaringClass().getTypeDeclaration().getQualifiedName();

                if (qualifiedName.contains(" ")) {
                    recursiveCalls(node);
                }

                if (inLibraries(qualifiedName)) {
                    String methodCalls = qualifiedName + "." + node.resolveMethodBinding().getName();

                    usages.add(methodCalls);
                    lineNumbers.add(cuCopy.getLineNumber(node.getStartPosition()));
                    filesUsages.add(sourceFilePath);
                }

                if (!node.arguments().isEmpty()) {
                    for (Object argument : node.arguments()) {
                        if (argument instanceof MethodInvocation) {
                            MethodInvocation argNode = (MethodInvocation) argument;
                            return super.visit(argNode);
                        }
                    }
                }

                return !declaredByApi && super.visit(node);
            }

            @Override
            public boolean visit(ConstructorInvocation node) {

                if (node.resolveConstructorBinding() == null) {
                    return super.visit(node);
                }

                boolean declaredByApi = isDeclaredByApiClass(node.resolveConstructorBinding());
                String qualifiedName = node.resolveConstructorBinding().getDeclaringClass().getTypeDeclaration().getQualifiedName();

                if (qualifiedName.contains(" ")) {
                    recursiveCalls(node);
                }

                if (inLibraries(qualifiedName)) {
                    usages.add(qualifiedName);
                    lineNumbers.add(cuCopy.getLineNumber(node.getStartPosition()));
                    filesUsages.add(sourceFilePath);
                }

                return !declaredByApi && super.visit(node);
            }

            @Override
            public boolean visit(ClassInstanceCreation node) {

                if (node.resolveConstructorBinding() == null) {
                    return super.visit(node);
                }

                boolean declaredByApi = isDeclaredByApiClass(node.resolveConstructorBinding());
                String qualifiedName = node.resolveConstructorBinding().getDeclaringClass().getTypeDeclaration().getQualifiedName();

                if (qualifiedName.contains(" ")) {
                    recursiveCalls(node);
                }

                if (inLibraries(qualifiedName)) {
                    usages.add(qualifiedName);
                    lineNumbers.add(cuCopy.getLineNumber(node.getStartPosition()));
                    filesUsages.add(sourceFilePath);
                }

                if (!node.arguments().isEmpty()) {
                    for (Object argument : node.arguments()) {
                        if (argument instanceof MethodInvocation) {
                            MethodInvocation argNode = (MethodInvocation) argument;
                            return super.visit(argNode);
                        }
                    }
                }

                return !declaredByApi && super.visit(node);
            }

            @Override
            public boolean preVisit2(ASTNode node) {
                return super.preVisit2(node);
            }

            private boolean isDeclaredByApiClass(IMethodBinding methodBinding) {
                if (methodBinding != null) {
                    String name = methodBinding.getDeclaringClass().getTypeDeclaration().getQualifiedName();
                    return inLibraries(name);
                }
                return false;
            }
        });
        return false;
    }

    private boolean inLibraries(String name) {
        return classes.contains(name);
    }

    private boolean endsWithLibrary(String name) {
        for (String clazz : classes) {
            if (clazz.endsWith("." + name))
                return true;
        }
        return false;
    }

    private void recursiveCalls(ASTNode node) {
        matches(node);
    }


    public HashMap<String, ArrayList<String>> getUsagesCodes() {
        HashMap<String, ArrayList<String>> usagesMaps = new HashMap<>();

        for (int i = 0; i < usages.size(); i++) {
            String file = filesUsages.get(i);
            if (usagesMaps.containsKey(file)) {
                ArrayList<String> values = usagesMaps.get(file);
                values.add(usages.get(i));
                usagesMaps.replace(file, values);
            } else {
                ArrayList<String> newValues = new ArrayList<>();
                newValues.add(usages.get(i));
                usagesMaps.put(file, newValues);
            }
        }
        return usagesMaps;
    }

    public HashMap<String, ArrayList<Integer>> getUsagesLineNumbers() {
        HashMap<String, ArrayList<Integer>> usagesMaps = new HashMap<>();

        for (int i = 0; i < usages.size(); i++) {
            String file = filesUsages.get(i);
            if (usagesMaps.containsKey(file)) {
                ArrayList<Integer> values = usagesMaps.get(file);
                values.add(lineNumbers.get(i));
                usagesMaps.replace(file, values);
            } else {
                ArrayList<Integer> newValues = new ArrayList<>();
                newValues.add(lineNumbers.get(i));
                usagesMaps.put(file, newValues);
            }
        }
        return usagesMaps;
    }
}
