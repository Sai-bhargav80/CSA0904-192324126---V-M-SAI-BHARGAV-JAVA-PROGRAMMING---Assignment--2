import java.util.*;

/** Q3 - experiments on the hashCode/equals contract. Each test prints PREDICTION (written before running) and OBSERVED. */
public class ContractExperiments {
    /** The junior developer's class: equals() on regNo, hashCode() NOT overridden. */
    static class Broken {
        final String regNo, name;
        Broken(String r, String n) { regNo = r; name = n; }
        @Override public boolean equals(Object o) { return o instanceof Broken && regNo.equals(((Broken) o).regNo); }
    }
    /** Corrected class. */
    static class Fixed {
        final String regNo, name;
        Fixed(String r, String n) { regNo = r; name = n; }
        @Override public boolean equals(Object o) { return this == o || (o instanceof Fixed && regNo.equals(((Fixed) o).regNo)); }
        @Override public int hashCode() { return regNo.hashCode(); }
    }
    /** Mutable key used by hashCode - Test 3. */
    static class Mutable {
        String regNo;
        Mutable(String r) { regNo = r; }
        @Override public boolean equals(Object o) { return o instanceof Mutable && regNo.equals(((Mutable) o).regNo); }
        @Override public int hashCode() { return regNo.hashCode(); }
    }
    /** compareTo uses name only - inconsistent with equals (regNo) - Test 4. */
    static class ByName implements Comparable<ByName> {
        final String regNo, name;
        ByName(String r, String n) { regNo = r; name = n; }
        @Override public boolean equals(Object o) { return o instanceof ByName && regNo.equals(((ByName) o).regNo); }
        @Override public int hashCode() { return regNo.hashCode(); }
        @Override public int compareTo(ByName o) { return name.compareTo(o.name); }
    }
    /** Counts calls, to show how HashSet uses hashCode() and equals(). */
    static class Counting {
        static int hashCalls, equalsCalls;
        final String regNo;
        Counting(String r) { regNo = r; }
        @Override public int hashCode() { hashCalls++; return regNo.hashCode(); }
        @Override public boolean equals(Object o) { equalsCalls++; return o instanceof Counting && regNo.equals(((Counting) o).regNo); }
    }
    static void report(String test, String predicted, String observed) {
        System.out.println(test + "\n    PREDICTED: " + predicted + "\n    OBSERVED : " + observed + "\n");
    }

    public static void main(String[] args) {
        // Test 1 - set designed so that the BROKEN class appears to work
        Broken a = new Broken("101", "Arun"), b = new Broken("102", "Divya");
        Set<Broken> s1 = new HashSet<>(List.of(a)); s1.add(b); s1.add(a);               // same OBJECT scanned again
        Set<Broken> s1b = new HashSet<>(); s1b.add(new Broken("1", "A")); s1b.add(new Broken("2", "B")); s1b.add(new Broken("3", "C")); // no repeats at all
        report("Test 1a: Broken class, the same Student object scanned twice (card object reused)", "size 2 (identity hash is equal for the same object, so the duplicate is detected)", "size " + s1.size());
        report("Test 1b: Broken class, three different students, no repeats", "size 3", "size " + s1b.size());

        // Test 2 - set designed to expose duplicates
        int[] sizes = new int[5]; int[] fixedSizes = new int[5];
        for (int run = 0; run < 5; run++) {
            Set<Broken> bs = new HashSet<>(); Set<Fixed> fs = new HashSet<>();
            for (int scan = 0; scan < 10; scan++) for (int id = 0; id < 10; id++) { bs.add(new Broken("R" + id, "N" + id)); fs.add(new Fixed("R" + id, "N" + id)); }
            sizes[run] = bs.size(); fixedSizes[run] = fs.size();
        }
        report("Test 2: 10 students, each scanned 10 times with a NEW object per scan (100 scans)", "Broken: 100 (every new object gets a different identity hash, equals() is never reached); Fixed: 10", "Broken " + Arrays.toString(sizes) + " in 5 runs; Fixed " + Arrays.toString(fixedSizes));
        Set<Broken> bs = new HashSet<>(); bs.add(new Broken("R1", "x"));
        report("Test 2b: Broken set.contains(new Broken(\"R1\", ...)) although R1 is in the set and equals() says they are equal", "false (lookup goes to a different bucket)", String.valueOf(bs.contains(new Broken("R1", "x"))));

        // Test 3 - mutate a field used by hashCode after insertion
        Mutable m = new Mutable("OLD");
        Set<Mutable> ms = new HashSet<>(); ms.add(m);
        m.regNo = "NEW";
        boolean contains = ms.contains(m), removed = ms.remove(m);
        report("Test 3: Fixed-style class, regNo changed from OLD to NEW AFTER insertion", "contains(m) false, remove(m) false, size stays 1 (element is 'lost' in the old bucket); iteration still sees it",
                "contains=" + contains + ", remove=" + removed + ", size=" + ms.size() + ", iteration sees: " + ms.iterator().next().regNo);

        // Test 4 - TreeSet whose compareTo is inconsistent with equals
        Set<ByName> hs = new HashSet<>(), ts = new TreeSet<>();
        ByName x = new ByName("201", "Arun"), y = new ByName("202", "Arun");
        hs.add(x); hs.add(y); ts.add(x); ts.add(y);
        report("Test 4: two DIFFERENT students (regNo 201, 202) with the same name; compareTo compares names only", "HashSet size 2 (equals says different), TreeSet size 1 (compareTo = 0 means 'same')", "HashSet size " + hs.size() + ", TreeSet size " + ts.size());

        // How HashSet uses hashCode and equals
        Counting.hashCalls = 0; Counting.equalsCalls = 0;
        Set<Counting> cs = new HashSet<>();
        cs.add(new Counting("A")); cs.add(new Counting("B"));
        int h1 = Counting.hashCalls, e1 = Counting.equalsCalls;
        cs.add(new Counting("A"));     // duplicate
        System.out.println("How HashSet works: after adding two different elements: hashCode calls = " + h1 + ", equals calls = " + e1
                + "; adding a duplicate caused " + (Counting.hashCalls - h1) + " hashCode call(s) and " + (Counting.equalsCalls - e1) + " equals call(s) -> size " + cs.size() + "\n");

        // Verification of the correction: contract properties on 10,000 random pairs
        Random r = new Random(5); int violations = 0, equalPairs = 0;
        for (int i = 0; i < 10_000; i++) {
            Fixed p = new Fixed("R" + r.nextInt(300), "n" + r.nextInt(5)), q = new Fixed("R" + r.nextInt(300), "n" + r.nextInt(5)), t = new Fixed(p.regNo, "other");
            if (!p.equals(p)) violations++;                                           // reflexive
            if (p.equals(q) != q.equals(p)) violations++;                             // symmetric
            if (p.equals(q) && q.equals(t) && !p.equals(t)) violations++;             // transitive
            if (p.equals(q)) { equalPairs++; if (p.hashCode() != q.hashCode()) violations++; }   // equal => same hash
            if (p.equals(null)) violations++;
            if (!p.equals(t) || p.hashCode() != t.hashCode()) violations++;           // same regNo, different name
        }
        System.out.println("Verification of corrected class on 10,000 random pairs (" + equalPairs + " equal pairs): contract violations = " + violations);
    }
}
