package naturalexceptions.minecraft.gear;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/** An item that is worn in a gear slot rather than held or carried. */
public interface IWearable {

    /** Which kind of gear slot takes this stack, or null if this particular stack cannot be worn. */
    GearKind gearKind(ItemStack stack);

    /** Called once a second on the server while worn. */
    default void onWornTick(EntityPlayer player, ItemStack stack) {}

    static GearKind kindOf(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof IWearable)) return null;
        return ((IWearable) stack.getItem()).gearKind(stack);
    }

    static boolean fits(GearSlot slot, ItemStack stack) {
        return slot != null && slot.kind() == kindOf(stack);
    }
}
