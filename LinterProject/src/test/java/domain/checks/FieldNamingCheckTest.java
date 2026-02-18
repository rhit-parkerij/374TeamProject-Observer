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

/**
 * Unit tests for FieldNamingCheck.
 * Tests that the checker correctly detects naming convention violations:
 * - Constants should be UPPER_SNAKE_CASE
 * - Regular fields should be camelCase
 *
 * @author Wenxin
 */
class FieldNamingCheckTest {

    private FieldNamingCheck check;
    private ClassFileReader reader;
    private Path targetDir;

    @BeforeEach
    void setUp() {
        check = new FieldNamingCheck();
        reader = new ClassFileReader();
        targetDir = Paths.get("target/classes");
    }

    // ════════════════════════════════════════════════════
    //  Tests on Demo_FieldNamingViolation (should find issues)
    // ════════════════════════════════════════════════════

    @Test
    void testDetectsBadConstantNaming() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_FieldNamingViolation");
        List<LintIssue> issues = check.check(classInfo);

        // Should detect constants not in UPPER_SNAKE_CASE
        boolean foundConstantIssue = issues.stream()
                .anyMatch(i -> i.getMessage().contains("UPPER_SNAKE_CASE"));
        assertTrue(foundConstantIssue,
                "Should detect constants not following UPPER_SNAKE_CASE convention");
    }

    @Test
    void testDetectsBadFieldNaming() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_FieldNamingViolation");
        List<LintIssue> issues = check.check(classInfo);

        // Should detect fields not in camelCase
        boolean foundCamelCaseIssue = issues.stream()
                .anyMatch(i -> i.getMessage().contains("camelCase"));
        assertTrue(foundCamelCaseIssue,
                "Should detect fields not following camelCase convention");
    }

    @Test
    void testViolationIssuesAreWarnings() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_FieldNamingViolation");
        List<LintIssue> issues = check.check(classInfo);

        assertFalse(issues.isEmpty(), "Should find naming issues in violation class");
        for (LintIssue issue : issues) {
            assertEquals(Severity.WARNING, issue.getSeverity(),
                    "Field naming issues should be WARNING severity");
        }
    }

    @Test
    void testViolationClassHasMultipleIssues() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_FieldNamingViolation");
        List<LintIssue> issues = check.check(classInfo);

        assertTrue(issues.size() >= 3,
                "Demo_FieldNamingViolation should have at least 3 naming issues, found: " + issues.size());
    }

    // ════════════════════════════════════════════════════
    //  Tests on Demo_FieldNamingGood (should be clean)
    // ════════════════════════════════════════════════════

    @Test
    void testNoIssuesForGoodNaming() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_FieldNamingGood");
        List<LintIssue> issues = check.check(classInfo);

        assertTrue(issues.isEmpty(),
                "Demo_FieldNamingGood should have no naming issues, but found: " + issues);
    }

    // ════════════════════════════════════════════════════
    //  Tests on well-known JDK class
    // ════════════════════════════════════════════════════

    @Test
    void testCheckNameIsCorrect() {
        assertEquals("FieldNamingCheck", check.getName());
    }

    @Test
    void testDescriptionIsNotEmpty() {
        assertFalse(check.getDescription().isEmpty());
    }

    // ════════════════════════════════════════════════════
    //  Issue location test
    // ════════════════════════════════════════════════════

    @Test
    void testIssueLocationIncludesClassName() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_FieldNamingViolation");
        List<LintIssue> issues = check.check(classInfo);

        for (LintIssue issue : issues) {
            assertTrue(issue.getLocation().contains("Demo_FieldNamingViolation"),
                    "Issue location should contain the class name");
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
