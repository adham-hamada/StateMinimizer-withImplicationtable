package stateminimizer.withimplicationtable.Gui;

import stateminimizer.withimplicationtable.Model.ImplicationTable;
import stateminimizer.withimplicationtable.Model.State;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * ImplicationTablePanel is a custom JPanel subclass that draws the
 * implication table triangle visually using Java2D graphics.
 *
 * ─── How the triangle works ───────────────────────────────────────
 * For n states (S0..Sn-1), the table has cells for every pair (Si, Sj)
 * where i > j. Drawn as a lower-left triangle:
 *
 *    S1 | [S1,S0]
 *    S2 | [S2,S0] [S2,S1]
 *    S3 | [S3,S0] [S3,S1] [S3,S2]
 *         ──────────────────────
 *           S0      S1      S2       ← column labels
 *
 * Row label  = states.get(i)     (i goes from 1 to n-1)
 * Col label  = states.get(j)     (j goes from 0 to i-1)
 * Cell drawn at grid position (col=j, row=i-1)
 * ──────────────────────────────────────────────────────────────────
 */
public class ImplicationTablePanel extends JPanel {

    // ─── Data provided by MainFrame after minimization runs ───────────────
    private ImplicationTable implicationTable;  // the computed table
    private List<State> states;                 // ordered state list

    // ─── Drawing configuration ────────────────────────────────────────────
    private int cellSize   = 60;    // pixels per cell (user can zoom)
    private int offsetX    = 70;    // left margin (space for row labels)
    private int offsetY    = 20;    // top margin

    // ─── Colors ───────────────────────────────────────────────────────────
    private static final Color COLOR_EQUIV       = new Color(39, 174, 96);    // green
    private static final Color COLOR_DIST        = new Color(192, 57, 43);    // red
    private static final Color COLOR_UNCHECKED   = Color.WHITE;
    private static final Color COLOR_GRID        = new Color(100, 100, 100);  // dark grey
    private static final Color COLOR_LABEL_BG    = new Color(44, 62, 80);     // navy
    private static final Color COLOR_LABEL_FG    = Color.WHITE;
    private static final Color COLOR_IMPLIED_FG  = new Color(52, 73, 94);     // dark text
    private static final Color COLOR_PANEL_BG    = new Color(245, 245, 250);  // light lavender

    // ─── Fonts ────────────────────────────────────────────────────────────
    private Font fontLabel;         // state labels around triangle
    private Font fontSymbol;        // ✓ and ✗ inside cells
    private Font fontImplied;       // implied pair text inside cells

    // ─── State: has data been loaded yet? ─────────────────────────────────
    private boolean hasData = false;

    // ─── Constructor ──────────────────────────────────────────────────────

    public ImplicationTablePanel() {
        setBackground(COLOR_PANEL_BG);
        setDoubleBuffered(true);        // smoother repaints

        // Initialize fonts
        fontLabel   = new Font("Arial",   Font.BOLD,  13);
        fontSymbol  = new Font("Segoe UI Symbol", Font.BOLD, cellSize / 2);
        fontImplied = new Font("Arial",   Font.PLAIN,  9);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  PUBLIC METHOD — loadData()
    //  Called by MainFrame after ImplicationTable.minimize() finishes
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Provides this panel with the data it needs to draw.
     * After calling this, call repaint() to trigger a redraw.
     *
     * @param table   the fully computed ImplicationTable
     * @param states  the ordered list of states from the StateMachine
     */
    public void loadData(ImplicationTable table, List<State> states) {
        this.implicationTable = table;
        this.states           = states;
        this.hasData          = true;

        // Recalculate how large this panel needs to be
        // so the JScrollPane can scroll correctly
        updatePreferredSize();
        repaint();
    }

    /**
     * Changes the cell size (zoom level) and redraws.
     * Called by the zoom buttons / slider in MainFrame.
     *
     * @param newSize  new pixel size per cell (suggest 40–100)
     */
    public void setCellSize(int newSize) {
        this.cellSize  = Math.max(40, Math.min(newSize, 120));
        this.fontSymbol = new Font("Segoe UI Symbol", Font.BOLD, cellSize / 2);
        updatePreferredSize();
        repaint();
    }

    public int getCellSize() { return cellSize; }

    // ══════════════════════════════════════════════════════════════════════
    //  CORE OVERRIDE — paintComponent()
    //  This is called automatically by Swing whenever repaint() is called
    //  or the window is resized/refreshed. All drawing happens here.
    // ══════════════════════════════════════════════════════════════════════

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);  // paints background (COLOR_PANEL_BG)

