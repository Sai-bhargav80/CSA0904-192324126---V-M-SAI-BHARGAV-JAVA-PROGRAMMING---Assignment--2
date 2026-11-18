import java.util.*;

/** Q4 - HashMap vs TreeMap vs LinkedHashMap (+ Hashtable, + sorted array with binarySearch) for pharmacy stock. */
public class PharmacyMaps {
    static class Medicine {
        final String code, name; int stock; final int seq;      // seq = arrival number, lets us rebuild "order added"
        Medicine(String c, String n, int s, int q) { code = c; name = n; stock = s; seq = q; }
    }
    static final int RUNS = 5, LOOKUPS = 100_000;
    static int sink;

    static Medicine[] make(int n) {
        Integer[] ids = new Integer[n]; for (int i = 0; i < n; i++) ids[i] = i;
        Collections.shuffle(Arrays.asList(ids), new Random(11));        // arrival order is NOT code order
        Medicine[] m = new Medicine[n];
        for (int i = 0; i < n; i++) m[i] = new Medicine(String.format("MED%07d", ids[i]), "Drug-" + ids[i], 10 + ids[i] % 90, i);
        return m;
    }
    static double mean(double[] a) { double s = 0; for (double v : a) s += v; return s / a.length; }

    public static void main(String[] args) {
        System.out.println("Pharmacy stock: build time, 100,000 random lookups, and the two evening reports (mean of " + RUNS + " runs, after warm-up)");
        System.out.printf("%9s %-14s %10s %14s %16s %16s %s%n", "n", "map", "build ms", "100k lookups", "code-order rpt", "added-order rpt", "order correct (code/added)");
        for (int n : new int[]{1_000, 10_000, 100_000, 1_000_000}) {
            Medicine[] meds = make(n);
            Random rg = new Random(2); String[] keys = new String[LOOKUPS]; for (int i = 0; i < LOOKUPS; i++) keys[i] = meds[rg.nextInt(n)].code;
            String[] codeOrder = new String[n]; for (int i = 0; i < n; i++) codeOrder[i] = meds[i].code; Arrays.sort(codeOrder);
            for (int mi = 0; mi < 4; mi++) {
                String[] nm = {"HashMap", "TreeMap", "LinkedHashMap", "Hashtable"};
                double[] bld = new double[RUNS], lk = new double[RUNS], rc = new double[RUNS], ra = new double[RUNS]; boolean okC = true, okA = true;
                for (int run = -2; run < RUNS; run++) {
                    Map<String, Medicine> map = mi == 0 ? new HashMap<>() : mi == 1 ? new TreeMap<>() : mi == 2 ? new LinkedHashMap<>() : new Hashtable<>();
                    long t0 = System.nanoTime(); for (Medicine m : meds) map.put(m.code, m); long t1 = System.nanoTime();
                    for (String k : keys) sink += map.get(k).stock; long t2 = System.nanoTime();
                    // Report 1: all medicines in CODE order
                    StringBuilder r1 = new StringBuilder(); List<String> codes;
                    if (mi == 1) codes = new ArrayList<>(map.keySet());                         // already sorted: just iterate
                    else { codes = new ArrayList<>(map.keySet()); Collections.sort(codes); }    // extra work: sort every time the report is printed
                    for (String c : codes) { Medicine m = map.get(c); r1.append(m.code).append(',').append(m.name).append(',').append(m.stock).append('\n'); }
                    long t3 = System.nanoTime();
                    // Report 2: medicines in the ORDER ADDED
                    StringBuilder r2 = new StringBuilder(); List<Medicine> added;
                    if (mi == 2) added = new ArrayList<>(map.values());                         // iteration order = insertion order
                    else { added = new ArrayList<>(map.values()); added.sort(Comparator.comparingInt((Medicine m) -> m.seq)); }   // extra work: sort by arrival number
                    for (Medicine m : added) r2.append(m.code).append(',').append(m.name).append(',').append(m.stock).append('\n');
                    long t4 = System.nanoTime();
                    if (run >= 0) { bld[run] = (t1 - t0) / 1e6; lk[run] = (t2 - t1) / 1e6; rc[run] = (t3 - t2) / 1e6; ra[run] = (t4 - t3) / 1e6; }
                    if (run == 0) {
                        for (int i = 0; i < n; i++) { if (!codes.get(i).equals(codeOrder[i])) { okC = false; break; } }
                        for (int i = 0; i < n; i++) { if (added.get(i).seq != i) { okA = false; break; } }
                    }
                    sink += r1.length() + r2.length();
                }
                System.out.printf("%9d %-14s %10.1f %11.1f ms %13.1f ms %13.1f ms %s/%s%n", n, nm[mi], mean(bld), mean(lk), mean(rc), mean(ra), okC, okA);
            }
            System.out.println();
        }

        // Scalability: what does ONE lookup cost, including the slowest ones?
        System.out.println("SINGLE LOOKUP latency at n = 1,000,000 (each of 100,000 lookups timed individually, ns)");
        Medicine[] meds = make(1_000_000);
        Random rg = new Random(9); String[] keys = new String[LOOKUPS]; for (int i = 0; i < LOOKUPS; i++) keys[i] = meds[rg.nextInt(meds.length)].code;
        for (int mi = 0; mi < 2; mi++) {
            Map<String, Medicine> map = mi == 0 ? new HashMap<>() : new TreeMap<>(); for (Medicine m : meds) map.put(m.code, m);
            for (int w = 0; w < 3; w++) for (String k : keys) sink += map.get(k).stock;
            long[] t = new long[LOOKUPS];
            for (int i = 0; i < LOOKUPS; i++) { long a = System.nanoTime(); sink += map.get(keys[i]).stock; t[i] = System.nanoTime() - a; }
            Arrays.sort(t);
            System.out.printf("  %-8s mean %8.0f ns   median %6d ns   p99 %8d ns   max %9d ns%n", mi == 0 ? "HashMap" : "TreeMap", Arrays.stream(t).average().getAsDouble(), t[LOOKUPS / 2], t[(int) (LOOKUPS * 0.99)], t[LOOKUPS - 1]);
        }

        // Alternative: sorted array + Arrays.binarySearch
        System.out.println("\nALTERNATIVE: array of codes sorted by code + Arrays.binarySearch");
        System.out.printf("%9s %16s %22s %20s%n", "n", "100k lookups ms", "insert 1 new (us/op)", "code-order report ms");
        for (int n : new int[]{1_000, 10_000, 100_000, 1_000_000}) {
            Medicine[] mm = make(n);
            Medicine[] sorted = mm.clone(); Arrays.sort(sorted, Comparator.comparing((Medicine m) -> m.code));
            String[] codes = new String[n]; for (int i = 0; i < n; i++) codes[i] = sorted[i].code;
            Random r2 = new Random(2); String[] ks = new String[LOOKUPS]; for (int i = 0; i < LOOKUPS; i++) ks[i] = mm[r2.nextInt(n)].code;
            double lk = 0; for (int run = -2; run < RUNS; run++) { long t0 = System.nanoTime(); for (String k : ks) sink += Arrays.binarySearch(codes, k); if (run >= 0) lk += (System.nanoTime() - t0) / 1e6; }
            // insertion into the sorted array: find position then shift (System.arraycopy) - 200 inserts
            String[] work = Arrays.copyOf(codes, n + 300); int size = n; Random r3 = new Random(4);
            long t0 = System.nanoTime();
            for (int i = 0; i < 200; i++) { String c = "MED9" + String.format("%06d", r3.nextInt(1_000_000)); int p = -Arrays.binarySearch(work, 0, size, c) - 1; if (p < 0) continue; System.arraycopy(work, p, work, p + 1, size - p); work[p] = c; size++; }
            double ins = (System.nanoTime() - t0) / 1e3 / 200;
            long t5 = System.nanoTime(); StringBuilder sb = new StringBuilder(); for (Medicine m : sorted) sb.append(m.code).append(',').append(m.name).append(',').append(m.stock).append('\n'); double rep = (System.nanoTime() - t5) / 1e6; sink += sb.length();
            System.out.printf("%9d %16.1f %22.1f %20.1f%n", n, lk / RUNS, ins, rep);
        }
        System.out.println("(sink " + (sink != 0) + ")");
    }
}
