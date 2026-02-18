package domain.checks;

import domain.ClassInfo;
import domain.FieldInfo;
import domain.MethodInfo;
import domain.Severity;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Style Check: Unused Variable Detection
 * 
 * Detects two kinds of unused variables using ASM bytecode analysis:
 * 
 * 1. Unused local variables: variables in the local variable table
 *    that are never loaded (read) by any instruction.
 * 
 * 2. Unused private fields: private fields never read by any method.
 * 
 * Skips 'this', synthetic variables, and method parameters.
 * 
 */
public class UnusedVariableCheck implements StyleCheck {

    @Override
    public String getName() {
        return "UnusedVariableCheck";
    }

    @Override
    public String getDescription() {
        return "Detects unused local variables and unused private fields";
    }

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        List<LintIssue> issues = new ArrayList<>();

        if (classInfo.isInterface()) {
            return issues;
        }

        checkUnusedLocalVariables(classInfo, issues);
        checkUnusedPrivateFields(classInfo, issues);

        return issues;
    }

    private void checkUnusedLocalVariables(ClassInfo classInfo, List<LintIssue> issues) {
        for (MethodInfo methodInfo : classInfo.getMethods()) {
            MethodNode methodNode = methodInfo.getMethodNode();

            if (methodNode.localVariables == null || methodNode.localVariables.isEmpty()) {
                continue;
            }

            // Collect all slot indices that are actually read
            Set<Integer> loadedIndices = new HashSet<>();
            for (AbstractInsnNode insn : methodNode.instructions) {
                if (insn instanceof VarInsnNode) {
                    VarInsnNode varInsn = (VarInsnNode) insn;
                    if (isLoadOpcode(varInsn.getOpcode())) {
                        loadedIndices.add(varInsn.var);
                    }
                }
                if (insn instanceof IincInsnNode) {
                    loadedIndices.add(((IincInsnNode) insn).var);
                }
            }

            for (LocalVariableNode localVar : methodNode.localVariables) {
                // Skip 'this'
                if (localVar.index == 0 && !methodInfo.isStatic()) {
                    continue;
                }

                // Skip synthetic
                if (localVar.name.contains("$")) {
                    continue;
                }

                // Skip method parameters
                if (isMethodParameter(methodNode, localVar.index, methodInfo.isStatic())) {
                    continue;
                }

                if (!loadedIndices.contains(localVar.index)) {
                    issues.add(new LintIssue(
                        getName(),
                        Severity.WARNING,
                        String.format("Local variable '%s' is declared but never used", localVar.name),
                        classInfo.getName() + "." + methodInfo.getName() + "()"
                    ));
                }
            }
        }
    }

    private boolean isMethodParameter(MethodNode methodNode, int index, boolean isStatic) {
        org.objectweb.asm.Type[] argTypes = org.objectweb.asm.Type.getArgumentTypes(methodNode.desc);
        int paramSlot = isStatic ? 0 : 1;
        for (org.objectweb.asm.Type argType : argTypes) {
            if (index == paramSlot) {
                return true;
            }
            paramSlot += argType.getSize();
        }
        return false;
    }

    private boolean isLoadOpcode(int opcode) {
        return opcode == Opcodes.ILOAD || opcode == Opcodes.LLOAD
            || opcode == Opcodes.FLOAD || opcode == Opcodes.DLOAD
            || opcode == Opcodes.ALOAD;
    }

    private void checkUnusedPrivateFields(ClassInfo classInfo, List<LintIssue> issues) {
        Set<String> privateFieldNames = new HashSet<>();
        for (FieldInfo field : classInfo.getFields()) {
            if (field.isPrivate()) {
                privateFieldNames.add(field.getName());
            }
        }

        if (privateFieldNames.isEmpty()) {
            return;
        }

        Set<String> readFields = new HashSet<>();
        String classInternalName = classInfo.getInternalName();

        for (MethodInfo methodInfo : classInfo.getMethods()) {
            MethodNode methodNode = methodInfo.getMethodNode();
            for (AbstractInsnNode insn : methodNode.instructions) {
                if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                    if ((fieldInsn.getOpcode() == Opcodes.GETFIELD
                            || fieldInsn.getOpcode() == Opcodes.GETSTATIC)
                            && fieldInsn.owner.equals(classInternalName)) {
                        readFields.add(fieldInsn.name);
                    }
                }
            }
        }

        for (String fieldName : privateFieldNames) {
            if (!readFields.contains(fieldName)) {
                issues.add(new LintIssue(
                    getName(),
                    Severity.WARNING,
                    String.format("Private field '%s' is never read (only written or completely unused)",
                        fieldName),
                    classInfo.getName() + "." + fieldName
                ));
            }
        }
    }
}