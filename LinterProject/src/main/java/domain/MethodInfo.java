package domain;

import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain object that represents method information.
 */
public class MethodInfo {
    private final MethodNode methodNode;

    public MethodInfo(MethodNode methodNode) {
        this.methodNode = methodNode;
    }

    public String getName() {
        return methodNode.name;
    }

    public String getDescriptor() {
        return methodNode.desc;
    }

    public String getReturnTypeName() {
        return Type.getReturnType(methodNode.desc).getClassName();
    }

    public List<String> getParameterTypeNames() {
        List<String> params = new ArrayList<>();
        for (Type argType : Type.getArgumentTypes(methodNode.desc)) {
            params.add(argType.getClassName());
        }
        return params;
    }

    public boolean isPublic() {
        return (methodNode.access & Opcodes.ACC_PUBLIC) != 0;
    }

    public boolean isPrivate() {
        return (methodNode.access & Opcodes.ACC_PRIVATE) != 0;
    }

    public boolean isProtected() {
        return (methodNode.access & Opcodes.ACC_PROTECTED) != 0;
    }

    public boolean isPackagePrivate() {
        return !isPublic() && !isPrivate() && !isProtected();
    }

    public boolean isStatic() {
        return (methodNode.access & Opcodes.ACC_STATIC) != 0;
    }

    public boolean isAbstract() {
        return (methodNode.access & Opcodes.ACC_ABSTRACT) != 0;
    }

    public boolean isFinal() {
        return (methodNode.access & Opcodes.ACC_FINAL) != 0;
    }

    public boolean isConstructor() {
        return methodNode.name.equals("<init>");
    }

    public boolean isStaticInitializer() {
        return methodNode.name.equals("<clinit>");
    }

    public int getInstructionCount() {
        return methodNode.instructions.size();
    }

    /**
     * Get all method calls made within this method.
     */
    public List<MethodCall> getMethodCalls() {
        List<MethodCall> calls = new ArrayList<>();
        for (int i = 0; i < methodNode.instructions.size(); i++) {
            AbstractInsnNode insn = methodNode.instructions.get(i);
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode methodCall = (MethodInsnNode) insn;
                calls.add(new MethodCall(methodCall.owner, methodCall.name, methodCall.desc));
            }
        }
        return calls;
    }

    public MethodNode getMethodNode() {
        return methodNode;
    }
}
