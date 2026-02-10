package domain.checks;

import domain.ClassInfo;

import java.util.List;
import java.util.Map;

/**
 * Interface for Pattern checks (Pattern Detectors).
 * Pattern checks detect the presence of design patterns in code.
 * 
 * Pattern checks can access all classes in the codebase to detect
 * patterns that involve relationships between multiple classes
 * (e.g., Adapter, Decorator, Strategy).
 */
public interface PatternCheck extends LintCheck {
    
    /**
     * Check for pattern violations with access to all classes.
     * This allows detecting patterns that span multiple classes.
     * 
     * Default implementation falls back to single-class check.
     * Override this method if your pattern needs multi-class analysis.
     * 
     * @param classInfo The class being checked
     * @param allClasses Map of all classes (className -> ClassInfo)
     * @return List of issues found
     */
    default List<LintIssue> checkWithContext(ClassInfo classInfo, Map<String, ClassInfo> allClasses) {
        // Default: just use the single-class check
        return check(classInfo);
    }
}
