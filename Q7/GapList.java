/**
 * Q7 redesign: a gap buffer list. The array keeps a movable "gap" of unused slots at the position of the last edit,
 * so consecutive edits near the same place need no shifting. Indexing stays O(1).
 *   [ elements before gap ][ ........gap........ ][ elements after gap ]
 */
public class GapList<E> {
    private Object[] buf;
    private int gapStart, gapEnd;          // gap occupies [gapStart, gapEnd)
    public long moves;                     // instrumentation: number of elements copied by gap moves and growth

    public GapList(int initialCapacity) { buf = new Object[Math.max(16, initialCapacity)]; gapStart = 0; gapEnd = buf.length; }
    public int size() { return buf.length - (gapEnd - gapStart); }
    public int capacity() { return buf.length; }

    @SuppressWarnings("unchecked")
    public E get(int i) {
        if (i < 0 || i >= size()) throw new IndexOutOfBoundsException("index " + i + ", size " + size());
        return (E) (i < gapStart ? buf[i] : buf[i + (gapEnd - gapStart)]);
    }
    public void add(E e) { add(size(), e); }
    public void add(int index, E e) {
        if (index < 0 || index > size()) throw new IndexOutOfBoundsException("index " + index + ", size " + size());
        if (gapStart == gapEnd) grow();
        moveGap(index);
        buf[gapStart++] = e;
    }
    @SuppressWarnings("unchecked")
    public E remove(int index) {
        if (index < 0 || index >= size()) throw new IndexOutOfBoundsException("index " + index + ", size " + size());
        moveGap(index);                                  // the element to remove is now the first one after the gap
        E e = (E) buf[gapEnd]; buf[gapEnd++] = null;     // widen the gap over it
        return e;
    }
    /** Moves the gap so that gapStart == pos. Cost = distance between the old and the new gap position. */
    private void moveGap(int pos) {
        if (pos < gapStart) {                            // shift elements [pos, gapStart) to the right end of the gap
            int n = gapStart - pos;
            System.arraycopy(buf, pos, buf, gapEnd - n, n);
            gapStart = pos; gapEnd -= n; moves += n;
            java.util.Arrays.fill(buf, gapStart, gapStart + Math.min(n, gapEnd - gapStart), null);
        } else if (pos > gapStart) {                     // shift elements [gapEnd, gapEnd + n) to the left end of the gap
            int n = pos - gapStart;
            System.arraycopy(buf, gapEnd, buf, gapStart, n);
            java.util.Arrays.fill(buf, Math.max(gapStart + n, gapEnd), gapEnd + n, null);
            gapStart += n; gapEnd += n; moves += n;
        }
    }
    private void grow() {
        int n = size(), newCap = buf.length * 2;
        Object[] nb = new Object[newCap];
        System.arraycopy(buf, 0, nb, 0, gapStart);
        int after = buf.length - gapEnd;
        System.arraycopy(buf, gapEnd, nb, newCap - after, after);
        moves += n; gapEnd = newCap - after; buf = nb;
    }
}
