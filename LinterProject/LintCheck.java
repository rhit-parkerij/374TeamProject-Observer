import java.util.List;

public interface LintCheck {
    
    public List<LintIssue> check(ClassInfo classInfo);
}
