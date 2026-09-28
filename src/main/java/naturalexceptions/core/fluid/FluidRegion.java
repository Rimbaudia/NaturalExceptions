package naturalexceptions.core.fluid;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/** Recomputed connected body index. Its identifier is derived from its lexicographically first cell. */
public final class FluidRegion {
    private final String stableId;
    private final Set<FluidCellKey> cells;
    FluidRegion(Set<FluidCellKey> cells) {
        if (cells == null || cells.isEmpty()) throw new IllegalArgumentException("A fluid region needs cells");
        TreeSet<FluidCellKey> sorted = new TreeSet<>(cells);
        this.cells = Collections.unmodifiableSet(sorted);
        this.stableId = "fluid-region:" + sorted.first().stableString();
    }
    public String stableId() { return stableId; }
    public Set<FluidCellKey> cells() { return cells; }
    public int cellCount() { return cells.size(); }
}
