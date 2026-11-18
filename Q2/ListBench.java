import java.util.*;

/** Q2 - ArrayList vs LinkedList vs ArrayDeque vs plain array with manual shifting. */
public class ListBench {
    static final int RUNS = 5, K = 2000;               // runs per measurement, operations per run
    static int sink;
    static final String[] OPS = {"add at end", "insert at front", "insert in middle", "remove at front", "remove in middle", "get k-th (random)", "iterate whole list"};
    static final String[] DS = {"ArrayList", "LinkedList", "ArrayDeque", "plain array"};

    /** Plain Integer[] with manual shifting loops. */
    static class ManualArray {
        Integer[] a; int size;
        ManualArray(int cap) { a = new Integer[cap]; }
        void add(int idx, Integer v) { for (int i = size; i > idx; i--) a[i] = a[i - 1]; a[idx] = v; size++; }
        Integer remove(int idx) { Integer v = a[idx]; for (int i = idx; i < size - 1; i++) a[i] = a[i + 1]; a[--size] = null; return v; }
        Integer get(int i) { return a[i]; }
    }
    static Object build(int ds, int n) {
        switch (ds) {
            case 0: { ArrayList<Integer> l = new ArrayList<>(); for (int i = 0; i < n; i++) l.add(i); return l; }
            case 1: { LinkedList<Integer> l = new LinkedList<>(); for (int i = 0; i < n; i++) l.add(i); return l; }
            case 2: { ArrayDeque<Integer> l = new ArrayDeque<>(); for (int i = 0; i < n; i++) l.add(i); return l; }
            default: { ManualArray m = new ManualArray(n + 250_000); for (int i = 0; i < n; i++) m.add(m.size, i); return m; }
        }
    }
    /** Runs one batch of K operations; returns elapsed ns, or -1 if the structure does not support the operation. */
    @SuppressWarnings("unchecked")
    static long batch(int ds, int op, Object o, int n, int[] rnd) {
        long t0, t1;
        switch (op) {
            case 0: { int cnt = 200_000; t0 = System.nanoTime();
                for (int i = 0; i < cnt; i++) { switch (ds) { case 0: case 1: ((List<Integer>) o).add(i); break; case 2: ((ArrayDeque<Integer>) o).addLast(i); break; default: ((ManualArray) o).add(((ManualArray) o).size, i); } }
                t1 = System.nanoTime(); return (t1 - t0) * K / cnt; }          // scaled to K ops so every op row uses the same unit below
            case 1: t0 = System.nanoTime();
                for (int i = 0; i < K; i++) { switch (ds) { case 0: case 1: ((List<Integer>) o).add(0, i); break; case 2: ((ArrayDeque<Integer>) o).addFirst(i); break; default: ((ManualArray) o).add(0, i); } }
                return System.nanoTime() - t0;
            case 2: if (ds == 2) return -1; t0 = System.nanoTime();
                for (int i = 0; i < K; i++) { if (ds == 3) ((ManualArray) o).add(((ManualArray) o).size / 2, i); else ((List<Integer>) o).add(((List<Integer>) o).size() / 2, i); }
                return System.nanoTime() - t0;
            case 3: t0 = System.nanoTime();
                for (int i = 0; i < K; i++) { switch (ds) { case 0: case 1: sink += ((List<Integer>) o).remove(0); break; case 2: sink += ((ArrayDeque<Integer>) o).pollFirst(); break; default: sink += ((ManualArray) o).remove(0); } }
                return System.nanoTime() - t0;
            case 4: if (ds == 2) return -1; t0 = System.nanoTime();
                for (int i = 0; i < K; i++) { if (ds == 3) sink += ((ManualArray) o).remove(((ManualArray) o).size / 2); else sink += ((List<Integer>) o).remove(((List<Integer>) o).size() / 2); }
                return System.nanoTime() - t0;
            case 5: if (ds == 2) return -1; t0 = System.nanoTime();
                for (int i = 0; i < K; i++) { int k = rnd[i] % n; if (ds == 3) sink += ((ManualArray) o).get(k); else sink += ((List<Integer>) o).get(k); }
                return System.nanoTime() - t0;
            default: t0 = System.nanoTime();
                for (int pass = 0; pass < 20; pass++) {
                    if (ds == 3) { ManualArray m = (ManualArray) o; for (int i = 0; i < m.size; i++) sink += m.a[i]; }
                    else for (int v : (Iterable<Integer>) o) sink += v;
                }
                return (System.nanoTime() - t0) * K / ((long) n * 20);                              // scaled to K elements
        }
    }
    public static void main(String[] args) throws Exception {
        int[] sizes = {10_000, 50_000, 100_000, 200_000};
        Random rg = new Random(7); int[] rnd = new int[K]; for (int i = 0; i < K; i++) rnd[i] = rg.nextInt(Integer.MAX_VALUE);
        StringBuilder csv = new StringBuilder("operation,size,structure,run,ns_per_op\n");
        System.out.println("Mean nanoseconds PER OPERATION (" + RUNS + " runs, K=" + K + " operations per run; 'n/a' = operation not supported by the structure)");
        for (int op = 0; op < OPS.length; op++) {
            System.out.println("\n" + OPS[op]);
            System.out.printf("%10s", "size"); for (String d : DS) System.out.printf("%16s", d); System.out.println();
            for (int n : sizes) {
                System.out.printf("%10d", n);
                for (int ds = 0; ds < 4; ds++) {
                    if (ds == 3 && n > 100_000 && (op == 1 || op == 2 || op == 3 || op == 4)) { }   // still run: manual loops are slowest but finish in time
                    for (int w = 0; w < 4; w++) batch(ds, op, build(ds, n), n, rnd);                  // warm-up (JIT)
                    double sum = 0; boolean na = false;
                    for (int r = 0; r < RUNS; r++) {
                        long ns = batch(ds, op, build(ds, n), n, rnd);
                        if (ns < 0) { na = true; break; }
                        double per = (double) ns / K; sum += per;
                        csv.append(OPS[op]).append(',').append(n).append(',').append(DS[ds]).append(',').append(r + 1).append(',').append(String.format("%.1f", per)).append('\n');
                    }
                    if (na) System.out.printf("%16s", "n/a"); else System.out.printf("%16.1f", sum / RUNS);
                }
                System.out.println();
            }
        }
        // Changed condition: "Up Next" queue - add at both ends, remove from the front
        System.out.println("\nUP NEXT workload: queue of 50,000 songs, then 200,000 operations (40% addLast, 20% addFirst, 40% removeFirst); total ms, mean of " + RUNS + " runs");
        csv.append("\nup_next,structure,run,total_ms\n");
        for (int ds = 0; ds < 3; ds++) {
            double sum = 0;
            for (int r = -1; r < RUNS; r++) {
                Object o = build(ds, 50_000); Random g = new Random(1); long t0 = System.nanoTime();
                for (int i = 0; i < 200_000; i++) {
                    int c = g.nextInt(10);
                    if (c < 4) { if (ds == 2) ((ArrayDeque<Integer>) o).addLast(i); else ((List<Integer>) o).add(i); }
                    else if (c < 6) { if (ds == 2) ((ArrayDeque<Integer>) o).addFirst(i); else ((List<Integer>) o).add(0, i); }
                    else { if (ds == 2) sink += ((ArrayDeque<Integer>) o).pollFirst(); else sink += ((List<Integer>) o).remove(0); }
                }
                double ms = (System.nanoTime() - t0) / 1e6;
                if (r >= 0) { sum += ms; csv.append("up_next,").append(DS[ds]).append(',').append(r + 1).append(',').append(String.format("%.2f", ms)).append('\n'); }
            }
            System.out.printf("  %-12s %10.1f ms%n", DS[ds], sum / RUNS);
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of("raw_data.csv"), csv.toString());
        System.out.println("\n(sink " + (sink != 0) + ") raw data in raw_data.csv");
    }
}
