package domain;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Dynamic configuration system for the Java Linter.
 *
 * This class centralizes all tunable rule thresholds so that checks
 * can be configured at runtime instead of relying on hardcoded values.
 *
 * Configuration is loaded in the following priority order:
 *   1. Runtime overrides (set programmatically or via user input)
 *   2. Properties file  (linter-config.properties on the classpath)
 *   3. Built-in defaults
 *
 * Design concepts used:
 *   - Dependency Injection: this object is injected into checks that need it
 *   - Single Responsibility: only handles configuration, not lint logic
 *   - Open-Closed: new keys can be added without modifying existing checks
 *
 * @author Sophia
 */
public class LinterConfig {

    private static final String CONFIG_FILE = "linter-config.properties";

    // ─── Default values ──────────────────────────────────────────
    private static final int DEFAULT_METHOD_LENGTH_WARNING = 50;
    private static final int DEFAULT_METHOD_LENGTH_ERROR   = 100;

    // ─── Current thresholds (mutable at runtime) ─────────────────
    private int methodLengthWarningThreshold;
    private int methodLengthErrorThreshold;

    // Track where the values came from (for display purposes)
    private String configSource = "defaults";

    // ═════════════════════════════════════════════════════════════
    //  CONSTRUCTORS
    // ═════════════════════════════════════════════════════════════

    /**
     * Create a config that loads from the properties file on the classpath.
     * Falls back to built-in defaults if the file is missing or incomplete.
     */
    public LinterConfig() {
        this.methodLengthWarningThreshold = DEFAULT_METHOD_LENGTH_WARNING;
        this.methodLengthErrorThreshold   = DEFAULT_METHOD_LENGTH_ERROR;
        loadFromFile();
    }

    /**
     * Create a config with explicit thresholds (for testing / programmatic use).
     *
     * @param warningThreshold instruction count to trigger a WARNING
     * @param errorThreshold   instruction count to trigger an ERROR
     */
    public LinterConfig(int warningThreshold, int errorThreshold) {
        this.methodLengthWarningThreshold = warningThreshold;
        this.methodLengthErrorThreshold   = errorThreshold;
        this.configSource = "constructor";
    }

    // ═════════════════════════════════════════════════════════════
    //  FILE LOADING
    // ═════════════════════════════════════════════════════════════

    /**
     * Load (or reload) thresholds from the properties file.
     * Only overrides values that are present and valid in the file.
     */
    public void loadFromFile() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                props.load(is);
                this.methodLengthWarningThreshold = getIntProperty(props,
                        "method.length.warning.threshold", this.methodLengthWarningThreshold);
                this.methodLengthErrorThreshold = getIntProperty(props,
                        "method.length.error.threshold", this.methodLengthErrorThreshold);
                this.configSource = "file (" + CONFIG_FILE + ")";
            }
        } catch (IOException e) {
            // Silently keep current values
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  RUNTIME SETTERS (for interactive / user-driven tuning)
    // ═════════════════════════════════════════════════════════════

    /**
     * Override the warning threshold at runtime.
     *
     * @param threshold new warning threshold (must be > 0)
     * @throws IllegalArgumentException if threshold is not positive
     */
    public void setMethodLengthWarningThreshold(int threshold) {
        if (threshold <= 0) {
            throw new IllegalArgumentException("Warning threshold must be positive, got: " + threshold);
        }
        this.methodLengthWarningThreshold = threshold;
        this.configSource = "runtime override";
    }

    /**
     * Override the error threshold at runtime.
     *
     * @param threshold new error threshold (must be > 0)
     * @throws IllegalArgumentException if threshold is not positive
     */
    public void setMethodLengthErrorThreshold(int threshold) {
        if (threshold <= 0) {
            throw new IllegalArgumentException("Error threshold must be positive, got: " + threshold);
        }
        this.methodLengthErrorThreshold = threshold;
        this.configSource = "runtime override";
    }

    /**
     * Convenience method to set both thresholds at once.
     *
     * @param warningThreshold new warning threshold
     * @param errorThreshold   new error threshold
     * @throws IllegalArgumentException if warning >= error or either is not positive
     */
    public void setMethodLengthThresholds(int warningThreshold, int errorThreshold) {
        if (warningThreshold <= 0 || errorThreshold <= 0) {
            throw new IllegalArgumentException("Thresholds must be positive");
        }
        if (warningThreshold >= errorThreshold) {
            throw new IllegalArgumentException(
                    "Warning threshold (" + warningThreshold + ") must be less than error threshold (" + errorThreshold + ")");
        }
        this.methodLengthWarningThreshold = warningThreshold;
        this.methodLengthErrorThreshold   = errorThreshold;
        this.configSource = "runtime override";
    }

    /**
     * Reset thresholds to built-in defaults (ignoring file and runtime overrides).
     */
    public void resetToDefaults() {
        this.methodLengthWarningThreshold = DEFAULT_METHOD_LENGTH_WARNING;
        this.methodLengthErrorThreshold   = DEFAULT_METHOD_LENGTH_ERROR;
        this.configSource = "defaults";
    }

    // ═════════════════════════════════════════════════════════════
    //  GETTERS
    // ═════════════════════════════════════════════════════════════

    public int getMethodLengthWarningThreshold() {
        return methodLengthWarningThreshold;
    }

    public int getMethodLengthErrorThreshold() {
        return methodLengthErrorThreshold;
    }

    /**
     * Returns a human-readable string describing where the current
     * configuration values came from.
     */
    public String getConfigSource() {
        return configSource;
    }

    // ═════════════════════════════════════════════════════════════
    //  DISPLAY
    // ═════════════════════════════════════════════════════════════

    @Override
    public String toString() {
        return String.format(
            "LinterConfig [source=%s, methodLength.warning=%d, methodLength.error=%d]",
            configSource, methodLengthWarningThreshold, methodLengthErrorThreshold);
    }

    // ─── Private helpers ─────────────────────────────────────────

    private int getIntProperty(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                // Invalid number in config, use current value
            }
        }
        return defaultValue;
    }
}
