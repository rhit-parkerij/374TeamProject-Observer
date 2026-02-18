package domain;

import domain.checks.LintCheck;
import domain.checks.LintIssue;

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
     * 
     * Uses checkWithContext() for every check — checks that need
     * cross-class analysis override this method; others fall back
     * to the default single-class check(). This avoids instanceof
     * checks and follows the Hollywood Principle.
     *
     * @param classes The classes to analyze
     * @return All issues found by all checks
     */
    public List<LintIssue> analyzeAll(List<ClassInfo> classes) {
        List<LintIssue> allIssues = new ArrayList<>();
        
        // Register all classes so cross-class checks can look up dependencies
        classRegistry.clear();
        for (ClassInfo classInfo : classes) {
            classRegistry.put(classInfo.getName(), classInfo);
        }
        
        // Run all checks — no instanceof needed thanks to default method in LintCheck
        for (ClassInfo classInfo : classes) {
            for (LintCheck check : checks) {
                List<LintIssue> issues = check.checkWithContext(classInfo, classRegistry);
                allIssues.addAll(issues);
            }
        }
        
        return allIssues;
    }
}
