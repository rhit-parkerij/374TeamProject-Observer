package domain.checks;

import java.util.List;

import domain.ClassInfo;

/**
 * Pattern Detector: Adapter Pattern Check
 * Detects classes that look like the Adapter Pattern.
 * 
 * Adapter Pattern characteristics:
 * 1. Implements an interface (the Target)
 * 2. Has a field of a different type (the Adaptee)
 * 3. Delegates method calls to the adaptee
 * 
 * @author Sophia
 */

public class AdapterPatternCheck implements PatternCheck {

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'check'");
    }

    @Override
    public String getName() {
        return "AdapterPatternCheck";
    }

    @Override
    public String getDescription() {
        return "Detects classes that implement the Adapter Pattern.";
    }
}
