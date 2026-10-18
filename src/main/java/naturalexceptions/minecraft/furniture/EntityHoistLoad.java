package naturalexceptions.minecraft.furniture;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;
import naturalexceptions.NaturalExceptions;
import naturalexceptions.minecraft.gui.NEGuiHandler;

/**
 * A hoist's basket on its rope: always straight under its sheave, as far down as the windlass has paid the rope out.
 * The windlass works the rope; this only hangs on it. Stand in it to ride (right-click it with an empty hand); right-
 * click it with anything else to load it as cargo (it goes into the windlass's store, which the basket carries);
 * sneak and right-click to open that store. Hit it while it is still to take it off the rope (it is a block again).
 * <p>
 * Never saved: the windlass keeps the rope and the cargo, and hangs a new basket when its chunk is loaded again.
 */
public final class EntityHoistLoad extends Entity implements IEntityAdditionalSpawnData {

    /** Watched: the rope paid out from the sheave, exactly, so a client sees it where the server has it. */
    private static final int ROPE = 20;
    /** Watched: where the rope comes down (the sheave, or a swinging boom's tip), and the way it leaves the wheel. */
    private static final int SHEAVE_X = 21, SHEAVE_Z = 22, AWAY = 23;
    /** The basket's floor is this far above its foot (the entity's position). */
    static final double FLOOR = 0.1;

    public int windlassX, windlassY, windlassZ, sheaveX, sheaveY, sheaveZ;
    /** Whoever was riding last tick: set down on their feet when they get out. */
    private Entity lastRider;

    public EntityHoistLoad(World world) {
        super(world);
        setSize(0.9F, 0.6F);
        noClip = true;
        ignoreFrustumCheck = true; // its rope reaches up to the sheave, far from its own box
    }

    EntityHoistLoad(World world, int[] windlass, int[] sheave, double paidOut) {
        this(world);
        windlassX = windlass[0];
        windlassY = windlass[1];
        windlassZ = windlass[2];
        sheaveX = sheave[0];
        sheaveY = sheave[1];
        sheaveZ = sheave[2];
        setRope(paidOut);
        setSheave(sheaveX + 0.5, sheaveZ + 0.5, 0.0);
        hang();
    }

    /** Where the rope comes down (block centre coordinates) and the way it leaves the wheel (radians). */
    void setSheave(double x, double z, double away) {
        dataWatcher.updateObject(SHEAVE_X, (float) x);
        dataWatcher.updateObject(SHEAVE_Z, (float) z);
        dataWatcher.updateObject(AWAY, (float) away);
    }

    /** The way the rope leaves the wheel to come down to the basket, as an angle (radians, from +x towards +z). */
    public double away() {
        return dataWatcher.getWatchableObjectFloat(AWAY);
    }

    @Override
    protected void entityInit() {
        dataWatcher.addObject(ROPE, 1.0F);
        dataWatcher.addObject(SHEAVE_X, 0.0F);
        dataWatcher.addObject(SHEAVE_Z, 0.0F);
        dataWatcher.addObject(AWAY, 0.0F);
    }

    public double rope() {
        return dataWatcher.getWatchableObjectFloat(ROPE);
    }

    void setRope(double paidOut) {
        dataWatcher.updateObject(ROPE, (float) paidOut);
    }

    /** Puts the basket where its rope holds it: the foot {@code rope} under the sheave's axle. */
    private void hang() {
        double x = dataWatcher.getWatchableObjectFloat(SHEAVE_X), z = dataWatcher.getWatchableObjectFloat(SHEAVE_Z);
        if (x == 0 && z == 0) {
            x = sheaveX + 0.5;
            z = sheaveZ + 0.5;
        }
        setPosition(x, sheaveY + 0.5 - rope(), z);
    }

    public double[] sheave() {
        return new double[] { posX, sheaveY + 0.5, posZ };
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        hang();
        if (worldObj.isRemote) return;
        TileEntity tile = worldObj.getTileEntity(windlassX, windlassY, windlassZ);
        if (!(tile instanceof TileEntityHoist) || ((TileEntityHoist) tile).load() != this) {
            if (riddenByEntity != null) riddenByEntity.mountEntity(null);
            setDead();
        }
        if (lastRider != null && riddenByEntity != lastRider) setDown(lastRider);
        lastRider = riddenByEntity;
        if (riddenByEntity != null) riddenByEntity.fallDistance = 0.0F;
    }

