import java.util.*;

/** Q6 - correctness check, growth experiment, starvation + aging rule, and the two alternative designs. */
public class TriageStudy {
    static class P {
        final String name; final int severity; final long arrival;
        P(String n, int s, long a) { name = n; severity = s; arrival = a; }
        @Override public String toString() { return name + "(S" + severity + ")"; }
    }
    static final Comparator<P> STRICT = Comparator.comparingInt((P p) -> p.severity).thenComparingLong(p -> p.arrival);
    /** Aging rule: score = arrival + severity * W. Lower score is served first. A static key, so the comparator stays consistent. */
    static Comparator<P> aging(long w) { return Comparator.comparingLong((P p) -> p.arrival + p.severity * w).thenComparingLong(p -> p.arrival); }

    /** Alternative A: LinkedList kept sorted - insert at the correct position (O(n)), serve from the head (O(1)). */
    static class SortedList {
        final LinkedList<P> l = new LinkedList<>();
        void add(P p) { ListIterator<P> it = l.listIterator(); while (it.hasNext()) { if (STRICT.compare(it.next(), p) > 0) { it.previous(); break; } } it.add(p); }
        P poll() { return l.pollFirst(); }
    }
    /** Alternative B: five FIFO queues, one per severity - add O(1), serve O(1) (scan at most 5 queues). */
    static class FiveQueues {
        @SuppressWarnings("unchecked") final ArrayDeque<P>[] q = new ArrayDeque[6];
        FiveQueues() { for (int i = 1; i <= 5; i++) q[i] = new ArrayDeque<>(); }
        void add(P p) { q[p.severity].addLast(p); }
        P poll() { for (int s = 1; s <= 5; s++) if (!q[s].isEmpty()) return q[s].pollFirst(); return null; }
    }
    static int sink;

