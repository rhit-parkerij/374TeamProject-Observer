package domain.checks;

import domain.ClassInfo;
import domain.FieldInfo;
import domain.MethodInfo;
import domain.MethodCall;
import domain.Severity;

import java.util.ArrayList;
import java.util.List;

/**
 * Pattern Detector: Decorator Pattern
 * 
 * Detects classes that implement an interface and hold a field
 * of that same interface type, delegating calls to it.
 * 
 */
public class DecoratorPatternDetector implements PatternCheck {

    @Override
    public String getName() {
        return "DecoratorPatternDetector";
    }

    @Override
    public String getDescription() {
        return "Detects the Decorator pattern (class wraps and delegates to a field of its own interface type)";
    }

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        List<LintIssue> issues = new ArrayList<>();

        // Skip interfaces and abstract classes
        if (classInfo.isInterface() || classInfo.isAbstract()) {
            return issues;
        }

        // Must implement at least one interface
        List<String> interfaces = classInfo.getInterfaces();
        if (interfaces.isEmpty()) {
            return issues;
        }

        // Check if any field's type matches an implemented interface
        for (FieldInfo field : classInfo.getFields()) {
            String fieldType = field.getTypeName();

            if (interfaces.contains(fieldType)) {
                // Found a field matching an interface
                int delegations = 0;
                for (MethodInfo method : classInfo.getMethods()) {
                    if (method.isConstructor()) {
                        continue;
                    }
                    for (MethodCall call : method.getMethodCalls()) {
                        if (call.getOwnerClassName().equals(fieldType)) {
                            delegations++;
                        }
                    }
                }

                if (delegations >= 1) {
                    issues.add(new LintIssue(
                        getName(),
                        Severity.INFO,
                        String.format("Possible Decorator: '%s' wraps field '%s' of type '%s' and delegates %d call(s)",
                            classInfo.getName(), field.getName(), fieldType, delegations),
                        classInfo.getName()
                    ));
                }
            }
        }

        return issues;
    }
}