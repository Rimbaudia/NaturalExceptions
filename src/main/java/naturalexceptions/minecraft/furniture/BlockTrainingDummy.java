package naturalexceptions.minecraft.furniture;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.block.PartModel;

/**
 * A training dummy: a straw man on a post with his arms out. Strike him and the blow he took rises off him as a
 * number, what that weapon does as you swing it now (strength and weakness, Sharpness, a falling blow's crit). Blows
 * never break him: to take him down, sneak and use an axe. Meta: the side he faces (2..5).
 */
public final class BlockTrainingDummy extends BlockParts {

    private static final double[][] PARTS = { { 0.44, 0.0, 0.44, 0.56, 0.7, 0.56 },
        { 0.28, 0.7, 0.36, 0.72, 1.45, 0.64 }, { 0.0, 1.2, 0.44, 1.0, 1.3, 0.56 },
        { 0.34, 1.45, 0.34, 0.66, 1.78, 0.66 }, { 0.2, 0.0, 0.2, 0.8, 0.1, 0.8 } };

    public BlockTrainingDummy() {
        super(Material.cloth);
        setBlockName("trainingDummy");
        setBlockTextureName("hay_block_side");
        setHardness(1.0F);
        setStepSound(soundTypeGrass);
    }

    /** The damage a player's blow does now, as it would fall on a plain target. */
    public static float blow(EntityPlayer player) {
        float damage = (float) player.getEntityAttribute(SharedMonsterAttributes.attackDamage)
            .getAttributeValue();
        ItemStack held = player.getHeldItem();
        if (held != null)
            damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, held) * 1.25F;
        boolean crit = player.fallDistance > 0.0F && !player.onGround
            && !player.isOnLadder()
            && !player.isInWater()
            && player.ridingEntity == null;
        return crit ? damage * 1.5F : damage;
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) {
        if (world.isRemote) {
            float damage = blow(player);
            world.spawnEntityInWorld(new EntityFloatingNumber(world, x + 0.5, y + 2.0, z + 0.5, damage));
        } else world.playSoundEffect(x + 0.5, y + 1.0, z + 0.5, "step.cloth", 1.0F, 0.6F);
    }

    /** Blows only make the numbers: he comes down to an axe, and only when you mean it (sneaking). */
    @Override
    public float getPlayerRelativeBlockHardness(EntityPlayer player, World world, int x, int y, int z) {
        ItemStack held = player.getHeldItem();
        if (!player.isSneaking() || held == null || !(held.getItem() instanceof ItemAxe)) return 0.0F;
        return super.getPlayerRelativeBlockHardness(player, world, x, y, z);
    }

    @Override
    public double[][] parts(int metadata) {
        int facing = metadata < 2 ? 2 : metadata;
        double[][] out = new double[PARTS.length][];
        for (int i = 0; i < PARTS.length; i++) out[i] = PartModel.rotate(PARTS[i], facing);
        return out;
    }

    @Override
    public int inventoryMetadata() {
        return 3;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        world.setBlockMetadataWithNotify(x, y, z, BlockPantryShelf.facingTowards(placer), 2);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {}

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return Blocks.hay_block.getIcon(2, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon partIcon(int index, int metadata) {
        if (index == 0 || index == 2) return Blocks.log.getIcon(2, 1);
        if (index == 3) return Blocks.wool.getIcon(2, 12); // a sacking head
        if (index == 4) return Blocks.planks.getIcon(1, 1);
        return getIcon(2, 0);
    }
}
