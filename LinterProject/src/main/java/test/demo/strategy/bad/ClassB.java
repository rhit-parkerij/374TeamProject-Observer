package test.demo.strategy.bad;

public class ClassB implements Strategy {
    @Override
    public int execute(int a, int b) {
        return a * b;
    }   
    
}
