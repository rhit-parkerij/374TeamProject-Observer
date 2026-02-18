package domain.checks;

import domain.ClassInfo;
import domain.MethodInfo;
import domain.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Style Check: Method Naming Convention
 * 
 * Checks that method names follow Java camelCase convention:
 *   - Must start with a lowercase letter
 *   - Should be camelCase (no underscores, no leading uppercase)
 * 
 * Skips constructors, static initializers, and compiler generated
 * methods (names containing '$').
 * 
 */
public class MethodNamingCheck implements StyleCheck {

    private static final Pattern CAMEL_CASE = Pattern.compile("^[a-z][a-zA-Z0-9]*$");

    @Override
    public String getName() {
        return "MethodNamingCheck";
    }

    @Override
    public String getDescription() {
        return "Checks that method names follow camelCase convention";
    }

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        List<LintIssue> issues = new ArrayList<>();

        if (classInfo.isInterface()) {
            return issues;
        }

        for (MethodInfo method : classInfo.getMethods()) {
            // Skip constructors and static initializers
            if (method.isConstructor() || method.isStaticInitializer()) {
                continue;
            }

            // Skip compiler generated methods
            String name = method.getName();
            if (name.contains("$")) {
                continue;
            }

            if (!CAMEL_CASE.matcher(name).matches()) {
                String suggestion = get_suggestion(name);
                issues.add(new LintIssue(
                    getName(),
                    Severity.WARNING,
                    String.format("Method '%s' does not follow camelCase convention. %s",
                        name, suggestion),
                    classInfo.getName() + "." + name + "()"
                ));
            }
        }

        return issues;
    }

    private String get_suggestion(String name) {
        if (name.isEmpty()) {
            return "Method name should not be empty.";
        }
        if (Character.isUpperCase(name.charAt(0))) {
            return "Method names should start with a lowercase letter.";
        }
        if (name.contains("_")) {
            return "Method names should not contain underscores. Use camelCase instead.";
        }
        return "Java convention: method names use camelCase (e.g., getTotal, calculateSum).";
    }
}