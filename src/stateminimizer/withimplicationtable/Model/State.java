/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package stateminimizer.withimplicationtable.Model;

/**
 *
 * @author Adham Hamada
 */
public class State {
    private String name;
    private String[] nextStates;   // index = input symbol
    private String[] outputs;      // Mealy: per input | Moore: single output

    public State(String name, int numInputs) {
        this.name = name;
        this.nextStates = new String[numInputs];
        this.outputs = new String[numInputs];
    }

    // Getters & Setters
    public String getName() { return name; }
    public String[] getNextStates() { return nextStates; }
    public String[] getOutputs() { return outputs; }

    public void setTransition(int inputIndex, String nextState, String output) {
        nextStates[inputIndex] = nextState;
        outputs[inputIndex] = output;
    }

    @Override
    public String toString() { return name; }
}
