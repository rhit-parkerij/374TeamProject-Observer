package test.demo.strategy.good;

public class ClassB implements Strategy {
    @Override
    public int execute(int a, int b) {
        return a * b;
    }   
    
}
