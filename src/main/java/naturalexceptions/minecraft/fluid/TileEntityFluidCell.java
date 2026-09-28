package naturalexceptions.minecraft.fluid;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import naturalexceptions.core.fluid.FluidCellKey;
import naturalexceptions.core.fluid.FluidParcel;
import naturalexceptions.core.material.MaterialAppearance;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;

/**
 * Non-ticking presentation companion of a fluid host block. The parcel stays authoritative in
 * {@code FluidWorldState}; this only carries its derived tint and glow to clients.
 */
public final class TileEntityFluidCell extends TileEntity {

    private int color = 0xFFFFFF;
    private int lightLevel;
    private boolean resolved;

    public int color() {
        return color;
    }

    public int lightLevel() {
        if (!resolved) refreshFromWorld();
        return lightLevel;
    }

    @Override
    public boolean canUpdate() {
        return false;
    }

    /** Derives the visible tint and glow of a parcel from canonical appearance and temperature. */
    public static int[] presentationFor(FluidParcel parcel) {
        if (parcel == null) return new int[] { 0xFFFFFF, 0 };
        double kelvin = parcel.thermalState()
            .temperatureKelvin();
        boolean liquid = parcel.phase() == MaterialPhase.LIQUID;
        int tint = liquid ? MaterialAppearance.getMoltenColor(parcel.materialRef(), parcel.stateKey(), kelvin)
            : MaterialAppearance.getSolidColor(parcel.materialRef(), parcel.stateKey());
        int light = new int[] { 0, 6, 10, 13 }[naturalexceptions.core.fluid.FluidWorldState.incandescenceBand(kelvin)];
        return new int[] { tint & 0xFFFFFF, light };
    }

    void refreshFromWorld() {
        if (worldObj == null || worldObj.isRemote) return;
        resolved = true;
        NEWorldSavedData saved = NEWorldSavedData.get(worldObj);
        if (saved == null) return;
        FluidParcel parcel = saved.fluids()
            .parcelAt(new FluidCellKey(worldObj.provider.dimensionId, xCoord, yCoord, zCoord));
        if (parcel == null) return;
        int[] presentation = presentationFor(parcel);
        color = presentation[0];
        lightLevel = presentation[1];
    }

    @Override
    public Packet getDescriptionPacket() {
        refreshFromWorld();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Color", color);
        tag.setByte("Light", (byte) lightLevel);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        NBTTagCompound tag = packet.func_148857_g();
        color = tag.getInteger("Color");
        lightLevel = tag.getByte("Light");
        resolved = true;
        if (worldObj != null) {
            worldObj.markBlockRangeForRenderUpdate(xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
            worldObj.func_147451_t(xCoord, yCoord, zCoord);
        }
    }
}
