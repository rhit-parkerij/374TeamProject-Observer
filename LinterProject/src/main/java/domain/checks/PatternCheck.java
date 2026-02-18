package domain.checks;

/**
 * Interface for Pattern checks (Pattern Detectors).
 * Pattern checks detect the presence of design patterns in code.
 * 
 * Pattern checks can access all classes in the codebase to detect
 * patterns that involve relationships between multiple classes
 * (e.g., Adapter, Decorator, Strategy).
 *
 * Cross-class analysis is supported via {@link LintCheck#checkWithContext},
 * which is inherited from the parent interface. Pattern detectors that
 * need multi-class context simply override that method.
 */
public interface PatternCheck extends LintCheck {
    
}
