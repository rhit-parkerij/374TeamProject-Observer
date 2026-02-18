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
 * Unit tests for TemplateMethodDetector.
 * Tests that the detector correctly identifies Template Method pattern:
 * - Abstract class with final method calling abstract steps -> detected
 * - Non-final method calling abstract steps -> flagged as "possible"
 * - Non-abstract class -> skipped
 *
 * @author Wenxin
 */
class TemplateMethodDetectorTest {

    private TemplateMethodDetector detector;
    private ClassFileReader reader;
    private Path targetDir;

    @BeforeEach
    void setUp() {
        detector = new TemplateMethodDetector();
        reader = new ClassFileReader();
        targetDir = Paths.get("target/classes");
    }

    // ════════════════════════════════════════════════════
    //  Detection of Template Method (final methods)
    // ════════════════════════════════════════════════════

    @Test
    void testDetectsTemplateMethodWithFinal() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_TemplateMethodGood");
        List<LintIssue> issues = detector.check(classInfo);

        boolean foundTemplate = issues.stream()
                .anyMatch(i -> i.getMessage().contains("Template Method pattern detected"));
        assertTrue(foundTemplate,
                "Should detect Template Method pattern in Demo_TemplateMethodGood");
    }

    @Test
    void testTemplateMethodReportsStepNames() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_TemplateMethodGood");
        List<LintIssue> issues = detector.check(classInfo);

        boolean reportsSteps = issues.stream()
                .anyMatch(i -> i.getMessage().contains("abstract steps"));
        assertTrue(reportsSteps,
                "Should report which abstract steps are called by the template method");
    }

    @Test
    void testTemplateMethodSeverityIsInfo() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_TemplateMethodGood");
        List<LintIssue> issues = detector.check(classInfo);

        for (LintIssue issue : issues) {
            assertEquals(Severity.INFO, issue.getSeverity(),
                    "Template Method detection should be INFO severity (informational)");
        }
    }

    // ════════════════════════════════════════════════════
    //  Detection of "Possible" Template Method (non-final)
    // ════════════════════════════════════════════════════

    @Test
    void testDetectsPossibleTemplateMethod() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_TemplateMethodPossible");
        List<LintIssue> issues = detector.check(classInfo);

        boolean foundPossible = issues.stream()
                .anyMatch(i -> i.getMessage().contains("Possible Template Method"));
        assertTrue(foundPossible,
                "Should detect possible Template Method pattern (non-final methods calling abstract steps)");
    }

    @Test
    void testPossibleTemplateMethodSuggestsFinal() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_TemplateMethodPossible");
        List<LintIssue> issues = detector.check(classInfo);

        boolean suggestsFinal = issues.stream()
                .anyMatch(i -> i.getMessage().contains("consider making it final"));
        assertTrue(suggestsFinal,
                "Should suggest making the method final for a possible template method");
    }

    // ════════════════════════════════════════════════════
    //  Non-abstract classes should be skipped
    // ════════════════════════════════════════════════════

    @Test
    void testSkipsNonAbstractClasses() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_GodClass");
        List<LintIssue> issues = detector.check(classInfo);

        assertTrue(issues.isEmpty(),
                "Non-abstract classes should produce no Template Method issues");
    }

    @Test
    void testSkipsConcreteClassEvenWithMethodCalls() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_HighCohesion");
        List<LintIssue> issues = detector.check(classInfo);

        assertTrue(issues.isEmpty(),
                "Concrete classes should not be flagged for Template Method pattern");
    }

    // ════════════════════════════════════════════════════
    //  Issue location format tests
    // ════════════════════════════════════════════════════

    @Test
    void testIssueLocationFormat() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_TemplateMethodGood");
        List<LintIssue> issues = detector.check(classInfo);

        for (LintIssue issue : issues) {
            assertTrue(issue.getLocation().contains("Demo_TemplateMethodGood"),
                    "Location should contain the class name");
            assertTrue(issue.getLocation().endsWith("()"),
                    "Location should end with () for method-level issues");
        }
    }

    // ════════════════════════════════════════════════════
    //  Metadata tests
    // ════════════════════════════════════════════════════

    @Test
    void testCheckNameIsCorrect() {
        assertEquals("TemplateMethodDetector", detector.getName());
    }

    @Test
    void testDescriptionIsNotEmpty() {
        assertFalse(detector.getDescription().isEmpty());
    }

    @Test
    void testImplementsPatternCheck() {
        assertTrue(detector instanceof PatternCheck,
                "TemplateMethodDetector should implement PatternCheck interface");
    }

    // ════════════════════════════════════════════════════
    //  Helper
    // ════════════════════════════════════════════════════

    private ClassInfo loadClass(String className) throws Exception {
        String classPath = className.replace('.', '/') + ".class";
        return reader.loadClassFromFile(targetDir.resolve(classPath));
    }
}
