package datasource;

import domain.ClassInfo;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Source Layer: Responsible for loading and parsing Java class files.
 * Uses ASM library to read bytecode and create ClassInfo domain objects.
 */
public class ClassFileReader {

    /**
     * Load a class by its fully qualified name (e.g., "java.lang.String").
     * The class must be on the classpath.
     * 
     * This method tries two approaches:
     * 1. Direct ClassReader(className) - simple and fast (works in normal Java environments)
     * 2. Manual resource loading - fallback for Maven exec:java and other complex classloader scenarios
     */
    public ClassInfo loadClass(String className) throws IOException {
        ClassReader reader = null;
        
        // Approach 1: Try the simple direct method first (works in most cases)
        try {
            reader = new ClassReader(className);
        } catch (IOException e) {
            // Approach 2: Fallback to manual resource loading
            // This is needed when running via Maven exec:java or other environments
            // where the thread context classloader doesn't match the system classloader
            String resourcePath = className.replace('.', '/') + ".class";
            
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
            if (is == null) {
                is = ClassLoader.getSystemResourceAsStream(resourcePath);
            }
            if (is == null) {
                is = ClassFileReader.class.getClassLoader().getResourceAsStream(resourcePath);
            }
            if (is == null) {
                throw new IOException("Class not found: " + className + " (tried both direct loading and resource path: " + resourcePath + ")", e);
            }
            
            try {
                reader = new ClassReader(is);
            } finally {
                is.close();
            }
        }
        
        // Parse the class bytecode into ClassNode
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);
        return new ClassInfo(classNode);
    }

    /**
     * Load a class from a .class file path.
     */
    public ClassInfo loadClassFromFile(Path classFilePath) throws IOException {
        try (InputStream is = Files.newInputStream(classFilePath)) {
            ClassReader reader = new ClassReader(is);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.EXPAND_FRAMES);
            return new ClassInfo(classNode);
        }
    }

    /**
     * Load multiple classes by their names.
     */
    public List<ClassInfo> loadClasses(String[] classNames) throws IOException {
        List<ClassInfo> classes = new ArrayList<>();
        for (String className : classNames) {
            classes.add(loadClass(className));
        }
        return classes;
    }

    /**
     * Load all .class files from a directory (recursively).
     */
    public List<ClassInfo> loadClassesFromDirectory(String directoryPath) throws IOException {
        List<ClassInfo> classes = new ArrayList<>();
        Path dirPath = Paths.get(directoryPath);

        if (!Files.isDirectory(dirPath)) {
            throw new IOException("Not a valid directory: " + directoryPath);
        }

        Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".class")) {
                    classes.add(loadClassFromFile(file));
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return classes;
    }

    /**
     * Load a single .class file from a file path string.
     */
    public ClassInfo loadClassFromFilePath(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + filePath);
        }
        if (!filePath.endsWith(".class")) {
            throw new IOException("Not a .class file: " + filePath);
        }
        return loadClassFromFile(path);
    }
}
