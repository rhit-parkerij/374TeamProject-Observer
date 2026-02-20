package domain.checks;

import domain.ClassInfo;
import domain.MethodInfo;
import domain.Severity;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.regex.Pattern;

public class SecurityPrincipleCheck implements PrincipleCheck {

    @Override
    public String getName() {
        return "SecurityPrincipleCheck";
    }

    @Override
    public String getDescription() {
        return "Detects potential security principle violations (hardcoded secrets, SQLi patterns, untrusted input usage).";
    }

    // ---- patterns for hardcoded secrets ----
    private static final Pattern SECRET_KEYWORDS = Pattern.compile(
            "(?i)(password|passwd|pwd|secret|api[_-]?key|apikey|token|access[_-]?token|auth|bearer|private[_-]?key)"
    );

    // “looks like a key”
    private static final Pattern HIGH_ENTROPY_LIKE = Pattern.compile("^[A-Za-z0-9+/=_\\-]{20,}$");
    private static final Pattern AWS_ACCESS_KEY = Pattern.compile("^AKIA[0-9A-Z]{16}$");
    private static final Pattern JWT_LIKE = Pattern.compile("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        List<LintIssue> issues = new ArrayList<>();
        if (classInfo == null) return issues;

        for (MethodInfo method : classInfo.getMethods()) {
            MethodNode mn = method.getMethodNode();
            if (mn == null || mn.instructions == null) continue;

            // Track if method appears to read untrusted input (heuristic)
            boolean readsUntrustedInput = false;

            // Track if method builds SQL dynamically
            boolean buildsSqlDynamically = false;

            // Track if method calls Statement.execute* (high risk if paired with dynamic SQL)
            boolean callsStatementExecute = false;

            // Track if method calls PreparedStatement with placeholders (reduces risk)
            boolean usesPreparedStatement = false;

            for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {

                // 1) Hardcoded secrets via LDC string constants
                if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof String s) {
                    LintIssue secretIssue = detectHardcodedSecret(classInfo, method, insn, s);
                    if (secretIssue != null) issues.add(secretIssue);

                    // quick heuristic: if string looks like SQL
                    if (looksLikeSql(s)) buildsSqlDynamically = true;
                }

                // 2) Detect untrusted input sources
                if (insn instanceof MethodInsnNode call) {
                    if (isUntrustedInputSource(call)) {
                        readsUntrustedInput = true;
                    }

                    // PreparedStatement usage reduces SQLi risk (not perfect, but a good sign)
                    if (isPreparedStatementCall(call)) {
                        usesPreparedStatement = true;
                    }

                    // 3) Statement.execute* calls
                    if (isStatementExecute(call)) {
                        callsStatementExecute = true;
                    }

                    // 4) String concat / StringBuilder patterns (dynamic SQL build)
                    if (isStringConcatOrAppend(call)) {
                        buildsSqlDynamically = true;
                    }
                }

                // INVOKEDYNAMIC string concat (Java 9+)
                if (insn instanceof InvokeDynamicInsnNode indy) {
                    if (isIndyStringConcat(indy)) {
                        buildsSqlDynamically = true;
                    }
                }
            }

            // Emit SQL injection-style issue if risky combo:
            // - builds dynamic SQL AND calls Statement.execute*
            if (buildsSqlDynamically && callsStatementExecute && !usesPreparedStatement) {
                issues.add(new LintIssue(
                        getName(),
                        Severity.ERROR,
                        "Potential SQL injection risk in method '" + method.getName() +
                                "': dynamic SQL construction combined with Statement.execute*. Prefer PreparedStatement with parameters.",
                        classInfo.getName() + "#" + method.getName()
                ));
            } else if (readsUntrustedInput && buildsSqlDynamically && !usesPreparedStatement) {
                issues.add(new LintIssue(
                        getName(),
                        Severity.WARNING,
                        "Untrusted input appears in method '" + method.getName() +
                                "' along with dynamic string building. Ensure inputs are validated/escaped and prefer parameterized APIs (PreparedStatement).",
                        classInfo.getName() + "#" + method.getName()
                ));
            }
        }

