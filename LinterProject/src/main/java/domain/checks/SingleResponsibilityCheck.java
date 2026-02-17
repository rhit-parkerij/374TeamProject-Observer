package domain.checks;

import domain.ClassInfo;
import domain.FieldInfo;
import domain.MethodInfo;
import domain.Severity;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Principle Check: Single Responsibility Check (Enhanced with LCOM4)
 *
 * <p>Basic detection: flags God Classes with too many methods/fields.
 *
 * <p><b>A-Level Feature: LCOM4 Cohesion Analysis</b>
 * Uses the LCOM4 metric (Henderson-Sellers, 1996) to measure class cohesion
 * via graph connectivity. The algorithm builds an undirected graph where nodes
 * are methods and edges connect methods that share at least one instance field.
 * The number of connected components equals the LCOM4 value.
 *
 * <ul>
 *   <li>LCOM4 = 1 → highly cohesive (good, single responsibility)</li>
 *   <li>LCOM4 &gt; 1 → class can potentially be split into LCOM4 separate classes</li>
 * </ul>
 *
 * <p><b>Algorithm:</b> Union-Find (Disjoint Set) for connected component counting.
 *
 * <p><b>Research References:</b>
 * <ol>
 *   <li>Chidamber &amp; Kemerer (1994) - "A Metrics Suite for Object-Oriented Design"</li>
 *   <li>Henderson-Sellers (1996) - "Object-Oriented Metrics: Measures of Complexity"</li>
 *   <li>Hitz &amp; Montazeri (1995) - "Measuring Coupling and Cohesion in OO Systems"</li>
 * </ol>
 *
 * @author Wenxin
 */
public class SingleResponsibilityCheck implements PrincipleCheck {

    // ──────────── Configurable thresholds (tunable rules) ────────────

    /** Maximum number of non-synthetic fields before a warning is issued. */
    private int maxFields = 10;

    /** Maximum number of non-constructor methods before a warning is issued. */
    private int maxMethods = 20;

    /**
     * LCOM4 threshold: if the number of connected components exceeds this
     * value the class is considered to lack cohesion.  A value of 1 means
     * "perfectly cohesive"; anything above indicates the class could be split.
     */
    private int lcom4Threshold = 1;

    /** Whether to enable LCOM4 analysis (A-Level feature). */
    private boolean lcom4Enabled = true;

    // ──────────── Union-Find (Disjoint Set) data structure ────────────

    /**
     * Classic Union-Find with path compression and union by rank.
     * Used to efficiently count connected components in the method graph.
     */
    static class UnionFind {
        private final int[] parent;
        private final int[] rank;
        private int components;

        UnionFind(int n) {
            parent = new int[n];
            rank   = new int[n];
            components = n;
            for (int i = 0; i < n; i++) parent[i] = i;
        }

        int find(int x) {
            if (parent[x] != x) parent[x] = find(parent[x]); // path compression
            return parent[x];
        }

        void union(int a, int b) {
            int ra = find(a), rb = find(b);
            if (ra == rb) return;
            if      (rank[ra] < rank[rb]) parent[ra] = rb;
            else if (rank[ra] > rank[rb]) parent[rb] = ra;
            else { parent[rb] = ra; rank[ra]++; }
            components--;
        }

        int getComponentCount() { return components; }
    }

    // ──────────── Setters for configurability ────────────

    public void setMaxFields(int maxFields)         { this.maxFields = maxFields; }
    public void setMaxMethods(int maxMethods)       { this.maxMethods = maxMethods; }
    public void setLcom4Threshold(int threshold)    { this.lcom4Threshold = threshold; }
    public void setLcom4Enabled(boolean enabled)    { this.lcom4Enabled = enabled; }

    // ──────────── LintCheck contract ────────────

    @Override
    public String getName() { return "SingleResponsibilityCheck"; }

    @Override
    public String getDescription() {
        return "Detects God Classes that violate Single Responsibility Principle"
             + (lcom4Enabled ? " (enhanced with LCOM4 cohesion analysis)" : "");
    }

    // ──────────── Main check logic ────────────

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        List<LintIssue> issues = new ArrayList<>();
        String location = classInfo.getName();

        // Skip interfaces - they have no implementation to measure
        if (classInfo.isInterface()) return issues;

        // ── Basic God-Class checks (original) ──

        long fieldCount = classInfo.getFields().stream()
                .filter(f -> !f.getName().startsWith("this$") && !f.getName().contains("$"))
                .count();

        long methodCount = classInfo.getMethods().stream()
                .filter(m -> !m.isConstructor() && !m.isStaticInitializer())
                .filter(m -> !m.getName().contains("$"))
                .count();

