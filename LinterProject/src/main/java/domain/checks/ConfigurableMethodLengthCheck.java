package domain.checks;

import domain.ClassInfo;
import domain.LinterConfig;
import domain.MethodInfo;
import domain.Severity;

import java.util.ArrayList;
import java.util.List;

/**
 * Style Check: Configurable Method Length Check
 * Checks if a method's length (number of instructions) exceeds a configurable limit.
 *
 * Instead of hardcoding the threshold, this check receives a {@link LinterConfig}
 * object via Dependency Injection. The config loads settings from a properties file
 * and also supports runtime user overrides.
 *
 * Thresholds are read from the injected LinterConfig every time {@code check()} runs,
 * so runtime changes take effect immediately without rebuilding the check.
 *
 * ASM Implementation: methodNode.instructions.size() > config.limit
 *
 * Design concepts:
 *   - Dependency Injection: LinterConfig is injected, not created internally
 *   - Strategy Pattern: this check is one of many interchangeable LintCheck strategies
 *   - Open-Closed: thresholds can be changed without modifying this class
 *
 * @author Sophia
 */
public class ConfigurableMethodLengthCheck implements StyleCheck {

    private final LinterConfig config;

    /**
     * Create a check with an injected LinterConfig.
     * The config determines the warning and error thresholds.
     * Thresholds are read live from the config on each check() call,
     * so runtime changes take effect immediately.
     *
     * @param config the shared LinterConfig instance
     */
    public ConfigurableMethodLengthCheck(LinterConfig config) {
        this.config = config;
    }

    /**
     * Convenience constructor: creates with a default LinterConfig
     * (loads from properties file, falls back to built-in defaults).
     */
    public ConfigurableMethodLengthCheck() {
        this(new LinterConfig());
    }

    /**
     * Create a check with explicit thresholds (for quick testing).
     *
     * @param warningThreshold instruction count to trigger a WARNING
     * @param errorThreshold   instruction count to trigger an ERROR
     */
    public ConfigurableMethodLengthCheck(int warningThreshold, int errorThreshold) {
        this(new LinterConfig(warningThreshold, errorThreshold));
    }

    // ─── Getters (for display / testing) ─────────────────────────

    public int getWarningThreshold() {
        return config.getMethodLengthWarningThreshold();
    }

    public int getErrorThreshold() {
        return config.getMethodLengthErrorThreshold();
    }

    /**
     * Returns the injected LinterConfig so the presentation layer
     * can modify thresholds at runtime.
     */
    public LinterConfig getConfig() {
        return config;
    }

    // ─── LintCheck interface ─────────────────────────────────────

    @Override
    public String getName() {
        return "ConfigurableMethodLengthCheck";
    }

    @Override
    public String getDescription() {
        return String.format(
            "Checks if a method exceeds a configurable instruction limit (warning=%d, error=%d)",
            getWarningThreshold(), getErrorThreshold());
    }

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        List<LintIssue> issues = new ArrayList<>();

        // Read thresholds live from config (supports runtime changes)
        int warningThreshold = config.getMethodLengthWarningThreshold();
        int errorThreshold   = config.getMethodLengthErrorThreshold();

        for (MethodInfo method : classInfo.getMethods()) {
            // Skip constructors and static initializers
            if (method.isConstructor() || method.isStaticInitializer()) {
                continue;
            }

            int instructionCount = method.getInstructionCount();
            String location = classInfo.getName() + "." + method.getName() + "()";

            if (instructionCount > errorThreshold) {
                issues.add(new LintIssue(
                    getName(),
                    Severity.ERROR,
                    String.format("Method has %d instructions, exceeds ERROR threshold of %d",
                        instructionCount, errorThreshold),
                    location
                ));
            } else if (instructionCount > warningThreshold) {
                issues.add(new LintIssue(
                    getName(),
                    Severity.WARNING,
                    String.format("Method has %d instructions, exceeds WARNING threshold of %d",
                        instructionCount, warningThreshold),
                    location
                ));
            }
        }

        return issues;
    }
}
