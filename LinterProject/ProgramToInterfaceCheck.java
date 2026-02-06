/**
 * Principle Check: Program to Interface Check
 * Detects violations of "Program to Interface, not Implementation".
 * For example: using ArrayList instead of List, HashMap instead of Map.
 * 
 * @author Sophia
 */

public class ProgramToInterfaceCheck implements PrincipleCheck {
    @Override
    public void visit(ClassInfo classInfo) {
        // Logic to check if the class is using concrete implementations instead of interfaces
        // This is a placeholder and should be implemented based on the specific AST structure
    }
}
