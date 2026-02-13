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

/**
 * Style Check: Unused Variable Detection
 * 
 * Detects private fields that are never read by any method in the class.
 * A field that is only written (or never accessed at all) is flagged.
 * 
 * TODO: Also detect unused local variables within methods.
 * 
 */
public class UnusedVariableCheck implements StyleCheck {

    @Override
    public String getName() {
        return "UnusedVariableCheck";
    }

    @Override
    public String getDescription() {
        return "Detects unused private fields";
    }

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        List<LintIssue> issues = new ArrayList<>();

        if (classInfo.isInterface()) {
            return issues;
        }

        // Collect all private field names
        Set<String> privateFields = new HashSet<>();
        for (FieldInfo field : classInfo.getFields()) {
            if (field.isPrivate()) {
                privateFields.add(field.getName());
            }
        }

        if (privateFields.isEmpty()) {
            return issues;
        }

        // Scan all methods for GETFIELD/GETSTATIC reads
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

        // Report private fields never read
        for (String fieldName : privateFields) {
            if (!readFields.contains(fieldName)) {
                issues.add(new LintIssue(
                    getName(),
                    Severity.WARNING,
                    String.format("Private field '%s' is never read", fieldName),
                    classInfo.getName() + "." + fieldName
                ));
            }
        }

        return issues;
    }
}