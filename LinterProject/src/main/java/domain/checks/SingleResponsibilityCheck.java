package domain.checks;

import domain.ClassInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Principle Check: Single Responsibility Check
 * Checks if a class has too many responsibilities.
 */
public class SingleResponsibilityCheck implements PrincipleCheck {

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        // TODO: implement single responsibility check logic
        return new ArrayList<>();
    }

    @Override
    public String getName() {
        return "SingleResponsibilityCheck";
    }

    @Override
    public String getDescription() {
        return "Checks if a class follows the Single Responsibility Principle.";
    }
}
