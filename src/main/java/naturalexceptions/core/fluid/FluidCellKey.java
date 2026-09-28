package naturalexceptions.core.fluid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Stable dimension/block address of one quantized fluid cell. */
public final class FluidCellKey implements Comparable<FluidCellKey> {
    private final int dimension, x, y, z;
    public FluidCellKey(int dimension, int x, int y, int z) {
        this.dimension = dimension; this.x = x; this.y = y; this.z = z;
    }
    public int dimension() { return dimension; }
    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }
    public List<FluidCellKey> neighbors() {
        List<FluidCellKey> result = new ArrayList<>(6);
        result.add(new FluidCellKey(dimension, x - 1, y, z));
        result.add(new FluidCellKey(dimension, x + 1, y, z));
        result.add(new FluidCellKey(dimension, x, y - 1, z));
        result.add(new FluidCellKey(dimension, x, y + 1, z));
        result.add(new FluidCellKey(dimension, x, y, z - 1));
        result.add(new FluidCellKey(dimension, x, y, z + 1));
        Collections.sort(result);
        return result;
    }
    public String stableString() { return dimension + ":" + x + ":" + y + ":" + z; }
    @Override public int compareTo(FluidCellKey o) {
        int c = Integer.compare(dimension, o.dimension); if (c != 0) return c;
        c = Integer.compare(x, o.x); if (c != 0) return c;
        c = Integer.compare(y, o.y); return c != 0 ? c : Integer.compare(z, o.z);
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FluidCellKey)) return false;
        FluidCellKey k = (FluidCellKey)o;
        return dimension == k.dimension && x == k.x && y == k.y && z == k.z;
    }
    @Override public int hashCode() {
        int h = dimension; h = 31 * h + x; h = 31 * h + y; return 31 * h + z;
    }
    @Override public String toString() { return stableString(); }
}
