package naturalexceptions.minecraft.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import naturalexceptions.core.biology.BiologicalInjury;
import naturalexceptions.core.biology.BiologicalState;
import naturalexceptions.core.biology.BiologicalTreatment;
import naturalexceptions.minecraft.biology.BiologicalProperties;

/** Minimal consumable dressing/splint kit that changes one active injury process. */
public final class ItemBandage extends Item {
    public ItemBandage() { setMaxStackSize(16); setUnlocalizedName("bandage"); setTextureName("naturalexceptions:field_dressing"); }
    @Override public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote || player == null) return stack;
        BiologicalState state = BiologicalProperties.get(player).state();
        if (state.injuries().isEmpty()) {
            player.addChatMessage(new ChatComponentText("There is no active injury to dress or stabilize."));
            return stack;
        }
        BiologicalInjury injury = state.injuries().get(state.injuries().size() - 1);
        state.treatInjury(injury.id(), injury.kind() == BiologicalInjury.Kind.CUT
            ? BiologicalTreatment.DRESS : BiologicalTreatment.SPLINT);
        if (!player.capabilities.isCreativeMode) stack.stackSize--;
        player.addChatMessage(new ChatComponentText("Treated " + injury.kind().name().toLowerCase()
            + " on " + injury.regionId() + ". Recovery continues over time."));
        return stack.stackSize <= 0 ? null : stack;
    }
}
