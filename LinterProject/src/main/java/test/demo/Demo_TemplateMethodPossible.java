package test.demo;

/**
 * Demo abstract class showing a possible Template Method pattern
 * without using the 'final' keyword (weaker pattern).
 * 
 * Expected Issue:
 * - Possible Template Method pattern detected (INFO level)
 * - The 'process()' method is not final but calls abstract steps
 * 
 * @author Wenxin
 */
public abstract class Demo_TemplateMethodPossible {
    
    protected abstract void initialize();
    protected abstract void execute();
    protected abstract void cleanup();
    
    // Not final - weaker template method pattern
    // TemplateMethodDetector should still detect this
    public void process() {
        initialize();
        execute();
        cleanup();
    }
    
    // Another non-final method calling abstract steps
    protected void run() {
        initialize();
        execute();
    }
}

class ConcreteProcessor extends Demo_TemplateMethodPossible {
    @Override
    protected void initialize() {
        System.out.println("Initializing...");
    }
    
    @Override
    protected void execute() {
        System.out.println("Executing...");
    }
    
    @Override
    protected void cleanup() {
        System.out.println("Cleaning up...");
    }
    
    public static void main(String[] args) {
        Demo_TemplateMethodPossible processor = new ConcreteProcessor();
        processor.process();
    }
}
