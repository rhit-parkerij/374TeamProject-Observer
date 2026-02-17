package test.demo;

/**
 * Demo class showing GOOD design with high cohesion.
 * All methods work together on the same set of fields.
 * 
 * Expected:
 * - LCOM4 = 1 (highly cohesive, good design)
 * - Positive INFO message about good cohesion
 * 
 * @author Wenxin
 */
public class Demo_HighCohesion {
    
    // All fields are related to a single responsibility: managing a person
    private String firstName;
    private String lastName;
    private int age;
    
    public Demo_HighCohesion(String firstName, String lastName, int age) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
    }
    
    // All methods access the same set of fields (highly cohesive)
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public int getAge() {
        return age;
    }
    
    public void setAge(int age) {
        this.age = age;
    }
    
    // This method accesses all three fields
    public String getFullInfo() {
        return firstName + " " + lastName + ", age " + age;
    }
    
    // This method accesses firstName and lastName
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    // This method accesses all fields
    public boolean isAdult() {
        return age >= 18;
    }
    
    // This method accesses firstName and age
    public String getGreeting() {
        return "Hello, " + firstName + "! You are " + age + " years old.";
    }
    
    public static void main(String[] args) {
        Demo_HighCohesion person = new Demo_HighCohesion("Bob", "Johnson", 25);
        System.out.println(person.getFullInfo());
        System.out.println(person.getFullName());
        System.out.println("Is Adult: " + person.isAdult());
        System.out.println(person.getGreeting());
    }
}
