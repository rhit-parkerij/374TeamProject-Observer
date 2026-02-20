package domain.checks;

import domain.ClassInfo;
import domain.MethodInfo;
import domain.Severity;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Open/Closed Principle Check
 *
 * Rules:
 * 1) Extensive use of if-statements (conditional branches) => ERROR
 * 2) Interface with implementing classes in analyzed files => INFO
 *
 * Notes:
 * - "if statements" are approximated by counting JVM conditional branch instructions (IF* opcodes).
 * - Optionally counts switch instructions too (TABLESWITCH/LOOKUPSWITCH) as conditional complexity.
 * - "Read files only": implementers found only inside allClasses.
 */
public class OpenClosePrincipleCheck implements PrincipleCheck {

    // Tune these to your codebase
    private static final int IF_THRESHOLD_PER_CLASS = 100;
    private static final int IF_THRESHOLD_PER_METHOD = 10;

    // If true, TABLESWITCH / LOOKUPSWITCH contribute to the conditional count
    private static final boolean COUNT_SWITCH_AS_CONDITIONAL = true;

    @Override
    public String getName() {
        return "OpenClosePrincipleCheck";
    }

    @Override
    public String getDescription() {
        return "Detects potential Open/Closed Principle issues: if-heavy branching (ERROR) and interface extension points (INFO).";
    }

    /**
     * No-context pass:
     * - Can still do the if-heavy rule (doesn't require allClasses).
     * - Cannot reliably compute interface implementers without allClasses.
     */
    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        List<LintIssue> issues = new ArrayList<>();
        if (classInfo == null) return issues;

        addIfHeavyIssues(classInfo, issues);
        return issues;
    }

    /**
     * Context-aware pass:
     * - If-heavy rule (again; or you can skip if your pipeline calls check() already)
     * - Interface implementers rule (INFO)
     */
    public List<LintIssue> checkWithContext(ClassInfo classInfo, Map<String, ClassInfo> allClasses) {
        List<LintIssue> issues = new ArrayList<>();
        if (classInfo == null || allClasses == null) return issues;

        // Rule 1: if-heavy branching
        addIfHeavyIssues(classInfo, issues);

        // Rule 2: interfaces with implementing classes => INFO
        // Enforce "read files only" by scanning allClasses only.
        if (classInfo.isInterface()) {
            List<String> implementers = findImplementingClasses(classInfo, allClasses);
            if (!implementers.isEmpty()) {
                issues.add(new LintIssue(
                        getName(),
                        Severity.INFO,
                        "Interface '" + classInfo.getName() + "' has implementing classes (" + implementers.size() +
                                "): " + String.join(", ", implementers) +
                                ". This supports Open/Closed extensibility.",
                        classInfo.getName()
                ));
            }
        }

        return issues;
    }

    // ---------------- Rule 1: if-heavy branching ----------------

    private void addIfHeavyIssues(ClassInfo classInfo, List<LintIssue> issues) {
        if (classInfo.isInterface() || classInfo.isAbstract()) {
            // Optional choice: you can still count conditionals on abstract classes if you want.
            // Most OCP "if-heavy logic" is about concrete behavior, so skipping these is reasonable.
            return;
        }

        int classIfCount = 0;
        int worstMethodIfCount = 0;
        String worstMethodName = null;

        for (MethodInfo mi : classInfo.getMethods()) {
            // Skip constructors / static init if you want to reduce noise:
            // if (mi.isConstructor() || mi.isStaticInitializer()) continue;

            int methodIfs = countConditionalsInMethod(mi);
            classIfCount += methodIfs;

            if (methodIfs > worstMethodIfCount) {
                worstMethodIfCount = methodIfs;
                worstMethodName = mi.getName();
            }
        }

        // Per-method threshold (catches one giant branchy method)
        if (worstMethodIfCount >= IF_THRESHOLD_PER_METHOD && worstMethodName != null) {
            issues.add(new LintIssue(
                    getName(),
                    Severity.ERROR,
                    "Class '" + classInfo.getName() + "' has a method '" + worstMethodName + "' with heavy conditional branching (" +
                            worstMethodIfCount + "). Extending behavior likely requires modifying this method (OCP risk).",
                    classInfo.getName() + "#" + worstMethodName
            ));
        }

        // Per-class threshold (catches generally branchy classes)
        if (classIfCount >= IF_THRESHOLD_PER_CLASS) {
            issues.add(new LintIssue(
                    getName(),
                    Severity.ERROR,
                    "Class '" + classInfo.getName() + "' contains extensive conditional branching (" + classIfCount +
                            "). This often indicates 'edit-to-extend' design and can violate Open/Closed.",
                    classInfo.getName()
            ));
        }
    }

    private static int countConditionalsInMethod(MethodInfo methodInfo) {
        if (methodInfo == null || methodInfo.getMethodNode() == null || methodInfo.getMethodNode().instructions == null) {
            return 0;
        }

        int count = 0;

        for (AbstractInsnNode insn = methodInfo.getMethodNode().instructions.getFirst();
             insn != null;
             insn = insn.getNext()) {

            if (insn instanceof JumpInsnNode) {
                int op = insn.getOpcode();
                if (isIfOpcode(op)) {
                    count++;
                }
            } else if (COUNT_SWITCH_AS_CONDITIONAL &&
                    (insn instanceof TableSwitchInsnNode || insn instanceof LookupSwitchInsnNode)) {
                count++;
            }
        }

        return count;
    }

    private static boolean isIfOpcode(int op) {
        return op == Opcodes.IFEQ || op == Opcodes.IFNE ||
               op == Opcodes.IFLT || op == Opcodes.IFGE ||
               op == Opcodes.IFGT || op == Opcodes.IFLE ||
               op == Opcodes.IF_ICMPEQ || op == Opcodes.IF_ICMPNE ||
               op == Opcodes.IF_ICMPLT || op == Opcodes.IF_ICMPGE ||
               op == Opcodes.IF_ICMPGT || op == Opcodes.IF_ICMPLE ||
               op == Opcodes.IF_ACMPEQ || op == Opcodes.IF_ACMPNE ||
               op == Opcodes.IFNULL || op == Opcodes.IFNONNULL;
    }

    // ---------------- Rule 2: interface implementers ----------------

    private static List<String> findImplementingClasses(ClassInfo iface, Map<String, ClassInfo> allClasses) {
        List<String> implementers = new ArrayList<>();
        if (iface == null || allClasses == null) return implementers;

        String ifaceName = iface.getName(); // dotted class name from your ClassInfo.getName()

        for (ClassInfo c : allClasses.values()) {
            if (c == null) continue;
            if (c.isInterface()) continue;

            for (String implemented : c.getInterfaces()) {
                // getInterfaces() returns dotted names too (because you convert with Type.getObjectType().getClassName())
                if (ifaceName.equals(implemented)) {
                    implementers.add(c.getName());
                    break;
                }
            }
        }

        return implementers;
    }
}