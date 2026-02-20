package domain.checks;

import domain.ClassInfo;
import domain.Severity;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class OpenClosePrincipleCheckTest {

    // ---------------- helpers ----------------

    private static ClassInfo makeConcreteClass(String internalName, List<MethodNode> methods) {
        ClassNode cn = new ClassNode();
        cn.version = Opcodes.V17; // any valid version
        cn.access = Opcodes.ACC_PUBLIC;
        cn.name = internalName;
        cn.superName = "java/lang/Object";
        cn.interfaces = new ArrayList<>();
        cn.fields = new ArrayList<>();
        cn.methods = new ArrayList<>();
        cn.methods.addAll(methods);
        return new ClassInfo(cn);
    }

    private static ClassInfo makeInterface(String internalName) {
        ClassNode cn = new ClassNode();
        cn.version = Opcodes.V17;
        cn.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT;
        cn.name = internalName;
        cn.superName = "java/lang/Object";
        cn.interfaces = new ArrayList<>();
        cn.fields = new ArrayList<>();
        cn.methods = new ArrayList<>();
        return new ClassInfo(cn);
    }

    private static ClassInfo makeImplementer(String internalName, String ifaceInternalName) {
        ClassNode cn = new ClassNode();
        cn.version = Opcodes.V17;
        cn.access = Opcodes.ACC_PUBLIC;
        cn.name = internalName;
        cn.superName = "java/lang/Object";
        cn.interfaces = new ArrayList<>();
        cn.interfaces.add(ifaceInternalName); // IMPORTANT: internal name here (ASM style)
        cn.fields = new ArrayList<>();
        cn.methods = new ArrayList<>();
        // add at least one method to keep it realistic
        cn.methods.add(new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null));
        return new ClassInfo(cn);
    }

    private static MethodNode makeMethodWithIfs(String name, int ifCount) {
        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC, name, "()V", null, null);
        InsnList insns = mn.instructions;

        // Make ifCount conditional branches: IFEQ label
        for (int i = 0; i < ifCount; i++) {
            LabelNode target = new LabelNode();
            insns.add(new InsnNode(Opcodes.ICONST_0));
            insns.add(new JumpInsnNode(Opcodes.IFEQ, target));
            insns.add(new InsnNode(Opcodes.NOP));
            insns.add(target);
        }

        insns.add(new InsnNode(Opcodes.RETURN));
        return mn;
    }

    private static boolean hasSeverity(List<LintIssue> issues, Severity s) {
        for (LintIssue li : issues) {
            if (li.getSeverity() == s) return true;
        }
        return false;
    }

    // ---------------- tests ----------------

    @Test
    void check_ifHeavyClass_emitsError() {
        // Your check uses:
        // IF_THRESHOLD_PER_CLASS = 15
        // IF_THRESHOLD_PER_METHOD = 10
        //
        // Put 12 IFs in one method to trigger per-method ERROR,
        // and 16 total IFs to trigger per-class ERROR too.
        MethodNode m1 = makeMethodWithIfs("branchy", 12);
        MethodNode m2 = makeMethodWithIfs("moreBranches", 4);

        ClassInfo ci = makeConcreteClass("test/BranchyClass", List.of(m1, m2));

        OpenClosePrincipleCheck check = new OpenClosePrincipleCheck();
        List<LintIssue> issues = check.check(ci);

        assertFalse(issues.isEmpty(), "Expected at least one lint issue");
        assertTrue(hasSeverity(issues, Severity.ERROR), "Expected at least one ERROR for if-heavy branching");

        // Optional: ensure at least one issue points at the worst method location format: Class#method
        boolean hasMethodLocation = issues.stream()
                .anyMatch(i -> i.getLocation() != null && i.getLocation().contains("#branchy"));
        assertTrue(hasMethodLocation, "Expected an issue located at Class#branchy");
    }

    @Test
    void check_ifLightClass_emitsNoError() {
        // Keep total below 15 and per-method below 10
        MethodNode m1 = makeMethodWithIfs("ok", 6);
        MethodNode m2 = makeMethodWithIfs("alsoOk", 6); // total 12

        ClassInfo ci = makeConcreteClass("test/NonBranchyClass", List.of(m1, m2));

        OpenClosePrincipleCheck check = new OpenClosePrincipleCheck();
        List<LintIssue> issues = check.check(ci);

        assertFalse(hasSeverity(issues, Severity.ERROR), "Did not expect ERROR for non-branchy class");
    }

    @Test
    void checkWithContext_interfaceWithImplementers_emitsInfo() {
        ClassInfo iface = makeInterface("test/MyIface");

        ClassInfo impl1 = makeImplementer("test/ImplOne", "test/MyIface");
        ClassInfo impl2 = makeImplementer("test/ImplTwo", "test/MyIface");

        // Map keys don't matter for this algorithm, it scans values.
        Map<String, ClassInfo> allClasses = new HashMap<>();
        allClasses.put(iface.getName(), iface);
        allClasses.put(impl1.getName(), impl1);
        allClasses.put(impl2.getName(), impl2);

        OpenClosePrincipleCheck check = new OpenClosePrincipleCheck();
        List<LintIssue> issues = check.checkWithContext(iface, allClasses);

        assertTrue(hasSeverity(issues, Severity.INFO), "Expected INFO when interface has implementing classes");

        // Optional: verify message contains implementers count
        boolean mentionsImpl = issues.stream()
                .anyMatch(i -> i.getSeverity() == Severity.INFO &&
                        i.getMessage() != null &&
                        (i.getMessage().contains("ImplOne") || i.getMessage().contains("ImplTwo")));
        assertTrue(mentionsImpl, "Expected INFO message to mention implementing class names");
    }

    @Test
    void checkWithContext_interfaceWithoutImplementers_emitsNoInfo() {
        ClassInfo iface = makeInterface("test/NoImplIface");

        Map<String, ClassInfo> allClasses = new HashMap<>();
        allClasses.put(iface.getName(), iface);

        OpenClosePrincipleCheck check = new OpenClosePrincipleCheck();
        List<LintIssue> issues = check.checkWithContext(iface, allClasses);

        assertFalse(hasSeverity(issues, Severity.INFO), "Did not expect INFO when interface has no implementers");
    }
}