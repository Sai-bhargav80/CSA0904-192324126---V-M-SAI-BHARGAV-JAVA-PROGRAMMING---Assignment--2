import java.util.*;

/** Q5 - correctness tests for comparators, wildcards/bounded types, and the Comparable / 3-stable-sorts alternatives. */
public class Q5Experiments {
    static class CollegeTeam extends Team { CollegeTeam(String n, int p, int g) { super(n, p, g); } }
    record Player(String name, int score) { }
    static class TeamC extends Team implements Comparable<TeamC> {        // Comparable alternative: ONE natural order baked into the class
        TeamC(String n, int p, int g) { super(n, p, g); }
        @Override public int compareTo(TeamC o) {
            if (points != o.points) return Integer.compare(o.points, points);
            if (goalDiff != o.goalDiff) return Integer.compare(o.goalDiff, goalDiff);
            return name.compareTo(o.name);
        }
    }
    static final Comparator<Team> GOOD = Comparator.comparingInt((Team t) -> t.points).reversed()
            .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed()).thenComparing(t -> t.name);
    static final Comparator<Team> SUBTRACT = (a, b) -> { int c = b.points - a.points; if (c != 0) return c; c = b.goalDiff - a.goalDiff; return c != 0 ? c : a.name.compareTo(b.name); };
    static final Comparator<Team> NO_TIEBREAK = Comparator.comparingInt((Team t) -> t.points).reversed().thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed());
    static final Comparator<Team> BAD_REVERSE = Comparator.comparingInt((Team t) -> t.points).thenComparingInt(t -> t.goalDiff).thenComparing(t -> t.name).reversed();

    /** Brute-force check of the principle on every triple: antisymmetry, reflexivity, transitivity. Returns the number of violations. */
    static <T> int violations(List<T> xs, Comparator<? super T> c) {
        int v = 0;
        for (T a : xs) { if (c.compare(a, a) != 0) v++; for (T b : xs) if (Integer.signum(c.compare(a, b)) != -Integer.signum(c.compare(b, a))) v++; }
        for (T a : xs) for (T b : xs) for (T d : xs) if (c.compare(a, b) <= 0 && c.compare(b, d) <= 0 && c.compare(a, d) > 0) v++;
        return v;
    }
    /** Wildcards: a board of ANY supertype can receive a list of subtypes (consumer super, producer extends). */
    static <T> void addAll(Leaderboard<? super T> board, List<? extends T> items) { for (T t : items) board.add(t); }
    /** Bounded type: works for any T that can compare itself (or a supertype). */
    static <T extends Comparable<? super T>> T best(Collection<? extends T> c) { T b = null; for (T t : c) if (b == null || t.compareTo(b) < 0) b = t; return b; }

    static List<Team> dataA() {
        return new ArrayList<>(List.of(new Team("Lions", 12, 5), new Team("Tigers", 12, 5), new Team("Hawks", 12, 2), new Team("Wolves", 12, 2),
                new Team("Sharks", 9, 7), new Team("Bears", 9, 7), new Team("Foxes", 9, -1), new Team("Owls", 9, -1), new Team("Ants", 3, 0), new Team("Bats", 3, 0), new Team("Cats", 0, -9)));
    }
    static String names(List<Team> l) { StringBuilder s = new StringBuilder(); for (Team t : l) s.append(t.name).append(' '); return s.toString().trim(); }

    static double mean(double[] a) { double s = 0; for (double v : a) s += v; return s / a.length; }

    public static void main(String[] args) {
        System.out.println("=== Data set A: ties at every level (points, then goal difference, then name) ===");
        Leaderboard<Team> board = new Leaderboard<>(GOOD); for (Team t : dataA()) board.add(t);
        String expected = "Lions Tigers Hawks Wolves Bears Sharks Foxes Owls Ants Bats Cats";
        System.out.println("Expected (by hand): " + expected + "\nObserved          : " + names(board.ranking()) + "\nMatch: " + expected.equals(names(board.ranking())));
        List<Team> shuffled = dataA(); Collections.shuffle(shuffled, new Random(99)); Leaderboard<Team> b2 = new Leaderboard<>(GOOD); for (Team t : shuffled) b2.add(t);
        System.out.println("Same data inserted in a different order gives the same ranking: " + names(b2.ranking()).equals(expected));

        System.out.println("\n=== Data set B: designed to break faulty comparators ===");
        List<Team> rnd = new ArrayList<>(); Random r = new Random(1);
        for (int i = 0; i < 40; i++) rnd.add(new Team("T" + i, r.nextInt(4) * 5, r.nextInt(3)));
        System.out.println("Principle check (all triples of 40 teams with many ties) - number of violations:");
        System.out.println("  correct chain (reversed points, reversed goalDiff, name) : " + violations(rnd, GOOD));
        System.out.println("  no name tie-breaker                                       : " + violations(rnd, NO_TIEBREAK) + "   (equal items still compare as equal, but order then depends on insertion order)");
        System.out.println("  reversed() applied to the WHOLE chain                      : " + violations(rnd, BAD_REVERSE) + "   (consistent, but wrong order: name Z-A, points low first)");
        List<Team> extreme = List.of(new Team("A", Integer.MAX_VALUE, 0), new Team("B", 0, 0), new Team("C", -Integer.MAX_VALUE, 0));
        System.out.println("  subtraction (b.points - a.points) with extreme points     : " + violations(extreme, SUBTRACT) + " violations on only 3 teams");
        System.out.println("      compare(A,B) = " + SUBTRACT.compare(extreme.get(0), extreme.get(1)) + ", compare(B,C) = " + SUBTRACT.compare(extreme.get(1), extreme.get(2))
                + ", compare(A,C) = " + SUBTRACT.compare(extreme.get(0), extreme.get(2)) + "  -> A before B, B before C, but C before A (overflow)");
        List<Team> big = new ArrayList<>(); for (int i = 0; i < 2000; i++) big.add(new Team("T" + i, (r.nextBoolean() ? 1 : -1) * r.nextInt(Integer.MAX_VALUE), 0));
        try { big.sort(SUBTRACT); System.out.println("      sorting 2000 teams with the subtraction comparator: no exception; result correctly ordered by the proper comparator? " + isSorted(big));}
        catch (IllegalArgumentException e) { System.out.println("      sorting 2000 teams with the subtraction comparator threw: " + e.getMessage()); }
        List<Team> ab = dataA(), ba = dataA(); Collections.reverse(ba); ab.sort(NO_TIEBREAK); ba.sort(NO_TIEBREAK);
        System.out.println("  no tie-breaker, input order 1: " + names(ab) + "\n  no tie-breaker, input order 2: " + names(ba) + "\n  rankings identical? " + names(ab).equals(names(ba)));

        System.out.println("\n=== Reuse: wildcards and bounded types ===");
        Leaderboard<Team> teams = new Leaderboard<>(GOOD);
        addAll(teams, List.of(new CollegeTeam("CollegeA", 5, 1), new CollegeTeam("CollegeB", 5, 3)));   // List<CollegeTeam> into Leaderboard<Team>
        Leaderboard<CollegeTeam> colleges = new Leaderboard<>(GOOD);                                     // Comparator<Team> accepted for CollegeTeam (? super T)
        colleges.add(new CollegeTeam("CollegeA", 5, 1)); colleges.add(new CollegeTeam("CollegeB", 5, 3));
        System.out.println("Leaderboard<Team> filled from List<CollegeTeam>: " + teams.ranking());
        System.out.println("Leaderboard<CollegeTeam> using Comparator<Team> : " + colleges.ranking());
        Leaderboard<Player> players = new Leaderboard<>(Comparator.comparingInt(Player::score).reversed().thenComparing(Player::name));
        players.add(new Player("Ravi", 80)); players.add(new Player("Asha", 95)); players.add(new Player("Kumar", 80));
        System.out.println("Same Leaderboard<T> class ranking players        : " + players.ranking());
        System.out.println("best() with T extends Comparable<? super T>      : " + best(List.of(new TeamC("X", 5, 1), new TeamC("Y", 5, 3), new TeamC("Z", 1, 9))));

        System.out.println("\n=== Alternative: Comparator chain vs Comparable vs three stable sorts (mean ms of 5 runs, after warm-up) ===");
        System.out.printf("%10s %22s %22s %26s %s%n", "n", "comparator chain (1 sort)", "Comparable (1 sort)", "3 stable sorts (3 sorts)", "all three give identical rankings");
        for (int n : new int[]{10_000, 100_000, 1_000_000}) {
            Random g = new Random(8); Team[] base = new Team[n]; TeamC[] baseC = new TeamC[n];
            for (int i = 0; i < n; i++) { int p = g.nextInt(40), d = g.nextInt(41) - 20; String nm = "Team" + g.nextInt(n * 10); base[i] = new Team(nm + "-" + i, p, d); baseC[i] = new TeamC(nm + "-" + i, p, d); }
            double[] t1 = new double[5], t2 = new double[5], t3 = new double[5]; boolean same = true;
            for (int run = -2; run < 5; run++) {
                List<Team> a = new ArrayList<>(Arrays.asList(base)); long s = System.nanoTime(); a.sort(GOOD); double m1 = (System.nanoTime() - s) / 1e6;
                List<TeamC> c = new ArrayList<>(Arrays.asList(baseC)); s = System.nanoTime(); Collections.sort(c); double m2 = (System.nanoTime() - s) / 1e6;
                List<Team> d3 = new ArrayList<>(Arrays.asList(base)); s = System.nanoTime();
                d3.sort(Comparator.comparing((Team t) -> t.name)); d3.sort(Comparator.comparingInt((Team t) -> t.goalDiff).reversed()); d3.sort(Comparator.comparingInt((Team t) -> t.points).reversed());
                double m3 = (System.nanoTime() - s) / 1e6;
                if (run >= 0) { t1[run] = m1; t2[run] = m2; t3[run] = m3; }
                if (run == 0) for (int i = 0; i < n && same; i++) same = a.get(i).name.equals(c.get(i).name) && a.get(i).name.equals(d3.get(i).name);
            }
            System.out.printf("%10d %22.1f %22.1f %26.1f %s%n", n, mean(t1), mean(t2), mean(t3), same);
        }
    }
    static boolean isSorted(List<Team> l) { for (int i = 1; i < l.size(); i++) if (GOOD.compare(l.get(i - 1), l.get(i)) > 0) return false; return true; }
}
