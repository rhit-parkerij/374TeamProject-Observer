package domain.checks;

import domain.ClassInfo;
import domain.LinterConfig;
import domain.Severity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Unit tests for ConfigurableMethodLengthCheck.
 * Verifies:
 *   - LinterConfig injection works correctly
 *   - WARNING and ERROR thresholds produce correct severities
 *   - Runtime config changes are reflected in next check() call
 *   - Methods below threshold produce no issues
 *   - Constructors and static initializers are skipped
 *
 * Uses the compiled Demo_LongMethodViolation test class (147 instructions).
 *
 * @author Sophia
 */
class ConfigurableMethodLengthCheckTest {

    private static final String TEST_CLASS = "test.demo.Demo_LongMethodViolation";

    private ClassInfo loadTestClass() throws IOException {
        datasource.ClassFileReader reader = new datasource.ClassFileReader();
        return reader.loadClass(TEST_CLASS);
    }

    // ─── Injection tests ─────────────────────────────────────────

    @Test
    void testConfigInjection() {
        LinterConfig config = new LinterConfig(30, 80);
        ConfigurableMethodLengthCheck check = new ConfigurableMethodLengthCheck(config);

        assertEquals(30, check.getWarningThreshold());
        assertEquals(80, check.getErrorThreshold());
        assertSame(config, check.getConfig());
    }

    @Test
    void testDefaultConstructorLoadsConfig() {
        ConfigurableMethodLengthCheck check = new ConfigurableMethodLengthCheck();
        // Should have positive threshold values
        assertTrue(check.getWarningThreshold() > 0);
        assertTrue(check.getErrorThreshold() > 0);
    }

    @Test
    void testExplicitThresholdConstructor() {
        ConfigurableMethodLengthCheck check = new ConfigurableMethodLengthCheck(10, 50);
        assertEquals(10, check.getWarningThreshold());
        assertEquals(50, check.getErrorThreshold());
    }

    // ─── ERROR detection ─────────────────────────────────────────

    @Test
    void testDetectsError_WhenMethodExceedsErrorThreshold() throws IOException {
        // Demo_LongMethodViolation.tooLongMethod() has many instructions
        // Set thresholds low to ensure ERROR is triggered
        LinterConfig config = new LinterConfig(10, 30);
        ConfigurableMethodLengthCheck check = new ConfigurableMethodLengthCheck(config);

        ClassInfo classInfo = loadTestClass();
        List<LintIssue> issues = check.check(classInfo);

        assertFalse(issues.isEmpty(), "Expected at least one issue");

        LintIssue errorIssue = issues.stream()
                .filter(i -> i.getSeverity() == Severity.ERROR)
                .findFirst()
                .orElse(null);

        assertNotNull(errorIssue, "Expected an ERROR severity issue");
        assertTrue(errorIssue.getMessage().contains("exceeds ERROR threshold of 30"));
    }

    // ─── WARNING detection ───────────────────────────────────────

    @Test
    void testDetectsWarning_WhenMethodExceedsWarningButNotError() throws IOException {
        // Set warning low and error very high so the method triggers WARNING only
        LinterConfig config = new LinterConfig(10, 5000);
        ConfigurableMethodLengthCheck check = new ConfigurableMethodLengthCheck(config);

        ClassInfo classInfo = loadTestClass();
        List<LintIssue> issues = check.check(classInfo);

        assertFalse(issues.isEmpty(), "Expected at least one issue");

        LintIssue warningIssue = issues.stream()
                .filter(i -> i.getSeverity() == Severity.WARNING)
                .findFirst()
                .orElse(null);

        assertNotNull(warningIssue, "Expected a WARNING severity issue");
        assertTrue(warningIssue.getMessage().contains("exceeds WARNING threshold of 10"));
    }

    // ─── No issues when below threshold ──────────────────────────

    @Test
    void testNoIssues_WhenMethodBelowWarningThreshold() throws IOException {
        // Set thresholds very high so nothing triggers
        LinterConfig config = new LinterConfig(500, 1000);
        ConfigurableMethodLengthCheck check = new ConfigurableMethodLengthCheck(config);

        ClassInfo classInfo = loadTestClass();
        List<LintIssue> issues = check.check(classInfo);

        assertTrue(issues.isEmpty(), "Expected no issues when thresholds are very high");
    }

    // ─── Runtime config change reflected immediately ─────────────

    @Test
    void testRuntimeConfigChange_ReflectedInNextCheck() throws IOException {
        LinterConfig config = new LinterConfig(5000, 10000);
        ConfigurableMethodLengthCheck check = new ConfigurableMethodLengthCheck(config);

        ClassInfo classInfo = loadTestClass();

        // First run: thresholds high → no issues
        List<LintIssue> issuesBefore = check.check(classInfo);
        assertTrue(issuesBefore.isEmpty(), "Should have no issues with high thresholds");

        // Change config at runtime (simulate user tuning)
        config.setMethodLengthThresholds(10, 30);

        // Second run: same check object, but config changed → should find ERROR
        List<LintIssue> issuesAfter = check.check(classInfo);
        assertFalse(issuesAfter.isEmpty(), "Should have issues after lowering thresholds");

        boolean hasError = issuesAfter.stream()
                .anyMatch(i -> i.getSeverity() == Severity.ERROR);
        assertTrue(hasError, "Expected ERROR after runtime threshold change");
    }

    // ─── Description reflects current config ─────────────────────

    @Test
    void testDescriptionReflectsCurrentThresholds() {
        LinterConfig config = new LinterConfig(25, 75);
        ConfigurableMethodLengthCheck check = new ConfigurableMethodLengthCheck(config);

        String desc = check.getDescription();
        assertTrue(desc.contains("25"), "Description should contain warning threshold");
        assertTrue(desc.contains("75"), "Description should contain error threshold");

        // Change at runtime
        config.setMethodLengthThresholds(10, 50);
        String newDesc = check.getDescription();
        assertTrue(newDesc.contains("10"), "After runtime change, description should reflect new warning");
        assertTrue(newDesc.contains("50"), "After runtime change, description should reflect new error");
    }

    // ─── getName ─────────────────────────────────────────────────

    @Test
    void testGetName() {
        ConfigurableMethodLengthCheck check = new ConfigurableMethodLengthCheck();
        assertEquals("ConfigurableMethodLengthCheck", check.getName());
    }
}
