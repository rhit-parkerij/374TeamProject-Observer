package test.demo;

import java.util.List;
import java.util.Map;

public class Demo_InterfaceGood {
    
    private List<String> names;
    
    private Map<String, Integer> scores;
    
    public Demo_InterfaceGood() {
        names = new java.util.ArrayList<>();
        scores = new java.util.HashMap<>();
    }
    
    public List<String> getNames() {
        return names;
    }

    public void setScores(Map<String, Integer> newScores) {
        scores = newScores;
    }
}