        // If no data loaded yet, show a placeholder message
        if (!hasData) {
            drawPlaceholder(g);
            return;
        }

        // Upgrade to Graphics2D for better quality drawing
        Graphics2D g2 = (Graphics2D) g;

        // ── Enable anti-aliasing for smooth edges ──
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int n = states.size();

        // ── Draw each component of the triangle in order ──
        drawCells(g2, n);          // 1. Filled colored rectangles
        drawCellContents(g2, n);   // 2. ✓ / ✗ / implied pairs text
        drawGridLines(g2, n);      // 3. Black border lines over cells
        drawRowLabels(g2, n);      // 4. State names on the left
        drawColumnLabels(g2, n);   // 5. State names on the bottom
        drawCornerLabel(g2);       // 6. Small decoration in bottom-left corner
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DRAWING STEP 1 — drawCells()
    //  Fills each triangle cell with its appropriate background color
    // ══════════════════════════════════════════════════════════════════════

    private void drawCells(Graphics2D g2, int n) {
        int[][] status = implicationTable.getStatus();

        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {

                int x = offsetX + j * cellSize;
                int y = offsetY + (i - 1) * cellSize;

                // Choose fill color based on cell status
                switch (status[i][j]) {
                    case ImplicationTable.EQUIVALENT:
                        g2.setColor(COLOR_EQUIV);
                        break;
                    case ImplicationTable.DISTINGUISHED:
                        g2.setColor(COLOR_DIST);
                        break;
                    default:
                        // UNCHECKED — shouldn't happen after minimize() but
                        // draw as white just in case
                        g2.setColor(COLOR_UNCHECKED);
                        break;
                }

                g2.fillRect(x, y, cellSize, cellSize);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DRAWING STEP 2 — drawCellContents()
    //  Draws ✓, ✗, or implied pair names inside each cell
    // ══════════════════════════════════════════════════════════════════════

    private void drawCellContents(Graphics2D g2, int n) {
        int[][] status       = implicationTable.getStatus();
        List<int[]>[][] imp  = implicationTable.getImpliedPairs();

        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {

                int x = offsetX + j * cellSize;
                int y = offsetY + (i - 1) * cellSize;

                if (status[i][j] == ImplicationTable.EQUIVALENT) {
                    // ── Draw ✓ centered in green cell ──
                    drawCenteredSymbol(g2, "✓", x, y, cellSize, Color.WHITE, fontSymbol);

                } else if (status[i][j] == ImplicationTable.DISTINGUISHED) {
                    // ── Draw ✗ centered in red cell ──
                    drawCenteredSymbol(g2, "✗", x, y, cellSize, Color.WHITE, fontSymbol);

                } else {
                    // ── UNCHECKED: draw implied pairs as small text ──
                    // Each implied pair is shown as "SiSj" on a separate line
                    g2.setFont(fontImplied);
                    g2.setColor(COLOR_IMPLIED_FG);

                    List<int[]> pairs = imp[i][j];
                    if (pairs == null || pairs.isEmpty()) {
                        // No implied pairs = equivalent (edge case)
                        drawCenteredSymbol(g2, "✓", x, y, cellSize,
                                           COLOR_EQUIV, fontSymbol);
                    } else {
                        // Draw each pair label, stacked vertically inside cell
                        int lineHeight = 11;
                        int startY     = y + 13;
                        int maxLines   = cellSize / lineHeight - 1;

                        for (int k = 0; k < Math.min(pairs.size(), maxLines); k++) {
                            int idxA = pairs.get(k)[0];
                            int idxB = pairs.get(k)[1];
                            String label = states.get(idxA).getName()
                                         + ","
                                         + states.get(idxB).getName();
                            g2.drawString(label, x + 4, startY + k * lineHeight);
                        }
                    }
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DRAWING STEP 3 — drawGridLines()
    //  Draws the black borders around every cell for the grid look
    // ══════════════════════════════════════════════════════════════════════

    private void drawGridLines(Graphics2D g2, int n) {
        g2.setColor(COLOR_GRID);
        g2.setStroke(new BasicStroke(1.2f)); // slightly thicker than default

        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                int x = offsetX + j * cellSize;
                int y = offsetY + (i - 1) * cellSize;
                g2.drawRect(x, y, cellSize, cellSize);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DRAWING STEP 4 — drawRowLabels()
    //  State names on the LEFT side of the triangle
    //  Row i (0-indexed in drawing) represents state states[i+1]
    // ══════════════════════════════════════════════════════════════════════

    private void drawRowLabels(Graphics2D g2, int n) {
        for (int i = 1; i < n; i++) {
            String label = states.get(i).getName();

            // Center the label vertically within the row's cell band
            int x = offsetX - 10 - getLabelWidth(g2, label, fontLabel);
            int y = offsetY + (i - 1) * cellSize + cellSize / 2 + 5;

            // Draw a small rounded pill background behind the label
            drawLabelPill(g2, label, x - 4, y - 14, fontLabel);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DRAWING STEP 5 — drawColumnLabels()
    //  State names BELOW the triangle (one per column)
    //  Column j represents state states[j]
    // ══════════════════════════════════════════════════════════════════════

    private void drawColumnLabels(Graphics2D g2, int n) {
        // Column labels sit below the bottom row of the triangle
        // Bottom row's bottom edge is at: offsetY + (n-1)*cellSize
        int baseY = offsetY + (n - 1) * cellSize + 20;

        for (int j = 0; j < n - 1; j++) {
            String label = states.get(j).getName();

            int x = offsetX + j * cellSize
                    + cellSize / 2
                    - getLabelWidth(g2, label, fontLabel) / 2;

            drawLabelPill(g2, label, x - 4, baseY, fontLabel);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DRAWING STEP 6 — drawCornerLabel()
    //  Small legend in the bottom-right corner of the panel
    // ══════════════════════════════════════════════════════════════════════

    private void drawCornerLabel(Graphics2D g2) {
        int legendX = getWidth()  - 200;
        int legendY = getHeight() - 100;

        // Only draw if there's enough space
        if (legendX < 50 || legendY < 50) return;

        g2.setFont(new Font("Arial", Font.BOLD, 11));

        // ── Equivalent entry ──
        g2.setColor(COLOR_EQUIV);
        g2.fillRoundRect(legendX, legendY, 18, 18, 4, 4);
        g2.setColor(Color.WHITE);
        g2.drawString("✓", legendX + 3, legendY + 14);
        g2.setColor(COLOR_GRID);
        g2.drawRoundRect(legendX, legendY, 18, 18, 4, 4);
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Equivalent", legendX + 24, legendY + 14);

        // ── Distinguished entry ──
        g2.setColor(COLOR_DIST);
        g2.fillRoundRect(legendX, legendY + 26, 18, 18, 4, 4);
        g2.setColor(Color.WHITE);
        g2.drawString("✗", legendX + 3, legendY + 40);
        g2.setColor(COLOR_GRID);
        g2.drawRoundRect(legendX, legendY + 26, 18, 18, 4, 4);
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Distinguished", legendX + 24, legendY + 40);

        // ── Implied pairs entry ──
        g2.setColor(COLOR_UNCHECKED);
        g2.fillRoundRect(legendX, legendY + 52, 18, 18, 4, 4);
        g2.setColor(COLOR_GRID);
        g2.drawRoundRect(legendX, legendY + 52, 18, 18, 4, 4);
        g2.setFont(new Font("Arial", Font.PLAIN, 8));
        g2.setColor(COLOR_IMPLIED_FG);
        g2.drawString("Si,Sj", legendX + 1, legendY + 64);
        g2.setFont(new Font("Arial", Font.BOLD, 11));
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Implied pairs", legendX + 24, legendY + 66);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DRAWING UTILITY — drawPlaceholder()
    //  Shown before any data is loaded
    // ══════════════════════════════════════════════════════════════════════

    private void drawPlaceholder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        String line1 = "Implication Table";
        String line2 = "Enter FSM data and click  ▶ Minimize FSM";

        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.setColor(new Color(150, 150, 170));
        drawCenteredString(g2, line1, getWidth(), getHeight() / 2 - 20);

        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        drawCenteredString(g2, line2, getWidth(), getHeight() / 2 + 10);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SMALL UTILITIES
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Draws a symbol (✓ or ✗) centered within a cell rectangle.
     */
    private void drawCenteredSymbol(Graphics2D g2, String symbol,
                                    int cellX, int cellY, int size,
                                    Color color, Font font) {
        g2.setFont(font);
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(symbol);
        int textH = fm.getAscent();
        int drawX = cellX + (size - textW) / 2;
        int drawY = cellY + (size + textH) / 2 - 4;
        g2.drawString(symbol, drawX, drawY);
    }

    /**
     * Draws a state name label with a pill-shaped navy background.
     * This makes the row/column labels stand out clearly.
     */
    private void drawLabelPill(Graphics2D g2, String label,
                               int x, int y, Font font) {
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(label) + 10;
        int h = fm.getHeight() + 4;

        // Pill background
        g2.setColor(COLOR_LABEL_BG);
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, 8, 8));

        // Label text
        g2.setColor(COLOR_LABEL_FG);
        g2.drawString(label, x + 5, y + fm.getAscent() + 2);
    }

    /**
     * Draws a string horizontally centered at a given Y position.
     */
    private void drawCenteredString(Graphics2D g2, String text,
                                    int panelWidth, int y) {
        FontMetrics fm = g2.getFontMetrics();
        int x = (panelWidth - fm.stringWidth(text)) / 2;
        g2.drawString(text, x, y);
    }

    /**
     * Returns the pixel width of a string rendered in a given font.
     * Used to position labels correctly.
     */
    private int getLabelWidth(Graphics2D g2, String text, Font font) {
        g2.setFont(font);
        return g2.getFontMetrics().stringWidth(text);
    }

    /**
     * Recalculates the preferred size of this panel based on the
     * current cell size and number of states.
     * This is critical so JScrollPane knows how much to scroll.
     */
    private void updatePreferredSize() {
        if (states == null) return;
        int n = states.size();
        // Width  = offsetX + (n-1) cells + right margin
        // Height = offsetY + (n-1) cells + bottom margin for labels
        int w = offsetX + (n - 1) * cellSize + 100;
        int h = offsetY + (n - 1) * cellSize + 60;
        setPreferredSize(new Dimension(w, h));
        revalidate(); // tells the scroll pane to update its scrollbars
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GETTER — for external access if needed
    // ══════════════════════════════════════════════════════════════════════

    public boolean hasData() { return hasData; }

    /**
     * Clears the panel back to placeholder state.
     * Called by MainFrame's reset button.
     */
    public void clear() {
        this.hasData           = false;
        this.implicationTable  = null;
        this.states            = null;
        updatePreferredSize();
        repaint();
    }
}