package domain.checks;

import domain.ClassInfo;
import domain.MethodInfo;
import domain.MethodCall;
import domain.Severity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Template Method Pattern Detector
 * Detects the Template Method pattern: abstract classes with a 
 * final template method that invokes abstract steps.
 */
public class TemplateMethodDetector implements PatternCheck {

    @Override
    public String getName() {
        return "TemplateMethodDetector";
    }

    @Override
    public String getDescription() {
        return "Detects the Template Method pattern (abstract class with final method invoking abstract steps)";
    }

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        List<LintIssue> issues = new ArrayList<>();

        // Template Method pattern requires an abstract class
        if (!classInfo.isAbstract() || classInfo.isInterface()) {
            return issues;
        }

        String internalName = classInfo.getInternalName();

        // Collect abstract method names defined in this class
        Set<String> abstractMethods = new HashSet<>();
        for (MethodInfo method : classInfo.getMethods()) {
            if (method.isAbstract()) {
                abstractMethods.add(method.getName());
            }
        }

        if (abstractMethods.isEmpty()) {
            return issues;
        }

        // Look for final methods that call this class's own abstract methods (template methods)
        for (MethodInfo method : classInfo.getMethods()) {
            if (method.isFinal() && !method.isAbstract() && 
                !method.isConstructor() && !method.isStaticInitializer()) {
                
                Set<String> calledAbstractMethods = findCalledAbstractMethods(
                        method, abstractMethods, internalName);

                if (calledAbstractMethods.size() >= 2) {
                    issues.add(new LintIssue(
                        getName(),
                        Severity.INFO,
                        String.format("Template Method pattern detected: '%s' is a template method calling abstract steps: %s",
                            method.getName(), calledAbstractMethods),
                        classInfo.getName() + "." + method.getName() + "()"
                    ));
                }
            }
        }

        // Also check non-final public/protected methods that call abstract methods
        // (some implementations don't use final keyword)
        for (MethodInfo method : classInfo.getMethods()) {
            if (!method.isFinal() && !method.isAbstract() && !method.isPrivate() &&
                !method.isConstructor() && !method.isStaticInitializer()) {
                
                Set<String> calledAbstractMethods = findCalledAbstractMethods(
                        method, abstractMethods, internalName);

                if (calledAbstractMethods.size() >= 2) {
                    issues.add(new LintIssue(
                        getName(),
                        Severity.INFO,
                        String.format("Possible Template Method pattern: '%s' calls abstract steps: %s (consider making it final)",
                            method.getName(), calledAbstractMethods),
                        classInfo.getName() + "." + method.getName() + "()"
                    ));
                }
            }
        }

        return issues;
    }

    /**
     * Find which of the class's own abstract methods are called by the given method.
     * Only counts calls where the owner matches the class itself (prevents false positives
     * from calls to other classes that happen to have methods with the same name).
     *
     * @param method           the concrete method to inspect
     * @param abstractMethods  set of abstract method names in the class
     * @param classInternalName internal name of the class (e.g. "test/demo/MyClass")
     * @return set of abstract method names called by this method
     */
    private Set<String> findCalledAbstractMethods(MethodInfo method,
                                                   Set<String> abstractMethods,
                                                   String classInternalName) {
        Set<String> called = new HashSet<>();
        for (MethodCall call : method.getMethodCalls()) {
            // Only count calls targeting this class's own abstract methods
            if (call.getOwner().equals(classInternalName)
                    && abstractMethods.contains(call.getName())) {
                called.add(call.getName());
            }
        }
        return called;
    }
}
