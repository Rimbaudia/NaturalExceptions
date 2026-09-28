package naturalexceptions.minecraft.metallurgy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;

import naturalexceptions.core.material.MaterialAppearance;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.minecraft.fabrication.TileEntityCastingTable;
import naturalexceptions.minecraft.item.ItemCastingMold;

/**
 * TileEntity controlling the physical metallurgical outlet and pouring stream.
 *
 * <p>
 * Connects a Starter Smelter vessel to a Casting Table below, streaming
 * glowing liquid metal across physical world space over active pour ticks.
 */
public class TileEntitySmelterOutlet extends TileEntity {

    public static final int TOTAL_POUR_TICKS = 30;

    private int facing = 2; // 2=North, 3=South, 4=West, 5=East
    private boolean isPouring = false;
    private int pourTicks = 0;
    private int streamColor = 0xFF8822;

    public TileEntitySmelterOutlet() {}

    public int getStreamColor() {
        return streamColor;
    }

    public int getFacing() {
        if (worldObj != null) {
            int metadata = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
            if (metadata >= 2 && metadata <= 5) return metadata;
        }
        return facing;
    }

    public void setFacing(int facing) {
        this.facing = facing >= 2 && facing <= 5 ? facing : 2;
        markDirty();
    }

    public boolean isPouring() {
        return isPouring;
    }

    public int getPourTicks() {
        return pourTicks;
    }

    public TileEntityStarterSmelter findConnectedSmelter() {
        return SmelterPortLookup.findOutletController(worldObj, xCoord, yCoord, zCoord, getFacing());
    }

    public TileEntityCastingTable findTargetCastingTable() {
        if (worldObj == null) return null;
        // The lip is at the center of this block; the receiving table must be beneath it.
        TileEntity te = worldObj.getTileEntity(xCoord, yCoord - 1, zCoord);
        if (te instanceof TileEntityCastingTable) {
            return (TileEntityCastingTable) te;
        }

        // Check 2 blocks below
        te = worldObj.getTileEntity(xCoord, yCoord - 2, zCoord);
        if (te instanceof TileEntityCastingTable) {
            return (TileEntityCastingTable) te;
        }

        return null;
    }

