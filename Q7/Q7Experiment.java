import java.util.*;

/** Q7 - Original (ArrayList playlist) vs Redesign (GapList) under the same test conditions. */
public class Q7Experiment {
    static final int RUNS = 5; static int sink;
    static double mean(double[] a) { double s = 0; for (double v : a) s += v; return s / a.length; }

    /** Insert I songs in bursts of 'burst' consecutive positions; between bursts the cursor jumps to a random place. */
    static int[] script(int n, int inserts, int burst, long seed) {
        Random r = new Random(seed); int[] pos = new int[inserts]; int size = n, cursor = n / 2;
        for (int i = 0; i < inserts; i++) {
            if (i % burst == 0) cursor = r.nextInt(size + 1);
            pos[i] = cursor++; size++;
        }
        return pos;
    }
    static double[] run(int n, int[] pos) {
        double[] tA = new double[RUNS], tG = new double[RUNS]; long movesA = 0, movesG = 0;
        for (int run = -3; run < RUNS; run++) {
            ArrayList<Integer> a = new ArrayList<>(); GapList<Integer> g = new GapList<>(n + pos.length);
            for (int i = 0; i < n; i++) { a.add(i); g.add(i); }
            g.moves = 0;
            long t0 = System.nanoTime(); long mv = 0;
            for (int i = 0; i < pos.length; i++) { mv += a.size() - pos[i]; a.add(pos[i], i); }
            long t1 = System.nanoTime();
            for (int i = 0; i < pos.length; i++) g.add(pos[i], i);
            long t2 = System.nanoTime();
            sink += a.size() + g.size() + a.get(n / 2) + g.get(n / 2);
            if (run >= 0) { tA[run] = (t1 - t0) / 1e6; tG[run] = (t2 - t1) / 1e6; }
            movesA = mv; movesG = g.moves;
        }
        return new double[]{mean(tA), mean(tG), movesA, movesG};
    }
    public static void main(String[] args) {
        System.out.println("PREDICTIONS (written before the experiment was run):");
        System.out.println("  P1. Clustered edits (bursts of 100 inserts): the gap buffer needs about N/3 element moves per burst instead of ~50*N, so moves fall by two orders of magnitude and time by at least 20x at N >= 100,000.");
        System.out.println("  P2. Burst length 1 (every insert at a new random place): no advantage - moves per insert about N/3 (gap) vs N/2 (ArrayList); times roughly equal.");
        System.out.println("  P3. Break-even around a burst length of 2-3 inserts; improvement grows roughly in proportion to the burst length.");
        System.out.println("  P4. get(i) stays O(1) but costs one extra comparison; memory: capacity slack of the gap buffer is similar to ArrayList.\n");
        int I = 20_000;
        System.out.println("EXPERIMENT 1: " + I + " inserts in bursts of 100 (clustered editing session); mean of " + RUNS + " runs after 3 warm-ups");
        System.out.printf("%10s %14s %14s %9s %18s %16s %10s%n", "N (start)", "ArrayList ms", "GapList ms", "speed-up", "ArrayList moves", "GapList moves", "move ratio");
        for (int n : new int[]{50_000, 100_000, 200_000}) {
            double[] r = run(n, script(n, I, 100, 5));
            System.out.printf("%10d %14.1f %14.1f %8.1fx %18d %16d %9.0fx%n", n, r[0], r[1], r[0] / r[1], (long) r[2], (long) r[3], r[2] / r[3]);
        }
        System.out.println("\nEXPERIMENT 2: sensitivity to burst length, N = 100,000, " + I + " inserts (burst 1 = adversarial random inserts)");
        System.out.printf("%10s %14s %14s %9s %18s %16s%n", "burst len", "ArrayList ms", "GapList ms", "speed-up", "ArrayList moves", "GapList moves");
        for (int b : new int[]{1, 2, 3, 5, 10, 100, 1000, 20000}) {
            double[] r = run(100_000, script(100_000, I, b, 6));
            System.out.printf("%10d %14.1f %14.1f %8.2fx %18d %16d%n", b, r[0], r[1], r[0] / r[1], (long) r[2], (long) r[3]);
        }
        // random get(k): is indexing slower?
        System.out.println("\nEXPERIMENT 3: 5,000,000 random get(k) on 100,000 songs (ms, mean of " + RUNS + " runs)");
        ArrayList<Integer> a = new ArrayList<>(); GapList<Integer> g = new GapList<>(100_000); for (int i = 0; i < 100_000; i++) { a.add(i); g.add(i); }
        g.add(50_000, -1); a.add(50_000, -1);          // put the gap in the middle
        int[] ks = new int[5_000_000]; Random r = new Random(1); for (int i = 0; i < ks.length; i++) ks[i] = r.nextInt(a.size());
        double[] ta = new double[RUNS], tg = new double[RUNS];
        for (int run = -3; run < RUNS; run++) {
            long t0 = System.nanoTime(); for (int k : ks) sink += a.get(k); long t1 = System.nanoTime(); for (int k : ks) sink += g.get(k); long t2 = System.nanoTime();
            if (run >= 0) { ta[run] = (t1 - t0) / 1e6; tg[run] = (t2 - t1) / 1e6; }
        }
        System.out.printf("  ArrayList.get %.1f ms   GapList.get %.1f ms   ratio %.2f%n", mean(ta), mean(tg), mean(tg) / mean(ta));
        // memory
        System.out.println("\nEXPERIMENT 4: spare capacity after building 100,000 songs by append: ArrayList default growth vs GapList (capacity is the number of reference slots held)");
        GapList<Integer> g2 = new GapList<>(16); for (int i = 0; i < 100_000; i++) g2.add(i);
        int cap = 10; while (cap < 100_000) cap += cap >> 1;
        System.out.println("  ArrayList capacity ~" + cap + " slots (grows x1.5), GapList capacity " + g2.capacity() + " slots (doubles); extra slots per element: " + String.format("%.2f vs %.2f", (cap - 100_000) / 100_000.0, (g2.capacity() - 100_000) / 100_000.0));
        System.out.println("(sink " + (sink != 0) + ")");
    }
}
