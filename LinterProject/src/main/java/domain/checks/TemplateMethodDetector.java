package domain.checks;

import domain.ClassInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Pattern Detector: Template Method Detector
 * Detects classes that look like the Template Method Pattern.
 */
public class TemplateMethodDetector implements PatternCheck {

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        // TODO: implement template method detection logic
        return new ArrayList<>();
    }

    @Override
    public String getName() {
        return "TemplateMethodDetector";
    }

    @Override
    public String getDescription() {
        return "Detects classes that implement the Template Method Pattern.";
    }
}
