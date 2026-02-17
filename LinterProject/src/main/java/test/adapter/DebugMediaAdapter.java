package test.adapter;

import datasource.ClassFileReader;
import domain.ClassInfo;
import domain.MethodInfo;
import domain.MethodCall;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Debug program to see what ASM actually detects in MediaAdapter
 */
public class DebugMediaAdapter {
    public static void main(String[] args) throws Exception {
        ClassFileReader reader = new ClassFileReader();
        Path targetDir = Paths.get("target/classes");
        ClassInfo mediaAdapter = reader.loadClassFromFile(
            targetDir.resolve("example/adapter/MediaAdapter.class"));
        
        System.out.println("Class: " + mediaAdapter.getName());
        System.out.println("Interfaces: " + mediaAdapter.getInterfaces());
        System.out.println("Fields:");
        mediaAdapter.getFields().forEach(f -> 
            System.out.println("  - " + f.getName() + ": " + f.getTypeName()));
        
        System.out.println("\nMethods:");
        for (MethodInfo method : mediaAdapter.getMethods()) {
            System.out.println("  " + method.getName() + "():");
            System.out.println("    Is constructor: " + method.isConstructor());
            System.out.println("    Method calls:");
            for (MethodCall call : method.getMethodCalls()) {
                System.out.println("      - " + call.getOwnerClassName() + "." + call.getName() + "()");
            }
        }
    }
}
