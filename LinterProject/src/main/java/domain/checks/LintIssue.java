package domain.checks;

import domain.Severity;

public class LintIssue {
    String checkName;
    Severity severity;
    String message;

    public LintIssue(String checkName, Severity severity, String message){
        this.checkName = checkName;
        this.severity = severity;
        this.message = message;
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLocation'");
    }

}
