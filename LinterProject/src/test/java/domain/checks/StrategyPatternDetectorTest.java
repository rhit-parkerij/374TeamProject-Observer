package domain.checks;

import datasource.ClassFileReader;
import domain.ClassInfo;
import domain.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StrategyPatternDetectorTest {

    private StrategyPatternDetector check;
    private ClassFileReader reader;
    private Path targetDir;

    @BeforeEach
    void setUp() {
        check = new StrategyPatternDetector();
        reader = new ClassFileReader();
        targetDir = Paths.get("target/classes");
    }

    // ════════════════════════════════════════════════════
    // GOOD: 2 implementers → INFO
    // ════════════════════════════════════════════════════

    @Test
    void testGoodStrategyProducesInfo() throws Exception {
        Map<String, ClassInfo> all = loadAll(
                "test.demo.strategy.good.Strategy",
                "test.demo.strategy.good.ClassA",
                "test.demo.strategy.good.ClassB",
                "test.demo.strategy.good.Context"
        );

        ClassInfo iface = all.get("test.demo.strategy.good.Strategy");
        List<LintIssue> issues = check.checkWithContext(iface, all);

        assertFalse(issues.isEmpty(), "Should detect Strategy pattern");

        assertTrue(
                issues.stream().anyMatch(i -> i.getSeverity() == Severity.INFO),
                "Should produce INFO severity for multiple implementers"
        );

        assertTrue(
                issues.stream().anyMatch(i ->
                        i.getMessage().contains("ClassA") &&
                        i.getMessage().contains("ClassB")),
                "Message should list ClassA and ClassB"
        );
    }

    // ════════════════════════════════════════════════════
    // BAD: 1 implementer → WARNING
    // ════════════════════════════════════════════════════

    @Test
    void testBadStrategyProducesWarning() throws Exception {
        Map<String, ClassInfo> all = loadAll(
                "test.demo.strategy.bad.Strategy",
                "test.demo.strategy.bad.ClassB",
                "test.demo.strategy.bad.Context"
        );

        ClassInfo iface = all.get("test.demo.strategy.bad.Strategy");
        List<LintIssue> issues = check.checkWithContext(iface, all);

        assertFalse(issues.isEmpty(), "Should detect incomplete Strategy pattern");

        assertTrue(
                issues.stream().anyMatch(i -> i.getSeverity() == Severity.WARNING),
                "Should produce WARNING severity for single implementer"
        );

        assertTrue(
                issues.stream().anyMatch(i ->
                        i.getMessage().contains("ClassB")),
                "Message should list the single implementer"
        );
    }

    // ════════════════════════════════════════════════════
    // BASIC single-class check
    // ════════════════════════════════════════════════════

    @Test
    void testBasicFlagsInterface() throws Exception {
        ClassInfo iface = loadClass("test.demo.strategy.good.Strategy");
        List<LintIssue> issues = check.check(iface);

        assertFalse(issues.isEmpty(), "Basic check should flag interfaces");
        assertTrue(
                issues.stream().anyMatch(i -> i.getSeverity() == Severity.INFO),
                "Basic detection should be INFO"
        );
    }

    @Test
    void testBasicDoesNotFlagConcreteClass() throws Exception {
        ClassInfo cls = loadClass("test.demo.strategy.good.Context");
        List<LintIssue> issues = check.check(cls);

        assertTrue(issues.isEmpty(), "Concrete class should not be flagged by basic check");
    }

    // ════════════════════════════════════════════════════
    // Metadata
    // ════════════════════════════════════════════════════

    @Test
    void testCheckNameIsCorrect() {
        assertEquals("StrategyPatternDetector", check.getName());
    }

    @Test
    void testDescriptionIsNotEmpty() {
        assertFalse(check.getDescription().isEmpty());
    }

    // ════════════════════════════════════════════════════
    // Location validation
    // ════════════════════════════════════════════════════

    @Test
    void testIssueLocationContainsInterfaceName() throws Exception {
        Map<String, ClassInfo> all = loadAll(
                "test.demo.strategy.good.Strategy",
                "test.demo.strategy.good.ClassA",
                "test.demo.strategy.good.ClassB"
        );

        ClassInfo iface = all.get("test.demo.strategy.good.Strategy");
        List<LintIssue> issues = check.checkWithContext(iface, all);

        for (LintIssue issue : issues) {
            assertTrue(issue.getLocation().contains("Strategy"));
        }
    }

    // ════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════

    private ClassInfo loadClass(String className) throws Exception {
        String classPath = className.replace('.', '/') + ".class";
        return reader.loadClassFromFile(targetDir.resolve(classPath));
    }

    private Map<String, ClassInfo> loadAll(String... classNames) throws Exception {
    Map<String, ClassInfo> all = new HashMap<>();
    for (String name : classNames) {
        ClassInfo ci = loadClass(name);
        all.put(name, ci); // ONLY ONE KEY
    }
    return all;
}
}