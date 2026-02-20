package test.demo.strategy.good;

public class ClassA implements Strategy {
    @Override
    public int execute(int a, int b) {
        return a + b;
    }
}

