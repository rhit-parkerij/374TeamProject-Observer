package test.demo;

/**
 * Demo class to test FieldNamingCheck
 * This class intentionally violates field naming conventions.
 * 
 * Expected Issues:
 * - Constants should be UPPER_SNAKE_CASE
 * - Regular fields should be camelCase
 * 
 * @author Wenxin
 */
public class Demo_FieldNamingViolation {
    
    // ✓ GOOD: Constant in UPPER_SNAKE_CASE
    public static final int MAX_SIZE = 100;
    public static final String DEFAULT_NAME = "Unknown";
    
    // ✗ BAD: Constant NOT in UPPER_SNAKE_CASE
    public static final int maxValue = 999;           // Should be MAX_VALUE
    public static final String defaultMessage = "Hi"; // Should be DEFAULT_MESSAGE
    
    // ✓ GOOD: Regular fields in camelCase
    private String userName;
    private int userAge;
    private boolean isActive;
    
    // ✗ BAD: Regular fields NOT in camelCase
    private String UserName;      // Should be userName (starts with lowercase)
    private int user_age;         // Should be userAge (no underscores)
    private boolean Is_Active;    // Should be isActive
    
    public Demo_FieldNamingViolation() {
        this.userName = "John";
        this.userAge = 25;
        this.isActive = true;
    }
    
    // Methods that use the fields
    public String getUserName() {
        return userName;
    }
    
    public int getUserAge() {
        return userAge;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public static void main(String[] args) {
        System.out.println("Max Size: " + MAX_SIZE);
        System.out.println("Default Name: " + DEFAULT_NAME);
        System.out.println("Max Value: " + maxValue);
        System.out.println("Default Message: " + defaultMessage);
        
        Demo_FieldNamingViolation demo = new Demo_FieldNamingViolation();
        System.out.println("User: " + demo.getUserName());
        System.out.println("Age: " + demo.getUserAge());
        System.out.println("Active: " + demo.isActive());
    }
}