    /**
     * Someone getting out sets foot beside the basket on something to stand on, at its floor: on the shaft's floor, or
     * the lip at the top; inside the basket if there is nowhere else.
     */
    private void setDown(Entity rider) {
        if (rider.ridingEntity != null) return;
        if (!isDead && !rider.isDead && !free(rider) && !landing()) {
            // Mid-shaft there is no stepping out: the rider stays standing in the basket.
            rider.mountEntity(this);
            if (rider instanceof EntityPlayer && worldObj.getTotalWorldTime() - stayToldAt > 60) {
                stayToldAt = worldObj.getTotalWorldTime();
                ((EntityPlayer) rider).addChatMessage(
                    new net.minecraft.util.ChatComponentText(
                        "Nowhere to step out here: ride on to the top or the bottom."));
            }
            return;
        }
        double floor = posY + FLOOR;
        int fy = (int) Math.floor(floor + 0.5);
        int bx = (int) Math.floor(posX), bz = (int) Math.floor(posZ);
        double[] spot = { posX, floor, posZ };
        for (int[] o : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
            int x = bx + o[0], z = bz + o[1];
            for (int y = fy - 1; y <= fy + 1; y++) {
                if (standable(x, y, z)) {
                    spot = new double[] { x + 0.5, y, z + 0.5 };
                    break;
                }
            }
            if (spot[0] != posX) break;
        }
        if (rider instanceof EntityPlayerMP) ((EntityPlayerMP) rider).setPositionAndUpdate(spot[0], spot[1], spot[2]);
        else rider.setPosition(spot[0], spot[1], spot[2]);
        rider.fallDistance = 0.0F;
    }

    private long stayToldAt = -1000;

    /** A rider who can fly out (creative) may leave anywhere. */
    private static boolean free(Entity rider) {
        return rider instanceof EntityPlayer && ((EntityPlayer) rider).capabilities.allowFlying;
    }

    /** Whether there is somewhere beside the basket's floor to step out onto. */
    private boolean landing() {
        int fy = (int) Math.floor(posY + FLOOR + 0.5);
        int bx = (int) Math.floor(posX), bz = (int) Math.floor(posZ);
        for (int[] o : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } })
            for (int y = fy - 1; y <= fy + 1; y++) if (standable(bx + o[0], y, bz + o[1])) return true;
        return false;
    }

    private boolean standable(int x, int y, int z) {
        return worldObj.getBlock(x, y - 1, z)
            .getMaterial()
            .isSolid() && worldObj.isAirBlock(x, y, z)
            && worldObj.isAirBlock(x, y + 1, z);
    }

    @Override
    public boolean interactFirst(EntityPlayer player) {
        if (worldObj.isRemote) return true;
        TileEntity tile = worldObj.getTileEntity(windlassX, windlassY, windlassZ);
        if (!(tile instanceof TileEntityHoist)) return true;
        TileEntityHoist hoist = (TileEntityHoist) tile;
        ItemStack held = player.getHeldItem();
        if (player.isSneaking()) {
            player.openGui(
                NaturalExceptions.instance,
                NEGuiHandler.GUI_STORAGE,
                worldObj,
                windlassX,
                windlassY,
                windlassZ);
        } else if (held != null) {
            ItemStack left = hoist.load(held);
            player.inventory.setInventorySlotContents(player.inventory.currentItem, left);
            player.inventoryContainer.detectAndSendChanges();
            player.addChatMessage(new ChatComponentText(hoist.describeCargo()));
        } else if (riddenByEntity == null) {
            player.mountEntity(this);
        }
        return true;
    }

    /** Hit while it hangs still, it comes off the rope and is a basket block again where it is. */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (worldObj.isRemote || !(source.getEntity() instanceof EntityPlayer) || riddenByEntity != null) return false;
        TileEntity tile = worldObj.getTileEntity(windlassX, windlassY, windlassZ);
        if (tile instanceof TileEntityHoist) ((TileEntityHoist) tile).unhook((EntityPlayer) source.getEntity());
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return !isDead;
    }

    @Override
    public double getMountedYOffset() {
        return FLOOR + 0.5; // a rider's feet on the basket's floor
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    /** The server's position packets: the client hangs the basket from the rope itself. */
    @Override
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int increments) {}

    @Override
    public void setVelocity(double x, double y, double z) {}

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public boolean isInRangeToRenderDist(double distanceSq) {
        return distanceSq < 96.0 * 96.0;
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        for (int v : new int[] { windlassX, windlassY, windlassZ, sheaveX, sheaveY, sheaveZ }) buffer.writeInt(v);
        buffer.writeFloat((float) rope());
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        windlassX = buffer.readInt();
        windlassY = buffer.readInt();
        windlassZ = buffer.readInt();
        sheaveX = buffer.readInt();
        sheaveY = buffer.readInt();
        sheaveZ = buffer.readInt();
        setRope(buffer.readFloat());
        hang();
        prevPosX = lastTickPosX = posX;
        prevPosY = lastTickPosY = posY;
        prevPosZ = lastTickPosZ = posZ;
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
