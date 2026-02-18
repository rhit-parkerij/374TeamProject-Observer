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
 * Unit tests for SingleResponsibilityCheck (with LCOM4).
 * Tests both basic God Class detection and A-level LCOM4 cohesion analysis.
 *
 * @author Wenxin
 */
class SingleResponsibilityCheckTest {

    private SingleResponsibilityCheck check;
    private ClassFileReader reader;
    private Path targetDir;

    @BeforeEach
    void setUp() {
        check = new SingleResponsibilityCheck();
        reader = new ClassFileReader();
        targetDir = Paths.get("target/classes");
    }

    // ════════════════════════════════════════════════════
    //  God Class detection tests
    // ════════════════════════════════════════════════════

    @Test
    void testDetectsGodClass() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_GodClass");
        List<LintIssue> issues = check.check(classInfo);

        boolean foundGodClass = issues.stream()
                .anyMatch(i -> i.getMessage().contains("God Class"));
        assertTrue(foundGodClass,
                "Should detect Demo_GodClass as a God Class");
    }

    @Test
    void testGodClassIsError() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_GodClass");
        List<LintIssue> issues = check.check(classInfo);

        boolean hasError = issues.stream()
                .anyMatch(i -> i.getSeverity() == Severity.ERROR);
        assertTrue(hasError,
                "God Class should be reported as ERROR severity");
    }

    // ════════════════════════════════════════════════════
    //  LCOM4 Cohesion tests (A-level feature)
    // ════════════════════════════════════════════════════

    @Test
    void testHighCohesionClassHasLowLCOM4() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_HighCohesion");
        List<LintIssue> issues = check.check(classInfo);

        // High cohesion class should not have God Class error
        boolean noGodClass = issues.stream()
                .noneMatch(i -> i.getMessage().contains("God Class"));
        assertTrue(noGodClass,
                "Demo_HighCohesion should NOT be flagged as God Class");
    }

    @Test
    void testGodClassHasHighLCOM4() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_GodClass");
        List<LintIssue> issues = check.check(classInfo);

        boolean foundLCOM = issues.stream()
                .anyMatch(i -> i.getMessage().contains("LCOM4"));
        assertTrue(foundLCOM,
                "God Class should have LCOM4 cohesion warning");
    }

    @Test
    void testLCOM4ReportsComponentCount() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_GodClass");
        List<LintIssue> issues = check.check(classInfo);

        boolean reportsCount = issues.stream()
                .anyMatch(i -> i.getMessage().contains("separate responsibilities"));
        assertTrue(reportsCount,
                "LCOM4 warning should report how many separate responsibilities exist");
    }

    @Test
    void testLCOM4ShowsMethodFieldMapping() throws Exception {
        ClassInfo classInfo = loadClass("test.demo.Demo_GodClass");
        List<LintIssue> issues = check.check(classInfo);

        boolean showsMapping = issues.stream()
                .anyMatch(i -> i.getMessage().contains("Method-field access"));
        assertTrue(showsMapping,
                "LCOM4 warning should show method-field access summary");
    }

    // ════════════════════════════════════════════════════
    //  Configurability tests (tunable rules)
    // ════════════════════════════════════════════════════

    @Test
    void testCustomThresholds() throws Exception {
        check.setMaxFields(100);
        check.setMaxMethods(100);
        ClassInfo classInfo = loadClass("test.demo.Demo_GodClass");
        List<LintIssue> issues = check.check(classInfo);

        boolean noGodClass = issues.stream()
                .noneMatch(i -> i.getMessage().contains("God Class"));
        assertTrue(noGodClass,
                "With relaxed thresholds, should not flag as God Class");
    }

    @Test
    void testDisableLCOM4() throws Exception {
        check.setLcom4Enabled(false);
        ClassInfo classInfo = loadClass("test.demo.Demo_GodClass");
        List<LintIssue> issues = check.check(classInfo);

        boolean noLCOM = issues.stream()
                .noneMatch(i -> i.getMessage().contains("LCOM4"));
        assertTrue(noLCOM,
                "With LCOM4 disabled, should not report LCOM4 issues");
    }

    @Test
    void testStrictThresholdsFlagMoreClasses() throws Exception {
        check.setMaxFields(2);
        check.setMaxMethods(2);
        ClassInfo classInfo = loadClass("test.demo.Demo_HighCohesion");
        List<LintIssue> issues = check.check(classInfo);

        // With very strict thresholds, even a well-designed class may be flagged
        assertFalse(issues.isEmpty(),
                "With very strict thresholds, should find issues even in good classes");
    }

    // ════════════════════════════════════════════════════
    //  Metadata tests
    // ════════════════════════════════════════════════════

    @Test
    void testCheckNameIsCorrect() {
        assertEquals("SingleResponsibilityCheck", check.getName());
    }

    @Test
    void testDescriptionMentionsLCOM4WhenEnabled() {
        check.setLcom4Enabled(true);
        assertTrue(check.getDescription().contains("LCOM4"),
                "Description should mention LCOM4 when enabled");
    }

    @Test
    void testDescriptionOmitsLCOM4WhenDisabled() {
        check.setLcom4Enabled(false);
        assertFalse(check.getDescription().contains("LCOM4"),
                "Description should not mention LCOM4 when disabled");
    }

    @Test
    void testImplementsPrincipleCheck() {
        assertTrue(check instanceof PrincipleCheck,
                "SingleResponsibilityCheck should implement PrincipleCheck interface");
    }

    // ════════════════════════════════════════════════════
    //  Helper
    // ════════════════════════════════════════════════════

    private ClassInfo loadClass(String className) throws Exception {
        String classPath = className.replace('.', '/') + ".class";
        return reader.loadClassFromFile(targetDir.resolve(classPath));
    }
}
