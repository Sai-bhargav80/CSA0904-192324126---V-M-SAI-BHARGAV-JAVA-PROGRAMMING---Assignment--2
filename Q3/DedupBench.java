import java.util.*;

/** Q3 alternative: HashSet<Student> vs TreeSet<Student> (Comparator on regNo) vs HashMap<String, Student>. */
public class DedupBench {
    static class Student {
        final String regNo, name;
        Student(String r, String n) { regNo = r; name = n; }
        @Override public boolean equals(Object o) { return this == o || (o instanceof Student && regNo.equals(((Student) o).regNo)); }
        @Override public int hashCode() { return regNo.hashCode(); }
    }
    static long used() { System.gc(); System.gc(); Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }
    static int sink;
    public static void main(String[] args) {
        System.out.println("De-duplication: n unique students, each scanned 3 times in random order (3n scans). Mean of 5 runs after warm-up.");
        System.out.printf("%10s %-26s %14s %16s %s%n", "n", "structure", "ns per scan", "unique found", "retained memory (MB)");
        for (int n : new int[]{10_000, 100_000, 1_000_000}) {
            Student[] scans = new Student[3 * n];
            for (int i = 0; i < n; i++) for (int k = 0; k < 3; k++) scans[3 * i + k] = new Student("1923" + String.format("%07d", i), "N" + i);
            List<Student> l = new ArrayList<>(Arrays.asList(scans)); Collections.shuffle(l, new Random(3)); scans = l.toArray(new Student[0]);
            String[] names = {"HashSet<Student>", "TreeSet<Student> + Comparator", "HashMap<String,Student>"};
            for (int ds = 0; ds < 3; ds++) {
                double sum = 0; int size = 0; double mem = 0;
                for (int run = -2; run < 5; run++) {
                    long m0 = used();
                    Collection<?> c; long t0 = System.nanoTime();
                    if (ds == 0) { HashSet<Student> s = new HashSet<>(); for (Student st : scans) s.add(st); c = s; }
                    else if (ds == 1) { TreeSet<Student> s = new TreeSet<>(Comparator.comparing((Student st) -> st.regNo)); for (Student st : scans) s.add(st); c = s; }
                    else { HashMap<String, Student> s = new HashMap<>(); for (Student st : scans) s.putIfAbsent(st.regNo, st); c = s.values(); }
                    long t1 = System.nanoTime();
                    long m1 = used();
                    if (run >= 0) { sum += (double) (t1 - t0) / scans.length; mem = Math.max(mem, (m1 - m0) / 1048576.0); }
                    size = c.size(); sink += size;
                }
                System.out.printf("%10d %-26s %14.1f %16d %.1f%n", n, names[ds], sum / 5, size, mem);
            }
            System.out.println();
        }
    }
}
