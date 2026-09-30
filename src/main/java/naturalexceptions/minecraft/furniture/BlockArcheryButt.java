package naturalexceptions.minecraft.furniture;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * An archery butt: a bale of straw with a painted face. An arrow in the face is scored by how near the middle it
 * struck ({@link #score}) and the archer told; a bullseye rings out, throws off a little experience, and gives a
 * short redstone pulse. Meta: the side the face is on (2..5), plus 8 while it pulses.
 */
public final class BlockArcheryButt extends Block {

    public static final int PULSE = 8;

    @SideOnly(Side.CLIENT)
    private IIcon face, straw, top;

    public BlockArcheryButt() {
        super(Material.cloth);
        setBlockName("archeryButt");
        setBlockTextureName("naturalexceptions:archery_butt_face");
        setHardness(0.8F);
        setStepSound(soundTypeGrass);
        setHarvestLevel("axe", 0);
    }

    /**
     * Points for an arrow striking the face at (u, v), each from -0.5 to 0.5 across it: ten in the gold, five in the
     * red, three in the blue, one on the straw.
     */
    public static int score(double u, double v) {
        double d = Math.sqrt(u * u + v * v) / 0.5;
        return d < 0.2 ? 10 : d < 0.45 ? 5 : d < 0.75 ? 3 : 1;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        world.setBlockMetadataWithNotify(x, y, z, BlockPantryShelf.facingTowards(placer), 2);
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        if (world.isRemote || !(entity instanceof EntityArrow)) return;
        EntityArrow arrow = (EntityArrow) entity;
        if (arrow.getEntityData()
            .getBoolean("naturalexceptions:scored")) return;
        arrow.getEntityData()
            .setBoolean("naturalexceptions:scored", true);
        int meta = world.getBlockMetadata(x, y, z), faceSide = meta & 7;
        double rx = arrow.posX - (x + 0.5), ry = arrow.posY - (y + 0.5), rz = arrow.posZ - (z + 0.5);
        // Only an arrow in the painted face counts: it stands out beyond the face's plane on that side.
        boolean inFace = faceSide == 2 ? rz < -0.3 : faceSide == 3 ? rz > 0.3 : faceSide == 4 ? rx < -0.3 : rx > 0.3;
        if (!inFace) return;
        int points = score(faceSide <= 3 ? rx : rz, ry);
        if (arrow.shootingEntity instanceof EntityPlayer) ((EntityPlayer) arrow.shootingEntity).addChatComponentMessage(
            new ChatComponentText(points == 10 ? "§6Bullseye! §710 points" : "§7" + points + (points == 1 ? " point" : " points")));
        if (points == 10) {
            world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "note.pling", 1.0F, 2.0F);
            world.spawnEntityInWorld(new EntityXPOrb(world, x + 0.5, y + 1.2, z + 0.5, 2));
            world.setBlockMetadataWithNotify(x, y, z, meta | PULSE, 3);
            world.notifyBlocksOfNeighborChange(x, y, z, this);
            world.scheduleBlockUpdate(x, y, z, this, 20);
        } else world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "dig.grass", 0.8F, 0.8F);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        int meta = world.getBlockMetadata(x, y, z);
        if ((meta & PULSE) == 0) return;
        world.setBlockMetadataWithNotify(x, y, z, meta & 7, 3);
        world.notifyBlocksOfNeighborChange(x, y, z, this);
    }

    @Override
    public boolean canProvidePower() {
        return true;
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
        return (world.getBlockMetadata(x, y, z) & PULSE) != 0 ? 15 : 0;
    }

    @Override
    public int damageDropped(int meta) {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        face = register.registerIcon("naturalexceptions:archery_butt_face");
        straw = register.registerIcon("hay_block_side");
        top = register.registerIcon("hay_block_top");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        int faceSide = meta & 7;
        if (faceSide < 2) faceSide = 3; // as an item, its face towards you
        if (side == faceSide) return face;
        return side < 2 ? top : straw;
    }
}
