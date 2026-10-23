package naturalexceptions.minecraft.fabrication;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import naturalexceptions.core.anomaly.P8EnvironmentQuery;
import naturalexceptions.core.anomaly.ResolvedEnvironmentalContext;
import naturalexceptions.core.calendar.NaturalCalendar;
import naturalexceptions.core.calendar.SeasonalContext;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.core.worldgen.OverworldGeneration;
import naturalexceptions.core.worldgen.OverworldSample;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;

/**
 * TileEntity hosting a physical casting mold on top of the casting table.
 * Supports world-tick thermodynamic cooling, mold placement, and extraction.
 */
public class TileEntityCastingTable extends TileEntity implements IInventory {

    private ItemStack moldStack;
    private long seasonalCacheDay = Long.MIN_VALUE;
    private SeasonalContext seasonalCache;
    private double seasonalWaterReaction;
    private boolean nearbyP8SitesRefined;

    public TileEntityCastingTable() {}

    public ItemStack getMold() {
        return moldStack;
    }

    public void setMold(ItemStack mold) {
        this.moldStack = mold;
        markDirty();
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }

        if (moldStack != null && moldStack.getItem() instanceof ItemCastingMold) {
            if (ItemCastingMold.hasMetal(moldStack)) {
                boolean wasSolid = ItemCastingMold.isSolidified(moldStack);

                coolMold(0.05);

                boolean nowSolid = ItemCastingMold.isSolidified(moldStack);
                if (wasSolid != nowSolid) {
                    worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 1.0D, zCoord + 0.5D, "random.fizz", 0.7F, 1.5F);
                    for (int i = 0; i < 8; i++) {
                        worldObj.spawnParticle(
                            "smoke",
                            xCoord + 0.3D + worldObj.rand.nextDouble() * 0.4D,
                            yCoord + 1.05D,
                            zCoord + 0.3D + worldObj.rand.nextDouble() * 0.4D,
                            0.0D,
                            0.04D,
                            0.0D);
                    }
                    markDirty();
                    worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                } else if (worldObj.getTotalWorldTime() % 20L == 0L) {
                    markDirty();
                    naturalexceptions.minecraft.network.TileSync.toWatchers(this); // cooling: its renderer redraws it
                }
            }
        }
    }

    /**
     * Calculates the local ambient temperature in Kelvin based on biome and dimension.
     */
    public double calculateAmbientTemperature() {
        if (worldObj == null) {
            return 293.15; // Standard 20 °C ambient
        }
        if (worldObj.provider != null && worldObj.provider.isHellWorld) {
            return 360.0; // Nether furnace environment (~87 °C)
        }
        if (worldObj.provider == null || worldObj.provider.dimensionId == 0) {
            SeasonalContext context = seasonalContext();
            if (context != null) return 263.15 + context.effectiveTemperature() * 37.5;
        }
        try {
            net.minecraft.world.biome.BiomeGenBase biome = worldObj.getBiomeGenForCoords(xCoord, zCoord);
            if (biome != null) {
                float biomeTemp = biome.temperature;
                // Vanilla temperature scale:
                // 0.0 (Taiga/Ice Plains) -> 263.15 K (-10 °C)
                // 0.8 (Plains/Forest) -> 293.15 K (20 °C)
                // 2.0 (Desert) -> 338.15 K (65 °C)
                return 263.15 + (biomeTemp * 37.5);
            }
        } catch (Throwable ignored) {}
        return 293.15;
    }

    private SeasonalContext seasonalContext() {
        if (worldObj == null || worldObj.isRemote) return null;
        long tick = worldObj.getTotalWorldTime();
        long day = tick / NaturalCalendar.TICKS_PER_DAY;
        if (seasonalCache != null && seasonalCacheDay == day) return seasonalCache;
        NEWorldSavedData saved = NEWorldSavedData.get(worldObj);
        if (saved == null || saved.metadata() == null || saved.p8() == null) return null;
        OverworldGeneration generation = new OverworldGeneration(
            worldObj.getSeed(),
            saved.metadata()
                .worldgenEpoch());
        OverworldSample sample = generation.sample(xCoord, zCoord);
        if (!nearbyP8SitesRefined) {
            if (saved.p8()
                .ensureAnomaliesNear(xCoord, zCoord)) saved.markDirty();
            nearbyP8SitesRefined = true;
        }
        ResolvedEnvironmentalContext environmental = P8EnvironmentQuery.at(
            sample,
            xCoord,
            zCoord,
            saved.p8()
                .anomalies(),
            saved.p8()
                .activeInfluencesAt(xCoord, zCoord, tick));
        seasonalCache = SeasonalContext.query(
            sample,
            NaturalCalendar.at(tick),
            saved.p8()
                .strangeSeasonAt(tick),
            environmental.moisture(),
            environmental.waterAvailability(),
            environmental.nocturnalReactivity());
        seasonalWaterReaction = environmental.waterReactionIntensity();
        seasonalCacheDay = day;
        return seasonalCache;
    }

    /**
     * Counts water blocks adjacent to the casting table and mold.
     */
    public int countAdjacentWaterBlocks() {
        if (worldObj == null) {
            return 0;
        }
        int count = 0;
        int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int[] d : dirs) {
            if (isWaterBlock(xCoord + d[0], yCoord, zCoord + d[1])
                || isWaterBlock(xCoord + d[0], yCoord + 1, zCoord + d[1])
                || isP9WaterCell(xCoord + d[0], yCoord, zCoord + d[1])
                || isP9WaterCell(xCoord + d[0], yCoord + 1, zCoord + d[1])) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns true if the mold cavity block above the table is immersed in water.
     */
    public boolean isSubmergedInWater() {
        if (worldObj == null) {
            return false;
        }
        return isWaterBlock(xCoord, yCoord + 1, zCoord) || isP9WaterCell(xCoord, yCoord + 1, zCoord);
    }

    private boolean isP9WaterCell(int x, int y, int z) {
        if (worldObj == null || worldObj.isRemote) return false;
        try {
            NEWorldSavedData saved = NEWorldSavedData.get(worldObj);
            if (saved == null) return false;
            naturalexceptions.core.fluid.FluidParcel parcel = saved.fluids()
                .parcelAt(new naturalexceptions.core.fluid.FluidCellKey(worldObj.provider.dimensionId, x, y, z));
            return P8EnvironmentQuery.qualifiesAsLiquidWater(parcel);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean isWaterBlock(int x, int y, int z) {
        if (worldObj == null) {
            return false;
        }
        try {
            net.minecraft.block.Block block = worldObj.getBlock(x, y, z);
            return block != null && block.getMaterial() == net.minecraft.block.material.Material.water;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Returns true if the mold is exposed to open sky and active precipitation.
     */
    public boolean isExposedToRain() {
        if (worldObj == null) {
            return false;
        }
        try {
            return worldObj.canBlockSeeTheSky(xCoord, yCoord + 1, zCoord) && worldObj.isRaining();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Cools mold contents using comprehensive environmental thermodynamics:
     * - Radiation to ambient (Stefan-Boltzmann)
     * - Natural convection in air
     * - Convective acceleration from open air / weather / rainfall evaporative cooling
     * - Aggressive water immersion / adjacent water block quenching
     */
    public void coolMold(double deltaSeconds) {
        if (moldStack == null || !(moldStack.getItem() instanceof ItemCastingMold)) {
            return;
        }
        if (!ItemCastingMold.hasMetal(moldStack)) {
            return;
        }
        ActiveThermalState thermal = ItemCastingMold.getThermalState(moldStack);
        if (thermal == null) {
            return;
        }

        double ambientTempK = calculateAmbientTemperature();
        double currentTempK = thermal.temperatureKelvin();
        if (currentTempK <= ambientTempK) {
            return;
        }

        // 1. Stefan-Boltzmann radiation
        double surfaceArea = 0.06; // Mold exposed top + rim surface area (m²)
        double emissivity = 0.80;
        double stefanBoltzmann = 5.670374419e-8;
        double qRad = emissivity * stefanBoltzmann
            * surfaceArea
            * (Math.pow(currentTempK, 4.0) - Math.pow(ambientTempK, 4.0))
            * deltaSeconds;

        // 2. Convective Heat Transfer
        double hConv = 40.0; // W / (m² · K) base air convection + stone table heat sink
        SeasonalContext season = seasonalContext();
        if (season != null) hConv *= season.moldCoolingMultiplier();

        // Biome humidity / rainfall factor
        float rainfall = 0.5F;
        if (worldObj != null) {
            try {
                net.minecraft.world.biome.BiomeGenBase biome = worldObj.getBiomeGenForCoords(xCoord, zCoord);
                if (biome != null) {
                    rainfall = biome.rainfall;
                }
            } catch (Throwable ignored) {}
        }
        hConv += rainfall * 10.0;

        // Weather exposure: open sky + rain
        boolean openToSky = false;
        if (worldObj != null) {
            try {
                openToSky = worldObj.canBlockSeeTheSky(xCoord, yCoord + 1, zCoord);
            } catch (Throwable ignored) {}
        }
        boolean raining = isExposedToRain();

        if (raining) {
            hConv += 100.0;
            if (currentTempK > 373.15 && worldObj != null && worldObj.rand.nextInt(4) == 0) {
                worldObj.spawnParticle("smoke", xCoord + 0.5D, yCoord + 1.05D, zCoord + 0.5D, 0.0D, 0.05D, 0.0D);
            }
        } else if (openToSky) {
            hConv += 15.0;
        }

        // Water quenching: adjacent blocks or complete immersion
        boolean submerged = isSubmergedInWater();
        int waterBlocks = countAdjacentWaterBlocks();

        if (submerged) {
            hConv += 500.0;
            // P8 composes regional/event context through this existing water response. Minecraft water is the
            // temporary host representation; this is not the later material-phase fluid system.
            hConv += 120.0 * seasonalWaterReaction;
            if (currentTempK > 373.15 && worldObj != null && worldObj.rand.nextInt(3) == 0) {
                worldObj.spawnParticle("splash", xCoord + 0.5D, yCoord + 1.1D, zCoord + 0.5D, 0.0D, 0.08D, 0.0D);
                worldObj.playSoundEffect(xCoord + 0.5D, yCoord + 1.0D, zCoord + 0.5D, "random.fizz", 0.5F, 1.8F);
            }
        } else if (waterBlocks > 0) {
            hConv += waterBlocks * 125.0;
            hConv += 120.0 * seasonalWaterReaction;
            if (currentTempK > 373.15 && worldObj != null && worldObj.rand.nextInt(6) == 0) {
                worldObj.spawnParticle("cloud", xCoord + 0.5D, yCoord + 1.02D, zCoord + 0.5D, 0.0D, 0.03D, 0.0D);
            }
        }

        double qConv = hConv * surfaceArea * (currentTempK - ambientTempK) * deltaSeconds;
        double totalLossJoules = Math.max(0.0, qRad + qConv);

        double ambientEnergy = naturalexceptions.core.thermal.ThermodynamicModel
            .energyForTemperature(ambientTempK, 0.0, thermal.massKg(), thermal.properties());
        if (thermal.energyJoules() - totalLossJoules < ambientEnergy) {
            thermal.setEnergyJoules(ambientEnergy);
        } else {
            thermal.removeEnergyJoules(totalLossJoules);
        }
        ItemCastingMold.saveThermalState(moldStack, thermal);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        try {
            super.readFromNBT(tag);
        } catch (RuntimeException ignored) {
            // Allows testing outside full LaunchClassLoader/FML mapping lifecycle
        }
        if (tag.hasKey("Mold")) {
            NBTTagCompound moldTag = tag.getCompoundTag("Mold");
            this.moldStack = ItemStack.loadItemStackFromNBT(moldTag);
            if (this.moldStack == null && moldTag.hasKey("tag")) {
                net.minecraft.item.Item item = naturalexceptions.minecraft.registry.NEItems.castingMold != null
                    ? naturalexceptions.minecraft.registry.NEItems.castingMold
                    : new ItemCastingMold();
                this.moldStack = new ItemStack(item);
                this.moldStack.setTagCompound(
                    (NBTTagCompound) moldTag.getCompoundTag("tag")
                        .copy());
            }
        } else {
            this.moldStack = null;
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        try {
            super.writeToNBT(tag);
        } catch (RuntimeException ignored) {
            // Allows testing outside full LaunchClassLoader/FML mapping lifecycle
        }
        if (moldStack != null) {
            NBTTagCompound moldTag = new NBTTagCompound();
            moldStack.writeToNBT(moldTag);
            tag.setTag("Mold", moldTag);
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
        // Its contents are drawn by its special renderer; nothing in the chunk needs rebuilding.
        readFromNBT(pkt.func_148857_g());
    }

    // --- IInventory Implementation (1 slot) ---

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? moldStack : null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (slot == 0 && moldStack != null) {
            ItemStack res = moldStack;
            moldStack = null;
            markDirty();
            if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            return res;
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (slot == 0 && moldStack != null) {
            ItemStack res = moldStack;
            moldStack = null;
            return res;
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot == 0) {
            moldStack = stack;
            markDirty();
            if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    @Override
    public String getInventoryName() {
        return "container.castingTable";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        if (worldObj == null || worldObj.getTileEntity(xCoord, yCoord, zCoord) != this) {
            return false;
        }
        return player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && stack != null && stack.getItem() instanceof ItemCastingMold;
    }
}
