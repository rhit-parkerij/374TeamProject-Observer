package domain.checks;

import domain.ClassInfo;
import domain.FieldInfo;
import domain.MethodInfo;
import domain.MethodCall;
import domain.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pattern Detector: Strategy Pattern Check
 * Detects classes that look like the Strategy Pattern.
 * 
 * Strategy Pattern characteristics:
 * 1. Has a field of an interface type (the Strategy)
 * 2. The field is assigned an instance of a class that implements the Strategy interface
 * 3. Delegates method calls to the Strategy instance
 * 
 * This detector will use multi-class analysis to verify:
 * - The Strategy interface actually exists
 * - The Strategy implementation classes actually exist
 * - The context class delegates to the Strategy instance
 * 
 * Isaac Parker
 * 
 * */

public class StrategyPatternDetector implements PatternCheck {
    @Override
    public String getName() {
        return "StrategyPatternDetector";
    }

    @Override
    public String getDescription() {
        return "Detects classes that implement the Strategy Pattern";
    }

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        // Single-class check: limited analysis without knowing other classes
        return basicStrategyCheck(classInfo);
    }

    @Override
    public List<LintIssue> checkWithContext(ClassInfo classInfo, Map<String, ClassInfo> allClasses) {
        // Multi-class check: can verify Strategy interface and implementations actually exist
        return advancedStrategyCheck(classInfo, allClasses);
    }

    /**
     * Basic single-class strategy detection (fallback when no context available)
     */

    private List<LintIssue> basicStrategyCheck(ClassInfo classInfo) {
        List<LintIssue> issues = new ArrayList<>();

        // Check for fields that are of an interface type (potential Strategy)
        for (FieldInfo field : classInfo.getFields()) {
            // Only consider reference/object types (descriptor starts with 'L')
            if (!field.getDescriptor().startsWith("L")) {
                continue;
            }
            // Without context we cannot know whether the type is an interface,
            // so report it as a potential Strategy candidate.
            String typeName = field.getTypeName();
            if(typeName == null || typeName.isEmpty()) {
                continue;}
                else if(typeName.contains("interface")) {
                    issues.add(new LintIssue(
                    getName(),
                    Severity.WARNING,
                    "Found a field of type '" + typeName + "'. This could be a Strategy, but we cannot verify without analyzing other classes.",
                    classInfo.getName()
                ));
                }
                
        }

        return issues;
    }

    /**
     * Advanced multi-class strategy detection
     */

    private List<LintIssue> advancedStrategyCheck(ClassInfo classInfo, Map<String, ClassInfo> allClasses) {
        List<LintIssue> issues = new ArrayList<>();

        // Check for fields that are of an interface type (potential Strategy)
        for (FieldInfo field : classInfo.getFields()) {
            // Only consider reference/object types
            if (!field.getDescriptor().startsWith("L")) {
                continue;
            }

            String strategyInterfaceName = field.getTypeName();
            ClassInfo strategyInterface = allClasses.get(strategyInterfaceName);
            if (strategyInterface == null) {
                issues.add(new LintIssue(
                    getName(),
                    Severity.ERROR,
                    "Field '" + field.getName() + "' is of type '" + strategyInterfaceName + "', but no such type was found in the codebase.",
                    classInfo.getName()
                ));
                continue;
            }

            // Verify the referenced type is actually an interface before treating it as a Strategy
            if (!strategyInterface.isInterface()) {
                // Not an interface, skip Strategy-specific checks
                continue;
            }

            // Check if there are any classes that implement this Strategy interface
            boolean hasImplementations = false;
            for (ClassInfo potentialImpl : allClasses.values()) {
                if (potentialImpl.getInterfaces().contains(strategyInterfaceName)) {
                    hasImplementations = true;
                    break;
                }
            }

            if (!hasImplementations) {
                issues.add(new LintIssue(
                        getName(),
                        Severity.WARNING,
                        "Field '" + field.getName() + "' is of interface type '" + strategyInterfaceName + "', but no classes were found that implement this interface.",
                        classInfo.getName()
                ));
            }

            // Check if the context class delegates to the Strategy instance
            boolean delegatesToStrategy = false;
            for (MethodInfo method : classInfo.getMethods()) {
                for (MethodCall call : method.getMethodCalls()) {
                    if (call.getOwnerClassName().equals(strategyInterfaceName)) {
                        delegatesToStrategy = true;
                        break;
                    }
                }
                if (delegatesToStrategy) {
                    break;
                }
            }

            if (!delegatesToStrategy) {
                issues.add(new LintIssue(
                        getName(),
                        Severity.WARNING,
                        "Field '" + field.getName() + "' is of interface type '" + strategyInterfaceName + "', but no method calls were found that delegate to this field.",
                        classInfo.getName()
                ));
            }
        }

        return issues;

    }
}
