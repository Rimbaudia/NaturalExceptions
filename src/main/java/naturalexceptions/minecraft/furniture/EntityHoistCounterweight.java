package naturalexceptions.minecraft.furniture;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;

/**
 * A counterweight box hanging in the well behind its windlass, as deep below the well's mouth as the rope on the drum
 * puts it (the windlass sets it). Right-click it with a block to weight it, with an empty hand to take a block back;
 * hit it, still, to take it off the rope. Never saved: the windlass hangs it again.
 */
public final class EntityHoistCounterweight extends Entity implements IEntityAdditionalSpawnData {

    private static final int DEPTH = 20;

    public int windlassX, windlassY, windlassZ, wellX, wellZ, mouthY;

    public EntityHoistCounterweight(World world) {
        super(world);
        setSize(0.7F, 0.7F);
        noClip = true;
        ignoreFrustumCheck = true;
    }

    EntityHoistCounterweight(World world, int[] windlass, int[] well, int mouthY, double depth) {
        this(world);
        windlassX = windlass[0];
        windlassY = windlass[1];
        windlassZ = windlass[2];
        wellX = well[0];
        wellZ = well[1];
        this.mouthY = mouthY;
        setDepth(depth);
        hang();
    }

    @Override
    protected void entityInit() {
        dataWatcher.addObject(DEPTH, 0.0F);
    }

    void setDepth(double depth) {
        dataWatcher.updateObject(DEPTH, (float) depth);
    }

    public double depth() {
        return dataWatcher.getWatchableObjectFloat(DEPTH);
    }

    private void hang() {
        setPosition(wellX + 0.5, mouthY - depth(), wellZ + 0.5);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        hang();
        if (worldObj.isRemote) return;
        TileEntity tile = worldObj.getTileEntity(windlassX, windlassY, windlassZ);
        if (!(tile instanceof TileEntityHoist) || !((TileEntityHoist) tile).counterHooked()) setDead();
    }

    @Override
    public boolean interactFirst(EntityPlayer player) {
        if (worldObj.isRemote) return true;
        TileEntity tile = worldObj.getTileEntity(windlassX, windlassY, windlassZ);
        if (tile instanceof TileEntityHoist) ((TileEntityHoist) tile).weightCounter(player, player.getHeldItem());
        return true;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (worldObj.isRemote || !(source.getEntity() instanceof EntityPlayer)) return false;
        TileEntity tile = worldObj.getTileEntity(windlassX, windlassY, windlassZ);
        if (tile instanceof TileEntityHoist) ((TileEntityHoist) tile).unhookCounter((EntityPlayer) source.getEntity());
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return !isDead;
    }

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
        for (int v : new int[] { windlassX, windlassY, windlassZ, wellX, wellZ, mouthY }) buffer.writeInt(v);
        buffer.writeFloat((float) depth());
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        windlassX = buffer.readInt();
        windlassY = buffer.readInt();
        windlassZ = buffer.readInt();
        wellX = buffer.readInt();
        wellZ = buffer.readInt();
        mouthY = buffer.readInt();
        setDepth(buffer.readFloat());
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