    public static void main(String[] args) {
        // ---- 1. hand-verifiable instance (8 patients) ----
        System.out.println("=== 1. Correctness check: 8 patients ===");
        String[][] in = {{"Ravi", "3"}, {"Meena", "1"}, {"John", "3"}, {"Asha", "5"}, {"Kumar", "1"}, {"Lata", "2"}, {"Dev", "2"}, {"Zoya", "1"}};
        PriorityQueue<P> pq = new PriorityQueue<>(STRICT); long c = 0;
        for (String[] a : in) pq.add(new P(a[0], Integer.parseInt(a[1]), c++));
        System.out.println("Arrival order : " + Arrays.stream(in).map(a -> a[0] + "(S" + a[1] + ")").toList());
        System.out.println("By hand       : [Meena(S1), Kumar(S1), Zoya(S1), Lata(S2), Dev(S2), Ravi(S3), John(S3), Asha(S5)]");
        System.out.println("printing the PriorityQueue directly (heap array order): " + pq);
        List<P> served = new ArrayList<>(); P x; while ((x = pq.poll()) != null) served.add(x);
        System.out.println("poll() until empty (treatment order)                  : " + served);
        System.out.println("Matches hand-worked order: " + served.toString().equals("[Meena(S1), Kumar(S1), Zoya(S1), Lata(S2), Dev(S2), Ravi(S3), John(S3), Asha(S5)]"));
        PriorityQueue<P> pq2 = new PriorityQueue<>(STRICT); c = 0; for (String[] a : in) pq2.add(new P(a[0], Integer.parseInt(a[1]), c++));
        List<P> display = new ArrayList<>(pq2); display.sort(STRICT);
        System.out.println("Waiting-list display without removing anyone (sorted COPY): " + display + "   queue size still " + pq2.size());

        // ---- 2. growth ----
        System.out.println("\n=== 2. Growth: cost of add and of poll (ns per operation, mean of 5 runs after warm-up) ===");
        System.out.printf("%10s %12s %12s %24s%n", "n", "add", "poll", "predicted add / poll");
        for (int n : new int[]{10_000, 100_000, 1_000_000}) {
            Random g = new Random(3); P[] ps = new P[n]; for (int i = 0; i < n; i++) ps[i] = new P("P" + i, 1 + g.nextInt(5), i);
            double add = 0, poll = 0;
            for (int run = -2; run < 5; run++) {
                PriorityQueue<P> q = new PriorityQueue<>(STRICT);
                long t0 = System.nanoTime(); for (P p : ps) q.add(p); long t1 = System.nanoTime();
                while (q.poll() != null) sink++; long t2 = System.nanoTime();
                if (run >= 0) { add += (double) (t1 - t0) / n; poll += (double) (t2 - t1) / n; }
            }
            System.out.printf("%10d %12.1f %12.1f %24s%n", n, add / 5, poll / 5, "O(log n) / O(log n)");
        }

        // ---- 3. starvation and a fair rule ----
        System.out.println("\n=== 3. Starvation simulation: one minor patient (S5) arrives at minute 0; every minute 1 critical (S1) patient arrives and the doctor sees 1 patient per minute ===");
        for (long w : new long[]{0, 30}) {
            Comparator<P> cmp = w == 0 ? STRICT : aging(w);
            PriorityQueue<P> q = new PriorityQueue<>(cmp); q.add(new P("MinorPatient", 5, 0));
            long servedAt = -1;
            for (long minute = 1; minute <= 5000; minute++) {
                q.add(new P("Critical" + minute, 1, minute));
                P p = q.poll();
                if (p.name.equals("MinorPatient")) { servedAt = minute; break; }
            }
            System.out.println(w == 0 ? "  strict priority            : " + (servedAt < 0 ? "S5 patient STILL WAITING after 5000 minutes (5000 critical patients were served ahead of them)" : "served at minute " + servedAt)
                                      : "  aging rule (W = 30 min/level): S5 patient served at minute " + servedAt + "   (predicted: minor score = 0 + 5*30 = 150; a new S1 patient at minute m scores m + 30, so the minor patient wins from m = 120 = 4*30)");
        }
        // aging still serves truly critical patients first when they arrive together
        PriorityQueue<P> q3 = new PriorityQueue<>(aging(30)); q3.add(new P("Minor", 5, 100)); q3.add(new P("Critical", 1, 100)); q3.add(new P("Medium", 3, 100));
        System.out.println("  aging rule, all arrive at minute 100: " + List.of(q3.poll(), q3.poll(), q3.poll()) + "  (severity still decides when waiting times are equal)");

        // ---- 4. alternatives ----
        System.out.println("\n=== 4. Alternatives: admit n patients, then serve all (ns per operation, mean of 5 runs after warm-up) ===");
        System.out.printf("%10s %-26s %12s %12s%n", "n", "design", "admit", "serve next");
        for (int n : new int[]{2_000, 20_000, 200_000}) {
            Random g = new Random(3); P[] ps = new P[n]; for (int i = 0; i < n; i++) ps[i] = new P("P" + i, 1 + g.nextInt(5), i);
            String[] nm = {"PriorityQueue", "sorted LinkedList", "5 FIFO queues"};
            for (int d = 0; d < 3; d++) {
                if (d == 1 && n > 20_000) { System.out.printf("%10d %-26s %12s %12s   (skipped: O(n) per admit, n^2 in total)%n", n, nm[d], "-", "-"); continue; }
                double add = 0, poll = 0;
                for (int run = -2; run < 5; run++) {
                    PriorityQueue<P> a = new PriorityQueue<>(STRICT); SortedList b = new SortedList(); FiveQueues f = new FiveQueues();
                    long t0 = System.nanoTime();
                    for (P p : ps) { if (d == 0) a.add(p); else if (d == 1) b.add(p); else f.add(p); }
                    long t1 = System.nanoTime();
                    for (int i = 0; i < n; i++) sink += (d == 0 ? a.poll() : d == 1 ? b.poll() : f.poll()).severity;
                    long t2 = System.nanoTime();
                    if (run >= 0) { add += (double) (t1 - t0) / n; poll += (double) (t2 - t1) / n; }
                }
                System.out.printf("%10d %-26s %12.1f %12.1f%n", n, nm[d], add / 5, poll / 5);
            }
        }

        // ---- 5. deployment: a city hospital with 5,000 patients a day ----
        long t0 = System.nanoTime(); PriorityQueue<P> day = new PriorityQueue<>(STRICT); Random g = new Random(5);
        for (int i = 0; i < 5_000; i++) day.add(new P("D" + i, 1 + g.nextInt(5), i));
        while (day.poll() != null) sink++;
        System.out.printf("%n=== 5. One whole day: 5,000 admissions + 5,000 treatments in %.2f ms (sink %b) ===%n", (System.nanoTime() - t0) / 1e6, sink != 0);
    }
}
