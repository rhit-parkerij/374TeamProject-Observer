package domain.checks;

import domain.ClassInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Style Check: Field Naming Check
 * Checks if fields follow naming conventions.
 */
public class FieldNamingCheck implements StyleCheck {

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        // TODO: implement field naming check logic
        return new ArrayList<>();
    }

    @Override
    public String getName() {
        return "FieldNamingCheck";
    }

    @Override
    public String getDescription() {
        return "Checks if fields follow naming conventions.";
    }
}
