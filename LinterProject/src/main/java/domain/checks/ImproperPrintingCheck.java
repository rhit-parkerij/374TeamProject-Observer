package domain.checks;

import domain.ClassInfo;
import domain.FieldInfo;
import domain.MethodInfo;
import domain.Severity;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImproperPrintingCheck implements StyleCheck {

    @Override
    public String getName() {
        return "ConsolePrintCheck";
    }

    @Override
    public String getDescription() {
        return "Detects usage of System.out.println (not ideal for production code)";
    }

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        List<LintIssue> issues = new ArrayList<>();

        for (MethodInfo method : classInfo.getMethods()) {
            MethodNode methodNode = method.getMethodNode();
            if (methodNode == null) {
                continue;
            }

            for (AbstractInsnNode insn : methodNode.instructions) {

                if (insn instanceof FieldInsnNode fieldInsn) {

                if (fieldInsn.getOpcode() == Opcodes.GETSTATIC &&
                    "java/lang/System".equals(fieldInsn.owner) &&
                   ("out".equals(fieldInsn.name) || "err".equals(fieldInsn.name))
            ) {

                    AbstractInsnNode next = insn.getNext();

            if (next instanceof org.objectweb.asm.tree.MethodInsnNode methodInsn) {

                if (methodInsn.getOpcode() == Opcodes.INVOKEVIRTUAL &&
                    "java/io/PrintStream".equals(methodInsn.owner) &&
                    methodInsn.name.startsWith("print")) {

                    issues.add(new LintIssue(
                        getName(),
                        Severity.WARNING,
                        "Direct console printing detected (System.out." + methodInsn.name +
                        ") in method '" + method.getName() +
                        "'. Confirm that this is intended for debugging purposes or output.",
                        classInfo.getName()
                    ));
                }
            }
        }
    }
}

        }

        return issues;
    }
}
