package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import naturalexceptions.minecraft.gear.GearKind;
import naturalexceptions.minecraft.gear.GearSlot;
import naturalexceptions.minecraft.gear.IWearable;
import naturalexceptions.minecraft.gear.PlayerGear;
import naturalexceptions.minecraft.gear.Treasure;

/** One of the sky-cache treasures. Worn in its Gear slot, where its effect applies. */
public final class ItemTreasureGear extends Item implements IWearable {

    private final Treasure treasure;

    public ItemTreasureGear(Treasure treasure) {
        this.treasure = treasure;
        setMaxStackSize(1);
        setUnlocalizedName(treasure.unlocalizedName());
        setTextureName(treasure.texture());
    }

    public Treasure treasure() {
        return treasure;
    }

    @Override
    public GearKind gearKind(ItemStack stack) {
        return treasure.kind();
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.rare;
    }

    /** How many of this treasure the player is wearing (two for a pair of claws, up to ten bands). */
    public static int worn(EntityPlayer player, Treasure treasure) {
        if (player == null) return 0;
        PlayerGear gear = PlayerGear.get(player);
        int count = 0;
        for (GearSlot slot : GearSlot.values()) {
            ItemStack stack = gear.get(slot);
            if (stack != null && stack.getItem() instanceof ItemTreasureGear
                && ((ItemTreasureGear) stack.getItem()).treasure == treasure) count++;
        }
        return count;
    }

    /** Right-click puts it on, like armour, when a slot of its kind is free. */
    @Override
    public ItemStack onItemRightClick(ItemStack stack, net.minecraft.world.World world, EntityPlayer player) {
        if (world.isRemote || player == null
            || !PlayerGear.get(player)
                .wear(stack))
            return stack;
        world.playSoundAtEntity(player, "step.cloth", 0.8F, 1.1F);
        stack.stackSize--;
        // Never return null here: vanilla's creative path writes the old count back into the returned stack.
        // An emptied stack is removed from the slot by vanilla in survival.
        return stack;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add("§7" + treasure.effect());
        lines.add(
            "§8Worn: " + treasure.kind()
                .label() + " (Gear tab)");
    }
}
