package test.demo;

/**
 * Demo class with no console printing.
 * Should produce zero issues.
 */
public class Demo_ImproperPrintingGood {

    public void noPrints() {
        int x = 1 + 2;
        String s = "ok" + x;

        if (x > 0) {
            s = s.toLowerCase();
        }

        helperMethod();
    }

    private void helperMethod() {
        int y = 10;
        y++;
    }
}