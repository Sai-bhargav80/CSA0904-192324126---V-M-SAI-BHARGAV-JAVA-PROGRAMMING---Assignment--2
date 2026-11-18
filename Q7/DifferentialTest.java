import java.util.*;

/** Correctness first: GapList must behave exactly like ArrayList for every sequence of add/remove/get. */
public class DifferentialTest {
    public static void main(String[] args) {
        long seed = 12345; int mismatches = 0, ops = 0;
        for (int trial = 0; trial < 20; trial++) {
            Random r = new Random(seed + trial); GapList<Integer> g = new GapList<>(4); ArrayList<Integer> a = new ArrayList<>();
            int cursor = 0;
            for (int i = 0; i < 20_000; i++, ops++) {
                int c = r.nextInt(10);
                if (r.nextInt(50) == 0) cursor = a.isEmpty() ? 0 : r.nextInt(a.size() + 1);        // jump to a new place now and then
                if (c < 5 || a.isEmpty()) { int idx = Math.min(cursor, a.size()); g.add(idx, i); a.add(idx, i); cursor = idx + 1; }
                else if (c < 7) { int idx = r.nextInt(a.size()); if (!g.remove(idx).equals(a.remove(idx))) mismatches++; cursor = Math.min(cursor, a.size()); }
                else if (c < 8) { g.add(i); a.add(i); }
                else { int idx = r.nextInt(a.size()); if (!g.get(idx).equals(a.get(idx))) mismatches++; }
                if (g.size() != a.size()) mismatches++;
            }
            for (int i = 0; i < a.size(); i++) if (!g.get(i).equals(a.get(i))) { mismatches++; break; }
        }
        System.out.println("Differential test: 20 random sequences, " + ops + " operations total (add at cursor / add at end / remove / get).");
        System.out.println("Mismatches between GapList and ArrayList: " + mismatches + (mismatches == 0 ? "   -> behaviour identical" : "   -> BUG"));
        try { new GapList<Integer>(4).get(0); } catch (IndexOutOfBoundsException e) { System.out.println("Bounds are checked: " + e.getMessage()); }
    }
}