        return issues;
    }

    // ----------------- hardcoded secret detection -----------------

    private LintIssue detectHardcodedSecret(ClassInfo classInfo, MethodInfo method, AbstractInsnNode at, String s) {
        if (s == null) return null;

        String trimmed = s.trim();
        if (trimmed.length() < 8) return null; // ignore tiny strings

        boolean keywordy = SECRET_KEYWORDS.matcher(trimmed).find();
        boolean looksKey = HIGH_ENTROPY_LIKE.matcher(trimmed).matches()
                || AWS_ACCESS_KEY.matcher(trimmed).matches()
                || JWT_LIKE.matcher(trimmed).matches();

        // Also catch patterns like "password=foo" or "apiKey: abc..."
        boolean assignmentLike = SECRET_KEYWORDS.matcher(trimmed).find() && (trimmed.contains("=") || trimmed.contains(":"));

        if (assignmentLike || (looksKey && trimmed.length() >= 20)) {
            int line = findLineNumber(at);
            String where = sourcePrefix(classInfo, line) + " in " + classInfo.getName() + "#" + method.getName();

            return new LintIssue(
                    getName(),
                    Severity.ERROR,
                    "Possible hardcoded secret detected in method '" + method.getName() +
                            "'. Remove secrets from code; use env vars / secret manager.",
                    where
            );
        }

        // If it’s just keywordy (like “password”) but not a value, skip.
        if (keywordy) {
            // optional: INFO if you want, but usually noisy
        }

        return null;
    }

    // ----------------- SQL-ish detection helpers -----------------

    private static boolean looksLikeSql(String s) {
        String u = s.toUpperCase(Locale.ROOT);
        // crude but effective; triggers “SQL literal present”
        return u.contains("SELECT ") || u.contains("INSERT ") || u.contains("UPDATE ")
                || u.contains("DELETE ") || u.contains(" FROM ") || u.contains(" WHERE ");
    }

    private static boolean isStatementExecute(MethodInsnNode call) {
        // java/sql/Statement execute, executeQuery, executeUpdate, addBatch, etc.
        if (call == null) return false;
        if (!"java/sql/Statement".equals(call.owner)) return false;
        return call.name.startsWith("execute") || call.name.equals("addBatch");
    }

    private static boolean isPreparedStatementCall(MethodInsnNode call) {
        if (call == null) return false;
        // PreparedStatement.execute* is fine (still can be misused, but less likely)
        if ("java/sql/PreparedStatement".equals(call.owner) && call.name.startsWith("execute")) return true;

        // Connection.prepareStatement / prepareCall
        if ("java/sql/Connection".equals(call.owner) &&
                (call.name.equals("prepareStatement") || call.name.equals("prepareCall"))) return true;

        return false;
    }

    private static boolean isStringConcatOrAppend(MethodInsnNode call) {
        if (call == null) return false;

        // StringBuilder.append or StringBuffer.append
        if (("java/lang/StringBuilder".equals(call.owner) || "java/lang/StringBuffer".equals(call.owner))
                && "append".equals(call.name)) {
            return true;
        }

        // String.concat
        return "java/lang/String".equals(call.owner) && "concat".equals(call.name);
    }

    private static boolean isIndyStringConcat(InvokeDynamicInsnNode indy) {
        // Java 9+ uses StringConcatFactory.makeConcatWithConstants/makeConcat
        if (indy == null) return false;
        if (!"java/lang/invoke/StringConcatFactory".equals(indy.bsm.getOwner())) return false;
        return "makeConcatWithConstants".equals(indy.bsm.getName()) || "makeConcat".equals(indy.bsm.getName());
    }

    // ----------------- untrusted input heuristics -----------------

    private static boolean isUntrustedInputSource(MethodInsnNode call) {
        if (call == null) return false;

        // Common servlet inputs
        if ("javax/servlet/http/HttpServletRequest".equals(call.owner) ||
            "jakarta/servlet/http/HttpServletRequest".equals(call.owner)) {

            return call.name.equals("getParameter")
                    || call.name.equals("getParameterValues")
                    || call.name.equals("getHeader")
                    || call.name.equals("getHeaders")
                    || call.name.equals("getQueryString")
                    || call.name.equals("getRequestURI")
                    || call.name.equals("getRequestURL");
        }

        // Console / stdin input (still untrusted)
        if ("java/util/Scanner".equals(call.owner) && call.name.startsWith("next")) return true;
        if ("java/io/BufferedReader".equals(call.owner) && call.name.equals("readLine")) return true;

        return false;
    }

    // ----------------- line number helpers (same approach as printing check) -----------------

    private static int findLineNumber(AbstractInsnNode insn) {
        for (AbstractInsnNode cur = insn; cur != null; cur = cur.getPrevious()) {
            if (cur instanceof LineNumberNode ln) {
                return ln.line;
            }
        }
        return -1;
    }

    private static String sourcePrefix(ClassInfo classInfo, int line) {
        String file = null;
        if (classInfo != null && classInfo.getClassNode() != null) {
            file = classInfo.getClassNode().sourceFile;
        }
        String base = (file != null ? file : classInfo.getName());
        return (line < 0) ? (base + ":?") : (base + ":" + line);
    }
}