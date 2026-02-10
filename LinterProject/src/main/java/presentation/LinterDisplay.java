package presentation;

import datasource.ClassFileReader;
import domain.ClassInfo;
import domain.FieldInfo;
import domain.MethodInfo;
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

    // All available checks (for enable/disable selection)
    private final List<LintCheck> allChecks;
    private final boolean[] checkEnabled;

    public LinterDisplay() {
        this.reader = new ClassFileReader();
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
     */
    private List<LintCheck> createAllChecks() {
        List<LintCheck> checks = new ArrayList<>();
        // STYLE CHECKS
        checks.add(new ConfigurableMethodLengthCheck());
        checks.add(new FieldNamingCheck());
        // PRINCIPLE CHECKS
        checks.add(new ProgramToInterfaceCheck());
        checks.add(new SingleResponsibilityCheck());
        // PATTERN DETECTORS
        checks.add(new AdapterPatternDetector());
        checks.add(new TemplateMethodDetector());
        checks.add(new StrategyPatternDetector());
        return checks;
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

        // Clear previous results
        ResultManager localResult = new ResultManager();

        for (ClassInfo classInfo : classes) {
            // Show ASM details if enabled
            if (showAsmDetails) {
                printAsmDetails(classInfo);
            }

            List<LintIssue> issues = engine.analyze(classInfo);
            localResult.addResult(classInfo.getName(), issues);
        }

        reporter.report(localResult.getAllIssues());
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
        System.out.println("Checks:");
        for (int i = 0; i < allChecks.size(); i++) {
            LintCheck check = allChecks.get(i);
            String status = checkEnabled[i] ? "ENABLED" : "DISABLED";
            String category = getCheckCategory(check);
            System.out.printf("  %d. [%s] [%s] %s%n", i + 1, status, category, check.getName());
        }
        System.out.println("════════════════════════════════════════════════════\n");
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
