package naturalexceptions.minecraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.gear.Treasure;
import naturalexceptions.minecraft.item.ItemTreasureGear;

/**
 * Cloud, gathered from the rims of sky islands: light enough to break by hand, and no floor. Whatever comes onto it
 * sinks slowly through, unhurt (a fall into it takes no damage, see {@code GearMovement.onFall}), and a player can
 * climb back up out of it by holding jump. Only a pair of Cloud Treads stands on it (sneak to sink, as on water).
 * Bottled, it makes a Cloud Flask.
 */
public final class BlockCloud extends Block {

    public BlockCloud() {
        super(Material.cloth);
        setBlockName("cloud");
        setBlockTextureName("naturalexceptions:cloud");
        setHardness(0.2F);
        setLightOpacity(1);
        setStepSound(soundTypeCloth);
    }

    /** The fastest anything sinks through cloud, in blocks a tick (about one a second). */
    public static final double SINK = -0.05;

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    /** Not solid enough to suffocate in. */
    @Override
    public boolean isNormalCube(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    /** A floor only under a pair of Cloud Treads, and only while their wearer is not sneaking. */
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List boxes,
        Entity entity) {
        if (standsOn(entity)) super.addCollisionBoxesToList(world, x, y, z, mask, boxes, entity);
    }

    public static boolean standsOn(Entity entity) {
        return entity instanceof EntityPlayer && !entity.isSneaking()
            && ItemTreasureGear.worn((EntityPlayer) entity, Treasure.CLOUD_TREADS) >= 2;
    }

    /** Inside it, nothing falls faster than a slow sink, and nothing has fallen at all. */
    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        if (entity.motionY < SINK) entity.motionY = SINK;
        entity.fallDistance = 0.0F;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderBlockPass() {
        return 1;
    }

    /** Faces between two cloud blocks are not drawn, so a cloud reads as one soft mass. */
    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        return world.getBlock(x, y, z) != this && super.shouldSideBeRendered(world, x, y, z, side);
    }
}
