/** Q1 design decision: several threads appending to ONE shared log. */
public class ThreadSafetyDemo {
    public static void main(String[] args) throws Exception {
        final int THREADS = 8, APPENDS = 50_000, TRIALS = 20, EXPECTED = THREADS * APPENDS * 4;
        System.out.println("8 threads x 50,000 appends of \"log;\"; expected length = " + EXPECTED);
        for (String kind : new String[]{"StringBuilder", "StringBuffer"}) {
            int corrupted = 0;
            for (int trial = 1; trial <= TRIALS; trial++) {
                final Appendable[] holder = new Appendable[1];
                final StringBuilder sb = new StringBuilder(); final StringBuffer sf = new StringBuffer();
                final java.util.concurrent.atomic.AtomicInteger errors = new java.util.concurrent.atomic.AtomicInteger();
                Thread[] ts = new Thread[THREADS];
                for (int i = 0; i < THREADS; i++) {
                    ts[i] = new Thread(() -> {
                        for (int k = 0; k < APPENDS; k++) {
                            try { if (kind.equals("StringBuilder")) sb.append("log;"); else sf.append("log;"); }
                            catch (RuntimeException e) { errors.incrementAndGet(); }
                        }
                    });
                    ts[i].start();
                }
                for (Thread t : ts) t.join();
                int len = kind.equals("StringBuilder") ? sb.length() : sf.length();
                boolean bad = !(len == EXPECTED && errors.get() == 0);
                if (bad) corrupted++;
                System.out.printf("%-14s trial %2d: length = %d  missing chars = %d  exceptions = %d  %s%n", kind, trial, len, EXPECTED - len, errors.get(), bad ? "CORRUPTED" : "OK");
            }
            System.out.println(">>> " + kind + ": " + corrupted + " of " + TRIALS + " trials corrupted\n");
        }
    }
}
