package naturalexceptions.minecraft.metallurgy;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;

import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.metallurgy.OreFeedstockDefinition;
import naturalexceptions.core.metallurgy.VesselState;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemMaterialSample;
import naturalexceptions.minecraft.persistence.VesselStateNbtCodec;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * TileEntity hosting the physical Starter Smelter vessel.
 *
 * <p>
 * Implements bounded, active-only simulation: cold and equilibrated vessels sleep.
 */
public final class TileEntityStarterSmelter extends TileEntity {

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
            // Minimal structure validation: must sit upon a solid block (e.g. stone or refractory hearth)
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
        if (!structureValid) {
            return;
        }

        // Active-only execution: dormant smelters sleep
        if (!vesselState.isDormant(293.15)) {
            vesselState.step(1, 293.15);
            markDirty();
        }
    }

    public boolean insertFuel(ItemStack stack) {
        if (stack == null) return false;
        int burnTicks = TileEntityFurnace.getItemBurnTime(stack);
        if (burnTicks > 0) {
            vesselState.addFuel(burnTicks);
            stack.stackSize--;
            markDirty();
            return true;
        }
        return false;
    }

    public boolean insertFeed(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return false;

        if (stack.getItem() == NEItems.copperOreChunk) {
            vesselState.addOreChunk(OreFeedstockDefinition.copperOreChunk());
            stack.stackSize--;
            markDirty();
            return true;
        }

        if (stack.getItem() instanceof ItemMaterialSample) {
            MaterialRef ref = ItemMaterialSample.getMaterialRef(stack);
            CanonicalMaterialKey key = ItemMaterialSample.getStateKey(stack);
            ActiveThermalState thermal = ItemMaterialSample
                .getThermalState(stack, naturalexceptions.core.property.MaterialProperties.of(8.96, 3.0, 100.0));
            if (ref != null) {
                MaterialQuantity mass = thermal != null ? MaterialQuantity.ofKilograms(thermal.massKg())
                    : MaterialQuantity.ofGrams(100.0);
                ActiveThermalState effectiveThermal = thermal != null ? thermal
                    : ActiveThermalState.ambient(
                        mass.toKilograms(),
                        naturalexceptions.core.property.MaterialProperties.of(8.96, 3.0, 100.0));
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
                    naturalexceptions.core.metallurgy.AggregatePhaseType.SOLID_CHARGE);
                vesselState.remelt(layer);
                stack.stackSize--;
                markDirty();
                return true;
            }
        }

        return false;
    }

    public ItemStack extractLiquidMetal(MaterialQuantity amount) {
        AggregateLayer extracted = vesselState.pourLiquidMetal(amount);
        if (extracted != null) {
            markDirty();
            return ItemMaterialSample
                .createStack(extracted.materialRef(), extracted.stateKey(), extracted.thermalState());
        }
        return null;
    }

    public ItemStack extractSlag(MaterialQuantity amount) {
        AggregateLayer extracted = vesselState.skimSlag(amount);
        if (extracted != null) {
            markDirty();
            return ItemMaterialSample
                .createStack(extracted.materialRef(), extracted.stateKey(), extracted.thermalState());
        }
        return null;
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        VesselStateNbtCodec.writeToNbt(vesselState, compound);
        compound.setBoolean("StructureValid", structureValid);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.vesselState = VesselStateNbtCodec.readFromNbt(compound, new MaterialPropertyResolver());
        if (compound.hasKey("StructureValid")) {
            this.structureValid = compound.getBoolean("StructureValid");
        }
    }
}
