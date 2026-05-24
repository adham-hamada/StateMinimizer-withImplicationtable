# State Minimizer — Implication Table Method

A desktop application built with **Java Swing** that minimizes
finite state machines (FSMs) using the **Implication Table Method**
(also known as the Pair Chart Method).

Developed as a bonus project for the **Digital Logic II** course at
**Alexandria University — Faculty of Engineering**.

\---

## Preview

<!-- Add a screenshot after first run -->

!\[App Screenshot](ss%20.jpeg)

\---

## Features

* Input any Mealy FSM via an interactive transition table
* Supports custom state names (A, B, C or q0, q1, q2 or any label)
* Visualizes the full implication table triangle with color-coded cells

  * Green  = Equivalent states  ✓
  * Red    = Distinguished states  ✗
  * White  = Implied pairs (shown as text inside cell)
* Step-by-step log showing every decision made by the algorithm
* Displays final equivalence classes as colored badges
* Shows the minimized FSM transition table
* Zoom in/out on the implication table via slider or buttons
* Input validation with red cell highlighting for errors

\---

## Algorithm — How It Works

The **Implication Table Method** minimizes a Mealy FSM in 4 steps:

1. Initial Marking Mark every state pair (Si, Sj) as Distinguished (✗) if they produce different outputs for any input.
2. Fill Implied Pairs For each unmarked pair, record which next-state pairs must also be equivalent for this pair to be equivalent.
3. Iterative Propagation Re-scan repeatedly — if any implied pair gets marked ✗, mark the parent pair ✗ too. Repeat until no new markings.
4. Merge Equivalent States All unmarked pairs are Equivalent (✓). Group them using Union-Find → these states can be merged.

\---

## How to Run

### Option A — From NetBeans

1. Open NetBeans
2. File → Open Project → select this folder
3. Right-click project → Run  (or press F6)

### Option B — Build \& Run JAR

1. In NetBeans:  Run → Clean and Build  (Shift + F11)
2. Navigate to the dist/ folder
3. Run:java -jar StateMinimizer-withImplicationtable.jar

### Option C — macOS (removes Gatekeeper block)

```bash
xattr -d com.apple.quarantine StateMinimizer-withImplicationtable.jar
java -jar StateMinimizer-withImplicationtable.jar
```

\---

## Requirements

|Requirement|Version|
|-|-|
|Java JDK|18 or higher|
|NetBeans IDE|17 or higher|
|OS|Windows / macOS / Linux|

\---

## How to Use

1. Set the number of states using the spinner
2. Set the number of inputs using the spinner
3. (Optional) Type custom state names: A,B,C  or  q0,q1,q2
4. Click "Build Transition Table"
5. Fill in every Next State and Output cell
	. Next State must be one of your defined state names
	. Output can be 0, 1, or any value
6. Click "▶ Minimize FSM"
7. View results:
	. Top panel   → Implication Table (color-coded triangle)
	. Step Log tab → Full step-by-step explanation
	. Results tab  → Equivalence classes + Minimized FSM table

\---

## Built With

* **Java 18**
* **Java Swing** (GUI framework)
* **NetBeans GUI Builder** (Matisse)
* **Union-Find** algorithm for equivalence class grouping

\---

## Author

**Adham Hamada**
Faculty of Engineering — Alexandria University
Digital Logic II — Bonus Project

