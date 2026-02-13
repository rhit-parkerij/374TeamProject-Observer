package domain.checks;

import domain.ClassInfo;
import domain.FieldInfo;
import domain.Severity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Check: Open/Closed Principle Check (Class-level only)
 *
 * Detects classes that may violate OCP using ONLY class metadata:
 * - class/interface/abstract status
 * - fields (composition)
 * - inheritance/interfaces relationships
 *
 * Heuristics (since we are NOT inspecting method bodies):
 * 1) Concrete class that composes many OTHER concrete classes (tight coupling)
 * 2) Concrete class has many reference fields but few/none are interfaces/abstracts
 * 3) Interface that has many implementations: INFO (good extensibility signal)
 *
 * "Read files only": only types found in allClasses are considered.
 *
 * @author Isaac Parker
 */
public class OpenClosePrincipleCheck implements PatternCheck {

    @Override
    public String getName() {
        return "OpenClosePrincipleCheck";
    }

    @Override
    public String getDescription() {
        return "Detects potential Open/Closed Principle issues using class-level coupling/abstraction heuristics";
    }

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        // Without context you can't resolve referenced types to determine abstract/interface/concrete reliably.
        // Keep it class-only: only report for interfaces (potential extension points).
        List<LintIssue> issues = new ArrayList<>();
        if (classInfo != null && classInfo.isInterface()) {
            issues.add(new LintIssue(
                    getName(),
                    Severity.INFO,
                    "Interface '" + classInfo.getName() + "' is a potential extension point (supports Open/Closed Principle).",
                    classInfo.getName()
            ));
        }
        return issues;
    }

    @Override
    public List<LintIssue> checkWithContext(ClassInfo classInfo, Map<String, ClassInfo> allClasses) {
        List<LintIssue> issues = new ArrayList<>();
        if (classInfo == null || allClasses == null) return issues;

        // Enforce "read files only"
        ClassInfo self = resolveClassStrict(classInfo.getName(), allClasses);
        if (self == null) return issues;

        // If this is an interface: report if it has multiple implementing classes (good OCP extensibility)
        if (self.isInterface()) {
            List<String> implementers = findImplementingClasses(self, allClasses);
            if (implementers.size() >= 2) {
                issues.add(new LintIssue(
                        getName(),
                        Severity.INFO,
                        "Interface '" + self.getName() + "' has multiple implementing classes (" + implementers.size() +
                                "): " + String.join(", ", implementers) +
                                ". This supports extensibility (Open/Closed).",
                        self.getName()
                ));
            }
            return issues;
        }

        // Concrete class coupling/abstraction heuristics
        if (isAbstract(self)) {
            // Abstract classes are usually OCP-friendly; don't warn by default.
            return issues;
        }

        int refFields = 0;
        int refFieldsToAbstractions = 0;
        Set<String> concreteDeps = new HashSet<>();

        for (FieldInfo f : self.getFields()) {
            if (!isObjectRef(f.getDescriptor())) continue;

            refFields++;
            String typeName = f.getTypeName();
            if (typeName == null || typeName.isEmpty()) continue;

            ClassInfo dep = resolveClassStrict(typeName, allClasses);
            if (dep == null) {
                // Not in read files -> ignore (your requirement)
                continue;
            }

            if (dep.isInterface() || isAbstract(dep)) {
                refFieldsToAbstractions++;
            } else {
                // Concrete dependency in the analyzed codebase
                concreteDeps.add(dep.getName());
            }
        }

        // Heuristic thresholds (tune to your codebase):
        // - Many concrete dependencies => likely needs modification to extend behavior
        // - Few/none abstraction-typed fields => rigid design
        int CONCRETE_DEP_THRESHOLD = 3;     // tweak as needed
        int REFFIELD_THRESHOLD = 4;         // tweak as needed

        if (concreteDeps.size() >= CONCRETE_DEP_THRESHOLD) {
            issues.add(new LintIssue(
                    getName(),
                    Severity.WARNING,
                    "Class '" + self.getName() + "' composes many concrete dependencies (" + concreteDeps.size() +
                            "): " + String.join(", ", concreteDeps) +
                            ". This can reduce extensibility and may violate Open/Closed Principle.",
                    self.getName()
            ));
        }

        if (refFields >= REFFIELD_THRESHOLD && refFieldsToAbstractions == 0) {
            issues.add(new LintIssue(
                    getName(),
                    Severity.WARNING,
                    "Class '" + self.getName() + "' has many reference fields (" + refFields +
                            ") but none are typed to interfaces/abstract classes in the analyzed files. " +
                            "This suggests tight coupling and may violate Open/Closed Principle.",
                    self.getName()
            ));
        }

        return issues;
    }

    // ---------------- helpers ----------------

    private static boolean isObjectRef(String descriptor) {
        return descriptor != null && descriptor.startsWith("L");
    }

    /**
     * If your ClassInfo has isAbstract(), use that instead.
     * This fallback assumes you might store access flags or similar.
     */
    private static boolean isAbstract(ClassInfo c) {
        // If you already have: return c.isAbstract();
        // Otherwise keep it conservative:
        try {
            // reflection-safe-ish pattern if method exists
            return (boolean) c.getClass().getMethod("isAbstract").invoke(c);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static List<String> findImplementingClasses(ClassInfo iface, Map<String, ClassInfo> allClasses) {
        String ifaceInternal = toInternalName(iface.getName());
        List<String> implementers = new ArrayList<>();

        for (ClassInfo c : allClasses.values()) {
            if (c == null) continue;
            if (c.isInterface()) continue; // strictly classes

            for (String implemented : c.getInterfaces()) {
                if (toInternalName(implemented).equals(ifaceInternal)) {
                    implementers.add(c.getName());
                    break;
                }
            }
        }
        return implementers;
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
        if (name == null) return null;
        if (name.startsWith("L") && name.endsWith(";") && name.length() > 2) {
            return name.substring(1, name.length() - 1);
        }
        return name;
    }

    private static String toInternalName(String name) {
        name = stripDescriptor(name);
        return name == null ? null : name.replace('.', '/');
    }

    private static String toDotName(String name) {
        name = stripDescriptor(name);
        return name == null ? null : name.replace('/', '.');
    }
}
