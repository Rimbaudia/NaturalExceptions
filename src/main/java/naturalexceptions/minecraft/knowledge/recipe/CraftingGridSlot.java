package naturalexceptions.minecraft.knowledge.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

/**
 * A slot in a 3x3 crafting grid display, supporting OreDictionary alternative cycling.
 */
public final class CraftingGridSlot {

    private final List<ItemStack> alternatives;
    private final String oreDictName;

    public CraftingGridSlot(List<ItemStack> alternatives, String oreDictName) {
        this.alternatives = new ArrayList<>();
        if (alternatives != null) {
            for (ItemStack alternative : alternatives) {
                if (alternative != null && alternative.getItem() != null) this.alternatives.add(alternative.copy());
            }
        }
        this.oreDictName = oreDictName;
    }

    public static CraftingGridSlot empty() {
        return new CraftingGridSlot(Collections.emptyList(), null);
    }

    public static CraftingGridSlot of(ItemStack stack) {
        if (stack == null) return empty();
        return new CraftingGridSlot(Collections.singletonList(stack.copy()), null);
    }

    public static CraftingGridSlot ofList(List<ItemStack> list, String oreDictName) {
        if (list == null || list.isEmpty()) return empty();
        return new CraftingGridSlot(list, oreDictName);
    }

    public boolean isEmpty() {
        return alternatives.isEmpty();
    }

    public List<ItemStack> alternatives() {
        return Collections.unmodifiableList(alternatives);
    }

    public String oreDictName() {
        return oreDictName;
    }

    /**
     * Gets the currently displayed ItemStack, cycling alternatives every second.
     */
    public ItemStack getDisplayStack(long systemTimeMs) {
        if (alternatives.isEmpty()) return null;
        if (alternatives.size() == 1) return alternatives.get(0)
            .copy();
        int index = (int) ((systemTimeMs / 1000L) % alternatives.size());
        return alternatives.get(index)
            .copy();
    }
}
