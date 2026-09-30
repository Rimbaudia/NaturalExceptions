package naturalexceptions.minecraft.furniture;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * The hook of a quarry hoist, with its rider on it: from the basket straight up the chain to the windlass's height,
 * then swung in over the lip to set you down beside the windlass; or the same way back down. It creaks as it goes.
 * Like a seat, it is never saved: it lasts only for the ride.
 */
public final class EntityHoistHook extends Entity {

    /** Blocks a tick. */
    static final double SPEED = 0.25;

    /** The ride, as the rider's feet at each point: {x, y, z} ... */
    private double[][] path;
    private int leg;

    public EntityHoistHook(World world) {
        super(world);
        setSize(0.0F, 0.0F);
        noClip = true;
    }

    /**
     * A hook for the ride between a windlass and its basket, starting at the windlass's end when {@code fromTop}.
     * The top of the ride is beside the windlass, on the side towards the basket.
     */
    static EntityHoistHook between(World world, int[] windlass, int[] basket, boolean fromTop) {
        int dx = basket[0] - windlass[0], dz = basket[2] - windlass[2];
        int sx = Math.abs(dx) >= Math.abs(dz) ? Integer.signum(dx) : 0, sz = sx == 0 ? Integer.signum(dz) : 0;
        double[] landing = { windlass[0] + sx + 0.5, windlass[1], windlass[2] + sz + 0.5 };
        double[] over = { basket[0] + 0.5, windlass[1], basket[2] + 0.5 };
        double[] foot = { basket[0] + 0.5, basket[1] + 1, basket[2] + 0.5 };
        EntityHoistHook hook = new EntityHoistHook(world);
        hook.path = fromTop ? new double[][] { landing, over, foot } : new double[][] { foot, over, landing };
        hook.at(hook.path[0]);
        return hook;
    }

    /** Puts the hook so that its rider's feet are at {@code feet}. */
    private void at(double[] feet) {
        setPosition(feet[0], feet[1] + 0.5, feet[2]);
    }

    @Override
    protected void entityInit() {}

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (worldObj.isRemote) {
            setPosition(posX + motionX, posY + motionY, posZ + motionZ); // until the server's word arrives
            return;
        }
        if (riddenByEntity == null || path == null) {
            setDead();
            return;
        }
        riddenByEntity.fallDistance = 0.0F;
        if (ticksExisted % 16 == 1)
            worldObj.playSoundAtEntity(this, "random.door_open", 0.3F, 0.5F + rand.nextFloat() * 0.1F);
        double[] next = path[Math.min(leg + 1, path.length - 1)];
        double tx = next[0] - posX, ty = next[1] + 0.5 - posY, tz = next[2] - posZ;
        double left = Math.sqrt(tx * tx + ty * ty + tz * tz);
        if (left <= SPEED) {
            at(next);
            motionX = motionY = motionZ = 0.0;
            if (++leg >= path.length - 1) arrive();
            return;
        }
        motionX = tx / left * SPEED;
        motionY = ty / left * SPEED;
        motionZ = tz / left * SPEED;
        setPosition(posX + motionX, posY + motionY, posZ + motionZ);
    }

    /** Sets the rider down at the end of the ride, on its feet. */
    private void arrive() {
        Entity rider = riddenByEntity;
        double[] end = path[path.length - 1];
        if (rider != null) {
            rider.mountEntity(null);
            if (rider instanceof EntityPlayerMP) ((EntityPlayerMP) rider).setPositionAndUpdate(end[0], end[1], end[2]);
            else rider.setPosition(end[0], end[1], end[2]);
            rider.fallDistance = 0.0F;
        }
        worldObj.playSoundAtEntity(this, "random.wood_click", 0.6F, 0.8F);
        setDead();
    }

    @Override
    public double getMountedYOffset() {
        return 0.0;
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {}

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {}

    /** Hooks are made again for each ride; none is saved with the world. */
    @Override
    public boolean writeToNBTOptional(NBTTagCompound tag) {
        return false;
    }
}
