package test.demo;

public class Demo_LongMethodViolation {
    
    public void tooLongMethod() {
        int sum = 0;
        
        for (int i = 0; i < 100; i++) {
            sum += i;
            if (sum > 50) {
                sum = sum * 2;
            }
            if (sum > 100) {
                sum = sum / 2;
            }
            if (sum > 200) {
                sum = 0;
            }
        }
        
        for (int j = 0; j < 100; j++) {
            sum += j;
            if (sum > 50) {
                sum = sum * 2;
            }
            if (sum > 100) {
                sum = sum / 2;
            }
        }
        
        for (int k = 0; k < 100; k++) {
            sum += k;
            if (sum > 50) {
                sum = sum * 2;
            }
        }
        
        System.out.println("Sum: " + sum);
    }
}
