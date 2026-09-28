package naturalexceptions.minecraft.multiblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;

/**
 * Authoritative, reusable definition of a multiblock structure.
 *
 * <p>
 * Shared identically by:
 * <ul>
 * <li>In-world gameplay validation</li>
 * <li>In-game Knowledge Browser structure view and layer rendering</li>
 * <li>Dynamic required block count derivation</li>
 * <li>Future ghost-block in-world builder projection</li>
 * </ul>
 */
public abstract class MultiblockDefinition {

    private final String id;
    private final String displayName;
    private final MultiblockMaturity maturity;
    private final String maturityNotice;
    private final List<MultiblockPattern> patterns = new ArrayList<>();

    protected MultiblockDefinition(String id, String displayName, MultiblockMaturity maturity, String maturityNotice) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.maturity = Objects.requireNonNull(maturity, "maturity");
        this.maturityNotice = maturityNotice != null ? maturityNotice : maturity.notice();
    }

    protected void addPattern(MultiblockPattern pattern) {
        if (pattern != null) {
            patterns.add(pattern);
        }
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public MultiblockMaturity maturity() {
        return maturity;
    }

    public String maturityNotice() {
        return maturityNotice;
    }

    public List<MultiblockPattern> patterns() {
        return Collections.unmodifiableList(patterns);
    }

    /**
     * Converts local coordinates (dx, dy, dz) to world coordinates based on controller facing.
     *
     * @param cx     Controller world X
     * @param cy     Controller world Y
     * @param cz     Controller world Z
     * @param facing Minecraft orientation metadata (2=North, 3=South, 4=West, 5=East)
     * @param dx     Local X (right offset)
     * @param dy     Local Y (vertical offset)
     * @param dz     Local Z (depth forward into multiblock)
     * @return [worldX, worldY, worldZ]
     */
    public static int[] localToWorld(int cx, int cy, int cz, int facing, int dx, int dy, int dz) {
        int wx = cx;
        int wy = cy + dy;
        int wz = cz;
        switch (facing) {
            case 2: // North: controller faces North (-Z), depth goes South (+Z), right is East (+X)
                wx += dx;
                wz += dz;
                break;
            case 3: // South: controller faces South (+Z), depth goes North (-Z), right is West (-X)
                wx -= dx;
                wz -= dz;
                break;
            case 4: // West: controller faces West (-X), depth goes East (+X), right is North (-Z)
                wx += dz;
                wz -= dx;
                break;
            case 5: // East: controller faces East (+X), depth goes West (-X), right is South (+Z)
                wx -= dz;
                wz += dx;
                break;
            default:
                wx += dx;
                wz += dz;
                break;
        }
        return new int[] { wx, wy, wz };
    }

    /**
     * Authoritative in-world validation of the structure.
     */
    public MultiblockValidationResult validate(IBlockAccess world, int cx, int cy, int cz, int facing) {
        if (world == null) {
            return MultiblockValidationResult.fail(cx, cy, cz, null, "World is null");
        }

        for (MultiblockPattern pattern : patterns) {
            if (!pattern.role()
                .isRequired()) {
                continue; // Optional attachments are not required for valid structure formation
            }

            int[] pos = localToWorld(cx, cy, cz, facing, pattern.dx(), pattern.dy(), pattern.dz());
            if (!pattern.matches(world, pos[0], pos[1], pos[2])) {
                return MultiblockValidationResult.fail(
                    pos[0],
                    pos[1],
                    pos[2],
                    pattern,
                    "Missing " + pattern.blockName() + " at (" + pos[0] + ", " + pos[1] + ", " + pos[2] + ")");
            }
        }

        return MultiblockValidationResult.ok();
    }

    /**
     * Automatically derives required block counts strictly from the structure definition.
     * Excludes optional attachments and cavities.
     */
    public Map<String, Integer> getRequiredCountsByName() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (MultiblockPattern p : patterns) {
            if (p.role()
                .countsAsMaterialItem()) {
                counts.put(p.blockName(), counts.getOrDefault(p.blockName(), 0) + 1);
            }
        }
        return counts;
    }

    /**
     * Automatically derives required items strictly from the structure definition.
     */
    public List<RequiredBlockEntry> getRequiredBlockEntries() {
        Map<String, RequiredBlockEntry> map = new LinkedHashMap<>();
        for (MultiblockPattern p : patterns) {
            if (p.role()
                .countsAsMaterialItem()) {
                RequiredBlockEntry entry = map.get(p.blockName());
                if (entry == null) {
                    entry = new RequiredBlockEntry(p.blockName(), p.displayStack(), 1);
                    map.put(p.blockName(), entry);
                } else {
                    entry.increment();
                }
            }
        }
        return new ArrayList<>(map.values());
    }

    public List<Integer> getLayerNumbers() {
        TreeSet<Integer> set = new TreeSet<>();
        for (MultiblockPattern p : patterns) {
            set.add(p.dy());
        }
        return new ArrayList<>(set);
    }

    public List<MultiblockPattern> getPatternsInLayer(int dy) {
        List<MultiblockPattern> list = new ArrayList<>();
        for (MultiblockPattern p : patterns) {
            if (p.dy() == dy) {
                list.add(p);
            }
        }
        return list;
    }

    public int getMinX() {
        int min = 0;
        for (MultiblockPattern p : patterns) if (p.dx() < min) min = p.dx();
        return min;
    }

    public int getMaxX() {
        int max = 0;
        for (MultiblockPattern p : patterns) if (p.dx() > max) max = p.dx();
        return max;
    }

    public int getMinY() {
        int min = 0;
        for (MultiblockPattern p : patterns) if (p.dy() < min) min = p.dy();
        return min;
    }

    public int getMaxY() {
        int max = 0;
        for (MultiblockPattern p : patterns) if (p.dy() > max) max = p.dy();
        return max;
    }

    public int getMinZ() {
        int min = 0;
        for (MultiblockPattern p : patterns) if (p.dz() < min) min = p.dz();
        return min;
    }

    public int getMaxZ() {
        int max = 0;
        for (MultiblockPattern p : patterns) if (p.dz() > max) max = p.dz();
        return max;
    }

    public static final class RequiredBlockEntry {

        private final String name;
        private final ItemStack stack;
        private int count;

        public RequiredBlockEntry(String name, ItemStack stack, int count) {
            this.name = name;
            this.stack = stack;
            this.count = count;
        }

        public void increment() {
            this.count++;
        }

        public String name() {
            return name;
        }

        public ItemStack stack() {
            return stack != null ? stack.copy() : null;
        }

        public int count() {
            return count;
        }
    }
}
