package presentation;

import datasource.ClassFileReader;
import domain.ClassInfo;
import domain.FieldInfo;
import domain.MethodInfo;
import domain.LinterConfig;
import domain.LinterEngine;
import domain.ResultManager;
import domain.checks.*;

import java.io.IOException;
import java.util.*;

/**
 * Main entry point for the Java Linter application (Presentation Layer).
 * This class sets up and coordinates all components using the 3-layer architecture:
 * - Presentation Layer (this class + ConsoleReporter)
 * - Domain Layer (LinterEngine + LintChecks)
 * - Data Source Layer (ClassFileReader)
 *
 * Supports interactive mode (no args) and CLI mode (with args).
 */
public class LinterDisplay {

    private final ClassFileReader reader;
    private LinterEngine engine;
    private final ConsoleReporter reporter;
    private final ResultManager resultManager;
    private final Scanner scanner;
    private boolean showAsmDetails = false;

    // Shared configuration for tunable rules (injected into checks)
    private final LinterConfig linterConfig;

    // All available checks (for enable/disable selection)
    private final List<LintCheck> allChecks;
    private final boolean[] checkEnabled;

    public LinterDisplay() {
        this.reader = new ClassFileReader();
        this.linterConfig = new LinterConfig();  // loads from properties file
        this.allChecks = createAllChecks();
        this.checkEnabled = new boolean[allChecks.size()];
        Arrays.fill(checkEnabled, true); // all enabled by default
        this.engine = buildEngineFromSelection();
        this.reporter = new ConsoleReporter();
        this.resultManager = new ResultManager();
        this.scanner = new Scanner(System.in);
    }

    /**
     * Create all available checks (used for interactive selection).
     *
     * Design Note: Each check is instantiated here. To follow the Open-Closed
     * Principle more strictly, a registry or service-loader approach could be
     * used so that new checks can be added without modifying this method.
     * For this project scope, we group checks by category for clarity.
     */
    private List<LintCheck> createAllChecks() {
        List<LintCheck> checks = new ArrayList<>();
        // STYLE CHECKS (inject shared LinterConfig into configurable checks)
        addCheck(checks, new ConfigurableMethodLengthCheck(linterConfig));
        addCheck(checks, new FieldNamingCheck());
        addCheck(checks, new UnusedVariableCheck());
        addCheck(checks, new MethodNamingCheck());
        // PRINCIPLE CHECKS
        addCheck(checks, new ProgramToInterfaceCheck());
        addCheck(checks, new SingleResponsibilityCheck());
        addCheck(checks, new OpenClosePrincipleCheck());
        addCheck(checks, new LeastKnowledgeCheck());
        // PATTERN DETECTORS
        addCheck(checks, new AdapterPatternDetector());
        addCheck(checks, new TemplateMethodDetector());
        addCheck(checks, new StrategyPatternDetector());
        addCheck(checks, new DecoratorPatternDetector());
        return checks;
    }

    /**
     * Helper to add a check to the list. Provides a single point of extension
     * for future features like logging, validation, or dependency injection.
     */
    private void addCheck(List<LintCheck> checks, LintCheck check) {
        checks.add(check);
    }

    /**
     * Build a LinterEngine with only the enabled checks.
     */
    private LinterEngine buildEngineFromSelection() {
        LinterEngine eng = new LinterEngine();
        for (int i = 0; i < allChecks.size(); i++) {
            if (checkEnabled[i]) {
                eng.addCheck(allChecks.get(i));
            }
        }
        return eng;
    }

    public static void main(String[] args) {
        LinterDisplay display = new LinterDisplay();
        display.run(args);
    }

