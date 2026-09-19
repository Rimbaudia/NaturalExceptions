package naturalexceptions.minecraft.metallurgy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;

import naturalexceptions.core.fabrication.ComponentGeometry;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.metallurgy.OreFeedstockDefinition;
import naturalexceptions.core.metallurgy.VesselState;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.item.ItemMaterialSample;
import naturalexceptions.minecraft.persistence.VesselStateNbtCodec;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * TileEntity hosting the physical Starter Smelter vessel.
 *
 * <p>
 * Implements bounded, active-only simulation: cold and equilibrated vessels sleep.
 * Implements IInventory to support container slots:
 * <ul>
 * <li>Slot 0: Fuel input (solid combustible items)</li>
 * <li>Slot 1: Feed input (ore chunks, solid scrap, raw charge)</li>
 * <li>Slot 2: Mold / physical extraction outlet (casting molds, receivers)</li>
 * </ul>
 */
public final class TileEntityStarterSmelter extends TileEntity implements IInventory {

    public static final int SLOT_FUEL = 0;
    public static final int SLOT_FEED = 1;
    public static final int SLOT_MOLD = 2;
    public static final int INVENTORY_SIZE = 3;

    private final ItemStack[] inventory = new ItemStack[INVENTORY_SIZE];
    private VesselState vesselState = new VesselState();
    private boolean structureValid = true;

    public TileEntityStarterSmelter() {}

    public VesselState vesselState() {
        return vesselState;
    }

    public boolean isStructureValid() {
        return structureValid;
    }

    public void validateStructure() {
        if (worldObj != null) {
            this.structureValid = worldObj.getBlock(xCoord, yCoord - 1, zCoord)
                .getMaterial()
                .isSolid();
        }
    }

