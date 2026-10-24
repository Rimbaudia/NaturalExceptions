package naturalexceptions.minecraft.power;

import net.minecraft.tileentity.TileEntity;

/** What every machine on a network does to join it: tell the network when it comes and when it goes. */
public abstract class TileSource extends TileEntity implements PowerDevice, PowerReadout {

    private boolean known;

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void updateEntity() {
        if (known || worldObj == null || worldObj.isRemote) return;
        known = true;
        PowerNetworks.dirty(worldObj, xCoord, yCoord, zCoord);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (worldObj != null) PowerNetworks.dirty(worldObj, xCoord, yCoord, zCoord);
    }
}
