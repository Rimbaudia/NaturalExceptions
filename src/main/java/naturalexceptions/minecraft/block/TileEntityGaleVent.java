package naturalexceptions.minecraft.block;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;

/** The Gale Vent's wind: which beat it is on, how far it blows, and what it carries. */
public final class TileEntityGaleVent extends TileEntity {

    public static final int REACH = 6;
    public static final int CYCLE_TICKS = 80;
    public static final int GUST_TICKS = 40;
    /** Speed added per tick along the wind, and the most it will carry anything. */
    static final double PUSH = 0.2;
    static final double MAX_SPEED_UP = 0.95;
    static final double MAX_SPEED_ACROSS = 0.9;

    /** Where in its beat a vent is, offset by position so that vents in a row gust one after another. */
    public static int beat(long worldTime, int x, int y, int z) {
        int offset = Math.floorMod(x * 7 + y * 13 + z * 5, CYCLE_TICKS / 4) * 4;
        return (int) Math.floorMod(worldTime + offset, (long) CYCLE_TICKS);
    }

    public static boolean gusting(long worldTime, int x, int y, int z) {
        return beat(worldTime, x, y, z) < GUST_TICKS;
    }

    @Override
    public void updateEntity() {
        if (worldObj == null) return;
        long time = worldObj.getTotalWorldTime();
        int beat = beat(time, xCoord, yCoord, zCoord);
        if (beat >= GUST_TICKS) return;
        ForgeDirection dir = ForgeDirection.getOrientation(BlockGaleVent.sideOf(getBlockMetadata()));
        if (dir == ForgeDirection.UNKNOWN) return;
        int reach = reach(dir);
        if (reach == 0) return;
        if (!worldObj.isRemote && beat == 0) worldObj.playSoundEffect(
            xCoord + 0.5 + dir.offsetX,
            yCoord + 0.5 + dir.offsetY,
            zCoord + 0.5 + dir.offsetZ,
            "naturalexceptions:trap.gale",
            0.6F,
            0.9F + worldObj.rand.nextFloat() * 0.2F);
        if (worldObj.isRemote) puff(dir, reach);
        double minX = xCoord + Math.min(0, dir.offsetX * reach) + (dir.offsetX > 0 ? 1 : 0);
        double minY = yCoord + Math.min(0, dir.offsetY * reach) + (dir.offsetY > 0 ? 1 : 0);
        double minZ = zCoord + Math.min(0, dir.offsetZ * reach) + (dir.offsetZ > 0 ? 1 : 0);
        double maxX = xCoord + 1 + Math.max(0, dir.offsetX * reach) - (dir.offsetX < 0 ? 1 : 0);
        double maxY = yCoord + 1 + Math.max(0, dir.offsetY * reach) - (dir.offsetY < 0 ? 1 : 0);
        double maxZ = zCoord + 1 + Math.max(0, dir.offsetZ * reach) - (dir.offsetZ < 0 ? 1 : 0);
        @SuppressWarnings("unchecked")
        List<Entity> caught = worldObj
            .getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
        for (Entity entity : caught) {
            // Each side moves what it owns: the server its creatures and items, a client its own player.
            boolean player = entity instanceof EntityPlayer;
            if (worldObj.isRemote ? player && ((EntityPlayer) entity).isClientWorld() : !player) carry(entity, dir);
            entity.fallDistance = 0.0F;
        }
    }

    /** How many open blocks the wind crosses before something solid stops it. */
    private int reach(ForgeDirection dir) {
        for (int i = 1; i <= REACH; i++) {
            int x = xCoord + dir.offsetX * i, y = yCoord + dir.offsetY * i, z = zCoord + dir.offsetZ * i;
            if (worldObj.getBlock(x, y, z)
                .getMaterial()
                .blocksMovement()) return i - 1;
        }
        return REACH;
    }

    static void carry(Entity entity, ForgeDirection dir) {
        if (dir.offsetY > 0) entity.motionY = Math.min(MAX_SPEED_UP, Math.max(entity.motionY, 0.0) + PUSH);
        else if (dir.offsetY < 0) entity.motionY = Math.max(-MAX_SPEED_UP, entity.motionY - PUSH);
        if (dir.offsetX != 0) entity.motionX = clamp(entity.motionX + dir.offsetX * PUSH * 0.75, MAX_SPEED_ACROSS);
        if (dir.offsetZ != 0) entity.motionZ = clamp(entity.motionZ + dir.offsetZ * PUSH * 0.75, MAX_SPEED_ACROSS);
        if (dir.offsetY == 0 && entity.onGround) entity.motionY = Math.max(entity.motionY, 0.12); // off the floor
    }

    private static double clamp(double v, double max) {
        return Math.max(-max, Math.min(max, v));
    }

    private void puff(ForgeDirection dir, int reach) {
        if (worldObj.rand.nextInt(3) != 0) return;
        double x = xCoord + 0.5
            + dir.offsetX * 0.6
            + (worldObj.rand.nextDouble() - 0.5) * 0.7 * (1 - Math.abs(dir.offsetX));
        double y = yCoord + 0.5
            + dir.offsetY * 0.6
            + (worldObj.rand.nextDouble() - 0.5) * 0.7 * (1 - Math.abs(dir.offsetY));
        double z = zCoord + 0.5
            + dir.offsetZ * 0.6
            + (worldObj.rand.nextDouble() - 0.5) * 0.7 * (1 - Math.abs(dir.offsetZ));
        double speed = 0.12 + 0.05 * reach;
        worldObj.spawnParticle("cloud", x, y, z, dir.offsetX * speed, dir.offsetY * speed, dir.offsetZ * speed);
    }
}
