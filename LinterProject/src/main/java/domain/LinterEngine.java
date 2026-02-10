package domain;

import domain.checks.LintCheck;
import domain.checks.LintIssue;

import java.util.ArrayList;
import java.util.List;

public class LinterEngine {

    private final List<LintCheck> checks = new ArrayList<>();
    
    public LinterEngine(){

    }

    public void addCheck(LintCheck check){
        checks.add(check);
    }

    public List<LintCheck> getChecks(){
        return checks;
    }

    public List<LintIssue> analyze(ClassInfo classInfo){
        List<LintIssue> allIssues = new ArrayList<>();
        for (LintCheck check : checks) {
            List<LintIssue> issues = check.check(classInfo);
            if (issues != null) {
                allIssues.addAll(issues);
            }
        }
        return allIssues;
    }
}
