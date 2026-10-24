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

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    private net.minecraft.util.IIcon trim;

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public void registerIcons(net.minecraft.client.renderer.texture.IIconRegister register) {
        super.registerIcons(register);
        if (treasure == Treasure.GLIDER_CLOAK) trim = register.registerIcon("naturalexceptions:glider_cloak_trim");
    }

    /** A Glider Cloak is drawn in two passes: its canvas in the colour of its wool, then its trim. */
    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public boolean requiresMultipleRenderPasses() {
        return treasure == Treasure.GLIDER_CLOAK;
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public net.minecraft.util.IIcon getIconFromDamageForRenderPass(int damage, int pass) {
        return pass == 1 && trim != null ? trim : itemIcon;
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return treasure == Treasure.GLIDER_CLOAK && pass == 0 ? GliderCloaks.rgb(GliderCloaks.colour(stack)) : 0xFFFFFF;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String name = super.getItemStackDisplayName(stack);
        return treasure == Treasure.GLIDER_CLOAK ? GliderCloaks.NAMES[GliderCloaks.colour(stack)] + " " + name : name;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return treasure.source() == Treasure.Source.CHEST ? EnumRarity.rare : EnumRarity.epic;
    }

    /** The Warden's Visor lends a soft Lumenite sight: night vision kept topped up while worn. */
    @Override
    public void onWornTick(EntityPlayer player, ItemStack stack) {
        if (treasure != Treasure.WARDENS_VISOR) return;
        net.minecraft.potion.PotionEffect current = player
            .getActivePotionEffect(net.minecraft.potion.Potion.nightVision);
        if (current == null || current.getDuration() < 240) player.addPotionEffect(
            new net.minecraft.potion.PotionEffect(net.minecraft.potion.Potion.nightVision.id, 260, 0, true));
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
        if (treasure.relic()) lines.add("\u00a75Castellan's Relic");
        if (treasure.source() == Treasure.Source.VAULT) lines.add("\u00a75Vault find");
        if (treasure.source() == Treasure.Source.SALVAGE) lines.add("\u00a75Automaton salvage");
        if (treasure.source() == Treasure.Source.GATHERED) lines.add("\u00a75Relic of the Gathered");
        lines.add("\u00a77" + treasure.effect());
        lines.add(
            "\u00a78Worn: " + treasure.kind()
                .label() + " (Gear tab)");
    }
}