        if (fieldCount > maxFields && methodCount > maxMethods) {
            issues.add(new LintIssue(getName(), Severity.ERROR,
                    String.format("God Class detected: %d fields (max: %d) and %d methods (max: %d)",
                            fieldCount, maxFields, methodCount, maxMethods),
                    location));
        } else if (fieldCount > maxFields) {
            issues.add(new LintIssue(getName(), Severity.WARNING,
                    String.format("Class has too many fields: %d (max: %d)", fieldCount, maxFields),
                    location));
        } else if (methodCount > maxMethods) {
            issues.add(new LintIssue(getName(), Severity.WARNING,
                    String.format("Class has too many methods: %d (max: %d)", methodCount, maxMethods),
                    location));
        }

        // ── A-Level: LCOM Cohesion Analysis ──

        if (lcom4Enabled) {
            issues.addAll(checkCohesion(classInfo));
        }

        return issues;
    }

    // ──────────── LCOM cohesion analysis ────────────

    private List<LintIssue> checkCohesion(ClassInfo classInfo) {
        List<LintIssue> issues = new ArrayList<>();

        // 1. Collect real (non-synthetic, non-constructor) instance methods
        List<MethodInfo> methods = classInfo.getMethods().stream()
                .filter(m -> !m.isConstructor() && !m.isStaticInitializer())
                .filter(m -> !m.getName().contains("$"))
                .filter(m -> !m.isAbstract())
                .collect(Collectors.toList());

        if (methods.size() <= 1) return issues; // trivially cohesive

        // 2. Collect real instance field names
        Set<String> instanceFields = classInfo.getFields().stream()
                .filter(f -> !f.isStatic())
                .filter(f -> !f.getName().startsWith("this$") && !f.getName().contains("$"))
                .map(FieldInfo::getName)
                .collect(Collectors.toSet());

        if (instanceFields.isEmpty()) return issues; // no fields -> nothing to measure

        // 3. Build method -> accessed-fields map (only instance fields of this class)
        String internalName = classInfo.getInternalName();
        Map<String, Set<String>> methodFieldMap = new LinkedHashMap<>();

        for (MethodInfo method : methods) {
            Set<String> accessed = method.getAccessedFields(internalName);
            accessed.retainAll(instanceFields); // keep only instance fields
            methodFieldMap.put(method.getName(), accessed);
        }

        // 4. Calculate LCOM4 using Union-Find algorithm
        int lcomValue = calculateLCOM4(methods, methodFieldMap);

        // 5. Build a readable summary of which methods access which fields
        String methodFieldSummary = buildMethodFieldSummary(methodFieldMap);

        // 6. Report
        if (lcomValue > lcom4Threshold) {
            issues.add(new LintIssue(getName(), Severity.WARNING,
                    String.format(
                        "LCOM4 = %d (threshold: %d) - class may have %d separate responsibilities. "
                      + "Consider splitting into %d classes.\n"
                      + "      Method-field access:\n%s",
                        lcomValue, lcom4Threshold,
                        lcomValue, lcomValue,
                        methodFieldSummary),
                    classInfo.getName()));
        } else if (lcomValue == 1 && methods.size() >= 3) {
            // Positive feedback for well-designed classes
            issues.add(new LintIssue(getName(), Severity.INFO,
                    String.format("LCOM4 = 1 - class is highly cohesive (good design!)"),
                    classInfo.getName()));
        }

        return issues;
    }

    /**
     * Calculate LCOM4 metric using Union-Find algorithm.
     * LCOM4 = number of connected components in method-field graph.
     */
    private int calculateLCOM4(List<MethodInfo> methods, Map<String, Set<String>> methodFieldMap) {
        int n = methods.size();
        if (n <= 1) return n;

        UnionFind uf = new UnionFind(n);

        // Two methods are connected if they access at least one common field
        for (int i = 0; i < n; i++) {
            Set<String> fieldsI = methodFieldMap.getOrDefault(
                    methods.get(i).getName(), Collections.emptySet());
            for (int j = i + 1; j < n; j++) {
                Set<String> fieldsJ = methodFieldMap.getOrDefault(
                        methods.get(j).getName(), Collections.emptySet());
                if (shareAnyElement(fieldsI, fieldsJ)) {
                    uf.union(i, j);
                }
            }
        }
        return uf.getComponentCount();
    }

    /**
     * Check if two sets share at least one element.
     */
    private boolean shareAnyElement(Set<String> a, Set<String> b) {
        if (a.size() > b.size()) { Set<String> tmp = a; a = b; b = tmp; }
        for (String s : a) {
            if (b.contains(s)) return true;
        }
        return false;
    }

    /**
     * Build a human-readable summary showing which method accesses which fields.
     * Example:
     *   getName -> [name]
     *   setName -> [name]
     *   getAge  -> [age]
     */
    private String buildMethodFieldSummary(Map<String, Set<String>> methodFieldMap) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Set<String>> entry : methodFieldMap.entrySet()) {
            Set<String> fields = entry.getValue();
            if (!fields.isEmpty()) {
                sb.append(String.format("        %s -> %s%n", entry.getKey(), fields));
            } else {
                sb.append(String.format("        %s -> (no instance fields)%n", entry.getKey()));
            }
        }
        return sb.toString();
    }
}
