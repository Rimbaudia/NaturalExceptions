package naturalexceptions.minecraft.furniture;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * The unseen thing a player sits on when using a bench, pew or chair. It lasts only while someone sits and the seat
 * stands; a seated player heals a heart's half every two seconds while fed (more than three shanks).
 */
public final class EntitySeat extends Entity {

    /** How high above the block's floor the sitter's seat is. */
    static final double HEIGHT = 0.1;
    public static final int HEAL_TICKS = 40;

    public EntitySeat(World world) {
        super(world);
        setSize(0.0F, 0.0F);
        noClip = true;
    }

    EntitySeat(World world, int x, int y, int z) {
        this(world);
        setPosition(x + 0.5, y + HEIGHT, z + 0.5);
    }

    @Override
    protected void entityInit() {}

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (worldObj.isRemote) return;
        int x = MathHelper.floor_double(posX), y = MathHelper.floor_double(posY), z = MathHelper.floor_double(posZ);
        if (riddenByEntity == null || !(worldObj.getBlock(x, y, z) instanceof BlockSeat)) {
            if (riddenByEntity != null) riddenByEntity.mountEntity(null);
            setDead();
            return;
        }
        if (ticksExisted % HEAL_TICKS == 0 && riddenByEntity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) riddenByEntity;
            if (player.getFoodStats()
                .getFoodLevel() > 6 && player.getHealth() < player.getMaxHealth()) player.heal(1.0F);
        }
    }

    @Override
    public double getMountedYOffset() {
        return 0.0;
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {}

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {}

    /** Seats are made again when sat on; none is saved with the world. */
    @Override
    public boolean writeToNBTOptional(NBTTagCompound tag) {
        return false;
    }
}