    public void run(String[] args) {
        printBanner();

        if (args.length > 0) {
            // CLI mode: use arguments directly
            runAnalysis(loadFromClassNames(args));
        } else {
            // Interactive mode
            interactiveMenu();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  INTERACTIVE MENU
    // ═══════════════════════════════════════════════════════════════════

    private void interactiveMenu() {
        boolean running = true;

        while (running) {
            System.out.println("\n╔════════════════════════════════════════════════════════════╗");
            System.out.println("║                     MAIN MENU                              ║");
            System.out.println("╠════════════════════════════════════════════════════════════╣");
            System.out.println("║  1. Analyze class(es) by fully qualified name              ║");
            System.out.println("║  2. Analyze all .class files in a folder                   ║");
            System.out.println("║  3. Analyze a single .class file by path                   ║");
            System.out.println("║  4. Configure checks (enable / disable)                    ║");
            System.out.println("║  5. Toggle ASM detail display  [" + (showAsmDetails ? "ON " : "OFF") + "]                       ║");
            System.out.println("║  6. Show current configuration                             ║");
            System.out.println("║  7. Tune method-length thresholds at runtime               ║");
            System.out.println("║  0. Exit                                                   ║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    handleClassNameInput();
                    break;
                case "2":
                    handleFolderInput();
                    break;
                case "3":
                    handleFileInput();
                    break;
                case "4":
                     handleCheckConfiguration();
                    break;
                case "5":
                    showAsmDetails = !showAsmDetails;
                    System.out.println("ASM detail display is now " + (showAsmDetails ? "ON" : "OFF"));
                    break;
                case "6":
                    printCurrentConfig();
                    break;
                case "7":
                    handleThresholdTuning();
                    break;
                case "0":
                    System.out.println("Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    // ─── Option 1: class names ─────────────────────────────────────

    private void handleClassNameInput() {
        System.out.println("\nEnter fully qualified class names (comma or space separated):");
        System.out.println("  Example: java.util.ArrayList, java.util.HashMap");
        System.out.print("> ");
        String line = scanner.nextLine().trim();
        if (line.isEmpty()) {
            System.out.println("No input provided.");
            return;
        }

        String[] names = line.split("[,\\s]+");
        List<ClassInfo> classes = loadFromClassNames(names);
        if (!classes.isEmpty()) {
            runAnalysis(classes);
        }
    }

    // ─── Option 2: folder ──────────────────────────────────────────

    private void handleFolderInput() {
        System.out.println("\nEnter the folder path containing .class files:");
        System.out.println("  Example: target/classes");
        System.out.print("> ");
        String folderPath = scanner.nextLine().trim();
        if (folderPath.isEmpty()) {
            System.out.println("No input provided.");
            return;
        }

        try {
            List<ClassInfo> classes = reader.loadClassesFromDirectory(folderPath);
            if (classes.isEmpty()) {
                System.out.println("No .class files found in: " + folderPath);
            } else {
                System.out.println("Loaded " + classes.size() + " class(es) from folder.");
                runAnalysis(classes);
            }
        } catch (IOException e) {
            System.err.println("Error loading from folder: " + e.getMessage());
        }
    }

    // ─── Option 3: single file path ───────────────────────────────

    private void handleFileInput() {
        System.out.println("\nEnter the path to a .class file:");
        System.out.println("  Example: target/classes/domain/ClassInfo.class");
        System.out.print("> ");
        String filePath = scanner.nextLine().trim();
        if (filePath.isEmpty()) {
            System.out.println("No input provided.");
            return;
        }

        try {
            ClassInfo classInfo = reader.loadClassFromFilePath(filePath);
            List<ClassInfo> classes = new ArrayList<>();
            classes.add(classInfo);
            System.out.println("Loaded class: " + classInfo.getName());
            runAnalysis(classes);
        } catch (IOException e) {
            System.err.println("Error loading file: " + e.getMessage());
        }
    }

    // ─── Option 4: configure checks ──────────────────────────────

    private void handleCheckConfiguration() {
        boolean configuring = true;

        while (configuring) {
            System.out.println("\n─────────────── CHECK CONFIGURATION ───────────────");
            for (int i = 0; i < allChecks.size(); i++) {
                LintCheck check = allChecks.get(i);
                String status = checkEnabled[i] ? "ENABLED " : "DISABLED";
                String category = getCheckCategory(check);
                System.out.printf("  %d. [%s] [%s] %s%n", i + 1, status, category, check.getName());
                System.out.printf("              └─ %s%n", check.getDescription());
            }
            System.out.println("───────────────────────────────────────────────────");
            System.out.println("  Enter a number to toggle, 'all' to enable all, 'none' to disable all, or 'done' to finish:");
            System.out.print("> ");

            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("done") || input.isEmpty()) {
                configuring = false;
            } else if (input.equals("all")) {
                Arrays.fill(checkEnabled, true);
                System.out.println("All checks enabled.");
            } else if (input.equals("none")) {
                Arrays.fill(checkEnabled, false);
                System.out.println("All checks disabled.");
            } else {
                try {
                    int idx = Integer.parseInt(input) - 1;
                    if (idx >= 0 && idx < allChecks.size()) {
                        checkEnabled[idx] = !checkEnabled[idx];
                        System.out.println(allChecks.get(idx).getName() + " is now "
                                + (checkEnabled[idx] ? "ENABLED" : "DISABLED"));
                    } else {
                        System.out.println("Invalid number.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input.");
                }
            }
        }

        // Rebuild engine with updated selection
        this.engine = buildEngineFromSelection();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CORE ANALYSIS
    // ═══════════════════════════════════════════════════════════════════

    private List<ClassInfo> loadFromClassNames(String[] names) {
        List<ClassInfo> classes = new ArrayList<>();
        for (String name : names) {
            try {
                classes.add(reader.loadClass(name.trim()));
            } catch (IOException e) {
                System.err.println("Failed to load class: " + name + " (" + e.getMessage() + ")");
            }
        }
        return classes;
    }

    private void runAnalysis(List<ClassInfo> classes) {
        printRegisteredChecks();
        System.out.println("Analyzing " + classes.size() + " class(es)...\n");

        // Show ASM details if enabled
        if (showAsmDetails) {
            for (ClassInfo classInfo : classes) {
                printAsmDetails(classInfo);
            }
        }

        // Use analyzeAll so PatternChecks get access to all classes via checkWithContext
        List<LintIssue> allIssues = engine.analyzeAll(classes);

        // Store results per class for result management
        ResultManager localResult = new ResultManager();
        for (LintIssue issue : allIssues) {
            localResult.addResult(issue.getLocation(), List.of(issue));
        }

        reporter.report(allIssues);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  ASM DETAIL DISPLAY (toggleable)
    // ═══════════════════════════════════════════════════════════════════

    private void printAsmDetails(ClassInfo classInfo) {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│  ASM ClassNode Details                                       │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");

        // --- Class Info ---
        System.out.println("  CLASS: " + classInfo.getName());
        System.out.println("    Internal Name : " + classInfo.getInternalName());
        System.out.println("    Super Class   : " + (classInfo.getSuperClassName() != null ? classInfo.getSuperClassName() : "none"));
        System.out.println("    Interfaces    : " + (classInfo.getInterfaces().isEmpty() ? "none" : String.join(", ", classInfo.getInterfaces())));
        System.out.println("    Is Public     : " + classInfo.isPublic());
        System.out.println("    Is Abstract   : " + classInfo.isAbstract());
        System.out.println("    Is Interface  : " + classInfo.isInterface());
        System.out.println("    Is Final      : " + classInfo.isFinal());

        // --- Fields ---
        System.out.println("\n  FIELDS (" + classInfo.getFields().size() + "):");
        if (classInfo.getFields().isEmpty()) {
            System.out.println("    (none)");
        } else {
            for (FieldInfo field : classInfo.getFields()) {
                String access = getAccessString(field.isPublic(), field.isPrivate(), field.isProtected());
                System.out.printf("    %-12s %-30s : %s%s%s%n",
                        access,
                        field.getName(),
                        field.getTypeName(),
                        field.isStatic() ? " [static]" : "",
                        field.isFinal() ? " [final]" : "");
            }
        }

        // --- Methods ---
        System.out.println("\n  METHODS (" + classInfo.getMethods().size() + "):");
        if (classInfo.getMethods().isEmpty()) {
            System.out.println("    (none)");
        } else {
            for (MethodInfo method : classInfo.getMethods()) {
                String access = getAccessString(method.isPublic(), method.isPrivate(), method.isProtected());
                String params = String.join(", ", method.getParameterTypeNames());
                System.out.printf("    %-12s %-25s(%s) : %s  [%d instructions]%s%s%n",
                        access,
                        method.getName(),
                        params,
                        method.getReturnTypeName(),
                        method.getInstructionCount(),
                        method.isStatic() ? " [static]" : "",
                        method.isAbstract() ? " [abstract]" : "");
            }
        }
        System.out.println("──────────────────────────────────────────────────────────────\n");
    }

    private String getAccessString(boolean isPublic, boolean isPrivate, boolean isProtected) {
        if (isPublic) return "[public]";
        if (isPrivate) return "[private]";
        if (isProtected) return "[protected]";
        return "[package]";
    }

    // ═══════════════════════════════════════════════════════════════════
    //  DISPLAY HELPERS
    // ═══════════════════════════════════════════════════════════════════

    private void printBanner() {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║              Java Linter v2.0 - CSSE374                    ║");
        System.out.println("╠════════════════════════════════════════════════════════════╣");
        System.out.println("║  Check Categories:                                         ║");
        System.out.println("║    [STYLE]     - Style Checks (naming, length)             ║");
        System.out.println("║    [PRINCIPLE] - Design Principle Checks                   ║");
        System.out.println("║    [PATTERN]   - Design Pattern Detectors                  ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private void printRegisteredChecks() {
        System.out.println("Active Checks:");
        System.out.println("─────────────────────────────────────────────────────────────");
        for (LintCheck check : engine.getChecks()) {
            String category = getCheckCategory(check);
            System.out.printf("  [%s] %s%n", category, check.getName());
            System.out.printf("         └─ %s%n", check.getDescription());
        }
        System.out.println();
    }

    private void printCurrentConfig() {
        System.out.println("\n═══════════════ CURRENT CONFIGURATION ═══════════════");
        System.out.println("ASM Detail Display : " + (showAsmDetails ? "ON" : "OFF"));
        System.out.println();
        System.out.println("Tunable Rules (via LinterConfig):");
        System.out.println("  Config source          : " + linterConfig.getConfigSource());
        System.out.println("  Method length WARNING  : " + linterConfig.getMethodLengthWarningThreshold());
        System.out.println("  Method length ERROR    : " + linterConfig.getMethodLengthErrorThreshold());
        System.out.println();
        System.out.println("Checks:");
        for (int i = 0; i < allChecks.size(); i++) {
            LintCheck check = allChecks.get(i);
            String status = checkEnabled[i] ? "ENABLED" : "DISABLED";
            String category = getCheckCategory(check);
            System.out.printf("  %d. [%s] [%s] %s%n", i + 1, status, category, check.getName());
        }
        System.out.println("════════════════════════════════════════════════════\n");
    }

    // ─── Option 7: tune method-length thresholds at runtime ────────

    private void handleThresholdTuning() {
        System.out.println("\n─────────── TUNE METHOD-LENGTH THRESHOLDS ───────────");
        System.out.println("  Current WARNING threshold : " + linterConfig.getMethodLengthWarningThreshold());
        System.out.println("  Current ERROR   threshold : " + linterConfig.getMethodLengthErrorThreshold());
        System.out.println("  Config source             : " + linterConfig.getConfigSource());
        System.out.println("─────────────────────────────────────────────────────");
        System.out.println("  Options:");
        System.out.println("    1. Set new thresholds");
        System.out.println("    2. Reload from properties file");
        System.out.println("    3. Reset to defaults (warning=50, error=100)");
        System.out.println("    0. Back");
        System.out.print("> ");

        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1":
                setNewThresholds();
                break;
            case "2":
                linterConfig.loadFromFile();
                System.out.println("Reloaded from properties file.");
                System.out.println("  WARNING threshold : " + linterConfig.getMethodLengthWarningThreshold());
                System.out.println("  ERROR   threshold : " + linterConfig.getMethodLengthErrorThreshold());
                break;
            case "3":
                linterConfig.resetToDefaults();
                System.out.println("Reset to defaults: WARNING=50, ERROR=100");
                break;
            case "0":
                break;
            default:
                System.out.println("Invalid option.");
        }
    }

    private void setNewThresholds() {
        try {
            System.out.print("  Enter new WARNING threshold: ");
            int warning = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("  Enter new ERROR threshold: ");
            int error = Integer.parseInt(scanner.nextLine().trim());

            linterConfig.setMethodLengthThresholds(warning, error);
            System.out.println("  Thresholds updated: WARNING=" + warning + ", ERROR=" + error);
            System.out.println("  These changes take effect on the next analysis run.");
        } catch (NumberFormatException e) {
            System.out.println("  Invalid number. Thresholds not changed.");
        } catch (IllegalArgumentException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    private String getCheckCategory(LintCheck check) {
        if (check instanceof StyleCheck) {
            return "STYLE    ";
        } else if (check instanceof PrincipleCheck) {
            return "PRINCIPLE";
        } else if (check instanceof PatternCheck) {
            return "PATTERN  ";
        }
        return "UNKNOWN  ";
    }

    private void printUsage() {
        System.out.println("Usage:");
        System.out.println("  Interactive mode : java presentation.LinterDisplay");
        System.out.println("  CLI mode         : java presentation.LinterDisplay <class1> [class2] ...");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  <class>  Fully qualified class name (e.g., java.util.ArrayList)");
        System.out.println();
        System.out.println("Example:");
        System.out.println("  java presentation.LinterDisplay java.util.ArrayList java.util.HashMap");
    }
}
