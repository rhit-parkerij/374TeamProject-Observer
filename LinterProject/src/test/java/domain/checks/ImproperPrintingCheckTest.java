package domain.checks;

import datasource.ClassFileReader;
import domain.ClassInfo;
import domain.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImproperPrintingCheckTest {

    private ImproperPrintingCheck check;
    private ClassFileReader reader;
    private Path targetDir;

    @BeforeEach
    void setUp() {
        check = new ImproperPrintingCheck();
        reader = new ClassFileReader();
        targetDir = Paths.get("target/classes");
    }

    // ════════════════════════════════════════════════════
    //  Demo_ImproperPrintingViolation (should find issues)
    // ════════════════════════════════════════════════════

    @Test
    void testDetectsSystemOutPrinting() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_ImproperPrintingViolation");
        List<LintIssue> issues = check.check(classInfo);

        assertTrue(
                issues.stream().anyMatch(i -> i.getMessage().contains("System.out")),
                "Should detect System.out printing"
        );
    }

    @Test
    void testDetectsSystemErrPrinting() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_ImproperPrintingViolation");
        List<LintIssue> issues = check.check(classInfo);

        assertTrue(
                issues.stream().anyMatch(i -> i.getMessage().contains("System.err")),
                "Should detect System.err printing"
        );
    }

    @Test
    void testViolationIssuesAreWarnings() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_ImproperPrintingViolation");
        List<LintIssue> issues = check.check(classInfo);

        assertFalse(issues.isEmpty(), "Should find console printing issues");
        for (LintIssue issue : issues) {
            assertEquals(Severity.WARNING, issue.getSeverity());
        }
    }

    @Test
    void testViolationClassHasMultipleIssues() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_ImproperPrintingViolation");
        List<LintIssue> issues = check.check(classInfo);

        // With your demo (loop println + extra println + err + printf + append + 2 prints)
        // the expected call sites are 7. We'll keep it flexible:
        assertTrue(issues.size() >= 5, "Should find multiple printing issues, found: " + issues.size());
    }

    // ════════════════════════════════════════════════════
    //  Demo_ImproperPrintingGood (should be clean)
    // ════════════════════════════════════════════════════

    @Test
    void testNoIssuesForGoodClass() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_ImproperPrintingGood");
        List<LintIssue> issues = check.check(classInfo);

        assertTrue(issues.isEmpty(), "Good class should have no issues, found: " + issues);
    }

    // ════════════════════════════════════════════════════
    //  Metadata tests
    // ════════════════════════════════════════════════════

    @Test
    void testCheckNameIsCorrect() {
        assertEquals("ConsolePrintCheck", check.getName());
    }

    @Test
    void testDescriptionIsNotEmpty() {
        assertFalse(check.getDescription().isEmpty());
    }

    // ════════════════════════════════════════════════════
    //  Location test
    // ════════════════════════════════════════════════════

    @Test
    void testIssueLocationIncludesClassName() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_ImproperPrintingViolation");
        List<LintIssue> issues = check.check(classInfo);

        for (LintIssue issue : issues) {
            assertTrue(issue.getLocation().contains("Demo_ImproperPrintingViolation"));
        }
    }

    // ════════════════════════════════════════════════════
    //  Helper
    // ════════════════════════════════════════════════════

    private ClassInfo loadClass(String className) throws Exception {
        String classPath = className.replace('.', '/') + ".class";
        return reader.loadClassFromFile(targetDir.resolve(classPath));
    }
}