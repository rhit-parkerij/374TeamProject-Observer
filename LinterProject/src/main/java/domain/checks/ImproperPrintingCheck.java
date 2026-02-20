package domain.checks;

import domain.ClassInfo;
import domain.MethodInfo;
import domain.Severity;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class ImproperPrintingCheck implements StyleCheck {

    @Override
    public String getName() {
        // Keep as-is if your system expects this name:
        return "ImproperPrintingCheck";
        // If you want it to match the class name, change to:
        // return "ImproperPrintingCheck";
    }

    @Override
    public String getDescription() {
        return "Detects direct usage of System.out/System.err print methods (junk debug prints)";
    }

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        List<LintIssue> issues = new ArrayList<>();

        for (MethodInfo method : classInfo.getMethods()) {
            MethodNode methodNode = method.getMethodNode();
            if (methodNode == null || methodNode.instructions == null) continue;

            for (AbstractInsnNode insn = methodNode.instructions.getFirst();
                 insn != null;
                 insn = insn.getNext()) {

                if (!(insn instanceof FieldInsnNode fieldInsn)) continue;

                if (fieldInsn.getOpcode() != Opcodes.GETSTATIC) continue;
                if (!"java/lang/System".equals(fieldInsn.owner)) continue;

                boolean isOut = "out".equals(fieldInsn.name);
                boolean isErr = "err".equals(fieldInsn.name);
                if (!isOut && !isErr) continue;

                MethodInsnNode printCall = findFollowingPrintCall(insn, 20);
                if (printCall == null) continue;

                String stream = isOut ? "out" : "err";

                int line = findLineNumber(printCall);
                if (line < 0) line = findLineNumber(insn);

            String where = sourcePrefix(classInfo, line) + " in " + classInfo.getName() + "#" + method.getName();

            issues.add(new LintIssue(
                 getName(),
                  Severity.WARNING,
                 "Direct console printing detected (System." + stream + "." + printCall.name +
                ") in method '" + method.getName() +
                "'. Confirm that this is intended for debugging purposes or output.",
                      where
                    ));
            }
        }

        return issues;
    }

    private static int findLineNumber(AbstractInsnNode insn) {
    // Walk backwards to find the nearest LineNumberNode
         for (AbstractInsnNode cur = insn; cur != null; cur = cur.getPrevious()) {
             if (cur instanceof org.objectweb.asm.tree.LineNumberNode ln) {
                 return ln.line;
             }
          }
         return -1; // no debug info
    }

    private static String sourcePrefix(ClassInfo classInfo, int line) {
        String file = null;
        if (classInfo != null && classInfo.getClassNode() != null) {
            file = classInfo.getClassNode().sourceFile; // may be null
        }
        if (line < 0) {
            return (file != null ? file : classInfo.getName()) + ":?";
        }
        return (file != null ? file : classInfo.getName()) + ":" + line;
    }

    private MethodInsnNode findFollowingPrintCall(AbstractInsnNode start, int maxLookahead) {
        int seenReal = 0;

        for (AbstractInsnNode cur = start.getNext();
             cur != null && seenReal < maxLookahead;
             cur = cur.getNext()) {

            // Skip metadata nodes
            int t = cur.getType();
            if (t == AbstractInsnNode.LABEL || t == AbstractInsnNode.LINE || t == AbstractInsnNode.FRAME) {
                continue;
            }

            seenReal++;

            // Bail if we hit another GETSTATIC (we likely left the expression)
            if (cur.getOpcode() == Opcodes.GETSTATIC) return null;

            if (cur instanceof MethodInsnNode m) {
                if (m.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && "java/io/PrintStream".equals(m.owner)
                        && m.name.startsWith("print")) {
                    return m;
                }
                // Any other call before PrintStream.print* => not our pattern
                return null;
            }
        }
        return null;
    }
}