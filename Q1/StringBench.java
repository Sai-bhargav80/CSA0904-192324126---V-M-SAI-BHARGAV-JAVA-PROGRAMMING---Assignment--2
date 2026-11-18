import java.lang.management.ManagementFactory;
import java.util.*;

/** Q1 - String (+), StringBuilder, StringBuffer (and join/StringJoiner/format) for building e-tickets. */
public class StringBench {
    static final int RUNS = 7, WARMUP = 3;
    static String[] fields;                       // pre-built field texts so only the joining is measured
    static int sink;

    static String viaString(int n) { String s = ""; for (int i = 0; i < n; i++) s = s + fields[i] + ";"; return s; }
    static String viaBuilder(int n) { StringBuilder b = new StringBuilder(); for (int i = 0; i < n; i++) b.append(fields[i]).append(';'); return b.toString(); }
    static String viaBuffer(int n) { StringBuffer b = new StringBuffer(); for (int i = 0; i < n; i++) b.append(fields[i]).append(';'); return b.toString(); }
    static String viaJoin(int n) { return String.join(";", Arrays.copyOf(fields, n)) + ";"; }
    static String viaJoiner(int n) { StringJoiner j = new StringJoiner(";", "", ";"); for (int i = 0; i < n; i++) j.add(fields[i]); return j.toString(); }
    static String viaFormat(int n) { return String.format("%s;".repeat(n), (Object[]) Arrays.copyOf(fields, n)); }

    interface Maker { String make(int n); }
    static final String[] NAMES = {"String +", "StringBuilder", "StringBuffer", "String.join", "StringJoiner", "String.format"};
    static final Maker[] MAKERS = {StringBench::viaString, StringBench::viaBuilder, StringBench::viaBuffer, StringBench::viaJoin, StringBench::viaJoiner, StringBench::viaFormat};

    static long allocated() {
        try { return ((com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean()).getThreadAllocatedBytes(Thread.currentThread().getId()); }
        catch (Throwable t) { return -1; }
    }
    static long used() { System.gc(); System.gc(); Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }

    /** Estimated number of intermediate String objects created per ticket of n fields. */
    static String intermediates(int a, int n) {
        switch (a) {
            case 0: return String.valueOf(n - 1);                     // one new String per iteration, n-1 are discarded
            case 1: case 2: { int len = 0; for (int i = 0; i < n; i++) len += fields[i].length() + 1;
                int cap = 16, grows = 0; while (cap < len) { cap = cap * 2 + 2; grows++; } return "0 (+" + grows + " internal array growths)"; }
            case 3: return "0 (+1 temp array)";
            case 4: return "0";
            default: return "~" + n + " (Formatter parses format + boxes)";
        }
    }

    static double[] measure(int a, int n, int tickets) {
        for (int w = 0; w < WARMUP; w++) for (int t = 0; t < Math.max(1, tickets / 5); t++) sink += MAKERS[a].make(n).length();
        double[] ms = new double[RUNS];
        for (int r = 0; r < RUNS; r++) {
            long t0 = System.nanoTime();
            for (int t = 0; t < tickets; t++) sink += MAKERS[a].make(n).length();
            ms[r] = (System.nanoTime() - t0) / 1e6;
        }
        return ms;
    }
    static double mean(double[] x) { double s = 0; for (double v : x) s += v; return s / x.length; }
    static double sd(double[] x) { double m = mean(x), s = 0; for (double v : x) s += (v - m) * (v - m); return Math.sqrt(s / (x.length - 1)); }

    public static void main(String[] args) throws Exception {
        fields = new String[40_000];
        for (int i = 0; i < fields.length; i++) fields[i] = "F" + (i % 97) + "-" + (i * 31 % 1000);
        // workload name, fields per ticket, number of tickets
        Object[][] wl = {
            {"W1 short ticket (10 fields) x 200,000 tickets", 10, 200_000},
            {"W2 bulk (50 fields) x 10,000 tickets", 50, 10_000},
            {"W3 group manifest (20,000 fields) x 3 tickets", 20_000, 3}};
        StringBuilder csv = new StringBuilder("workload,approach,run,ms\n");
        System.out.println("MAIN WORKLOADS (mean +- sd over " + RUNS + " runs, after " + WARMUP + " warm-up rounds)");
        System.out.printf("%-48s %-14s %12s %10s %14s %s%n", "workload", "approach", "mean ms", "sd ms", "alloc MB/run", "intermediate Strings per ticket");
        for (Object[] w : wl) {
            int n = (int) w[1], tickets = (int) w[2];
            for (int a = 0; a < 6; a++) {
                if (a == 0 && n == 20_000 && tickets > 3) continue;
                double[] ms = measure(a, n, tickets);
                long a0 = allocated(); for (int t = 0; t < tickets; t++) sink += MAKERS[a].make(n).length(); long a1 = allocated();
                for (int r = 0; r < RUNS; r++) csv.append(w[0]).append(',').append(NAMES[a]).append(',').append(r + 1).append(',').append(String.format("%.3f", ms[r])).append('\n');
                System.out.printf("%-48s %-14s %12.2f %10.2f %14.1f %s%n", w[0], NAMES[a], mean(ms), sd(ms), (a1 - a0) / 1048576.0, intermediates(a, n));
            }
            System.out.println();
        }
        // scaling of ONE ticket with n fields: shows the O(n^2) vs O(n) growth
        System.out.println("SCALING: time to build ONE ticket of n fields (mean of " + RUNS + " runs of 20 tickets each, microseconds per ticket)");
        System.out.printf("%8s %14s %14s %14s %12s%n", "n", "String +", "StringBuilder", "StringBuffer", "String/SB");
        csv.append("\nscaling,n,approach,microseconds_per_ticket\n");
        for (int n : new int[]{500, 1000, 2000, 4000, 8000, 16000, 32000}) {
            double[] us = new double[3];
            for (int a = 0; a < 3; a++) { double[] ms = measure(a, n, 20); us[a] = mean(ms) * 1000 / 20; csv.append("scaling,").append(n).append(',').append(NAMES[a]).append(',').append(String.format("%.2f", us[a])).append('\n'); }
            System.out.printf("%8d %14.1f %14.1f %14.1f %12.1f%n", n, us[0], us[1], us[2], us[0] / us[1]);
        }
        // memory retained: 10,000 W2 tickets kept in a list
        System.out.println("\nMEMORY (retained, MB) after building and keeping 10,000 tickets of 50 fields:");
        for (int a = 0; a < 3; a++) {
            long before = used(); List<String> keep = new ArrayList<>();
            for (int t = 0; t < 10_000; t++) keep.add(MAKERS[a].make(50));
            long after = used();
            System.out.printf("  %-14s before %.1f MB, after %.1f MB, retained %.1f MB (%d tickets)%n", NAMES[a], before / 1048576.0, after / 1048576.0, (after - before) / 1048576.0, keep.size());
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of("raw_data.csv"), csv.toString());
        System.out.println("\n(sink " + (sink != 0) + ")  raw data written to raw_data.csv");
    }
}
