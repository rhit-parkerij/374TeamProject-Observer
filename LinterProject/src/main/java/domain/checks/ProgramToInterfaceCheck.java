package domain.checks;

import java.util.List;

import domain.ClassInfo;

/**
 * Principle Check: Program to Interface Check
 * Detects violations of "Program to Interface, not Implementation".
 * For example: using ArrayList instead of List, HashMap instead of Map.
 * 
 * @author Sophia
 */

public class ProgramToInterfaceCheck implements PrincipleCheck {

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'check'");
    }

    @Override
    public String getName() {
        return "ProgramToInterfaceCheck";
    }

    @Override
    public String getDescription() {
        return "Detects violations of 'Program to Interface, not Implementation'.";
    }
}
