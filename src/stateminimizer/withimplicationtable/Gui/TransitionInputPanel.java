package stateminimizer.withimplicationtable.Gui;

import stateminimizer.withimplicationtable.Model.State;
import stateminimizer.withimplicationtable.Model.StateMachine;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.*;
import java.util.List;

public class TransitionInputPanel {

    // ── GUI references ─────────────────────────────────────────────
    private final JTable      table;
    private final JScrollPane scrollPane;
    private final JLabel      statusLabel;

    // ── State ──────────────────────────────────────────────────────
    private int      numStates;
    private int      numInputs;
    private String[] inputSymbols;
    private String[] stateNames;

    // ── Colors ─────────────────────────────────────────────────────
    private static final Color COLOR_HEADER_BG  = new Color(44,  62,  80);
    private static final Color COLOR_HEADER_FG  = Color.WHITE;
    private static final Color COLOR_STATE_COL  = new Color(235, 245, 255);
    private static final Color COLOR_NEXT_COL   = new Color(245, 255, 245);
    private static final Color COLOR_OUT_COL    = new Color(255, 252, 235);
    private static final Color COLOR_ERROR_CELL = new Color(255, 200, 200);

    // ══════════════════════════════════════════════════════════════
    //  Constructor — exactly 3 args to match MainFrame
    // ══════════════════════════════════════════════════════════════

    public TransitionInputPanel(JTable table,
                                JScrollPane scrollPane,
                                JLabel statusLabel) {
        this.table       = table;
        this.scrollPane  = scrollPane;
        this.statusLabel = statusLabel;

        // Ensures the last edited cell is committed when user
        // clicks a button without pressing Enter first
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    }

    // ══════════════════════════════════════════════════════════════
    //  buildTable(int, int, String)
    //  Called by buildTableButtonActionPerformed in MainFrame.
    //  customNamesRaw is stateNamesField.getText() read directly
    //  in MainFrame — so it is NEVER null or from a stale reference.
    // ══════════════════════════════════════════════════════════════

