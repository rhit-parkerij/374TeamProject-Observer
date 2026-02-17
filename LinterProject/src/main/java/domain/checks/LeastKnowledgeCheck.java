package domain.checks;

import domain.ClassInfo;
import domain.MethodInfo;
import domain.Severity;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Principle Check: Principle of Least Knowledge (Law of Demeter)
 * 
 * Detects "train wreck" method chains where a method calls a method on
 * the return value of another method call: a.getX().doY().
 * 
 * Exceptions (not flagged):
 *   - Builder/fluent patterns where the return type equals the owner type
 *   - Calls on common JDK types like String, StringBuilder, streams, Optional
 *   - Static method chains
 */
public class LeastKnowledgeCheck implements PrincipleCheck {

    @Override
    public String getName() {
        return "PrincipleOfLeastKnowledge";
    }

    @Override
    public String getDescription() {
        return "Detects 'train wreck' method chains that violate the Law of Demeter (a.getB().doC())";
    }

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        List<LintIssue> issues = new ArrayList<>();

        if (classInfo.isInterface()) {
            return issues;
        }

        for (MethodInfo methodInfo : classInfo.getMethods()) {
            if (methodInfo.isConstructor() || methodInfo.isStaticInitializer()) {
                continue;
            }
            checkMethodForChaining(classInfo, methodInfo, issues);
        }

        return issues;
    }

    private void checkMethodForChaining(ClassInfo classInfo, MethodInfo methodInfo,
                                         List<LintIssue> issues) {
        MethodNode methodNode = methodInfo.getMethodNode();
        int chainCount = 0;

        AbstractInsnNode prev = null;

        for (AbstractInsnNode insn : methodNode.instructions) {
            if (insn instanceof MethodInsnNode && prev instanceof MethodInsnNode) {
                MethodInsnNode firstCall = (MethodInsnNode) prev;
                MethodInsnNode secondCall = (MethodInsnNode) insn;

                if (isTrainWreck(firstCall, secondCall)) {
                    chainCount++;
                }
            }

            if (insn.getOpcode() >= 0) {
                prev = insn;
            }
        }

        if (chainCount > 0) {
            String location = classInfo.getName() + "." + methodInfo.getName() + "()";
            issues.add(new LintIssue(
                getName(),
                chainCount >= 3 ? Severity.WARNING : Severity.INFO,
                String.format("Method contains %d method chain(s) that may violate "
                    + "the Principle of Least Knowledge (Law of Demeter). "
                    + "Consider introducing local variables or moving behavior closer to the data.",
                    chainCount),
                location
            ));
        }
    }

    private boolean isTrainWreck(MethodInsnNode firstCall, MethodInsnNode secondCall) {
        // First call must return non-void
        org.objectweb.asm.Type returnType = org.objectweb.asm.Type.getReturnType(firstCall.desc);
        if (returnType.getSort() == org.objectweb.asm.Type.VOID) {
            return false;
        }

        // Skip static calls
        if (secondCall.getOpcode() == Opcodes.INVOKESTATIC) {
            return false;
        }

        // Exclude builder/fluent patterns
        String firstOwner = firstCall.owner;
        String firstReturnInternal = returnType.getInternalName();
        if (firstOwner.equals(firstReturnInternal)) {
            return false;
        }

        // Exclude safe JDK types
        String returnClassName = returnType.getClassName();
        if (isFluentSafeType(returnClassName)) {
            return false;
        }

        return true;
    }

    private boolean isFluentSafeType(String typeName) {
        return typeName.equals("java.lang.String")
            || typeName.equals("java.lang.StringBuilder")
            || typeName.equals("java.lang.StringBuffer")
            || typeName.startsWith("java.util.stream.")
            || typeName.equals("java.util.Optional");
    }
}