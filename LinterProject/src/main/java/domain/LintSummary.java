package domain;

/**
 * Summary of lint results.
 */
public class LintSummary {
    private int errorCount;
    private int warningCount;
    private int infoCount;

    public LintSummary() {
        this.errorCount = 0;
        this.warningCount = 0;
        this.infoCount = 0;
    }

    public void incrementErrorCount() {
        errorCount++;
    }

    public void incrementWarningCount() {
        warningCount++;
    }

    public void incrementInfoCount() {
        infoCount++;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public int getInfoCount() {
        return infoCount;
    }

    public int getTotalCount() {
        return errorCount + warningCount + infoCount;
    }

    @Override
    public String toString() {
        return String.format("Summary: %d errors, %d warnings, %d info", 
            errorCount, warningCount, infoCount);
    }
}
