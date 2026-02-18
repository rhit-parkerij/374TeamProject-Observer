package presentation;

import domain.Severity;
import domain.checks.LintIssue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Presentation Layer: Responsible for formatting and displaying lint results.
 * This class handles all console output formatting.
 */
public class ConsoleReporter {

    /**
     * Print all issues to the console.
     */
    public void report(List<LintIssue> issues) {
        if (issues.isEmpty()) {
            System.out.println(":) No issues found!");
            return;
        }

        System.out.println("Found " + issues.size() + " issue(s):\n");

        // Group issues by severity
        Map<Severity, List<LintIssue>> bySeverity = issues.stream()
            .collect(Collectors.groupingBy(LintIssue::getSeverity));

        // Print errors first
        if (bySeverity.containsKey(Severity.ERROR)) {
            System.out.println("=== ERRORS ===");
            for (LintIssue issue : bySeverity.get(Severity.ERROR)) {
                printIssue(issue);
            }
            System.out.println();
        }

        // Then warnings
        if (bySeverity.containsKey(Severity.WARNING)) {
            System.out.println("=== WARNINGS ===");
            for (LintIssue issue : bySeverity.get(Severity.WARNING)) {
                printIssue(issue);
            }
            System.out.println();
        }

        // Then info
        if (bySeverity.containsKey(Severity.INFO)) {
            System.out.println("=== INFO ===");
            for (LintIssue issue : bySeverity.get(Severity.INFO)) {
                printIssue(issue);
            }
            System.out.println();
        }

        // Print summary
        printSummary(bySeverity);
    }

    private void printIssue(LintIssue issue) {
        System.out.println(" [" + issue.getCheckName() + "] " + issue.getMessage());
        System.out.println("   at " + issue.getLocation());
    }

    private void printSummary(Map<Severity, List<LintIssue>> bySeverity) {
        int errors = bySeverity.getOrDefault(Severity.ERROR, List.of()).size();
        int warnings = bySeverity.getOrDefault(Severity.WARNING, List.of()).size();
        int infos = bySeverity.getOrDefault(Severity.INFO, List.of()).size();

        System.out.println("─────────────────────────────────────");
        System.out.println("Summary: " + errors + " error(s), " + warnings + " warning(s), " + infos + " info(s)");
    }
}
