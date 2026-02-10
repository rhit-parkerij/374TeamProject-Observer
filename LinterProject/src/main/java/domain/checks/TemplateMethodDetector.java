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

        // Collect abstract method names
        Set<String> abstractMethods = new HashSet<>();
        for (MethodInfo method : classInfo.getMethods()) {
            if (method.isAbstract()) {
                abstractMethods.add(method.getName());
            }
        }

        if (abstractMethods.isEmpty()) {
            return issues;
        }

        // Look for final methods that call abstract methods (template methods)
        for (MethodInfo method : classInfo.getMethods()) {
            if (method.isFinal() && !method.isAbstract() && 
                !method.isConstructor() && !method.isStaticInitializer()) {
                
                // Check if this final method calls abstract methods
                Set<String> calledAbstractMethods = new HashSet<>();
                for (MethodCall call : method.getMethodCalls()) {
                    // Check if the call is to one of the class's own abstract methods
                    if (abstractMethods.contains(call.getName())) {
                        calledAbstractMethods.add(call.getName());
                    }
                }

                if (calledAbstractMethods.size() >= 2) {
                    issues.add(new LintIssue(
                        getName(),
                        Severity.INFO,
                        String.format("Template Method pattern detected: '%s' is a template method calling abstract steps: %s in %s",
                            method.getName(), calledAbstractMethods, classInfo.getName() + "." + method.getName() + "()")
                    ));
                }
            }
        }

        // Also check non-final public/protected methods that call abstract methods
        // (some implementations don't use final keyword)
        for (MethodInfo method : classInfo.getMethods()) {
            if (!method.isFinal() && !method.isAbstract() && !method.isPrivate() &&
                !method.isConstructor() && !method.isStaticInitializer()) {
                
                Set<String> calledAbstractMethods = new HashSet<>();
                for (MethodCall call : method.getMethodCalls()) {
                    if (abstractMethods.contains(call.getName())) {
                        calledAbstractMethods.add(call.getName());
                    }
                }

                if (calledAbstractMethods.size() >= 2) {
                    issues.add(new LintIssue(
                        getName(),
                        Severity.INFO,
                        String.format("Possible Template Method pattern: '%s' calls abstract steps: %s (consider making it final) in %s",
                            method.getName(), calledAbstractMethods, classInfo.getName() + "." + method.getName() + "()")
                    ));
                }
            }
        }

        return issues;
    }
}
