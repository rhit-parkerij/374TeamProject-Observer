package test.demo;

import java.util.ArrayList;
import java.util.HashMap;

public class Demo_InterfaceViolation {
    
    private ArrayList<String> names;

    private HashMap<String, Integer> scores;
    
    public Demo_InterfaceViolation() {
        names = new ArrayList<>();
        scores = new HashMap<>();
    }
    
    public ArrayList<String> getNames() {
        return names;
    }
    
    public void setScores(HashMap<String, Integer> newScores) {
        scores = newScores;
    }
}