    public void buildTable(int numStates, int numInputs,
                           String customNamesRaw) {
        this.numStates = numStates;
        this.numInputs = numInputs;

        // Build input symbol labels
        inputSymbols = new String[numInputs];
        for (int i = 0; i < numInputs; i++) {
            inputSymbols[i] = "x=" + i;
        }

        // Parse names — this is where custom names are applied
        stateNames = parseStateNames(numStates, customNamesRaw);

        // Confirm to the user what names are actually being used
        setStatus("Table built — states: "
                + Arrays.toString(stateNames)
                + "  |  Fill all cells then click  ▶ Minimize.");

        // Build column headers:  State | Next(x=0) | Out(x=0) | ...
        int totalCols = 1 + numInputs * 2;
        String[] cols = new String[totalCols];
        cols[0] = "State";
        for (int i = 0; i < numInputs; i++) {
            cols[1 + i * 2]     = "Next (x=" + i + ")";
            cols[1 + i * 2 + 1] = "Out  (x=" + i + ")";
        }

        // Pre-fill the State column; leave all other cells blank
        Object[][] rows = new Object[numStates][totalCols];
        for (int i = 0; i < numStates; i++) {
            rows[i][0] = stateNames[i];
            for (int j = 1; j < totalCols; j++) rows[i][j] = "";
        }

        // State column is read-only; everything else is editable
        DefaultTableModel model = new DefaultTableModel(rows, cols) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col != 0;
            }
        };

        table.setModel(model);
        styleTable();
    }

    // ══════════════════════════════════════════════════════════════
    //  parseStateNames — the core of the fix
    //  Receives the raw String directly, never touches a field ref.
    // ══════════════════════════════════════════════════════════════

    private String[] parseStateNames(int count, String raw) {

        // ── Blank / null → use S0, S1, S2 ... ──
        if (raw == null || raw.trim().isEmpty()) {
            setStatus("No custom names entered — using S0, S1, S2 ...");
            return defaultNames(count);
        }

        raw = raw.trim();

        // ── Split on comma, semicolon, pipe, or any whitespace ──
        //    Handles:  "A,B,C"  "A, B, C"  "A;B;C"  "A B C"
        String[] parts = raw.split("[,;|\\s]+");

        // Filter any empty tokens produced by the split
        List<String> valid = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) valid.add(t);
        }

        // ── Reject duplicates ──
        Set<String>  seen = new LinkedHashSet<>();
        List<String> dups = new ArrayList<>();
        for (String name : valid) {
            if (!seen.add(name.toLowerCase())) dups.add(name);
        }
        if (!dups.isEmpty()) {
            showError("Duplicate state names: " + dups
                    + "\nEvery state must have a unique name.");
            return defaultNames(count);
        }

        // ── Reject names with spaces or special characters ──
        List<String> bad = new ArrayList<>();
        for (String name : valid) {
            if (!name.matches("[A-Za-z0-9_]+")) bad.add(name);
        }
        if (!bad.isEmpty()) {
            showError("Invalid name(s): " + bad
                    + "\nOnly letters, digits, and underscores are allowed."
                    + "\nExamples:  A  B  C   or   q0  q1  q2");
            return defaultNames(count);
        }

        // ── Exact match — perfect ──
        if (valid.size() == count) {
            String[] names = valid.toArray(new String[0]);
            setStatus("✅ Custom names accepted: " + Arrays.toString(names));
            return names;
        }

        // ── Too many names — trim to count ──
        if (valid.size() > count) {
            String[] names = valid.subList(0, count).toArray(new String[0]);
            setStatus("⚠ Too many names provided — using first "
                    + count + ": " + Arrays.toString(names));
            return names;
        }

        // ── Too few names — show a helpful error, fall back to defaults ──
        showError("You entered " + valid.size()
                + " name(s) but selected " + count + " states.\n\n"
                + "Please enter exactly " + count
                + " names separated by commas.\n"
                + "Example:  " + exampleNames(count));
        return defaultNames(count);
    }

    // ══════════════════════════════════════════════════════════════
    //  buildAndValidate — called by Minimize button
    // ══════════════════════════════════════════════════════════════

    public StateMachine buildAndValidate() {

        // Commit any cell that is still being edited
        if (table.isEditing()) table.getCellEditor().stopCellEditing();

        if (table.getModel().getRowCount() == 0) {
            showError("Please build the transition table first.");
            return null;
        }

        boolean      hasErrors = false;
        List<String> errors    = new ArrayList<>();
        resetCellColors();

        for (int row = 0; row < numStates; row++) {
            for (int inp = 0; inp < numInputs; inp++) {
                int nextCol = 1 + inp * 2;
                int outCol  = 2 + inp * 2;

                String nextVal = getCellValue(row, nextCol);
                String outVal  = getCellValue(row, outCol);

                if (nextVal.isEmpty()) {
                    highlightRed(row, nextCol);
                    hasErrors = true;
                    errors.add("Row " + (row+1)
                            + ": Next state for input " + inp + " is empty.");

                } else if (!isValidName(nextVal)) {
                    highlightRed(row, nextCol);
                    hasErrors = true;
                    errors.add("\"" + nextVal + "\" is not a valid state.  "
                            + "Valid states: " + Arrays.toString(stateNames));
                }

                if (outVal.isEmpty()) {
                    highlightRed(row, outCol);
                    hasErrors = true;
                    errors.add("Row " + (row+1)
                            + ": Output for input " + inp + " is empty.");
                }
            }
        }

        if (hasErrors) {
            StringBuilder sb = new StringBuilder("Fix these errors:\n\n");
            int show = Math.min(errors.size(), 6);
            for (int i = 0; i < show; i++)
                sb.append("• ").append(errors.get(i)).append("\n");
            if (errors.size() > 6)
                sb.append("... and ").append(errors.size() - 6)
                  .append(" more.");
            showError(sb.toString());
            setStatus("❌ Fix the red-highlighted cells before minimizing.");
            return null;
        }

        return buildMachine();
    }

    // ══════════════════════════════════════════════════════════════
    //  reset — called by Reset button
    // ══════════════════════════════════════════════════════════════

    public void reset() {
        table.setModel(new DefaultTableModel());
        numStates = 0;
        numInputs = 0;
        setStatus("Reset complete. Set states/inputs and click 'Build Table'.");
    }

    // ══════════════════════════════════════════════════════════════
    //  Private helpers
    // ══════════════════════════════════════════════════════════════

    private StateMachine buildMachine() {
        StateMachine machine = new StateMachine(numInputs, inputSymbols);
        for (int row = 0; row < numStates; row++) {
            State s = new State(stateNames[row], numInputs);
            for (int inp = 0; inp < numInputs; inp++) {
                s.setTransition(inp,
                        getCellValue(row, 1 + inp * 2),
                        getCellValue(row, 2 + inp * 2));
            }
            machine.addState(s);
        }
        setStatus("✅ FSM constructed. Running minimization...");
        return machine;
    }

    private String[] defaultNames(int count) {
        String[] n = new String[count];
        for (int i = 0; i < count; i++) n[i] = "S" + i;
        return n;
    }

    private String exampleNames(int count) {
        String[] pool = {"A","B","C","D","E","F","G","H",
                         "q0","q1","q2","q3","q4","q5"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(", ");
            sb.append(i < pool.length ? pool[i] : "S" + i);
        }
        return sb.toString();
    }

    private String getCellValue(int row, int col) {
        Object v = table.getModel().getValueAt(row, col);
        return (v == null) ? "" : v.toString().trim();
    }

    // Case-insensitive match against the actual parsed state names
    private boolean isValidName(String name) {
        for (String s : stateNames)
            if (s.equalsIgnoreCase(name)) return true;
        return false;
    }

    private void highlightRed(int row, int col) {
        table.putClientProperty("error_" + row + "_" + col, Boolean.TRUE);
        table.repaint();
    }

    private void resetCellColors() {
        for (int r = 0; r < numStates; r++)
            for (int c = 1; c < table.getColumnCount(); c++)
                table.putClientProperty("error_" + r + "_" + c, Boolean.FALSE);
        table.repaint();
    }

    private void styleTable() {
        table.setRowHeight(30);
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.setShowGrid(true);
        table.setGridColor(new Color(200, 200, 200));
        table.setSelectionBackground(new Color(173, 216, 230));
        table.setSelectionForeground(Color.BLACK);

        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        for (int i = 1; i < table.getColumnCount(); i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(95);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t,
                    Object val, boolean sel, boolean focus,
                    int row, int col) {
                Component c = super.getTableCellRendererComponent(
                        t, val, sel, focus, row, col);
                if (!sel) {
                    if (col == 0) {
                        c.setBackground(COLOR_STATE_COL);
                        ((JLabel) c).setFont(
                                new Font("Arial", Font.BOLD, 13));
                    } else if (col % 2 == 1) {
                        c.setBackground(COLOR_NEXT_COL);
                    } else {
                        c.setBackground(COLOR_OUT_COL);
                    }
                    // Override with red if this cell was flagged
                    if (Boolean.TRUE.equals(t.getClientProperty(
                            "error_" + row + "_" + col)))
                        c.setBackground(COLOR_ERROR_CELL);
                }
                ((JLabel) c).setHorizontalAlignment(JLabel.CENTER);
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++)
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);

        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        table.getTableHeader().setBackground(COLOR_HEADER_BG);
        table.getTableHeader().setForeground(COLOR_HEADER_FG);
        table.getTableHeader().setPreferredSize(new Dimension(0, 35));
    }

    private void setStatus(String msg) {
        if (statusLabel != null)
            statusLabel.setText("  " + msg);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(table, msg,
                "Input Error", JOptionPane.ERROR_MESSAGE);
    }

    // ── Getters used by MainFrame ──────────────────────────────────
    public int      getNumStates()    { return numStates;    }
    public int      getNumInputs()    { return numInputs;    }
    public String[] getStateNames()   { return stateNames;   }
    public String[] getInputSymbols() { return inputSymbols; }
}