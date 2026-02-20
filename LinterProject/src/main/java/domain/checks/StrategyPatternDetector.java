package domain.checks;

import domain.ClassInfo;
import domain.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        return basicStrategyCheck(classInfo);
    }

    @Override
    public List<LintIssue> checkWithContext(ClassInfo classInfo, Map<String, ClassInfo> allClasses) {
        return advancedStrategyCheck(classInfo, allClasses);
    }

   private List<LintIssue> basicStrategyCheck(ClassInfo classInfo) {
    List<LintIssue> issues = new ArrayList<>();

    if (classInfo != null && classInfo.isInterface()) {
        issues.add(new LintIssue(
                getName(),
                Severity.INFO,
                "Class '" + classInfo.getName() + "' is an interface. This could be a Strategy interface (single-class analysis).",
                classInfo.getName()
        ));
    }

    return issues;
}

private List<LintIssue> advancedStrategyCheck(ClassInfo classInfo, Map<String, ClassInfo> allClasses) {
    List<LintIssue> issues = new ArrayList<>();

    if (classInfo == null || !classInfo.isInterface() || allClasses == null) {
        return issues;
    }

    // Ensure we only analyze interfaces that are actually in the read files (allClasses)
    ClassInfo iface = resolveClassStrict(classInfo.getName(), allClasses);
    if (iface == null || !iface.isInterface()) {
        return issues;
    }

    String ifaceInternal = toInternalName(iface.getName());

    List<String> implementers = new ArrayList<>();
    for (ClassInfo c : allClasses.values()) {
        if (c == null) continue;

        // "strictly classes": skip interfaces
        if (c.isInterface()) continue;

        for (String implemented : c.getInterfaces()) {
            if (toInternalName(implemented).equals(ifaceInternal)) {
                implementers.add(c.getName());
                break;
            }
        }
    }

    if (implementers.size() >= 2) {
        issues.add(new LintIssue(
                getName(),
                Severity.INFO,
                "Interface '" + iface.getName() + "' has multiple implementing classes in the analyzed files (" +
                        implementers.size() + "): " + String.join(", ", implementers) +
                        ". This strongly suggests a Strategy interface.",
                iface.getName()
        ));
    }else{
        issues.add(new LintIssue(
                getName(),
                Severity.WARNING,
                "Interface '" + iface.getName() + "' has only " + implementers.size() + " implementing class(es) in the analyzed files: " +
                        String.join(", ", implementers) +
                        ". This may be a Strategy interface, but either lacks implementation diversity or is not a full Strategy pattern.",
                iface.getName()
        ));
    }

    return issues;
}


private static ClassInfo resolveClassStrict(String typeName, Map<String, ClassInfo> allClasses) {
    if (typeName == null || allClasses == null) return null;

    ClassInfo ci = allClasses.get(typeName);
    if (ci != null) return ci;

    String stripped = stripDescriptor(typeName);

    ci = allClasses.get(stripped);
    if (ci != null) return ci;

    String internal = toInternalName(stripped);
    ci = allClasses.get(internal);
    if (ci != null) return ci;

    String dotted = toDotName(stripped);
    return allClasses.get(dotted);
}

    private static String stripDescriptor(String name) {
        // If someone passed "Lpkg/Foo;" return "pkg/Foo"
        if (name == null) return null;
        if (name.startsWith("L") && name.endsWith(";") && name.length() > 2) {
            return name.substring(1, name.length() - 1);
        }
        return name;
    }

    private static String toInternalName(String name) {
        name = stripDescriptor(name);
        if (name == null) return null;
        return name.replace('.', '/');
    }

    private static String toDotName(String name) {
        name = stripDescriptor(name);
        if (name == null) return null;
        return name.replace('/', '.');
    }
}
