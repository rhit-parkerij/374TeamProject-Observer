package domain.checks;

import domain.ClassInfo;
import domain.FieldInfo;
import domain.MethodInfo;
import domain.Severity;
import domain.checks.LintIssue;

import java.util.ArrayList;
import java.util.List;

/**
 *Single Responsibility Principle
 * - A class should have only one reason to change
 * - A class should do one thing and do it well
 */
public class SingleResponsibilityCheck implements PrincipleCheck {

    private final int maxFields;
    private final int maxMethods;
    private final int maxPublicMethods;

    public SingleResponsibilityCheck() {
        this(10, 20, 10);
    }

    public SingleResponsibilityCheck(int maxFields, int maxMethods, int maxPublicMethods) {
        this.maxFields = maxFields;
        this.maxMethods = maxMethods;
        this.maxPublicMethods = maxPublicMethods;
    }

    @Override
    public String getName() {
        return "SingleResponsibilityPrinciple";
    }

    @Override
    public String getDescription() {
        return "Detects classes that may violate Single Responsibility Principle " +
               "(too many fields/methods indicates multiple responsibilities)";
    }

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        List<LintIssue> issues = new ArrayList<>();
        String className = classInfo.getName();

        // Skip interfaces
        if (classInfo.isInterface()) {
            return issues;
        }

        // Count fields (excluding synthetic)
        int fieldCount = 0;
        for (FieldInfo field : classInfo.getFields()) {
            if (!field.getName().contains("$")) {
                fieldCount++;
            }
        }

        // Count methods and public methods
        int methodCount = 0;
        int publicMethodCount = 0;
        for (MethodInfo method : classInfo.getMethods()) {
            if (method.isConstructor() || method.isStaticInitializer() || 
                method.getName().contains("$")) {
                continue;
            }
            methodCount++;
            if (method.isPublic()) {
                publicMethodCount++;
            }
        }

        // Check for too many fields
        if (fieldCount > maxFields) {
            issues.add(new LintIssue(
                getName(),
                Severity.WARNING,
                String.format("Class has " + fieldCount + " fields (recommended max: " + maxFields + "). " +
                "This may indicate the class has too many responsibilities. " +
                "Consider extracting related fields into separate classes."), 
                className
            ));
        }

        // Check for too many methods
        if (methodCount > maxMethods) {
            issues.add(new LintIssue(
                getName(),
                Severity.WARNING,
                String.format("Class has " + methodCount + " methods (recommended max: " + maxMethods + "). " +
                "This may indicate the class is doing too much. " +
                "Consider splitting into multiple focused classes."), 
                className
            ));
        }

        // Check for too many public methods (large public interface)
        if (publicMethodCount > maxPublicMethods) {
            issues.add(new LintIssue(
                getName(),
                Severity.WARNING,
                String.format("Class has " + publicMethodCount + " public methods (recommended max: " + maxPublicMethods + "). " +
                "The public interface is too large. " +
                "Consider using the Interface Segregation Principle to split the interface."), 
                className
            ));
        }

        // Severe case: both too many fields AND methods
        if (fieldCount > maxFields && methodCount > maxMethods) {
            issues.add(new LintIssue(
                getName(),
                Severity.ERROR,
                String.format("Class appears to be a 'God Class' with " + fieldCount + " fields and " + methodCount + " methods. " +
                "This strongly suggests multiple responsibilities. " +
                "Refactor by identifying distinct responsibilities and extracting them into separate classes."),
                className
            ));
        }

        return issues;
    }
}
