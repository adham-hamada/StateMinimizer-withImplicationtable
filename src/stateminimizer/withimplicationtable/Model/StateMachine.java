/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package stateminimizer.withimplicationtable.Model;
import java.util.*;
/**
 *
 * @author Adham Hamada
 */
public class StateMachine {
    private List<State> states;
    private int numInputs;
    private String[] inputSymbols;

    public StateMachine(int numInputs, String[] inputSymbols) {
        this.numInputs = numInputs;
        this.inputSymbols = inputSymbols;
        this.states = new ArrayList<>();
    }

    public void addState(State s) { states.add(s); }

    public State getState(String name) {
        return states.stream()
                     .filter(s -> s.getName().equals(name))
                     .findFirst().orElse(null);
    }

    public List<State> getStates() { return states; }
    public int getNumInputs() { return numInputs; }
    public String[] getInputSymbols() { return inputSymbols; }
    public int getNumStates() { return states.size(); }
}