    @Override
    public void updateEntity() {
        if (worldObj != null && worldObj.isRemote) {
            return;
        }

        validateStructure();

        // 1. Auto-feed fuel if burning expired and fuel is available in slot 0
        if (!vesselState.fuelState()
            .isBurning() && inventory[SLOT_FUEL] != null) {
            int burnTicks = TileEntityFurnace.getItemBurnTime(inventory[SLOT_FUEL]);
            if (burnTicks > 0) {
                vesselState.addFuel(burnTicks);
                ItemStack fuelStack = inventory[SLOT_FUEL];
                if (fuelStack.getItem() != null && fuelStack.getItem()
                    .hasContainerItem(fuelStack)) {
                    inventory[SLOT_FUEL] = fuelStack.getItem()
                        .getContainerItem(fuelStack);
                } else if (Items.lava_bucket != null && fuelStack.getItem() == Items.lava_bucket) {
                    inventory[SLOT_FUEL] = Items.bucket != null ? new ItemStack(Items.bucket) : null;
                } else {
                    decrStackSize(SLOT_FUEL, 1);
                }
                markDirty();
                if (worldObj != null) {
                    worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                }
            }
        }

        // 2. Auto-feed charge from slot 1 if capacity permits
        if (inventory[SLOT_FEED] != null && inventory[SLOT_FEED].stackSize > 0) {
            if (insertFeed(inventory[SLOT_FEED])) {
                if (inventory[SLOT_FEED].stackSize <= 0) {
                    inventory[SLOT_FEED] = null;
                }
                markDirty();
                if (worldObj != null) {
                    worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                }
            }
        }

        // 3. Active-only thermal stepping
        if (!vesselState.isDormant(293.15)) {
            vesselState.step(1, 293.15);
            markDirty();
        }

        // 4. Periodic network synchronization for tracking clients
        if (worldObj != null && worldObj.getTotalWorldTime() % 10L == 0L) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    public boolean insertFuel(ItemStack stack) {
        if (stack == null) return false;
        int burnTicks = TileEntityFurnace.getItemBurnTime(stack);
        if (burnTicks > 0) {
            vesselState.addFuel(burnTicks);
            stack.stackSize--;
            markDirty();
            if (worldObj != null) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
            return true;
        }
        return false;
    }

    public boolean insertFeed(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return false;

        if (stack.getItem() == NEItems.copperOreChunk
            || net.minecraft.block.Block.getBlockFromItem(stack.getItem()) == NEBlocks.copperOre) {
            vesselState.addOreChunk(OreFeedstockDefinition.copperOreChunk());
            stack.stackSize--;
            markDirty();
            if (worldObj != null) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
            return true;
        }

        if (stack.getItem() == NEItems.copperIngot) {
            MaterialQuantity mass = MaterialQuantity.ofKilograms(1.0);
            ActiveThermalState thermal = ActiveThermalState.ambient(
                1.0,
                OreFeedstockDefinition.copperOreChunk()
                    .metallicProperties());
            AggregateLayer layer = new AggregateLayer(
                naturalexceptions.content.material.BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(naturalexceptions.content.material.BuiltinMaterials.COPPER_COMPOSITION),
                naturalexceptions.content.material.BuiltinMaterials.COPPER_COMPOSITION,
                mass,
                thermal,
                AggregatePhaseType.SOLID_CHARGE);
            vesselState.remelt(layer);
            stack.stackSize--;
            markDirty();
            if (worldObj != null) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
            return true;
        }

        if (stack.getItem() instanceof ItemGenericComponent) {
            GeometryId geom = ItemGenericComponent.getGeometry(stack);
            double vol = ComponentGeometry.forId(geom)
                .volumeCm3();
            double massKg = (vol * 8.96) / 1000.0;
            MaterialQuantity mass = MaterialQuantity.ofKilograms(massKg);
            ActiveThermalState thermal = ActiveThermalState.ambient(
                massKg,
                OreFeedstockDefinition.copperOreChunk()
                    .metallicProperties());
            AggregateLayer layer = new AggregateLayer(
                naturalexceptions.content.material.BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(naturalexceptions.content.material.BuiltinMaterials.COPPER_COMPOSITION),
                naturalexceptions.content.material.BuiltinMaterials.COPPER_COMPOSITION,
                mass,
                thermal,
                AggregatePhaseType.SOLID_CHARGE);
            vesselState.remelt(layer);
            stack.stackSize--;
            markDirty();
            if (worldObj != null) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
            return true;
        }

        if (stack.getItem() instanceof ItemMaterialSample) {
            MaterialRef ref = ItemMaterialSample.getMaterialRef(stack);
            CanonicalMaterialKey key = ItemMaterialSample.getStateKey(stack);
            ActiveThermalState thermal = ItemMaterialSample
                .getThermalState(stack, MaterialProperties.of(8.96, 3.0, 100.0));
            if (ref != null) {
                MaterialQuantity mass = thermal != null ? MaterialQuantity.ofKilograms(thermal.massKg())
                    : MaterialQuantity.ofGrams(100.0);
                ActiveThermalState effectiveThermal = thermal != null ? thermal
                    : ActiveThermalState.ambient(mass.toKilograms(), MaterialProperties.of(8.96, 3.0, 100.0));
                AggregateLayer layer = new AggregateLayer(
                    ref,
                    key != null ? key
                        : CanonicalMaterialKey.of(
                            key != null ? key.composition()
                                : naturalexceptions.content.material.BuiltinMaterials.COPPER_COMPOSITION),
                    key != null ? key.composition()
                        : naturalexceptions.content.material.BuiltinMaterials.COPPER_COMPOSITION,
                    mass,
                    effectiveThermal,
                    AggregatePhaseType.SOLID_CHARGE);
                vesselState.remelt(layer);
                stack.stackSize--;
                markDirty();
                if (worldObj != null) {
                    worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                }
                return true;
            }
        }

        return false;
    }

    public boolean pourIntoMold(ItemStack moldStack) {
        if (moldStack == null || ItemCastingMold.isFilled(moldStack)) {
            return false;
        }
        GeometryId geom = ItemCastingMold.getGeometry(moldStack);
        if (geom == null) {
            geom = GeometryId.BLANK;
        }
        ComponentGeometry geomDef = ComponentGeometry.forId(geom);

        for (AggregateLayer l : vesselState.layers()) {
            if (l.phaseType() == AggregatePhaseType.MOLTEN_METAL && (l.physicalPhase() == MaterialPhase.LIQUID
                || l.thermalState()
                    .meltFraction() >= 0.50
                || l.temperatureKelvin() >= l.thermalState()
                    .properties()
                    .meltingPointKelvin())) {
                MaterialQuantity needed = geomDef.requiredMass(
                    l.thermalState()
                        .properties()
                        .density());
                AggregateLayer poured = vesselState.pourLiquidMetal(needed);
                if (poured != null) {
                    ItemCastingMold.fillMold(moldStack, poured);
                    markDirty();
                    if (worldObj != null) {
                        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public ItemStack skimSlag() {
        for (int i = vesselState.layers()
            .size() - 1; i >= 0; i--) {
            AggregateLayer l = vesselState.layers()
                .get(i);
            if (l.phaseType() == AggregatePhaseType.SLAG) {
                MaterialQuantity amount = l.mass()
                    .compareTo(MaterialQuantity.ofGrams(50.0)) <= 0 ? l.mass() : MaterialQuantity.ofGrams(50.0);
                AggregateLayer skimmed = vesselState.skimSlag(amount);
                if (skimmed != null) {
                    markDirty();
                    if (worldObj != null) {
                        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                    }
                    return new ItemStack(NEItems.slagLump != null ? NEItems.slagLump : NEItems.copperOreChunk);
                }
            }
        }
        return null;
    }

    public ItemStack extractLiquidMetal(MaterialQuantity amount) {
        AggregateLayer extracted = vesselState.pourLiquidMetal(amount);
        if (extracted != null) {
            markDirty();
            if (worldObj != null) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
            return ItemMaterialSample
                .createStack(extracted.materialRef(), extracted.stateKey(), extracted.thermalState());
        }
        return null;
    }

    public ItemStack extractSlag(MaterialQuantity amount) {
        AggregateLayer extracted = vesselState.skimSlag(amount);
        if (extracted != null) {
            markDirty();
            if (worldObj != null) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
            return ItemMaterialSample
                .createStack(extracted.materialRef(), extracted.stateKey(), extracted.thermalState());
        }
        return null;
    }

    // --- IInventory Implementation ---

    @Override
    public int getSizeInventory() {
        return INVENTORY_SIZE;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot >= 0 && slot < INVENTORY_SIZE) {
            return inventory[slot];
        }
        return null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        if (slot >= 0 && slot < INVENTORY_SIZE && inventory[slot] != null) {
            if (inventory[slot].stackSize <= count) {
                ItemStack itemstack = inventory[slot];
                inventory[slot] = null;
                markDirty();
                return itemstack;
            } else {
                ItemStack itemstack = inventory[slot].splitStack(count);
                if (inventory[slot].stackSize == 0) {
                    inventory[slot] = null;
                }
                markDirty();
                return itemstack;
            }
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (slot >= 0 && slot < INVENTORY_SIZE && inventory[slot] != null) {
            ItemStack itemstack = inventory[slot];
            inventory[slot] = null;
            return itemstack;
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot >= 0 && slot < INVENTORY_SIZE) {
            inventory[slot] = stack;
            if (stack != null && stack.stackSize > getInventoryStackLimit()) {
                stack.stackSize = getInventoryStackLimit();
            }
            markDirty();
        }
    }

    @Override
    public String getInventoryName() {
        return "container.starterSmelter";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        if (worldObj == null || worldObj.getTileEntity(xCoord, yCoord, zCoord) != this) {
            return false;
        }
        return player.getDistanceSq((double) xCoord + 0.5D, (double) yCoord + 0.5D, (double) zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack == null) return false;
        if (slot == SLOT_FUEL) {
            return TileEntityFurnace.getItemBurnTime(stack) > 0;
        }
        if (slot == SLOT_FEED) {
            return isSmeltableFeed(stack);
        }
        if (slot == SLOT_MOLD) {
            return stack.getItem() instanceof ItemCastingMold;
        }
        return true;
    }

    public static boolean isSmeltableFeed(ItemStack stack) {
        if (stack == null) return false;
        if (stack.getItem() == NEItems.copperOreChunk) return true;
        if (stack.getItem() == NEItems.copperIngot) return true;
        if (stack.getItem() instanceof ItemGenericComponent) return true;
        if (stack.getItem() instanceof ItemMaterialSample) return true;
        if (net.minecraft.block.Block.getBlockFromItem(stack.getItem()) == NEBlocks.copperOre) return true;
        if (net.minecraft.block.Block.getBlockFromItem(stack.getItem()) == NEBlocks.copperBlock) return true;
        return false;
    }

    // --- Persistence & Networking ---

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        VesselStateNbtCodec.writeToNbt(vesselState, compound);
        compound.setBoolean("StructureValid", structureValid);

        NBTTagList itemsList = new NBTTagList();
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] != null) {
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setByte("Slot", (byte) i);
                inventory[i].writeToNBT(itemTag);
                itemsList.appendTag(itemTag);
            }
        }
        compound.setTag("Items", itemsList);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.vesselState = VesselStateNbtCodec.readFromNbt(compound, new MaterialPropertyResolver());
        if (compound.hasKey("StructureValid")) {
            this.structureValid = compound.getBoolean("StructureValid");
        }

        NBTTagList itemsList = compound.getTagList("Items", 10);
        for (int i = 0; i < inventory.length; i++) {
            inventory[i] = null;
        }
        for (int i = 0; i < itemsList.tagCount(); i++) {
            NBTTagCompound itemTag = itemsList.getCompoundTagAt(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < inventory.length) {
                inventory[slot] = ItemStack.loadItemStackFromNBT(itemTag);
            }
        }
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound compound = new NBTTagCompound();
        writeToNBT(compound);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, compound);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
    }
}
