package domain.checks;

import domain.ClassInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Pattern Detector: Adapter Pattern Detector
 * Detects classes that look like the Adapter Pattern.
 */
public class AdapterPatternDetector implements PatternCheck {

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        // TODO: implement adapter pattern detection logic
        return new ArrayList<>();
    }

    @Override
    public String getName() {
        return "AdapterPatternDetector";
    }

    @Override
    public String getDescription() {
        return "Detects classes that implement the Adapter Pattern.";
    }
}
