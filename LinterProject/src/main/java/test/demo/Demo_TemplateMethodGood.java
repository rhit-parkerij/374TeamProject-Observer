package test.demo;

/**
 * Demo abstract class to test TemplateMethodDetector
 * This class implements the Template Method pattern correctly.
 * 
 * Expected Issue:
 * - Template Method pattern detected (INFO level)
 * - The 'processData()' method is a template method that calls
 *   abstract steps: readData() and writeData()
 * 
 * @author Wenxin
 */
public abstract class Demo_TemplateMethodGood {
    
    // Abstract steps that subclasses must implement
    protected abstract String readData();
    protected abstract void writeData(String data);
    
    // Template method - defines the algorithm skeleton
    // This should be detected by TemplateMethodDetector
    public final void processData() {
        System.out.println("Starting data processing...");
        
        // Step 1: Read data (abstract step)
        String data = readData();
        
        // Step 2: Transform data (concrete step)
        String transformed = transformData(data);
        
        // Step 3: Write data (abstract step)
        writeData(transformed);
        
        System.out.println("Data processing complete.");
    }
    
    // Concrete method - shared implementation
    protected String transformData(String data) {
        return data.toUpperCase();
    }
    
    // Another template method with multiple abstract steps
    public final void execute() {
        readData();
        writeData("test");
    }
}

/**
 * Concrete implementation for file-based processing
 */
class FileProcessor extends Demo_TemplateMethodGood {
    @Override
    protected String readData() {
        System.out.println("Reading data from file...");
        return "file data";
    }
    
    @Override
    protected void writeData(String data) {
        System.out.println("Writing to file: " + data);
    }
    
    public static void main(String[] args) {
        Demo_TemplateMethodGood processor = new FileProcessor();
        processor.processData();
    }
}
