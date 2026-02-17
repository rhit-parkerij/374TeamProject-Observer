package test.demo;

import datasource.ClassFileReader;
import domain.ClassInfo;
import domain.LinterEngine;
import domain.checks.*;
import domain.checks.LintIssue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Test runner for Wenxin's three checkers:
 * 1. FieldNamingCheck
 * 2. SingleResponsibilityCheck (with LCOM4)
 * 3. TemplateMethodDetector
 * 
 * This demonstrates all three checkers on demo classes.
 * 
 * @author Wenxin
 */
public class TestWenxinCheckers {
    
    public static void main(String[] args) throws Exception {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║    Testing Wenxin's Three Checkers                    ║");
        System.out.println("║    1. FieldNamingCheck                                ║");
        System.out.println("║    2. SingleResponsibilityCheck (with LCOM4)          ║");
        System.out.println("║    3. TemplateMethodDetector                          ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println();
        
        ClassFileReader reader = new ClassFileReader();
        
        // Auto-detect correct target directory - try multiple locations
        Path targetDir = Paths.get("target/classes");
        if (!targetDir.toFile().exists()) {
            // Try LinterProject subdirectory (when running from parent folder)
            targetDir = Paths.get("LinterProject/target/classes");
        }
        if (!targetDir.toFile().exists()) {
            // Try current directory (when running from VS Code)
            targetDir = Paths.get("").toAbsolutePath().resolve("target/classes");
        }
        
        if (!targetDir.toFile().exists()) {
            System.err.println("Error: Cannot find target/classes directory!");
            System.err.println("Current working directory: " + Paths.get("").toAbsolutePath());
            System.err.println("Tried locations:");
            System.err.println("  1. target/classes");
            System.err.println("  2. LinterProject/target/classes");
            System.err.println("  3. " + Paths.get("").toAbsolutePath().resolve("target/classes"));
            System.err.println("\nPlease run 'mvn compile' first from the project root.");
            System.exit(1);
        }
        
        System.out.println("Using target directory: " + targetDir.toAbsolutePath());
        System.out.println();
        
        // Create linter engine with Wenxin's three checkers
        LinterEngine engine = new LinterEngine();
        engine.addCheck(new FieldNamingCheck());
        engine.addCheck(new SingleResponsibilityCheck());
        engine.addCheck(new TemplateMethodDetector());
        
        // ═══════════════════════════════════════════════════════════
        //  TEST 1: FieldNamingCheck
        // ═══════════════════════════════════════════════════════════
        
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("TEST 1: FieldNamingCheck");
        System.out.println("═══════════════════════════════════════════════════════════\n");
        
        System.out.println("--- Testing BAD naming (Demo_FieldNamingViolation) ---");
        testClass(reader, targetDir, "test.demo.Demo_FieldNamingViolation", engine);
        
        System.out.println("\n--- Testing GOOD naming (Demo_FieldNamingGood) ---");
        testClass(reader, targetDir, "test.demo.Demo_FieldNamingGood", engine);
        
        // ═══════════════════════════════════════════════════════════
        //  TEST 2: SingleResponsibilityCheck (with LCOM4)
        // ═══════════════════════════════════════════════════════════
        
        System.out.println("\n\n═══════════════════════════════════════════════════════════");
        System.out.println("TEST 2: SingleResponsibilityCheck (with LCOM4)");
        System.out.println("═══════════════════════════════════════════════════════════\n");
        
        System.out.println("--- Testing God Class (Demo_GodClass) ---");
        testClass(reader, targetDir, "test.demo.Demo_GodClass", engine);
        
        System.out.println("\n--- Testing High Cohesion (Demo_HighCohesion) ---");
        testClass(reader, targetDir, "test.demo.Demo_HighCohesion", engine);
        
        // ═══════════════════════════════════════════════════════════
        //  TEST 3: TemplateMethodDetector
        // ═══════════════════════════════════════════════════════════
        
        System.out.println("\n\n═══════════════════════════════════════════════════════════");
        System.out.println("TEST 3: TemplateMethodDetector");
        System.out.println("═══════════════════════════════════════════════════════════\n");
        
        System.out.println("--- Testing Template Method (final) ---");
        testClass(reader, targetDir, "test.demo.Demo_TemplateMethodGood", engine);
        
        System.out.println("\n--- Testing Template Method (non-final) ---");
        testClass(reader, targetDir, "test.demo.Demo_TemplateMethodPossible", engine);
        
        System.out.println("\n\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║                   TESTING COMPLETE                     ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
    }
    
    private static void testClass(ClassFileReader reader, Path targetDir, 
                                   String className, LinterEngine engine) {
        try {
            // Load class from compiled .class file
            String classPath = className.replace('.', '/') + ".class";
            ClassInfo classInfo = reader.loadClassFromFile(targetDir.resolve(classPath));
            
            System.out.println("Analyzing: " + classInfo.getName());
            System.out.println("─────────────────────────────────────────────────────");
            
            // Run all checks
            List<LintIssue> issues = engine.analyzeAll(List.of(classInfo));
            
            if (issues.isEmpty()) {
                System.out.println("✓ No issues found - Clean code!");
            } else {
                System.out.println("Found " + issues.size() + " issue(s):\n");
                
                for (LintIssue issue : issues) {
                    String icon;
                    switch (issue.getSeverity()) {
                        case ERROR:
                            icon = "X ";
                            break;
                        case WARNING:
                            icon = "! ";
                            break;
                        case INFO:
                            icon = "i ";
                            break;
                        default:
                            icon = "- ";
                            break;
                    }
                    
                    System.out.println(icon + " [" + issue.getSeverity() + "] " + issue.getCheckName());
                    System.out.println("  Message: " + issue.getMessage());
                    System.out.println("  Location: " + issue.getLocation());
                    System.out.println();
                }
            }
            
        } catch (Exception e) {
            System.out.println("✗ Failed to analyze " + className);
            System.out.println("  Error: " + e.getMessage());
        }
    }
}
