package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import naturalexceptions.minecraft.gear.GearKind;
import naturalexceptions.minecraft.gear.IWearable;
import naturalexceptions.minecraft.gear.PlayerGear;

/** A cloth filter mask worn on the face. While worn it takes most, not all, of the smoke out of each breath. */
public final class ItemExposureMask extends Item implements IWearable {

    /** Share of inhaled smoke the mask stops. */
    public static final double INHALATION_ATTENUATION = 0.75;

    public ItemExposureMask() {
        setMaxStackSize(1);
        setUnlocalizedName("exposureMask");
        setTextureName("naturalexceptions:filter_mask");
    }

    @Override
    public GearKind gearKind(ItemStack stack) {
        return GearKind.FACE;
    }

    /** True while {@code player} has a filter mask in their Face slot. */
    public static boolean isWornBy(EntityPlayer player) {
        ItemStack face = player == null ? null
            : PlayerGear.get(player)
                .worn(GearKind.FACE);
        return face != null && face.getItem() instanceof ItemExposureMask;
    }

    /** Right-click puts the mask on, like armour, when the Face slot is free. */
    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player == null || PlayerGear.get(player)
            .worn(GearKind.FACE) != null) return stack;
        if (world.isRemote) return stack;
        if (!PlayerGear.get(player)
            .wear(stack)) return stack;
        world.playSoundAtEntity(player, "step.cloth", 0.8F, 1.1F);
        stack.stackSize--;
        return stack.stackSize <= 0 ? null : stack;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("§7Worn on the face (Gear tab). Eases smoke,");
        lines.add("§7but does not make it safe.");
    }
}
