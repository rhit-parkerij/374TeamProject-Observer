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
     */
    public ClassInfo loadClass(String className) throws IOException {
        ClassReader reader = new ClassReader(className);
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
