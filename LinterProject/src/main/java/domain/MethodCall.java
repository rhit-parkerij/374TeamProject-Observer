package domain;

import org.objectweb.asm.Type;

/**
 * Represents a method call within a method body.
 */
public class MethodCall {
    private final String owner;
    private final String name;
    private final String descriptor;

    public MethodCall(String owner, String name, String descriptor) {
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
    }

    public String getOwner() {
        return owner;
    }

    public String getOwnerClassName() {
        return Type.getObjectType(owner).getClassName();
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    @Override
    public String toString() {
        return getOwnerClassName() + "." + name;
    }
}
