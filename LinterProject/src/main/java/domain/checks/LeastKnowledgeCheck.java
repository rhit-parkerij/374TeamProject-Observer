package domain.checks;

import domain.ClassInfo;
import domain.MethodInfo;
import domain.Severity;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Principle Check: Principle of Least Knowledge (Law of Demeter)
 * 
 * Detects method chains where the return value of one method call
 * is immediately used as the receiver of another: a.getB().doC()
 * 
 * This is a basic version that flags all consecutive non-void
 * method call pairs.
 * 
 * TODO: Add exceptions for String chaining,
 *       streams, and other Java patterns.
 * 
 */
public class LeastKnowledgeCheck implements PrincipleCheck {

    @Override
    public String getName() {
        return "PrincipleOfLeastKnowledge";
    }

    @Override
    public String getDescription() {
        return "Detects method chains that may violate the Law of Demeter";
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

            MethodNode methodNode = methodInfo.getMethodNode();
            int chainCount = 0;
            AbstractInsnNode prev = null;

            for (AbstractInsnNode insn : methodNode.instructions) {
                if (insn instanceof MethodInsnNode && prev instanceof MethodInsnNode) {
                    MethodInsnNode firstCall = (MethodInsnNode) prev;

                    // Check that first call returns something (non-void)
                    org.objectweb.asm.Type returnType = org.objectweb.asm.Type.getReturnType(firstCall.desc);
                    if (returnType.getSort() != org.objectweb.asm.Type.VOID) {
                        chainCount++;
                    }
                }

                // Track previous real instruction (skip labels, frames, etc.)
                if (insn.getOpcode() >= 0) {
                    prev = insn;
                }
            }

            if (chainCount > 0) {
                issues.add(new LintIssue(
                    getName(),
                    Severity.WARNING,
                    String.format("Method contains %d method chain(s) that may violate the Law of Demeter",
                        chainCount),
                    classInfo.getName() + "." + methodInfo.getName() + "()"
                ));
            }
        }

        return issues;
    }
}