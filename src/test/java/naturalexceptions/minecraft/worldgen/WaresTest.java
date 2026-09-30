package naturalexceptions.minecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

/** Crates and barrels found in the sky hold supplies, and the same spot always holds the same. */
final class WaresTest {

    private static String contents(InventoryBasic inventory) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null) out.append(
                stack.getItem()
                    .getUnlocalizedName())
                .append('x')
                .append(stack.stackSize)
                .append(' ');
        }
        return out.toString();
    }

    @Test
    void cratesAreNeverEmptyAndAlwaysTheSame() {
        for (long seed = 0; seed < 200; seed++) {
            InventoryBasic a = new InventoryBasic("crate", false, 18), b = new InventoryBasic("crate", false, 18);
            Wares.stockCrate(a, seed);
            Wares.stockCrate(b, seed);
            int stacks = 0;
            for (int i = 0; i < a.getSizeInventory(); i++) if (a.getStackInSlot(i) != null) stacks++;
            assertTrue(stacks >= 3, "a crate holds at least three kinds: " + contents(a));
            assertEquals(contents(a), contents(b), "the same crate every time");
        }
    }

    @Test
    void dryBarrelsHoldProduceByTheDozen() {
        for (long seed = 0; seed < 50; seed++) {
            ItemStack store = Wares.barrelStore(seed);
            assertNotNull(store);
            assertTrue(store.stackSize >= 8, "by the dozen: " + store.stackSize);
            assertEquals(
                store.getItem(),
                Wares.barrelStore(seed)
                    .getItem());
        }
    }
}
