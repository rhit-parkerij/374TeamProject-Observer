package domain.checks;

import domain.Severity;

public class LintIssue {
    private final String checkName;
    private final Severity severity;
    private final String message;
    private final String location;

    public LintIssue(String checkName, Severity severity, String message, String location){
        this.checkName = checkName;
        this.severity = severity;
        this.message = message;
        this.location = location;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getCheckName() {
        return checkName;
    }

    public String getMessage() {
        return message;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s (at %s)", severity, checkName, message, location);
    }

}
