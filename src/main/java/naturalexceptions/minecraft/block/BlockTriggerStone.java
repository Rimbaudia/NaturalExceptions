package naturalexceptions.minecraft.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import naturalexceptions.core.worldgen.dungeon.TrapLinks;

/**
 * A floor stone of a sky dungeon with a faint seam round it. A player stepping (or landing) on it sets off every trap
 * device within reach: dispensers loose their arrows or fire charges, buried TNT is lit, Sealed Alcoves burst open.
 * It clicks down, then re-arms after a few seconds. Sneaking over it, as over a pressure plate, does not press it.
 */
public final class BlockTriggerStone extends Block {

    /** Meta 0: armed. 1: pressed, re-arming. */
    static final int PRESSED = 1;
    static final int REARM_TICKS = 60;
    static final int TNT_FUSE = 30;

    public BlockTriggerStone() {
        super(Material.rock);
        setBlockName("triggerStone");
        setBlockTextureName("naturalexceptions:trigger_stone");
        setHardness(2.0F);
        setResistance(6000.0F);
        setStepSound(soundTypeStone);
    }

    @Override
    public void onEntityWalking(World world, int x, int y, int z, Entity entity) {
        press(world, x, y, z, entity);
    }

    @Override
    public void onFallenUpon(World world, int x, int y, int z, Entity entity, float fallDistance) {
        press(world, x, y, z, entity);
    }

    private void press(World world, int x, int y, int z, Entity entity) {
        if (world.isRemote || !(entity instanceof EntityPlayer) || world.getBlockMetadata(x, y, z) == PRESSED) return;
        if (naturalexceptions.minecraft.item.ItemTreasureGear
            .worn((EntityPlayer) entity, naturalexceptions.minecraft.gear.Treasure.FEATHERSTEP_SLIPPERS) > 0) return;
        world.setBlockMetadataWithNotify(x, y, z, PRESSED, 2);
        world.scheduleBlockUpdate(x, y, z, this, REARM_TICKS);
        world.playSoundEffect(x + 0.5, y + 1.0, z + 0.5, "random.click", 0.5F, 0.5F);
        fire(world, x, y + 1, z);
    }

    /** Sets off every device in reach of a trigger standing at (x, y, z); returns how many. */
    public static int fire(World world, int x, int y, int z) {
        int fired = 0;
        for (int dx = -TrapLinks.REACH_H; dx <= TrapLinks.REACH_H; dx++) {
            for (int dy = -TrapLinks.REACH_V; dy <= TrapLinks.REACH_V; dy++) {
                for (int dz = -TrapLinks.REACH_H; dz <= TrapLinks.REACH_H; dz++) {
                    int bx = x + dx, by = y + dy, bz = z + dz;
                    Block block = world.getBlock(bx, by, bz);
                    if (block instanceof BlockDispenser) {
                        // A dispenser's scheduled tick always dispenses; a little stagger makes a volley.
                        world.scheduleBlockUpdate(bx, by, bz, block, 1 + (fired % 3) * 2);
                        fired++;
                    } else if (block == Blocks.tnt) {
                        world.setBlockToAir(bx, by, bz);
                        EntityTNTPrimed tnt = new EntityTNTPrimed(world, bx + 0.5, by + 0.5, bz + 0.5, null);
                        tnt.fuse = TNT_FUSE;
                        world.spawnEntityInWorld(tnt);
                        world.playSoundAtEntity(tnt, "game.tnt.primed", 1.0F, 1.0F);
                        fired++;
                    } else if (block instanceof BlockSealedAlcove) {
                        BlockSealedAlcove.release(world, bx, by, bz);
                        fired++;
                    }
                }
            }
        }
        return fired;
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (!world.isRemote && world.getBlockMetadata(x, y, z) == PRESSED)
            world.setBlockMetadataWithNotify(x, y, z, 0, 2);
    }

    @Override
    public int damageDropped(int meta) {
        return 0;
    }
}
