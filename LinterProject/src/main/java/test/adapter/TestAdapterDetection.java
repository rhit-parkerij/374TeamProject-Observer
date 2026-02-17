package test.adapter;

import datasource.ClassFileReader;
import domain.ClassInfo;
import domain.LinterEngine;
import domain.checks.AdapterPatternDetector;
import domain.checks.LintIssue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Test program to demonstrate Adapter Pattern detection
 * with multi-class context
 */
public class TestAdapterDetection {
    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("ADAPTER PATTERN DETECTION TEST");
        System.out.println("========================================\n");
        
        // Read all three classes involved in the pattern from compiled .class files
        ClassFileReader reader = new ClassFileReader();
        
        // Assuming classes are in target/classes after compilation
        Path targetDir = Paths.get("target/classes");
        ClassInfo mediaPlayer = reader.loadClassFromFile(
            targetDir.resolve("example/adapter/MediaPlayer.class"));
        ClassInfo mp3Player = reader.loadClassFromFile(
            targetDir.resolve("example/adapter/Mp3Player.class"));
        ClassInfo mediaAdapter = reader.loadClassFromFile(
            targetDir.resolve("example/adapter/MediaAdapter.class"));
        
        System.out.println("Loaded classes:");
        System.out.println("  - " + mediaPlayer.getName() + " (interface: " + mediaPlayer.isInterface() + ")");
        System.out.println("  - " + mp3Player.getName());
        System.out.println("  - " + mediaAdapter.getName());
        System.out.println();
        
        // Create linter engine with Adapter Pattern detector
        LinterEngine engine = new LinterEngine();
        engine.addCheck(new AdapterPatternDetector());
        
        // Analyze all classes together (multi-class context)
        List<ClassInfo> allClasses = List.of(mediaPlayer, mp3Player, mediaAdapter);
        List<LintIssue> issues = engine.analyzeAll(allClasses);
        
        // Print results
        System.out.println("========================================");
        System.out.println("DETECTION RESULTS:");
        System.out.println("========================================");
        
        if (issues.isEmpty()) {
            System.out.println("No patterns detected.");
        } else {
            for (LintIssue issue : issues) {
                System.out.println("\n✓ " + issue.getCheckName());
                System.out.println("  Severity: " + issue.getSeverity());
                System.out.println("  Location: " + issue.getLocation());
                System.out.println("  Message: " + issue.getMessage());
            }
        }
        
        System.out.println("\n========================================");
        System.out.println("Expected output:");
        System.out.println("  HIGH confidence Adapter Pattern");
        System.out.println("  MediaAdapter implements MediaPlayer");
        System.out.println("  and delegates to Mp3Player");
        System.out.println("========================================");
    }
}
