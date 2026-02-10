package domain;

import domain.checks.LintCheck;
import domain.checks.LintIssue;
import domain.checks.PatternCheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The main Linter engine that coordinates all checks.
 * This class uses the Composite pattern to run multiple checks on classes.
 */
public class LinterEngine {

    private final List<LintCheck> checks;
    private final Map<String, ClassInfo> classRegistry;

    public LinterEngine() {
        this.checks = new ArrayList<>();
        this.classRegistry = new HashMap<>();
    }

    /**
     * Register a check with the linter.
     */
    public void addCheck(LintCheck check) {
        checks.add(check);
    }

    /**
     * Remove a check from the linter.
     */
    public void removeCheck(LintCheck check) {
        checks.remove(check);
    }

    /**
     * Get all registered checks.
     */
    public List<LintCheck> getChecks() {
        return new ArrayList<>(checks);
    }

    /**
     * Run all registered checks on a single class.
     * @param classInfo The class to analyze
     * @return All issues found by all checks
     */
    public List<LintIssue> analyze(ClassInfo classInfo) {
        List<LintIssue> allIssues = new ArrayList<>();
        
        for (LintCheck check : checks) {
            List<LintIssue> issues = check.check(classInfo);
            allIssues.addAll(issues);
        }
        
        return allIssues;
    }

    /**
     * Run all registered checks on multiple classes.
     * @param classes The classes to analyze
     * @return All issues found by all checks
     */
    public List<LintIssue> analyzeAll(List<ClassInfo> classes) {
        List<LintIssue> allIssues = new ArrayList<>();
        
        // First, register all classes so pattern checks can look up dependencies
        classRegistry.clear();
        for (ClassInfo classInfo : classes) {
            classRegistry.put(classInfo.getName(), classInfo);
        }
        
        // Then run all checks
        for (ClassInfo classInfo : classes) {
            for (LintCheck check : checks) {
                // If it's a pattern check, give it access to all classes
                if (check instanceof PatternCheck) {
                    PatternCheck patternCheck = (PatternCheck) check;
                    List<LintIssue> issues = patternCheck.checkWithContext(classInfo, classRegistry);
                    allIssues.addAll(issues);
                } else {
                    // Regular checks only see one class at a time
                    List<LintIssue> issues = check.check(classInfo);
                    allIssues.addAll(issues);
                }
            }
        }
        
        return allIssues;
    }
}
