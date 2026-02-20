package domain.checks;

import domain.ClassInfo;
import domain.Severity;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityPrincipleCheckTest {

    // ---------------- helpers ----------------

    private static ClassInfo makeConcreteClass(String internalName, String sourceFile, List<MethodNode> methods) {
        ClassNode cn = new ClassNode();
        cn.version = Opcodes.V17;
        cn.access = Opcodes.ACC_PUBLIC;
        cn.name = internalName;                 // internal name (slashes)
        cn.sourceFile = sourceFile;             // for "file:line" output
        cn.superName = "java/lang/Object";
        cn.interfaces = new ArrayList<>();
        cn.fields = new ArrayList<>();
        cn.methods = new ArrayList<>();
        cn.methods.addAll(methods);
        return new ClassInfo(cn);
    }

    private static MethodNode methodWithHardcodedSecretAtLine(String name, int line, String secretValue) {
        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC, name, "()V", null, null);

        LabelNode L0 = new LabelNode();
        mn.instructions.add(L0);
        mn.instructions.add(new LineNumberNode(line, L0));

        // LDC "AKIA...." (or other secret-like string)
        mn.instructions.add(new LdcInsnNode(secretValue));

        mn.instructions.add(new InsnNode(Opcodes.POP));
        mn.instructions.add(new InsnNode(Opcodes.RETURN));
        return mn;
    }

    private static MethodNode methodWithDynamicSqlAndStatementExecuteAtLine(String name, int line) {
        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC, name, "()V", null, null);

        LabelNode L0 = new LabelNode();
        mn.instructions.add(L0);
        mn.instructions.add(new LineNumberNode(line, L0));

        // Pretend we're building SQL dynamically (detector looks for StringBuilder.append / indy concat)
        mn.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
        mn.instructions.add(new InsnNode(Opcodes.DUP));
        mn.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                "java/lang/StringBuilder",
                "<init>",
                "()V",
                false
        ));

        // append("SELECT * FROM users WHERE name = '")
        mn.instructions.add(new LdcInsnNode("SELECT * FROM users WHERE name = '"));
        mn.instructions.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false
        ));

        // append(userInput)  (we won't build real taint here; append alone marks dynamic building)
        mn.instructions.add(new LdcInsnNode("bob")); // placeholder for user input
        mn.instructions.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false
        ));

        // append("'")
        mn.instructions.add(new LdcInsnNode("'"));
        mn.instructions.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/StringBuilder",
                "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                false
        ));

        // Now pretend we call Statement.executeQuery(...) (this is the risky sink)
        mn.instructions.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                "java/sql/Statement",
                "executeQuery",
                "(Ljava/lang/String;)Ljava/sql/ResultSet;",
                true
        ));

        mn.instructions.add(new InsnNode(Opcodes.RETURN));
        return mn;
    }

    private static boolean hasIssue(List<LintIssue> issues, Severity severity, String containsMsg) {
        for (LintIssue li : issues) {
            if (li.getSeverity() == severity && li.getMessage() != null && li.getMessage().contains(containsMsg)) {
                return true;
            }
        }
        return false;
    }

    // ---------------- demo test ----------------

    @Test
    void demo_detectsHardcodedSecret_and_DynamicSqlWithStatementExecute() {
        // Hardcoded AWS access key format triggers the "hardcoded secret" ERROR
        MethodNode secret = methodWithHardcodedSecretAtLine(
                "hardcodedSecret",
                42,
                "AKIA1234567890ABCDEF" // matches ^AKIA[0-9A-Z]{16}$
        );

        // Dynamic SQL building + Statement.executeQuery triggers SQLi ERROR
        MethodNode sqli = methodWithDynamicSqlAndStatementExecuteAtLine(
                "queryUsers",
                88
        );

        ClassInfo ci = makeConcreteClass("demo/SecurityDemo", "SecurityDemo.java", List.of(secret, sqli));

        SecurityPrincipleCheck check = new SecurityPrincipleCheck();
        List<LintIssue> issues = check.check(ci);

        // Print issues as a "demo" (optional but helpful when running tests)
        // issues.forEach(System.out::println);

        assertFalse(issues.isEmpty(), "Expected security issues to be reported");

        assertTrue(
                hasIssue(issues, Severity.ERROR, "hardcoded secret"),
                "Expected ERROR mentioning hardcoded secret"
        );

        assertTrue(
                hasIssue(issues, Severity.ERROR, "SQL injection risk"),
                "Expected ERROR mentioning SQL injection risk"
        );

        // Optional: verify the secret issue location includes file:line (since we inserted LineNumberNode)
        boolean hasLine42 = issues.stream().anyMatch(i ->
                i.getSeverity() == Severity.ERROR &&
                i.getLocation() != null &&
                i.getLocation().contains("SecurityDemo.java:42")
        );
        assertTrue(hasLine42, "Expected at least one ERROR located at SecurityDemo.java:42");
    }
}