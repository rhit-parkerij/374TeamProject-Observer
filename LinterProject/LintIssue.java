public class LintIssue {
    String checkName;
    Severity severity;
    String message;

    public LintIssue(String checkName, Severity severity, String message){
        this.checkName = checkName;
        this.severity = severity;
        this.message = message;
    }

}