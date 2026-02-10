package domain;

import domain.checks.LintIssue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages lint results and provides summary statistics.
 */
public class ResultManager {
    
    private final Map<String, List<LintIssue>> resultsByClass;
    private final LintSummary summary;

    public ResultManager() {
        this.resultsByClass = new HashMap<>();
        this.summary = new LintSummary();
    }

    public void addResult(String className, List<LintIssue> issues) {
        resultsByClass.computeIfAbsent(className, k -> new ArrayList<>()).addAll(issues);
        
        for (LintIssue issue : issues) {
            switch (issue.getSeverity()) {
                case ERROR:
                    summary.incrementErrorCount();
                    break;
                case WARNING:
                    summary.incrementWarningCount();
                    break;
                case INFO:
                    summary.incrementInfoCount();
                    break;
            }
        }
    }

    public List<LintIssue> getIssuesForClass(String className) {
        return resultsByClass.getOrDefault(className, new ArrayList<>());
    }

    public List<LintIssue> getAllIssues() {
        List<LintIssue> allIssues = new ArrayList<>();
        for (List<LintIssue> issues : resultsByClass.values()) {
            allIssues.addAll(issues);
        }
        return allIssues;
    }

    public LintSummary getSummary() {
        return summary;
    }

    public Map<String, List<LintIssue>> getResultsByClass() {
        return new HashMap<>(resultsByClass);
    }
}
