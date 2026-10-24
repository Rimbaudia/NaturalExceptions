package naturalexceptions.core.power;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A DC electrical network solved by nodal analysis, single-line: every node is a point of the network (a length of
 * cable, a machine's terminal), measured against a common return ("ground"), and every element is a resistance
 * between two nodes or from a node to ground. Sources are real ones (an EMF behind an internal resistance); loads are
 * resistances; a battery is a source like any other. The solve is a sparse conjugate gradient on the conductance
 * matrix (symmetric, positive definite once every node has some path to ground), warm-started from the last voltages,
 * so a large network that hardly changed re-solves in a few steps.
 */
public final class Grid {

    /** The return every element can connect to. */
    public static final int GROUND = -1;
    /** A tiny leak from every node to ground, so a node with no other path still has a voltage (0). */
    static final double LEAK = 1.0E-12;
    static final double TOLERANCE = 1.0E-13;

    private final int nodes;
    private final List<double[]> resistors = new ArrayList<>(); // {a, b, ohms}
    private final List<double[]> sources = new ArrayList<>(); // {node, emf, ohms}
    private double[] voltages;

    public Grid(int nodes) {
        if (nodes < 0) throw new IllegalArgumentException("nodes");
        this.nodes = nodes;
        this.voltages = new double[nodes];
    }

    public int nodes() {
        return nodes;
    }

    /** A resistance between two nodes (or a node and {@link #GROUND}); returns its index. */
    public int resistor(int a, int b, double ohms) {
        check(a);
        check(b);
        if (!(ohms > 0.0) || Double.isInfinite(ohms)) throw new IllegalArgumentException("ohms " + ohms);
        resistors.add(new double[] { a, b, ohms });
        return resistors.size() - 1;
    }

    /** A load from a node to ground; returns its resistor index. */
    public int load(int node, double ohms) {
        return resistor(node, GROUND, ohms);
    }

    /** A source from ground to a node: an EMF behind an internal resistance; returns its index. */
    public int source(int node, double emf, double internalOhms) {
        check(node);
        if (!(internalOhms > 0.0) || !Double.isFinite(emf)) throw new IllegalArgumentException("source");
        sources.add(new double[] { node, emf, internalOhms });
        return sources.size() - 1;
    }

    private void check(int node) {
        if (node != GROUND && (node < 0 || node >= nodes)) throw new IllegalArgumentException("node " + node);
    }

    /** Whether anything drives it: with no source, every node sits at nothing. */
    public boolean hasSources() {
        return !sources.isEmpty();
    }

    /** Starts the next solve from these voltages (the last solve's, for a network that hardly changed). */
    public void warmStart(double[] previous) {
        if (previous != null && previous.length == nodes) voltages = previous.clone();
    }

    /** Solves the network; returns the number of iterations it took. */
    public int solve() {
        int n = nodes;
        if (n == 0) return 0;
        // Sparse symmetric matrix as per-row lists, plus the diagonal.
        double[] diagonal = new double[n];
        Arrays.fill(diagonal, LEAK);
        double[] rhs = new double[n];
        int[][] cols = new int[n][];
        double[][] vals = new double[n][];
        int[] count = new int[n];
        for (double[] r : resistors) {
            int a = (int) r[0], b = (int) r[1];
            double g = 1.0 / r[2];
            if (a != GROUND) diagonal[a] += g;
            if (b != GROUND) diagonal[b] += g;
            if (a != GROUND && b != GROUND && a != b) {
                count[a]++;
                count[b]++;
            }
        }
        for (int i = 0; i < n; i++) {
            cols[i] = new int[count[i]];
            vals[i] = new double[count[i]];
            count[i] = 0;
        }
        for (double[] r : resistors) {
            int a = (int) r[0], b = (int) r[1];
            if (a == GROUND || b == GROUND || a == b) continue;
            double g = 1.0 / r[2];
            cols[a][count[a]] = b;
            vals[a][count[a]++] = -g;
            cols[b][count[b]] = a;
            vals[b][count[b]++] = -g;
        }
        for (double[] s : sources) {
            int node = (int) s[0];
            double g = 1.0 / s[2];
            diagonal[node] += g;
            rhs[node] += s[1] * g;
        }
        // Conjugate gradient, Jacobi-preconditioned.
        double[] x = voltages.length == n ? voltages : new double[n];
        double[] r = new double[n], z = new double[n], p = new double[n], ap = new double[n];
        multiply(diagonal, cols, vals, x, ap);
        double norm = 0.0;
        for (int i = 0; i < n; i++) {
            r[i] = rhs[i] - ap[i];
            norm += rhs[i] * rhs[i];
        }
        double target = Math.max(TOLERANCE * TOLERANCE * Math.max(norm, 1.0), 1.0E-30);
        for (int i = 0; i < n; i++) {
            z[i] = r[i] / diagonal[i];
            p[i] = z[i];
        }
        double rz = dot(r, z);
        int iterations = 0;
        int limit = Math.max(100, 10 * n);
        while (dot(r, r) > target && iterations < limit) {
            multiply(diagonal, cols, vals, p, ap);
            double alpha = rz / dot(p, ap);
            for (int i = 0; i < n; i++) {
                x[i] += alpha * p[i];
                r[i] -= alpha * ap[i];
            }
            for (int i = 0; i < n; i++) z[i] = r[i] / diagonal[i];
            double next = dot(r, z);
            double beta = next / rz;
            rz = next;
            for (int i = 0; i < n; i++) p[i] = z[i] + beta * p[i];
            iterations++;
        }
        voltages = x;
        return iterations;
    }

    private static void multiply(double[] diagonal, int[][] cols, double[][] vals, double[] x, double[] out) {
        for (int i = 0; i < x.length; i++) {
            double sum = diagonal[i] * x[i];
            for (int k = 0; k < cols[i].length; k++) sum += vals[i][k] * x[cols[i][k]];
            out[i] = sum;
        }
    }

    private static double dot(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) sum += a[i] * b[i];
        return sum;
    }

    // --- Results --------------------------------------------------------------------------------------------

    public double[] voltages() {
        return voltages.clone();
    }

    public double voltage(int node) {
        return node == GROUND ? 0.0 : voltages[node];
    }

    /** Current through a resistor, from its first node to its second. */
    public double current(int resistor) {
        double[] r = resistors.get(resistor);
        return (voltage((int) r[0]) - voltage((int) r[1])) / r[2];
    }

    /** Power dissipated in a resistor (watts). */
    public double power(int resistor) {
        double i = current(resistor);
        return i * i * resistors.get(resistor)[2];
    }

    /** Current a source drives into its node (negative when it is being charged). */
    public double sourceCurrent(int source) {
        double[] s = sources.get(source);
        return (s[1] - voltage((int) s[0])) / s[2];
    }

    /** Power a source delivers to the network, after its internal losses (negative when it is being charged). */
    public double sourcePower(int source) {
        return sourceCurrent(source) * voltage((int) sources.get(source)[0]);
    }
}
