package domain;

import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;

/**
 * Domain object that represents field information.
 */
public class FieldInfo {
    private final FieldNode fieldNode;

    public FieldInfo(FieldNode fieldNode) {
        this.fieldNode = fieldNode;
    }

    public String getName() {
        return fieldNode.name;
    }

    public String getTypeName() {
        return Type.getType(fieldNode.desc).getClassName();
    }

    public String getDescriptor() {
        return fieldNode.desc;
    }

    public boolean isPublic() {
        return (fieldNode.access & Opcodes.ACC_PUBLIC) != 0;
    }

    public boolean isPrivate() {
        return (fieldNode.access & Opcodes.ACC_PRIVATE) != 0;
    }

    public boolean isProtected() {
        return (fieldNode.access & Opcodes.ACC_PROTECTED) != 0;
    }

    public boolean isPackagePrivate() {
        return !isPublic() && !isPrivate() && !isProtected();
    }

    public boolean isStatic() {
        return (fieldNode.access & Opcodes.ACC_STATIC) != 0;
    }

    public boolean isFinal() {
        return (fieldNode.access & Opcodes.ACC_FINAL) != 0;
    }

    public FieldNode getFieldNode() {
        return fieldNode;
    }
}
