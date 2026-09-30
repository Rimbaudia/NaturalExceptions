package naturalexceptions.minecraft.furniture;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * A number rising off a training dummy and fading: the damage of the blow it took. Only ever made on the client that
 * struck it, and never saved.
 */
public final class EntityFloatingNumber extends Entity {

    public static final int LIFE = 30;
    public final float value;

    public EntityFloatingNumber(World world) {
        this(world, 0, 0, 0, 0.0F);
    }

    public EntityFloatingNumber(World world, double x, double y, double z, float value) {
        super(world);
        this.value = value;
        setSize(0.1F, 0.1F);
        noClip = true;
        setPosition(x + (world.rand.nextDouble() - 0.5) * 0.4, y, z + (world.rand.nextDouble() - 0.5) * 0.4);
    }

    /** How the number reads: one decimal place, without a trailing ".0". */
    public String text() {
        float rounded = Math.round(value * 10.0F) / 10.0F;
        return rounded == (int) rounded ? Integer.toString((int) rounded) : Float.toString(rounded);
    }

    @Override
    protected void entityInit() {}

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        setPosition(posX, posY + 0.03, posZ);
        if (++ticksExisted > LIFE) setDead();
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {}

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {}

    @Override
    public boolean writeToNBTOptional(NBTTagCompound tag) {
        return false;
    }
}
