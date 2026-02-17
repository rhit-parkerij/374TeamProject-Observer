package test.demo;

/**
 * Demo class showing GOOD field naming practices.
 * All fields follow proper naming conventions.
 * 
 * Expected: No naming issues
 * 
 * @author Wenxin
 */
public class Demo_FieldNamingGood {
    
    // ✓ Constants in UPPER_SNAKE_CASE
    public static final int MAX_CONNECTIONS = 100;
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final double PI_VALUE = 3.14159;
    
    // ✓ Regular fields in camelCase
    private String firstName;
    private String lastName;
    private int accountNumber;
    private boolean isVerified;
    private double balanceAmount;
    
    public Demo_FieldNamingGood(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.accountNumber = 0;
        this.isVerified = false;
        this.balanceAmount = 0.0;
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public int getAccountNumber() {
        return accountNumber;
    }
    
    public void setAccountNumber(int accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public boolean isVerified() {
        return isVerified;
    }
    
    public void setVerified(boolean verified) {
        isVerified = verified;
    }
    
    public double getBalanceAmount() {
        return balanceAmount;
    }
    
    public void setBalanceAmount(double balanceAmount) {
        this.balanceAmount = balanceAmount;
    }
    
    public static void main(String[] args) {
        System.out.println("Max Connections: " + MAX_CONNECTIONS);
        System.out.println("Default Encoding: " + DEFAULT_ENCODING);
        
        Demo_FieldNamingGood demo = new Demo_FieldNamingGood("Alice", "Smith");
        demo.setAccountNumber(12345);
        demo.setVerified(true);
        demo.setBalanceAmount(1000.50);
        
        System.out.println("Full Name: " + demo.getFullName());
        System.out.println("Account: " + demo.getAccountNumber());
        System.out.println("Verified: " + demo.isVerified());
        System.out.println("Balance: $" + demo.getBalanceAmount());
    }
}
