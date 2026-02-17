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
 * Detects classes that implement an interface or extend a class AND hold
 * a field of that same supertype, delegating calls to it.
 * 
 */
public class DecoratorPatternDetector implements PatternCheck {

    @Override
    public String getName() {
        return "DecoratorPatternDetector";
    }

    @Override
    public String getDescription() {
        return "Detects the Decorator pattern (class wraps and delegates to a field of its own supertype)";
    }

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        List<LintIssue> issues = new ArrayList<>();

        // Decorators are concrete classes
        if (classInfo.isInterface() || classInfo.isAbstract()) {
            return issues;
        }

        // Gather all supertypes: superclass + implemented interfaces
        List<String> supertypes = new ArrayList<>();
        if (classInfo.getSuperClassName() != null
                && !classInfo.getSuperClassName().equals("java.lang.Object")) {
            supertypes.add(classInfo.getSuperClassName());
        }
        supertypes.addAll(classInfo.getInterfaces());

        if (supertypes.isEmpty()) {
            return issues;
        }

        // Look for fields whose type matches one of our supertypes
        for (FieldInfo field : classInfo.getFields()) {
            String fieldType = field.getTypeName();

            for (String supertype : supertypes) {
                if (fieldType.equals(supertype)) {
                    int delegationCount = countDelegationsTo(classInfo, fieldType);

                    if (delegationCount >= 1) {
                        issues.add(new LintIssue(
                            getName(),
                            Severity.INFO,
                            String.format(
                                "Decorator pattern detected: '%s' wraps field '%s' of type '%s' "
                                + "and delegates %d call(s) to it",
                                getSimpleName(classInfo.getName()),
                                field.getName(),
                                getSimpleName(supertype),
                                delegationCount),
                            classInfo.getName()
                        ));
                    }
                    break; // Don't double-report for same field
                }
            }
        }

        return issues;
    }

    private int countDelegationsTo(ClassInfo classInfo, String targetType) {
        int count = 0;
        for (MethodInfo method : classInfo.getMethods()) {
            if (method.isConstructor() || method.isStaticInitializer()) {
                continue;
            }
            for (MethodCall call : method.getMethodCalls()) {
                if (call.getOwnerClassName().equals(targetType)) {
                    count++;
                }
            }
        }
        return count;
    }

    private String getSimpleName(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;
    }
}