    public boolean startPouring(EntityPlayer player) {
        if (worldObj != null && worldObj.isRemote) return true;
        if (isPouring) {
            if (player != null) {
                player.addChatMessage(
                    new ChatComponentText("\u00a7e[Smelter Outlet] Metal is currently flowing into the mold!"));
            }
            return true;
        }

        TileEntityStarterSmelter smelter = findConnectedSmelter();
        if (smelter == null) {
            if (player != null) {
                player.addChatMessage(
                    new ChatComponentText(
                        "\u00a7c[Smelter Outlet] No connected Starter Smelter found behind this outlet."));
            }
            return false;
        }

        TileEntityCastingTable table = findTargetCastingTable();
        if (table == null) {
            if (player != null) {
                player.addChatMessage(
                    new ChatComponentText(
                        "\u00a7c[Smelter Outlet] No Casting Table found below this outlet spout. Place one below."));
            }
            return false;
        }

        ItemStack tableMold = table.getMold();
        if (tableMold == null) {
            if (player != null) {
                player.addChatMessage(
                    new ChatComponentText(
                        "\u00a7e[Smelter Outlet] Place an empty casting mold on the Casting Table below first."));
            }
            return false;
        }

        if (ItemCastingMold.isFilled(tableMold)) {
            if (player != null) {
                player.addChatMessage(
                    new ChatComponentText("\u00a7e[Smelter Outlet] Mold on Casting Table is already full of metal!"));
            }
            return false;
        }

        MaterialRef existingRef = ItemCastingMold.getContainedMaterialRef(tableMold);

        // Check thermal state: must have liquid metal
        boolean hasLiquid = false;
        double maxTemp = 293.15;
        double minMeltPoint = Double.POSITIVE_INFINITY;
        AggregateLayer liquidToPour = null;
        for (AggregateLayer l : smelter.vesselState()
            .layers()) {
            if (l.phaseType() == AggregatePhaseType.SLAG) {
                continue;
            }
            if (l.temperatureKelvin() > maxTemp) {
                maxTemp = l.temperatureKelvin();
            }
            double mp = l.thermalState()
                .properties()
                .meltingPointKelvin();
            if (mp > 0 && mp < minMeltPoint) {
                minMeltPoint = mp;
            }
            if (l.phaseType() == AggregatePhaseType.MOLTEN_METAL
                && (l.physicalPhase() == MaterialPhase.LIQUID || l.thermalState()
                    .meltFraction() >= 0.50 || (mp > 0 && l.temperatureKelvin() >= mp))) {
                hasLiquid = true;
                liquidToPour = l;
                break;
            }
        }

        if (!hasLiquid) {
            if (player != null) {
                String requirement = Double.isInfinite(minMeltPoint) ? "no material melting point is available"
                    : String.format("need >= %.0f K", minMeltPoint);
                player.addChatMessage(
                    new ChatComponentText(
                        String.format(
                            "\u00a7c[Smelter Outlet] Crucible charge has no compatible molten metal ready (Current: %.0f K, %s).",
                            maxTemp,
                            requirement)));
            }
            return false;
        }

        // VesselState orders reachable dense liquid layers first. An outlet cannot skip a
        // physically intervening layer to draw a requested material from deeper in the vessel.
        if (liquidToPour != null && existingRef != null && !existingRef.equals(liquidToPour.materialRef())) {
            if (player != null) player.addChatMessage(
                new ChatComponentText(
                    "\u00a7c[Smelter Outlet] The lowest accessible liquid layer is a different material."));
            return false;
        }

        if (liquidToPour != null) {
            this.streamColor = MaterialAppearance
                .getMoltenColor(liquidToPour.materialRef(), liquidToPour.stateKey(), liquidToPour.temperatureKelvin());
        }

        // Initiate physical flowing stream
        this.isPouring = true;
        this.pourTicks = TOTAL_POUR_TICKS;
        markDirty();
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, "liquid.lavapop", 1.0F, 1.0F);
        }

        if (player != null) {
            player.addChatMessage(
                new ChatComponentText(
                    "\u00a76[Smelter Outlet] Opened tap! Molten metal is streaming into \u00a7f"
                        + tableMold.getDisplayName()
                        + "\u00a76..."));
        }
        return true;
    }

    public boolean pourIntoTarget(TileEntityStarterSmelter smelter, TileEntityCastingTable table) {
        if (smelter == null || table == null) {
            return false;
        }
        ItemStack tableMold = table.getMold();
        if (tableMold == null || ItemCastingMold.isFilled(tableMold)) {
            return false;
        }
        boolean success = smelter.pourIntoMold(tableMold);
        if (success) {
            table.markDirty();
            if (worldObj != null) {
                worldObj.markBlockForUpdate(table.xCoord, table.yCoord, table.zCoord);
                worldObj.playSoundEffect(
                    table.xCoord + 0.5D,
                    table.yCoord + 0.5D,
                    table.zCoord + 0.5D,
                    "liquid.lavapop",
                    1.0F,
                    0.8F);
            }
        }
        return success;
    }

    @Override
    public void updateEntity() {
        if (!isPouring) {
            return;
        }

        // Particle and sound effects while flowing
        if (worldObj != null) {
            double spoutX = xCoord + SmelterOutletGeometry.STREAM_X;
            double spoutY = yCoord + SmelterOutletGeometry.STREAM_Y;
            double spoutZ = zCoord + SmelterOutletGeometry.STREAM_Z;

            // Spurt lava droplets and flame down along the stream
            worldObj.spawnParticle("lava", spoutX, spoutY, spoutZ, 0.0D, -0.1D, 0.0D);
            worldObj.spawnParticle("flame", spoutX, spoutY - 0.2D, spoutZ, 0.0D, -0.08D, 0.0D);

            if (worldObj.rand.nextInt(4) == 0) {
                worldObj.spawnParticle(
                    "smoke",
                    spoutX + (worldObj.rand.nextDouble() - 0.5D) * 0.2D,
                    spoutY - 0.5D,
                    spoutZ + (worldObj.rand.nextDouble() - 0.5D) * 0.2D,
                    0.0D,
                    0.05D,
                    0.0D);
            }

            if (worldObj.getTotalWorldTime() % 8L == 0L) {
                worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, "liquid.lavapop", 0.8F, 1.2F);
            }
        }

        if (worldObj != null && worldObj.isRemote) {
            return;
        }

        TileEntityStarterSmelter smelter = findConnectedSmelter();
        TileEntityCastingTable table = findTargetCastingTable();
        if (smelter == null || table == null || table.getMold() == null || ItemCastingMold.isFilled(table.getMold())) {
            isPouring = false;
            pourTicks = 0;
            markDirty();
            if (worldObj != null) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
            return;
        }

        pourTicks--;
        if (pourTicks <= 0) {
            // Pour complete: transfer metal
            isPouring = false;
            pourIntoTarget(smelter, table);

            markDirty();
            if (worldObj != null) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
        }
    }

    public static int getFrontOffsetX(int facing) {
        switch (facing) {
            case 4:
                return -1;
            case 5:
                return 1;
            default:
                return 0;
        }
    }

    public static int getFrontOffsetZ(int facing) {
        switch (facing) {
            case 2:
                return -1;
            case 3:
                return 1;
            default:
                return 0;
        }
    }

    // --- Persistence & Network Sync ---

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        try {
            super.writeToNBT(tag);
        } catch (RuntimeException ignored) {}
        tag.setInteger("Facing", facing);
        tag.setBoolean("IsPouring", isPouring);
        tag.setInteger("PourTicks", pourTicks);
        tag.setInteger("StreamColor", streamColor);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        try {
            super.readFromNBT(tag);
        } catch (RuntimeException ignored) {}
        int savedFacing = tag.getInteger("Facing");
        this.facing = savedFacing >= 2 && savedFacing <= 5 ? savedFacing : 2;
        this.isPouring = tag.getBoolean("IsPouring");
        this.pourTicks = tag.getInteger("PourTicks");
        if (tag.hasKey("StreamColor")) {
            this.streamColor = tag.getInteger("StreamColor");
        }
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }
}
