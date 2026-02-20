package test.demo.strategy.bad;

public class Context {
    private final Strategy strategy = new ClassB();

    public int run(int a, int b) {
        return strategy.execute(a, b);
    }
}