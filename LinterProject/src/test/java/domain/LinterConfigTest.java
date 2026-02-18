package domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LinterConfig.
 * Verifies:
 *   - Default values are correct
 *   - Constructor injection works
 *   - Runtime setters update thresholds
 *   - Validation rejects bad input
 *   - Reset restores defaults
 *   - Config source tracking is accurate
 *
 * @author Sophia
 */
class LinterConfigTest {

    private LinterConfig config;

    @BeforeEach
    void setUp() {
        config = new LinterConfig(50, 100);
    }

    // ─── Constructor tests ───────────────────────────────────────

    @Test
    void testConstructorWithExplicitThresholds() {
        LinterConfig c = new LinterConfig(30, 80);
        assertEquals(30, c.getMethodLengthWarningThreshold());
        assertEquals(80, c.getMethodLengthErrorThreshold());
        assertEquals("constructor", c.getConfigSource());
    }

    @Test
    void testDefaultConstructorUsesFileOrDefaults() {
        // The no-arg constructor tries to load from the file.
        // In the test environment the file may or may not be on the classpath,
        // so we just verify the values are positive and warning < error.
        LinterConfig c = new LinterConfig();
        assertTrue(c.getMethodLengthWarningThreshold() > 0);
        assertTrue(c.getMethodLengthErrorThreshold() > 0);
        assertTrue(c.getMethodLengthWarningThreshold() < c.getMethodLengthErrorThreshold());
    }

    // ─── Runtime setter tests ────────────────────────────────────

    @Test
    void testSetWarningThreshold() {
        config.setMethodLengthWarningThreshold(25);
        assertEquals(25, config.getMethodLengthWarningThreshold());
        assertEquals("runtime override", config.getConfigSource());
    }

    @Test
    void testSetErrorThreshold() {
        config.setMethodLengthErrorThreshold(200);
        assertEquals(200, config.getMethodLengthErrorThreshold());
        assertEquals("runtime override", config.getConfigSource());
    }

    @Test
    void testSetBothThresholds() {
        config.setMethodLengthThresholds(20, 60);
        assertEquals(20, config.getMethodLengthWarningThreshold());
        assertEquals(60, config.getMethodLengthErrorThreshold());
    }

    // ─── Validation tests ────────────────────────────────────────

    @Test
    void testSetWarningThresholdRejectsZero() {
        assertThrows(IllegalArgumentException.class, () ->
            config.setMethodLengthWarningThreshold(0));
    }

    @Test
    void testSetWarningThresholdRejectsNegative() {
        assertThrows(IllegalArgumentException.class, () ->
            config.setMethodLengthWarningThreshold(-5));
    }

    @Test
    void testSetErrorThresholdRejectsZero() {
        assertThrows(IllegalArgumentException.class, () ->
            config.setMethodLengthErrorThreshold(0));
    }

    @Test
    void testSetBothThresholdsRejectsWarningGreaterThanError() {
        assertThrows(IllegalArgumentException.class, () ->
            config.setMethodLengthThresholds(100, 50));
    }

    @Test
    void testSetBothThresholdsRejectsEqualValues() {
        assertThrows(IllegalArgumentException.class, () ->
            config.setMethodLengthThresholds(50, 50));
    }

    // ─── Reset tests ─────────────────────────────────────────────

    @Test
    void testResetToDefaults() {
        config.setMethodLengthThresholds(10, 20);
        config.resetToDefaults();

        assertEquals(50, config.getMethodLengthWarningThreshold());
        assertEquals(100, config.getMethodLengthErrorThreshold());
        assertEquals("defaults", config.getConfigSource());
    }

    // ─── toString test ───────────────────────────────────────────

    @Test
    void testToString() {
        String str = config.toString();
        assertTrue(str.contains("50"));
        assertTrue(str.contains("100"));
        assertTrue(str.contains("constructor"));
    }

    // ─── Runtime change reflected immediately ────────────────────

    @Test
    void testRuntimeChangePersists() {
        config.setMethodLengthWarningThreshold(10);
        config.setMethodLengthErrorThreshold(30);

        // Verify the config still has the new values (not reverted)
        assertEquals(10, config.getMethodLengthWarningThreshold());
        assertEquals(30, config.getMethodLengthErrorThreshold());
    }
}
