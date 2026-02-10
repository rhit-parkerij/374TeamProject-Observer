package domain.checks;

import domain.ClassInfo;
import domain.MethodInfo;
import domain.Severity;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Style Check: Configurable Method Length Check
 * Checks if a method's length (number of instructions) exceeds a configurable limit.
 *
 * Instead of hardcoding the threshold, this check reads from a configuration file
 * (linter-config.properties) to determine the warning and error limits.
 * Users can also override thresholds via constructor parameters.
 *
 * ASM Implementation: methodNode.instructions.size() > config.limit
 *
 * @author Sophia
 */
public class ConfigurableMethodLengthCheck implements StyleCheck {

    private static final String CONFIG_FILE = "linter-config.properties";
    private static final int DEFAULT_WARNING_THRESHOLD = 50;
    private static final int DEFAULT_ERROR_THRESHOLD = 100;

    private final int warningThreshold;
    private final int errorThreshold;

    /**
     * Create a check that reads thresholds from the configuration file.
     * If the config file is not found or missing keys, defaults are used.
     *   - method.length.warning.threshold (default: 50)
     *   - method.length.error.threshold   (default: 100)
     */
    public ConfigurableMethodLengthCheck() {
        Properties props = loadConfig();
        this.warningThreshold = getIntProperty(props, "method.length.warning.threshold", DEFAULT_WARNING_THRESHOLD);
        this.errorThreshold   = getIntProperty(props, "method.length.error.threshold",   DEFAULT_ERROR_THRESHOLD);
    }

    /**
     * Create a check with explicit thresholds (overrides config file).
     * Useful for testing or interactive user input.
     *
     * @param warningThreshold instruction count to trigger a WARNING
     * @param errorThreshold   instruction count to trigger an ERROR
     */
    public ConfigurableMethodLengthCheck(int warningThreshold, int errorThreshold) {
        this.warningThreshold = warningThreshold;
        this.errorThreshold   = errorThreshold;
    }

    // ─── Config loading ──────────────────────────────────────────

    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            // Silently fall back to defaults
        }
        return props;
    }

    private int getIntProperty(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                // Invalid number in config, use default
            }
        }
        return defaultValue;
    }

    // ─── Getters (for display / testing) ─────────────────────────

    public int getWarningThreshold() {
        return warningThreshold;
    }

    public int getErrorThreshold() {
        return errorThreshold;
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
            warningThreshold, errorThreshold);
    }

    @Override
    public List<LintIssue> check(ClassInfo classInfo) {
        List<LintIssue> issues = new ArrayList<>();

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
