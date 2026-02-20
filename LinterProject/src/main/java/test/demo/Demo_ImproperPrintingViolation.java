package test.demo;

public class Demo_ImproperPrintingViolation {
    public void outPrintln() {
        // Print inside loop (one call site in bytecode)
        for (int i = 0; i < 3; i++) {
            System.out.println("Count: " + i);
        }

        // Another call site
        System.out.println("hello");
    }

    public void errPrint() {
        System.err.print("oops");
    }

    public void outPrintf() {
        System.out.printf("x=%d%n", 42);
    }

    public void outAppend() {
        System.out.append('A');
    }

    public void multiplePrints() {
        System.out.println("1");
        System.out.println("2");
    }
}
