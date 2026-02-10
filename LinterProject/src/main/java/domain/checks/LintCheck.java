package domain.checks;

import domain.ClassInfo;

import java.util.List;

public interface LintCheck {
    
    public List<LintIssue> check(ClassInfo classInfo);

    public String getName();

    public String getDescription();
}
