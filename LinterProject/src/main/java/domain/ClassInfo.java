package domain;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain object that represents parsed class information.
 * This class serves as an adapter between ASM's ClassNode and our domain model.
 */
public class ClassInfo {
    private final ClassNode classNode;
    private final List<FieldInfo> fields;
    private final List<MethodInfo> methods;

    public ClassInfo(ClassNode classNode) {
        this.classNode = classNode;
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        parseFields();
        parseMethods();
    }

    private void parseFields() {
        for (FieldNode field : classNode.fields) {
            fields.add(new FieldInfo(field));
        }
    }

    private void parseMethods() {
        for (MethodNode method : classNode.methods) {
            methods.add(new MethodInfo(method));
        }
    }

    // Getters
    public String getName() {
        return Type.getObjectType(classNode.name).getClassName();
    }

    public String getInternalName() {
        return classNode.name;
    }

    public String getSuperClassName() {
        return classNode.superName != null ? 
            Type.getObjectType(classNode.superName).getClassName() : null;
    }

    public List<String> getInterfaces() {
        List<String> interfaceNames = new ArrayList<>();
        for (String iface : classNode.interfaces) {
            interfaceNames.add(Type.getObjectType(iface).getClassName());
        }
        return interfaceNames;
    }

    public boolean isPublic() {
        return (classNode.access & Opcodes.ACC_PUBLIC) != 0;
    }

    public boolean isAbstract() {
        return (classNode.access & Opcodes.ACC_ABSTRACT) != 0;
    }

    public boolean isInterface() {
        return (classNode.access & Opcodes.ACC_INTERFACE) != 0;
    }

    public boolean isFinal() {
        return (classNode.access & Opcodes.ACC_FINAL) != 0;
    }

    public List<FieldInfo> getFields() {
        return fields;
    }

    public List<MethodInfo> getMethods() {
        return methods;
    }

    public ClassNode getClassNode() {
        return classNode;
    }
}
