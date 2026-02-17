package test.demo;

/**
 * Demo class to test SingleResponsibilityCheck (God Class)
 * This class violates SRP by having too many methods and fields,
 * and also has low cohesion (high LCOM4).
 * 
 * Expected Issues:
 * - Too many fields (>10)
 * - Too many methods (>20)
 * - High LCOM4 value (multiple responsibilities)
 * 
 * @author Wenxin
 */
public class Demo_GodClass {
    
    // User management fields (Responsibility 1)
    private String userId;
    private String userName;
    private String userEmail;
    private String userPassword;
    
    // Order management fields (Responsibility 2)
    private int orderId;
    private double orderTotal;
    private String orderStatus;
    
    // Product management fields (Responsibility 3)
    private String productId;
    private String productName;
    private double productPrice;
    private int productStock;
    
    // Notification fields (Responsibility 4)
    private String emailSubject;
    private String emailBody;
    
    // Constructor
    public Demo_GodClass() {
        this.userId = "";
        this.userName = "";
        this.userEmail = "";
        this.userPassword = "";
        this.orderId = 0;
        this.orderTotal = 0.0;
        this.orderStatus = "";
        this.productId = "";
        this.productName = "";
        this.productPrice = 0.0;
        this.productStock = 0;
        this.emailSubject = "";
        this.emailBody = "";
    }
    
    // User management methods (only access user fields)
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    
    public String getUserPassword() { return userPassword; }
    public void setUserPassword(String userPassword) { this.userPassword = userPassword; }
    
    public boolean validateUser() {
        return userName != null && !userName.isEmpty() && userPassword != null;
    }
    
    // Order management methods (only access order fields)
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    
    public double getOrderTotal() { return orderTotal; }
    public void setOrderTotal(double orderTotal) { this.orderTotal = orderTotal; }
    
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    
    public boolean isOrderComplete() {
        return "COMPLETED".equals(orderStatus);
    }
    
    // Product management methods (only access product fields)
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public double getProductPrice() { return productPrice; }
    public void setProductPrice(double productPrice) { this.productPrice = productPrice; }
    
    public int getProductStock() { return productStock; }
    public void setProductStock(int productStock) { this.productStock = productStock; }
    
    public boolean isProductAvailable() {
        return productStock > 0;
    }
    
    // Notification methods (only access email fields)
    public String getEmailSubject() { return emailSubject; }
    public void setEmailSubject(String emailSubject) { this.emailSubject = emailSubject; }
    
    public String getEmailBody() { return emailBody; }
    public void setEmailBody(String emailBody) { this.emailBody = emailBody; }
    
    public void sendEmail() {
        System.out.println("Sending email: " + emailSubject);
    }
    
    public static void main(String[] args) {
        Demo_GodClass god = new Demo_GodClass();
        
        // User operations
        god.setUserName("Alice");
        god.setUserEmail("alice@example.com");
        System.out.println("User: " + god.getUserName());
        
        // Order operations
        god.setOrderId(123);
        god.setOrderTotal(99.99);
        System.out.println("Order: " + god.getOrderId());
        
        // Product operations
        god.setProductName("Laptop");
        god.setProductPrice(1299.99);
        System.out.println("Product: " + god.getProductName());
        
        // Email operations
        god.setEmailSubject("Order Confirmation");
        god.sendEmail();
    }
}
