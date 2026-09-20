package naturalexceptions.minecraft.metallurgy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;

import naturalexceptions.core.material.MaterialPhase;
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
public final class TileEntitySmelterOutlet extends TileEntity {

    public static final int TOTAL_POUR_TICKS = 30;

    private int facing = 2; // 2=North, 3=South, 4=West, 5=East
    private boolean isPouring = false;
    private int pourTicks = 0;

    public TileEntitySmelterOutlet() {}

    public int getFacing() {
        return facing;
    }

    public void setFacing(int facing) {
        this.facing = facing;
        markDirty();
    }

    public boolean isPouring() {
        return isPouring;
    }

    public int getPourTicks() {
        return pourTicks;
    }

    public TileEntityStarterSmelter findConnectedSmelter() {
        if (worldObj == null) return null;
        // The smelter is behind the outlet (opposite to facing direction)
        int backX = xCoord - getFrontOffsetX(facing);
        int backY = yCoord;
        int backZ = zCoord - getFrontOffsetZ(facing);

        TileEntity te = worldObj.getTileEntity(backX, backY, backZ);
        if (te instanceof TileEntityStarterSmelter) {
            return (TileEntityStarterSmelter) te;
        }

        // Fallback: check all 4 horizontal neighbors
        int[][] dirs = { { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 } };
        for (int[] d : dirs) {
            TileEntity neighbor = worldObj.getTileEntity(xCoord + d[0], yCoord, zCoord + d[1]);
            if (neighbor instanceof TileEntityStarterSmelter) {
                return (TileEntityStarterSmelter) neighbor;
            }
        }
        return null;
    }

    public TileEntityCastingTable findTargetCastingTable() {
        if (worldObj == null) return null;
        // The casting table is directly below the spout, or 1 block down and forward
        int frontX = xCoord + getFrontOffsetX(facing);
        int frontZ = zCoord + getFrontOffsetZ(facing);

        // Check directly below spout
        TileEntity te = worldObj.getTileEntity(frontX, yCoord - 1, frontZ);
        if (te instanceof TileEntityCastingTable) {
            return (TileEntityCastingTable) te;
        }

        // Check directly below outlet block
        te = worldObj.getTileEntity(xCoord, yCoord - 1, zCoord);
        if (te instanceof TileEntityCastingTable) {
            return (TileEntityCastingTable) te;
        }

        // Check 2 blocks below
        te = worldObj.getTileEntity(frontX, yCoord - 2, frontZ);
        if (te instanceof TileEntityCastingTable) {
            return (TileEntityCastingTable) te;
        }

        return null;
    }

    public boolean startPouring(EntityPlayer player) {
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

        // Check thermal state: must have liquid metal
        boolean hasLiquid = false;
        double maxTemp = 293.15;
        for (AggregateLayer l : smelter.vesselState()
            .layers()) {
            if (l.temperatureKelvin() > maxTemp) {
                maxTemp = l.temperatureKelvin();
            }
            if (l.phaseType() == AggregatePhaseType.MOLTEN_METAL && (l.physicalPhase() == MaterialPhase.LIQUID
                || l.thermalState()
                    .meltFraction() >= 0.50
                || l.temperatureKelvin() >= l.thermalState()
                    .properties()
                    .meltingPointKelvin())) {
                hasLiquid = true;
                break;
            }
        }

        if (!hasLiquid) {
            if (player != null) {
                player.addChatMessage(
                    new ChatComponentText(
                        String.format(
                            "\u00a7c[Smelter Outlet] Crucible charge is not molten (Current: %.0f K, need >= 1358 K).",
                            maxTemp)));
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

    @Override
    public void updateEntity() {
        if (!isPouring) {
            return;
        }

        // Particle and sound effects while flowing
        if (worldObj != null) {
            double spoutX = xCoord + 0.5D + getFrontOffsetX(facing) * 0.4D;
            double spoutY = yCoord + 0.35D;
            double spoutZ = zCoord + 0.5D + getFrontOffsetZ(facing) * 0.4D;

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

        pourTicks--;
        if (pourTicks <= 0) {
            // Pour complete: transfer metal
            isPouring = false;
            TileEntityStarterSmelter smelter = findConnectedSmelter();
            TileEntityCastingTable table = findTargetCastingTable();

            if (smelter != null && table != null
                && table.getMold() != null
                && !ItemCastingMold.isFilled(table.getMold())) {
                if (smelter.pourIntoMold(table.getMold())) {
                    table.markDirty();
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
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        try {
            super.readFromNBT(tag);
        } catch (RuntimeException ignored) {}
        this.facing = tag.getInteger("Facing");
        this.isPouring = tag.getBoolean("IsPouring");
        this.pourTicks = tag.getInteger("PourTicks");
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
