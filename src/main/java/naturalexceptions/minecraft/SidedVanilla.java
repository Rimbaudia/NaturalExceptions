package naturalexceptions.minecraft;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.FoodStats;

import cpw.mods.fml.relauncher.ReflectionHelper;

/**
 * What NE needs from vanilla that 1.7.10 only gives the client a method for: the method is marked client-only and is
 * missing from a dedicated server, so calling it works in single player and crashes a server (kicking the player, for
 * the food level). Here the state is reached through its field, which both sides have, or the call is made only where
 * the method exists. {@code ServerSideLinkageTest} keeps NE from calling such methods elsewhere.
 */
public final class SidedVanilla {

    private static final Field FOOD_LEVEL = ReflectionHelper.findField(FoodStats.class, "foodLevel", "field_75127_a");
    private static final Field FULL_3D = ReflectionHelper.findField(Item.class, "bFull3D", "field_77789_bW");
    /** Whether this side has Item.getSubItems (a client, or the dev and test classpath): a server has not. */
    private static final boolean HAS_SUB_ITEMS = hasSubItems();

    private SidedVanilla() {}

    /** FoodStats.setFoodLevel, which is client-only. */
    public static void setFoodLevel(FoodStats stats, int level) {
        try {
            FOOD_LEVEL.setInt(stats, level);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Item.isFull3D, which is client-only: whether it is held like a tool or weapon (Item.setFull3D). */
    public static boolean isFull3D(Item item) {
        try {
            return FULL_3D.getBoolean(item);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** An item's variants as its creative tabs list them, where that is known (not on a server: none are added). */
    public static void subItems(Item item, List<ItemStack> out) {
        if (HAS_SUB_ITEMS) clientSubItems(item, out);
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    private static void clientSubItems(Item item, List<ItemStack> out) {
        try {
            item.getSubItems(item, (CreativeTabs) null, out);
        } catch (Throwable ignored) {
            // An item whose list needs more of the client than there is (no world, no player yet).
        }
    }

    private static boolean hasSubItems() {
        for (java.lang.reflect.Method m : Item.class.getMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 3 && p[0] == Item.class && p[1] == CreativeTabs.class && p[2] == List.class) return true;
        }
        return false;
    }
}
