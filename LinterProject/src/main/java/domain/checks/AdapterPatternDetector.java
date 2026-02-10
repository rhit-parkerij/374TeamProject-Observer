package domain.checks;

import domain.ClassInfo;
import domain.FieldInfo;
import domain.MethodInfo;
import domain.MethodCall;
import domain.Severity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pattern Detector: Adapter Pattern Check
 * Detects classes that look like the Adapter Pattern.
 * 
 * Adapter Pattern characteristics:
 * 1. Implements an interface (the Target)
 * 2. Has a field of a different type (the Adaptee)
 * 3. Delegates method calls to the adaptee
 * 
 * This detector now uses multi-class analysis to verify:
 * - The Target interface actually exists
 * - The Adaptee class actually exists
 * - The adapter delegates Target methods to Adaptee methods
 * 
 * @author Sophia
 */
public class AdapterPatternDetector implements PatternCheck {

    @Override
    public String getName() {
        return "AdapterPatternDetector";
    }

    @Override
    public String getDescription() {
        return "Detects classes that implement the Adapter Pattern";
    }

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        // Single-class check: limited analysis without knowing other classes
        return basicAdapterCheck(classInfo);
    }

    @Override
    public List<LintIssue> checkWithContext(ClassInfo classInfo, Map<String, ClassInfo> allClasses) {
        // Multi-class check: can verify Target and Adaptee actually exist
        return advancedAdapterCheck(classInfo, allClasses);
    }

    /**
     * Basic single-class adapter detection (fallback when no context available)
     */
    private List<LintIssue> basicAdapterCheck(ClassInfo classInfo) {
        List<LintIssue> issues = new ArrayList<>();

        // Skip interfaces and abstract classes
        if (classInfo.isInterface() || classInfo.isAbstract()) {
            return issues;
        }

        // Must implement at least one interface (the Target)
        List<String> interfaces = classInfo.getInterfaces();
        if (interfaces.isEmpty()) {
            return issues;
        }

        // Collect non-primitive field types as potential adaptees
        Set<String> potentialAdaptees = new HashSet<>();
        for (FieldInfo field : classInfo.getFields()) {
            String fieldType = field.getTypeName();
            // Skip primitive types and common JDK types
            if (!isPrimitive(fieldType) && !isCommonJdkType(fieldType)) {
                potentialAdaptees.add(fieldType);
            }
        }

        if (potentialAdaptees.isEmpty()) {
            return issues;
        }

        // Check if methods delegate to the adaptee
        for (String adapteeType : potentialAdaptees) {
            int delegationCount = countDelegationsTo(classInfo, adapteeType);
            
            // At least 1 delegation indicates adapter pattern
            if (delegationCount >= 1) {
                issues.add(new LintIssue(
                    getName(),
                    Severity.INFO,
                    String.format("Class appears to be an Adapter: implements %s and delegates %d call(s) to %s",
                        interfaces.get(0), delegationCount, adapteeType),
                    classInfo.getName()
                ));
            }
        }

        return issues;
    }

    /**
     * Advanced multi-class adapter detection with context
     */
    private List<LintIssue> advancedAdapterCheck(ClassInfo classInfo, Map<String, ClassInfo> allClasses) {
        List<LintIssue> issues = new ArrayList<>();

        // Skip interfaces and abstract classes
        if (classInfo.isInterface() || classInfo.isAbstract()) {
            return issues;
        }

        // Must implement at least one interface (the Target)
        List<String> interfaces = classInfo.getInterfaces();
        if (interfaces.isEmpty()) {
            return issues;
        }

        // Collect non-primitive field types as potential adaptees
        Set<String> potentialAdaptees = new HashSet<>();
        for (FieldInfo field : classInfo.getFields()) {
            String fieldType = field.getTypeName();
            // Skip primitive types and common JDK types
            if (!isPrimitive(fieldType) && !isCommonJdkType(fieldType)) {
                potentialAdaptees.add(fieldType);
            }
        }

        if (potentialAdaptees.isEmpty()) {
            return issues;
        }

        // Check each potential adaptee
        for (String adapteeType : potentialAdaptees) {
            int delegationCount = countDelegationsTo(classInfo, adapteeType);
            
            // Adapter pattern: at least 1 delegation (can be very simple adapter)
            if (delegationCount >= 1) {
                // Enhanced: Check if adaptee class exists in codebase
                ClassInfo adapteeClass = allClasses.get(adapteeType);
                boolean adapteeExists = adapteeClass != null;
                
                // Check if target interface exists in codebase
                String targetInterface = interfaces.get(0);
                ClassInfo targetClass = allClasses.get(targetInterface);
                boolean targetExists = targetClass != null && targetClass.isInterface();
                
                String confidence = "HIGH";
                if (!adapteeExists || !targetExists) {
                    confidence = "MEDIUM (external classes)";
                }
                
                issues.add(new LintIssue(
                    getName(),
                    Severity.INFO,
                    String.format("Adapter Pattern detected [%s]: '%s' implements '%s' and delegates %d call(s) to '%s'",
                        confidence, 
                        getSimpleName(classInfo.getName()), 
                        getSimpleName(targetInterface), 
                        delegationCount, 
                        getSimpleName(adapteeType)),
                    classInfo.getName()
                ));
            }
        }

        return issues;
    }

    private String getSimpleName(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;
    }

    private int countDelegationsTo(ClassInfo classInfo, String adapteeType) {
        int count = 0;

        for (MethodInfo method : classInfo.getMethods()) {
            if (method.isConstructor() || method.isStaticInitializer()) {
                continue;
            }

            for (MethodCall call : method.getMethodCalls()) {
                if (call.getOwnerClassName().equals(adapteeType)) {
                    count++;
                }
            }
        }

        return count;
    }

    private boolean isPrimitive(String typeName) {
        return typeName.equals("int") || typeName.equals("long") ||
               typeName.equals("double") || typeName.equals("float") ||
               typeName.equals("boolean") || typeName.equals("char") ||
               typeName.equals("byte") || typeName.equals("short");
    }

    private boolean isCommonJdkType(String typeName) {
        return typeName.startsWith("java.lang.") ||
               typeName.startsWith("java.util.") ||
               typeName.startsWith("java.io.");
    }
}
