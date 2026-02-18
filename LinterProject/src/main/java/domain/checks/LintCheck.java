package domain.checks;

import domain.ClassInfo;

import java.util.List;
import java.util.Map;

/**
 * Strategy interface for all lint checks.
 * 
 * Each check implements this interface with its own detection logic.
 * This follows the Strategy pattern: the LinterEngine delegates
 * analysis to interchangeable check implementations.
 *
 * The default {@link #checkWithContext} method follows the Hollywood
 * Principle — subclasses that need multi-class context (like pattern
 * detectors) simply override it, without the engine needing to know.
 */
public interface LintCheck {
    
    /**
     * Run this check on a single class.
     */
    List<LintIssue> check(ClassInfo classInfo);

    /**
     * Run this check with access to all loaded classes.
     * Override this method if the check needs cross-class analysis
     * (e.g., Adapter or Decorator pattern detection).
     *
     * Default implementation delegates to {@link #check(ClassInfo)}.
     * This eliminates the need for {@code instanceof} checks in LinterEngine
     * (Hollywood Principle: "Don't call us, we'll call you").
     *
     * @param classInfo  the class being checked
     * @param allClasses map of all classes (className → ClassInfo)
     * @return list of issues found
     */
    default List<LintIssue> checkWithContext(ClassInfo classInfo, Map<String, ClassInfo> allClasses) {
        return check(classInfo);
    }

    String getName();

    String getDescription();
}
