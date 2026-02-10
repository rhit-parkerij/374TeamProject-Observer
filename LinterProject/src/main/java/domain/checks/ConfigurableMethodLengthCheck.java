package domain.checks;

import java.util.List;

import domain.ClassInfo;

/**
 * Style Check: Configurable Method Length Check
 * Checks if a method exceeds a certain number of instructions.
 * 
 * @author Sophia
 */

public class ConfigurableMethodLengthCheck implements StyleCheck {
    private int maxInstructions;

    public ConfigurableMethodLengthCheck(int maxInstructions) {
        this.maxInstructions = maxInstructions;
    }

    public ConfigurableMethodLengthCheck() {
        //TODO Auto-generated constructor stub
    }

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'check'");
    }

    @Override
    public String getName() {
        return "ConfigurableMethodLengthCheck";
    }

    @Override
    public String getDescription() {
        return "Checks if a method exceeds a configurable number of instructions.";
    }
}
