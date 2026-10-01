package naturalexceptions.minecraft.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.entity.EntitySkyWarden;
import naturalexceptions.minecraft.entity.EntitySkystoneSentinel;
import naturalexceptions.minecraft.entity.EntityStormwisp;

/** Spawn eggs for the sky dungeons' creatures (vanilla's eggs cover only vanilla's): one damage value each. */
public final class ItemDungeonEgg extends Item {

    /** Name, shell colour, spot colour. */
    static final Object[][] KINDS = { { "stormwisp", 0x5A3E96, 0xEDE4FF }, { "skystoneSentinel", 0xC8D2E0, 0x7A5AC0 },
        { "skyWarden", 0xAEB8CC, 0xD6AC3C }, { "galeHusk", 0x7E8A9A, 0xD8DCE0 }, { "stormboneArcher", 0xD6D2C4, 0x8E62E0 },
        { "citadelGuard", 0x4A5468, 0xC8D2E0 } };

    @SideOnly(Side.CLIENT)
    private IIcon spots;

    public ItemDungeonEgg() {
        setHasSubtypes(true);
        setUnlocalizedName("dungeonEgg");
        setTextureName("spawn_egg");
    }

    static EntityLiving create(int kind, World world) {
        switch (kind) {
            case 1:
                return new EntitySkystoneSentinel(world);
            case 2:
                return new EntitySkyWarden(world);
            case 3:
                return new naturalexceptions.minecraft.entity.EntityGaleHusk(world);
            case 4:
                return new naturalexceptions.minecraft.entity.EntityStormboneArcher(world);
            case 5:
                return new naturalexceptions.minecraft.entity.EntityCitadelGuard(world);
            default:
                return new EntityStormwisp(world);
        }
    }

    private static int kind(ItemStack stack) {
        return Math.max(0, Math.min(KINDS.length - 1, stack.getItemDamage()));
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return "item.dungeonEgg." + KINDS[kind(stack)][0];
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        int[][] offsets = { { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { -1, 0, 0 }, { 1, 0, 0 } };
        int[] o = offsets[side];
        EntityLiving mob = create(kind(stack), world);
        mob.setLocationAndAngles(
            x + o[0] + 0.5,
            y + o[1] + (side == 1 ? 0.0 : 0.1),
            z + o[2] + 0.5,
            world.rand.nextFloat() * 360.0F,
            0.0F);
        if (mob instanceof naturalexceptions.minecraft.entity.EntityCitadelGuard)
            ((naturalexceptions.minecraft.entity.EntityCitadelGuard) mob).post(x + o[0], y + o[1], z + o[2]);
        world.spawnEntityInWorld(mob);
        mob.playLivingSound();
        if (!player.capabilities.isCreativeMode) stack.stackSize--;
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        for (int i = 0; i < KINDS.length; i++) list.add(new ItemStack(item, 1, i));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamageForRenderPass(int damage, int pass) {
        return pass > 0 ? spots : itemIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return (Integer) KINDS[kind(stack)][pass == 0 ? 1 : 2];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        super.registerIcons(register);
        spots = register.registerIcon("spawn_egg_overlay");
    }
}
