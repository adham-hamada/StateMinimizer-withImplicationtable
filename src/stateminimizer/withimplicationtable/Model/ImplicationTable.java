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
public class ImplicationTable {
    // Cell status
    public static final int UNCHECKED   = 0;
    public static final int EQUIVALENT  = 1;
    public static final int DISTINGUISHED = -1;

    private StateMachine machine;
    private int n; // number of states

    // Lower triangular matrix [i][j] where i > j
    private int[][] status;

    // Implied pairs for each cell: status[i][j] depends on these
    private List<int[]>[][] impliedPairs;

    // Step log for display
    private List<String> stepLog = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public ImplicationTable(StateMachine machine) {
        this.machine = machine;
        this.n = machine.getNumStates();
        this.status = new int[n][n];
        this.impliedPairs = new List[n][n];

        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                impliedPairs[i][j] = new ArrayList<>();
    }

    public void minimize() {
        stepLog.clear();
        stepLog.add("=== STEP 1: Initial Marking ===");
        initialMarking();

        stepLog.add("\n=== STEP 2: Filling Implied Pairs ===");
        fillImpliedPairs();

        stepLog.add("\n=== STEP 3: Iterative Marking ===");
        iterativeMarking();

        stepLog.add("\n=== STEP 4: Equivalence Classes ===");
        markEquivalent();
    }

    // Step 1: Mark pairs with different outputs as distinguished
    private void initialMarking() {
        List<State> states = machine.getStates();
        int numInputs = machine.getNumInputs();

        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                State si = states.get(i);
                State sj = states.get(j);

                boolean diff = false;
                for (int k = 0; k < numInputs; k++) {
                    if (!si.getOutputs()[k].equals(sj.getOutputs()[k])) {
                        diff = true;
                        break;
                    }
                }

                if (diff) {
                    status[i][j] = DISTINGUISHED;
                    stepLog.add("  ✗ (" + si.getName() + "," + sj.getName()
                                + ") — different outputs");
                }
            }
        }
    }

    // Step 2: For unchecked pairs, record implied next-state pairs
    private void fillImpliedPairs() {
        List<State> states = machine.getStates();
        int numInputs = machine.getNumInputs();

        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                if (status[i][j] == DISTINGUISHED) continue;

                State si = states.get(i);
                State sj = states.get(j);

                for (int k = 0; k < numInputs; k++) {
                    String nsI = si.getNextStates()[k];
                    String nsJ = sj.getNextStates()[k];

                    if (!nsI.equals(nsJ)) {
                        // Get indices of these next states
                        int idxI = getStateIndex(nsI);
                        int idxJ = getStateIndex(nsJ);

                        // Normalize so row > col
                        int row = Math.max(idxI, idxJ);
                        int col = Math.min(idxI, idxJ);

                        // Avoid self-dependency
                        if (row != col) {
                            impliedPairs[i][j].add(new int[]{row, col});
                        }
                    }
                }

                if (impliedPairs[i][j].isEmpty()) {
                    stepLog.add("  ✓ (" + si.getName() + "," + sj.getName()
                                + ") — same next states, marking EQUIVALENT");
                    status[i][j] = EQUIVALENT;
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("  ? (").append(si.getName()).append(",")
                      .append(sj.getName()).append(") — depends on: ");
                    for (int[] p : impliedPairs[i][j]) {
                        sb.append("(")
                          .append(states.get(p[0]).getName()).append(",")
                          .append(states.get(p[1]).getName()).append(") ");
                    }
                    stepLog.add(sb.toString());
                }
            }
        }
    }

    // Step 3: Iteratively mark pairs as distinguished
    private void iterativeMarking() {
        List<State> states = machine.getStates();
        boolean changed = true;
        int pass = 1;

        while (changed) {
            changed = false;
            stepLog.add("  Pass " + pass++ + ":");

            for (int i = 1; i < n; i++) {
                for (int j = 0; j < i; j++) {
                    if (status[i][j] != UNCHECKED) continue;

                    for (int[] pair : impliedPairs[i][j]) {
                        if (status[pair[0]][pair[1]] == DISTINGUISHED) {
                            status[i][j] = DISTINGUISHED;
                            changed = true;
                            stepLog.add("    ✗ (" + states.get(i).getName()
                                        + "," + states.get(j).getName()
                                        + ") — implied pair ("
                                        + states.get(pair[0]).getName() + ","
                                        + states.get(pair[1]).getName()
                                        + ") is distinguished");
                            break;
                        }
                    }
                }
            }

            if (!changed) stepLog.add("    No new markings — done.");
        }
    }

    // Step 4: Mark all remaining unchecked as equivalent
    private void markEquivalent() {
        List<State> states = machine.getStates();
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                if (status[i][j] == UNCHECKED) {
                    status[i][j] = EQUIVALENT;
                    stepLog.add("  ✓ (" + states.get(i).getName() + ","
                                + states.get(j).getName() + ") — EQUIVALENT");
                }
            }
        }
    }

    // Build equivalence groups (Union-Find)
    public List<Set<String>> getEquivalenceClasses() {
        List<State> states = machine.getStates();
        Map<String, String> parent = new HashMap<>();
        for (State s : states) parent.put(s.getName(), s.getName());

        // Union equivalent pairs
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                if (status[i][j] == EQUIVALENT) {
                    union(parent, states.get(i).getName(),
                                  states.get(j).getName());
                }
            }
        }

        // Group by root
        Map<String, Set<String>> groups = new LinkedHashMap<>();
        for (State s : states) {
            String root = find(parent, s.getName());
            groups.computeIfAbsent(root, k -> new LinkedHashSet<>())
                  .add(s.getName());
        }

        return new ArrayList<>(groups.values());
    }

    // Union-Find helpers
    private String find(Map<String, String> parent, String x) {
        if (!parent.get(x).equals(x))
            parent.put(x, find(parent, parent.get(x)));
        return parent.get(x);
    }

    private void union(Map<String, String> parent, String a, String b) {
        String ra = find(parent, a);
        String rb = find(parent, b);
        if (!ra.equals(rb)) parent.put(ra, rb);
    }

    // Getters for GUI
    public int[][] getStatus() { return status; }
    public List<int[]>[][] getImpliedPairs() { return impliedPairs; }
    public List<String> getStepLog() { return stepLog; }

    private int getStateIndex(String name) {
        List<State> states = machine.getStates();
        for (int i = 0; i < states.size(); i++)
            if (states.get(i).getName().equals(name)) return i;
        return -1;
    }
}
