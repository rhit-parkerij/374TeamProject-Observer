package domain;

import domain.checks.LintIssue;

import java.util.ArrayList;
import java.util.List;

public class ResultManager {

    private final List<LintIssue> allIssues = new ArrayList<>();
    
    public void addResult(String className, List<LintIssue> issues){
        if (issues != null) {
            allIssues.addAll(issues);
        }
    }

    public List<LintIssue> getAllIssues(){
        return allIssues;
    }

    public LintSummary getSummary(){
        //TODO: implement summary retrieval logic
        return null;
    }

}
