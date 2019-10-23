package com.vub.be.utils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

public class Utils {

    public Utils() {}

    public static ASTNode parseSource(String source, String path, String name, String[] classpaths) {
        Map options = JavaCore.getOptions();
        options.put("org.eclipse.jdt.core.compiler.compliance", "1.8");
        options.put("org.eclipse.jdt.core.compiler.codegen.targetPlatform", "1.8");
        options.put("org.eclipse.jdt.core.compiler.source", "1.8");
        String srcDir = getSrcDir(source, path, name);
        ASTParser parser = ASTParser.newParser(8);
        parser.setCompilerOptions(options);
        parser.setEnvironment(classpaths == null ? new String[0] : classpaths, new String[]{srcDir}, new String[]{"UTF-8"}, true);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);
        parser.setSource(source.toCharArray());
        parser.setUnitName(name);
        return parser.createAST((IProgressMonitor)null);
    }

    private static String getSrcDir(String source, String path, String name) {
        Map options = JavaCore.getOptions();
        options.put("org.eclipse.jdt.core.compiler.compliance", "1.8");
        options.put("org.eclipse.jdt.core.compiler.codegen.targetPlatform", "1.8");
        options.put("org.eclipse.jdt.core.compiler.source", "1.8");
        ASTParser parser = ASTParser.newParser(8);
        parser.setCompilerOptions(options);
        parser.setSource(source.toCharArray());
        ASTNode ast = parser.createAST((IProgressMonitor)null);
        CompilationUnit cu = (CompilationUnit)ast;
        String srcDir = path;
        if (cu.getPackage() != null) {
            String p = cu.getPackage().getName().getFullyQualifiedName();
            int end = path.length() - p.length() - 1 - name.length();
            if (end > 0) {
                srcDir = path.substring(0, end);
            }
        } else {
            int end = path.length() - name.length();
            if (end > 0) {
                srcDir = path.substring(0, end);
            }
        }

        return srcDir;
    }

    public static String readStringFromFile(String inputFile) {
        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(inputFile));
            byte[] bytes = new byte[(int)(new File(inputFile)).length()];
            in.read(bytes);
            in.close();
            return new String(bytes);
        } catch (Exception var3) {
            var3.printStackTrace();
            return null;
        }
    }
}
