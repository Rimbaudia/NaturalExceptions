package naturalexceptions.minecraft.metallurgy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentGeometry;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.core.metallurgy.ExhaustSink;
import naturalexceptions.core.metallurgy.FuelBurnState;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.metallurgy.VesselLining;
import naturalexceptions.core.metallurgy.VesselState;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.item.ItemFluidVessel;
import naturalexceptions.minecraft.item.ItemFoundryLadle;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.item.ItemMaterialSample;
import naturalexceptions.minecraft.persistence.VesselStateNbtCodec;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * TileEntity hosting the physical Starter Smelter vessel.
 *
 * <p>
 * Simulates real multicomponent molten metal and slag layers, conservation of mass and enthalpy,
 * fractional pouring, environmental heat loss, fuel combustion kinetics, and tuyere forced air draft.
 * Implements bounded, active-only simulation: cold and equilibrated vessels sleep.
 * Implements ISidedInventory to support automated container slots:
 * <ul>
 * <li>Slot 0: Fuel buffer (charcoal, coal, wood)</li>
 * <li>Slot 1: Feed input (ore chunks, solid scrap, raw charge)</li>
 * <li>Slot 2: Mold / physical extraction outlet (casting molds, receivers, ladles)</li>
 * </ul>
 */
public final class TileEntityStarterSmelter extends TileEntity implements ISidedInventory {

    public static final int SLOT_FUEL = 0;
    public static final int SLOT_FEED = 1;
    public static final int SLOT_MOLD = 2;
    public static final int SLOT_OUTPUT = SLOT_MOLD;
    public static final int INVENTORY_SIZE = 3;
    /** One ladle scoop of slag. */
    public static final MaterialQuantity SLAG_SKIM_AMOUNT = MaterialQuantity.ofGrams(250.0);
    /** Emergency mass-safety ceiling only. Player capacity is geometry-derived in litres. */
    public static final MaterialQuantity CAPACITY = MaterialQuantity.ofKilograms(5000.0);

    private final ItemStack[] inventory = new ItemStack[INVENTORY_SIZE];
    private VesselState vesselState = new VesselState(
        CAPACITY,
        new VesselLining(),
        new FuelBurnState(),
        new ExhaustSink(),
        new MaterialPropertyResolver());
    private boolean structureValid = true;
    private int forcedDraftTicks = 0;
    private ItemStack hearthWorkpiece;
    private naturalexceptions.minecraft.multiblock.SmelterVesselGeometry vesselGeometry;
    private boolean overCapacity;

    public TileEntityStarterSmelter() {}

    public ItemStack getHearthWorkpiece() {
        return hearthWorkpiece;
    }

    public void setHearthWorkpiece(ItemStack stack) {
        this.hearthWorkpiece = stack;
        markDirty();
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    public double getHearthTemperature() {
        double temp = 293.15;
        if (vesselState != null && vesselState.fuelState()
            .isBurning()) {
            double flameTemp = forcedDraftTicks > 0 ? 1800.0 : 1300.0;
            if (flameTemp > temp) {
                temp = flameTemp;
            }
        }
        if (vesselState != null) {
            for (AggregateLayer layer : vesselState.layers()) {
                if (layer.temperatureKelvin() > temp) {
                    temp = layer.temperatureKelvin();
                }
            }
        }
        return temp;
    }

    /** The state last sent to watching players by the periodic sync. */
    private NBTTagCompound lastSynced;

    public VesselState vesselState() {
        return vesselState;
    }

    private naturalexceptions.minecraft.multiblock.MultiblockValidationResult structureValidationResult = naturalexceptions.minecraft.multiblock.MultiblockValidationResult
        .ok();

    public boolean isStructureValid() {
        return structureValid;
    }

    public naturalexceptions.minecraft.multiblock.MultiblockValidationResult getStructureValidationResult() {
        return structureValidationResult;
    }

    public int getFacing() {
        if (worldObj == null) return 2;
        int meta = getBlockMetadata();
        return meta >= 2 && meta <= 5 ? meta : 2;
    }

    public void validateStructure() {
        if (worldObj != null) {
            vesselGeometry = naturalexceptions.minecraft.multiblock.StarterSmelterDefinition.INSTANCE
                .discoverVessel(worldObj, xCoord, yCoord, zCoord, getFacing());
            overCapacity = vesselGeometry != null && occupiedVolumeLitres() > vesselGeometry.capacityLitres();
            this.structureValidationResult = naturalexceptions.minecraft.multiblock.StarterSmelterDefinition.INSTANCE
                .validate(worldObj, xCoord, yCoord, zCoord, getFacing());
            if (overCapacity)
                this.structureValidationResult = naturalexceptions.minecraft.multiblock.MultiblockValidationResult.fail(
                    xCoord,
                    yCoord,
                    zCoord,
                    null,
                    "Vessel contents exceed the repaired cavity capacity; remove material before restarting.");
            this.structureValid = structureValidationResult.isValid();
        }
    }

    public void invalidateVesselGeometry() {
        vesselGeometry = null;
    }

    public naturalexceptions.minecraft.multiblock.SmelterVesselGeometry vesselGeometry() {
        if (vesselGeometry == null && worldObj != null) {
            vesselGeometry = naturalexceptions.minecraft.multiblock.StarterSmelterDefinition.INSTANCE
                .discoverVessel(worldObj, xCoord, yCoord, zCoord, getFacing());
        }
        return vesselGeometry;
    }

    /**
     * The vessel's contents are drawn by this controller across the whole cavity behind it. Without a box covering
     * the cavity, the contents vanish whenever the controller block itself is out of view (looking down the shaft).
     */
    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public net.minecraft.util.AxisAlignedBB getRenderBoundingBox() {
        naturalexceptions.minecraft.multiblock.SmelterVesselGeometry geometry = vesselGeometry();
        if (geometry == null) return super.getRenderBoundingBox();
        int[] box = renderBounds(xCoord, yCoord, zCoord, geometry.cavityCells());
        return net.minecraft.util.AxisAlignedBB.getBoundingBox(box[0], box[1], box[2], box[3], box[4], box[5]);
    }

    /**
     * The cavity's box relative to the controller at (x, y, z): {minX, minY, minZ, maxX, maxY, maxZ}, where max is
     * one past the last cell. The cavity is always an axis-aligned block of cells, whatever way the smelter faces.
     */
    public static double[] cavityBoxRelative(int x, int y, int z, java.util.List<int[]> worldCells) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (int[] w : worldCells) {
            minX = Math.min(minX, w[0]);
            minY = Math.min(minY, w[1]);
            minZ = Math.min(minZ, w[2]);
            maxX = Math.max(maxX, w[0] + 1);
            maxY = Math.max(maxY, w[1] + 1);
            maxZ = Math.max(maxZ, w[2] + 1);
        }
        if (minX == Integer.MAX_VALUE) return new double[] { 0, 0, 0, 0, 0, 0 };
        return new double[] { minX - x, minY - y, minZ - z, maxX - x, maxY - y, maxZ - z };
    }

    /** {minX, minY, minZ, maxX, maxY, maxZ} covering the controller and every cavity cell, one block of margin. */
    static int[] renderBounds(int x, int y, int z, java.util.List<int[]> worldCells) {
        int[] box = { x, y, z, x + 1, y + 1, z + 1 };
        for (int[] w : worldCells) {
            box[0] = Math.min(box[0], w[0]);
            box[1] = Math.min(box[1], w[1]);
            box[2] = Math.min(box[2], w[2]);
            box[3] = Math.max(box[3], w[0] + 1);
            box[4] = Math.max(box[4], w[1] + 1);
            box[5] = Math.max(box[5], w[2] + 1);
        }
        box[0]--;
        box[1]--;
        box[2]--;
        box[3]++;
        box[4]++;
        box[5]++;
        return box;
    }

    public double vesselCapacityLitres() {
        naturalexceptions.minecraft.multiblock.SmelterVesselGeometry geometry = vesselGeometry();
        return geometry == null ? 0.0 : geometry.capacityLitres();
    }

    public double occupiedVolumeLitres() {
        return vesselState.totalOccupiedVolumeLitres();
    }

    public double fillFraction() {
        double capacity = vesselCapacityLitres();
        return capacity <= 0.0 ? 0.0 : Math.min(1.0, occupiedVolumeLitres() / capacity);
    }

    public boolean isOverCapacity() {
        return overCapacity;
    }

    /** State of the canonical smelter, outlet, casting-table path, for the GUI and for pour feedback. */
    public SmelterOutletState outletState() {
        if (!structureValid) return SmelterOutletState.STRUCTURE_INCOMPLETE;
        TileEntitySmelterOutlet outlet = SmelterPortLookup.findOutlet(worldObj, vesselGeometry());
        if (outlet == null) return SmelterOutletState.NO_OUTLET;
        if (outlet.isPouring()) return SmelterOutletState.POURING;
        return outlet.stateFor(this);
    }

    /** True when the lowest reachable metal layer can flow; a deeper layer cannot be tapped past it. */
    public boolean hasPourableMetal() {
        for (AggregateLayer layer : vesselState.layers()) {
            if (layer.phaseType() != AggregatePhaseType.MOLTEN_METAL) continue;
            return layer.isPourable();
        }
        return false;
    }

    private boolean hasGeometricRoom(MaterialQuantity addedMass, MaterialProperties properties) {
        double density = properties != null ? properties.density() : 5.0;
        if (!Double.isFinite(density) || density <= 0.0) density = 5.0;
        return hasGeometricRoomLitres(addedMass.toKilograms() / density);
    }

    private boolean hasGeometricRoomLitres(double addedLitres) {
        naturalexceptions.minecraft.multiblock.SmelterVesselGeometry geometry = vesselGeometry();
        if (geometry == null) return true; // Domain-only callers have no world geometry to validate.
        return !overCapacity
            && occupiedVolumeLitres() + Math.max(0.0, addedLitres) <= geometry.capacityLitres() + 1.0e-8;
    }

    public void boostForcedDraft(int ticks) {
        if (ticks <= 0) return;
        this.forcedDraftTicks = Math.max(this.forcedDraftTicks, Math.min(100, ticks));
        markDirty();
    }

    public int getForcedDraftTicks() {
        return forcedDraftTicks;
    }

    public int countAttachedTuyeres() {
        if (worldObj == null) return 0;
        return SmelterPortLookup.countAttachedTuyeres(worldObj, vesselGeometry());
    }

    @Override
    public void updateEntity() {
        if (worldObj != null && worldObj.isRemote) {
            return;
        }

        // Older saves could hold an item in the retired mold slot; hand it back rather than hide it.
        if (inventory[SLOT_MOLD] != null && worldObj != null) {
            worldObj.spawnEntityInWorld(
                new net.minecraft.entity.item.EntityItem(
                    worldObj,
                    xCoord + 0.5D,
                    yCoord + 1.2D,
                    zCoord + 0.5D,
                    inventory[SLOT_MOLD]));
            inventory[SLOT_MOLD] = null;
            markDirty();
        }

        if (vesselGeometry == null || worldObj == null || worldObj.getTotalWorldTime() % 10L == 0L) validateStructure();

        // A damaged/downsized shell pauses processing. Its contents remain authoritative and are never discarded.
        if (worldObj != null && !structureValid) {
            if (forcedDraftTicks > 0) forcedDraftTicks--;
            return;
        }

        // 1. Auto-feed fuel from slot 0 if fire is low/empty (buffers up to ~3200 ticks of fire)
        if (inventory[SLOT_FUEL] != null && vesselState.fuelState()
            .remainingTicks() <= 1600) {
            int burnTicks = getFuelBurnTime(inventory[SLOT_FUEL]);
            if (burnTicks > 0) {
                double flameTemp = getFuelFlameTemp(inventory[SLOT_FUEL]);
                String heatLabel = getFuelHeatLabel(inventory[SLOT_FUEL]);
                vesselState.addFuel(burnTicks, flameTemp, heatLabel);
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

        // 3. Continuous physical heating, annealing, and meltdown of hearth workpiece
        if (hearthWorkpiece != null) {
            double hearthTemp = getHearthTemperature();
            ActiveThermalState thermal = ItemGenericComponent.getThermalState(hearthWorkpiece);
            MaterialProperties props = thermal != null ? thermal.properties() : null;
            if (props == null) {
                ComponentInstance comp = ItemGenericComponent.getComponent(hearthWorkpiece);
                if (comp != null) {
                    props = new MaterialPropertyResolver().resolve(
                        comp.materialRef(),
                        comp.stateKey()
                            .composition(),
                        comp.stateKey()
                            .phase())
                        .properties();
                }
            }
            if (props == null) {
                props = BuiltinMaterials.COPPER_PROPERTIES;
            }
            double curTemp = thermal != null ? thermal.temperatureKelvin() : 293.15;
            double massKg = thermal != null ? thermal.massKg() : 0.5;

            if (curTemp < hearthTemp) {
                double dt = 0.05; // 1 tick = 0.05 s
                double h = 250.0; // Convective/radiative heat transfer coefficient in flame hearth
                double area = 0.02; // Representative workpiece surface area (~200 cm²)
                double q = h * area * (hearthTemp - curTemp) * dt;

                double curEnergy = thermal != null ? thermal.energyJoules()
                    : naturalexceptions.core.thermal.ThermodynamicModel
                        .energyForTemperature(curTemp, 0.0, massKg, props);
                double newEnergy = curEnergy + q;
                ActiveThermalState newThermal = new ActiveThermalState(newEnergy, massKg, props);
                ItemGenericComponent.saveThermalState(hearthWorkpiece, newThermal);

                // Continuous physical annealing when reaching recrystallization temperature
                ItemGenericComponent.checkAnneal(hearthWorkpiece);

                // Meltdown into vessel molten layer if heated past melting point and fully melted
                if (newThermal.phase() == MaterialPhase.LIQUID && newThermal.meltFraction() >= 1.0) {
                    ComponentInstance comp = ItemGenericComponent.getComponent(hearthWorkpiece);
                    MaterialRef ref = comp != null ? comp.materialRef() : BuiltinMaterials.COPPER.ref();
                    CanonicalMaterialKey key = comp != null ? comp.stateKey()
                        : CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION);
                    AggregateLayer molten = new AggregateLayer(
                        ref,
                        key,
                        key.composition(),
                        MaterialQuantity.ofKilograms(massKg),
                        newThermal,
                        AggregatePhaseType.MOLTEN_METAL);
                    vesselState.remelt(molten);
                    hearthWorkpiece = null;
                    if (worldObj != null) {
                        worldObj
                            .playSoundEffect(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D, "liquid.lavapop", 1.0F, 1.0F);
                    }
                }
                markDirty();
            } else if (curTemp > hearthTemp) {
                double dt = 0.05;
                double h = 25.0;
                double area = 0.02;
                double q = h * area * (curTemp - hearthTemp) * dt;
                double curEnergy = thermal != null ? thermal.energyJoules()
                    : naturalexceptions.core.thermal.ThermodynamicModel
                        .energyForTemperature(curTemp, 0.0, massKg, props);
                double ambientEnergy = naturalexceptions.core.thermal.ThermodynamicModel
                    .energyForTemperature(hearthTemp, 0.0, massKg, props);
                double newEnergy = Math.max(ambientEnergy, curEnergy - q);
                ActiveThermalState newThermal = new ActiveThermalState(newEnergy, massKg, props);
                ItemGenericComponent.saveThermalState(hearthWorkpiece, newThermal);
                markDirty();
            }
        }

        // 4. Only actively pumped, connected nozzles supply forced air. Draft expires even without fuel.
        if (worldObj != null && forcedDraftTicks > 0 && countAttachedTuyeres() == 0) forcedDraftTicks = 0;
        if (!vesselState.isDormant(293.15)) {
            double airMultiplier = forcedDraftTicks > 0 ? 1.60 : 1.0;
            vesselState.step(1, 293.15, airMultiplier);
            markDirty();
        }
        if (forcedDraftTicks > 0) {
            forcedDraftTicks--;
            markDirty();
        }

        // 5. Periodic sync for the players who can see it, only when its state has moved (a cold smelter sends
        // nothing), and without resending the block (its melt and workpiece are drawn by its special renderer).
        if (worldObj != null && worldObj.getTotalWorldTime() % 10L == 0L)
            lastSynced = naturalexceptions.minecraft.network.TileSync.toWatchersIfChanged(this, lastSynced);
    }

    public boolean insertFuel(ItemStack stack) {
        if (stack == null) return false;
        int burnTicks = getFuelBurnTime(stack);
        if (burnTicks > 0) {
            double flameTemp = getFuelFlameTemp(stack);
            String heatLabel = getFuelHeatLabel(stack);
            vesselState.addFuel(burnTicks, flameTemp, heatLabel);
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
        // Lumenite is a flux, not a charge: it readies the next few ore melts to give up more metal.
        if (stack.getItem() instanceof naturalexceptions.minecraft.item.ItemLumeniteShard) {
            if (vesselState.fluxCharges() + VesselState.FLUX_PER_SHARD > VesselState.MAX_FLUX_CHARGES) return false;
            vesselState.addFlux(VesselState.FLUX_PER_SHARD);
            stack.stackSize--;
            markDirty();
            if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            return true;
        }
        if (!naturalexceptions.core.material.VanillaResourceClassification.isMeltable(stack)
            && naturalexceptions.core.material.VanillaResourceClassification.getRejectionReason(stack) != null) {
            return false;
        }

        FeedstockItemRegistry.ResolvedInput registeredInput = FeedstockItemRegistry.resolve(stack);
        if (registeredInput != null) {
            MaterialQuantity needed = registeredInput.inputMass();
            if (!hasGeometricRoom(needed, registeredInput.properties())) return false;
            if (vesselState.totalMass()
                .add(needed)
                .compareTo(vesselState.capacity()) > 0) {
                return false;
            }
            if (registeredInput.kind() == FeedstockItemRegistry.InputKind.RAW_ORE) {
                vesselState.addRawOre(registeredInput.definition());
            } else {
                remeltIngot(registeredInput);
            }
            stack.stackSize--;
            markDirty();
            if (worldObj != null) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
            return true;
        }

        // 1. NE Generic Components (Pick head, blade, plate, rod, blank)
        if (stack.getItem() instanceof ItemGenericComponent) {
            ComponentInstance comp = ItemGenericComponent.getComponent(stack);
            if (comp != null) {
                if (!hasGeometricRoom(comp.mass(), ItemGenericComponent.resolveProperties(comp))) return false;
                if (vesselState.totalMass()
                    .add(comp.mass())
                    .compareTo(vesselState.capacity()) > 0) {
                    return false;
                }
                if (remeltComponent(comp)) {
                    stack.stackSize--;
                    markDirty();
                    if (worldObj != null) {
                        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                    }
                    return true;
                }
            }
        }

        // 2. Tool Assembly (Head + Handle remelt)
        if (stack.getItem() instanceof ItemGenericAssembly) {
            AssemblyGraph assembly = ItemGenericAssembly.getAssembly(stack);
            if (assembly != null) {
                MaterialQuantity totalAssemblyMass = MaterialQuantity.ZERO;
                if (assembly.head() != null) totalAssemblyMass = totalAssemblyMass.add(
                    assembly.head()
                        .mass());
                if (assembly.handle() != null && !BuiltinMaterials.WOOD.ref()
                    .equals(
                        assembly.handle()
                            .materialRef())) {
                    totalAssemblyMass = totalAssemblyMass.add(
                        assembly.handle()
                            .mass());
                }
                double assemblyLitres = 0.0;
                if (assembly.head() != null) assemblyLitres += volumeLitres(
                    assembly.head()
                        .mass(),
                    ItemGenericComponent.resolveProperties(assembly.head()));
                if (assembly.handle() != null && !BuiltinMaterials.WOOD.ref()
                    .equals(
                        assembly.handle()
                            .materialRef()))
                    assemblyLitres += volumeLitres(
                        assembly.handle()
                            .mass(),
                        ItemGenericComponent.resolveProperties(assembly.handle()));
                if (!hasGeometricRoomLitres(assemblyLitres)) return false;
                if (vesselState.totalMass()
                    .add(totalAssemblyMass)
                    .compareTo(vesselState.capacity()) > 0) {
                    return false;
                }
                ComponentInstance head = assembly.head();
                if (head != null) {
                    remeltComponent(head);
                }
                ComponentInstance handle = assembly.handle();
                if (handle != null && !BuiltinMaterials.WOOD.ref()
                    .equals(handle.materialRef())) {
                    remeltComponent(handle);
                }
                stack.stackSize--;
                markDirty();
                if (worldObj != null) {
                    worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                }
                return true;
            }
        }

        // 3. Filled Casting Mold (Remelt contained metal)
        if (stack.getItem() instanceof ItemCastingMold) {
            if (ItemCastingMold.hasMetal(stack)) {
                MaterialQuantity mass = ItemCastingMold.getFilledMass(stack);
                if (!hasGeometricRoom(mass, propertiesFor(stack))) return false;
                if (vesselState.totalMass()
                    .add(mass)
                    .compareTo(vesselState.capacity()) > 0) {
                    return false;
                }
                MaterialRef ref = ItemCastingMold.getContainedMaterialRef(stack);
                CanonicalMaterialKey key = ItemCastingMold.getContainedStateKey(stack);
                CanonicalMaterialKey effectiveKey = canonicalKeyFor(ref, key);
                if (ref == null || effectiveKey == null) {
                    ItemCastingMold.clearContainedPayload(stack);
                    markDirty();
                    if (worldObj != null) {
                        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                    }
                    return true;
                }
                ActiveThermalState thermal = ItemCastingMold.getThermalState(stack);
                if (thermal == null) {
                    thermal = ActiveThermalState
                        .ambient(mass.toKilograms(), resolveMaterialProperties(ref, effectiveKey));
                }
                AggregateLayer layer = new AggregateLayer(
                    ref,
                    effectiveKey,
                    effectiveKey.composition(),
                    mass,
                    thermal,
                    thermal.phase() == MaterialPhase.LIQUID ? AggregatePhaseType.MOLTEN_METAL
                        : AggregatePhaseType.SOLID_CHARGE);
                vesselState.remelt(layer);
                if (stack.hasTagCompound()) {
                    ItemCastingMold.clearContainedPayload(stack);
                }
                markDirty();
                if (worldObj != null) {
                    worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                }
                return true;
            }
        }

        // 3b. Canonical molten matter from the P9 vessel uses the same aggregate/remelting path.
        if (stack.getItem() instanceof ItemFluidVessel) {
            naturalexceptions.core.fluid.FluidParcel parcel = ItemFluidVessel.contents(stack);
            if (parcel == null) return false;
            MaterialQuantity mass = parcel.mass();
            if (!hasGeometricRoom(
                mass,
                parcel.thermalState()
                    .properties())
                || vesselState.totalMass()
                    .add(mass)
                    .compareTo(vesselState.capacity()) > 0)
                return false;
            AggregateLayer layer = parcel.toAggregateLayer();
            vesselState.remelt(layer);
            // The reusable vessel is emptied, never consumed with its contents.
            ItemFluidVessel.setContents(stack, null);
            markDirty();
            if (worldObj != null) worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            return true;
        }

        // 4. Material Samples
        if (stack.getItem() instanceof ItemMaterialSample) {
            MaterialRef ref = ItemMaterialSample.getMaterialRef(stack);
            CanonicalMaterialKey key = ItemMaterialSample.getStateKey(stack);
            CanonicalMaterialKey effectiveKey = canonicalKeyFor(ref, key);
            if (ref == null || effectiveKey == null) {
                return false;
            }
            MaterialProperties props = resolveMaterialProperties(ref, effectiveKey);
            ActiveThermalState thermal = ItemMaterialSample.getThermalState(stack, props);
            MaterialQuantity mass = ItemMaterialSample.getMass(stack);
            if (!hasGeometricRoom(mass, props)) return false;
            if (vesselState.totalMass()
                .add(mass)
                .compareTo(vesselState.capacity()) > 0) {
                return false;
            }
            ActiveThermalState effectiveThermal = thermal != null ? thermal
                : ActiveThermalState.ambient(mass.toKilograms(), props);
            AggregateLayer layer = new AggregateLayer(
                ref,
                effectiveKey,
                effectiveKey.composition(),
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

        // 5. Foundry Ladle (Remelt contained metal)
        if (stack.getItem() instanceof ItemFoundryLadle) {
            if (ItemFoundryLadle.hasMetal(stack)) {
                MaterialQuantity mass = ItemFoundryLadle.getFilledMass(stack);
                if (!hasGeometricRoom(mass, propertiesFor(stack))) return false;
                if (vesselState.totalMass()
                    .add(mass)
                    .compareTo(vesselState.capacity()) > 0) {
                    return false;
                }
                AggregateLayer drained = ItemFoundryLadle.drainMetal(stack, mass);
                if (drained != null) {
                    vesselState.remelt(drained);
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

    private static double volumeLitres(MaterialQuantity mass, MaterialProperties properties) {
        double density = properties != null ? properties.density() : 5.0;
        return mass.toKilograms() / (Double.isFinite(density) && density > 0 ? density : 5.0);
    }

    private MaterialProperties propertiesFor(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof ItemCastingMold) {
            ActiveThermalState thermal = ItemCastingMold.getThermalState(stack);
            if (thermal != null) return thermal.properties();
            MaterialRef ref = ItemCastingMold.getContainedMaterialRef(stack);
            CanonicalMaterialKey key = ItemCastingMold.getContainedStateKey(stack);
            if (ref != null && key != null)
                return new MaterialPropertyResolver().resolve(ref, key.composition(), key.phase())
                    .properties();
        }
        if (stack != null && stack.getItem() instanceof ItemFoundryLadle) {
            ActiveThermalState thermal = ItemFoundryLadle.getThermalState(stack);
            if (thermal != null) return thermal.properties();
        }
        return BuiltinMaterials.COPPER_PROPERTIES;
    }

    private void remeltIngot(FeedstockItemRegistry.ResolvedInput input) {
        MaterialQuantity mass = input.inputMass();
        ActiveThermalState thermal = ActiveThermalState.ambient(mass.toKilograms(), input.properties());
        AggregateLayer layer = new AggregateLayer(
            input.materialRef(),
            CanonicalMaterialKey.of(input.composition()),
            input.composition(),
            mass,
            thermal,
            AggregatePhaseType.SOLID_CHARGE);
        vesselState.remelt(layer);
    }

    private boolean remeltComponent(ComponentInstance comp) {
        MaterialRef ref = comp.materialRef();
        CanonicalMaterialKey key = comp.stateKey();
        CanonicalMaterialKey effectiveKey = canonicalKeyFor(ref, key);
        if (ref == null || effectiveKey == null) {
            return false;
        }
        MaterialQuantity mass = comp.mass();
        MaterialProperties props = resolveMaterialProperties(ref, effectiveKey);
        ActiveThermalState thermal = ActiveThermalState.ambient(mass.toKilograms(), props);
        AggregateLayer layer = new AggregateLayer(
            ref,
            effectiveKey,
            effectiveKey.composition(),
            mass,
            thermal,
            AggregatePhaseType.SOLID_CHARGE);
        vesselState.remelt(layer);
        return true;
    }

    private static CanonicalMaterialKey canonicalKeyFor(MaterialRef ref, CanonicalMaterialKey key) {
        if (key != null) {
            return key;
        }
        if (ref != null && ref.isAuthored()) {
            return CanonicalMaterialKey.of(CanonicalComposition.of(ref.authoredId()));
        }
        return null;
    }

    private static MaterialProperties resolveMaterialProperties(MaterialRef ref, CanonicalMaterialKey key) {
        MaterialRef effectiveRef = ref != null ? ref : MaterialRef.generatedFrom(key);
        return new MaterialPropertyResolver().resolve(effectiveRef, key.composition(), key.phase())
            .properties();
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
        MaterialRef existingRef = ItemCastingMold.getContainedMaterialRef(moldStack);

        for (AggregateLayer l : vesselState.layers()) {
            if (l.isPourable()) {
                if (existingRef != null && !existingRef.equals(l.materialRef())) {
                    continue; // Skip incompatible metal for partially filled mold
                }
                MaterialQuantity required = geomDef.requiredMass(
                    l.thermalState()
                        .properties()
                        .density());
                MaterialQuantity current = ItemCastingMold.getFilledMass(moldStack);
                MaterialQuantity needed = required.compareTo(current) > 0 ? required.subtract(current)
                    : MaterialQuantity.ZERO;
                if (needed.isZero()) {
                    return false;
                }
                MaterialQuantity transferAmount = l.mass()
                    .compareTo(needed) <= 0 ? l.mass() : needed;
                AggregateLayer poured = vesselState.pourLiquidMetal(l.materialRef(), transferAmount);
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

    public boolean pourIntoLadle(ItemStack ladleStack) {
        if (ladleStack == null || !(ladleStack.getItem() instanceof ItemFoundryLadle)) {
            return false;
        }
        MaterialQuantity current = ItemFoundryLadle.getFilledMass(ladleStack);
        if (current.compareTo(ItemFoundryLadle.CAPACITY) >= 0) {
            return false;
        }
        MaterialQuantity room = ItemFoundryLadle.CAPACITY.subtract(current);
        MaterialRef existingRef = ItemFoundryLadle.getContainedMaterialRef(ladleStack);

        for (AggregateLayer l : vesselState.layers()) {
            if (l.isPourable()) {
                if (existingRef != null && !existingRef.equals(l.materialRef())) {
                    continue; // Skip incompatible metal for partially filled ladle
                }
                MaterialQuantity transferAmount = l.mass()
                    .compareTo(room) <= 0 ? l.mass() : room;
                AggregateLayer poured = vesselState.pourLiquidMetal(l.materialRef(), transferAmount);
                if (poured != null) {
                    ItemFoundryLadle.fillLadle(ladleStack, poured);
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
                    return new ItemStack(
                        NEItems.slagLump != null ? NEItems.slagLump : net.minecraft.init.Items.clay_ball);
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
                .createStack(extracted.materialRef(), extracted.stateKey(), extracted.mass(), extracted.thermalState());
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
                .createStack(extracted.materialRef(), extracted.stateKey(), extracted.mass(), extracted.thermalState());
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

    private static final int[] SLOTS_TOP = new int[] { SLOT_FEED };
    private static final int[] SLOTS_BOTTOM = new int[] { SLOT_MOLD, SLOT_FUEL };
    private static final int[] SLOTS_SIDES = new int[] { SLOT_FUEL, SLOT_FEED, SLOT_MOLD };

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        if (side == 0) return SLOTS_BOTTOM;
        if (side == 1) return SLOTS_TOP;
        return SLOTS_SIDES;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        if (slot == SLOT_MOLD)
            return stack != null && stack.getItem() == naturalexceptions.minecraft.registry.NEItems.slagLump;
        if (slot == SLOT_FUEL) {
            return stack != null && stack.getItem() != null && TileEntityFurnace.getItemBurnTime(stack) == 0;
        }
        return false;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack == null) return false;
        if (slot == SLOT_FUEL) {
            return getFuelBurnTime(stack) > 0;
        }
        if (slot == SLOT_FEED) {
            return isSmeltableFeed(stack);
        }
        if (slot == SLOT_MOLD) return false;
        return true;
    }

    public static boolean isSmeltableFeed(ItemStack stack) {
        if (stack == null) return false;
        if (stack.getItem() instanceof naturalexceptions.minecraft.item.ItemLumeniteShard) return true;
        if (!naturalexceptions.core.material.VanillaResourceClassification.isMeltable(stack)
            && naturalexceptions.core.material.VanillaResourceClassification.getRejectionReason(stack) != null) {
            return false;
        }
        if (FeedstockItemRegistry.isRegistered(stack)) return true;
        if (stack.getItem() instanceof ItemGenericComponent) {
            return ItemGenericComponent.getComponent(stack) != null;
        }
        if (stack.getItem() instanceof ItemMaterialSample) {
            return ItemMaterialSample.getMaterialRef(stack) != null;
        }
        if (stack.getItem() instanceof ItemFluidVessel) {
            return ItemFluidVessel.contents(stack) != null;
        }
        if (stack.getItem() instanceof ItemGenericAssembly) {
            return ItemGenericAssembly.getAssembly(stack) != null;
        }
        if (stack.getItem() instanceof ItemCastingMold) {
            return ItemCastingMold.hasMetal(stack);
        }
        if (stack.getItem() instanceof ItemFoundryLadle) {
            return ItemFoundryLadle.hasMetal(stack);
        }
        return false;
    }

    public static int getFuelBurnTime(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return 0;
        if (stack.getItem() == Items.blaze_powder) {
            return 1200;
        }
        return TileEntityFurnace.getItemBurnTime(stack);
    }

    public static double getFuelFlameTemp(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return FuelBurnState.DEFAULT_FLAME_TEMPERATURE_K;
        if (stack.getItem() == Items.blaze_rod || stack.getItem() == Items.blaze_powder) {
            return 2600.0;
        }
        if (stack.getItem() == Items.coal) {
            if (stack.getItemDamage() == 1) {
                return 1950.0;
            }
            return 2010.0;
        }
        return 1950.0;
    }

    public static String getFuelHeatLabel(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return "Ordinary Heat";
        if (stack.getItem() == Items.blaze_rod || stack.getItem() == Items.blaze_powder) {
            return "Blaze Fuel — Extreme Heat";
        }
        if (stack.getItem() == Items.coal) {
            if (stack.getItemDamage() == 1) {
                return "Charcoal — Ordinary Heat";
            }
            return "Coal — Strong Heat";
        }
        return "Solid Fuel — Ordinary Heat";
    }

    public static String getFeedRejectionDiagnostic(ItemStack stack) {
        if (stack == null) return null;
        return naturalexceptions.core.material.VanillaResourceClassification.getRejectionReason(stack);
    }

    // --- Break safety ---

    /** Stack-tag key carrying a broken controller's vessel, fire, hearth, and slot contents. */
    public static final String CONTENTS_TAG = "SmelterContents";

    /** True when breaking this controller would otherwise discard matter, fuel, or heat. */
    private boolean creativeRemoval;

    public void markCreativeRemoval() {
        creativeRemoval = true;
    }

    public boolean isCreativeRemoval() {
        return creativeRemoval;
    }

    public boolean hasPreservableContents() {
        if (hearthWorkpiece != null) return true;
        for (ItemStack stack : inventory) {
            if (stack != null) return true;
        }
        return !vesselState.layers()
            .isEmpty()
            || vesselState.fuelState()
                .remainingTicks() > 0;
    }

    /** Serializes everything that must survive the controller being broken and re-placed. */
    public NBTTagCompound writeContentsTag() {
        NBTTagCompound contents = new NBTTagCompound();
        writeToNBT(contents);
        contents.removeTag("id");
        contents.removeTag("x");
        contents.removeTag("y");
        contents.removeTag("z");
        return contents;
    }

    /** Restores contents carried by a dropped controller. */
    public void readContentsTag(NBTTagCompound contents) {
        NBTTagCompound positioned = (NBTTagCompound) contents.copy();
        positioned.setString("id", "starter_smelter");
        positioned.setInteger("x", xCoord);
        positioned.setInteger("y", yCoord);
        positioned.setInteger("z", zCoord);
        readFromNBT(positioned);
        markDirty();
    }

    // --- Persistence & Networking ---

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        VesselStateNbtCodec.writeToNbt(vesselState, compound);
        compound.setBoolean("StructureValid", structureValid);
        compound.setInteger("ForcedDraftTicks", forcedDraftTicks);
        if (hearthWorkpiece != null) {
            NBTTagCompound hwTag = new NBTTagCompound();
            hearthWorkpiece.writeToNBT(hwTag);
            compound.setTag("HearthWorkpiece", hwTag);
        }

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
        vesselGeometry = null;
        this.vesselState = VesselStateNbtCodec.readFromNbt(compound, new MaterialPropertyResolver(), CAPACITY);
        if (compound.hasKey("StructureValid")) {
            this.structureValid = compound.getBoolean("StructureValid");
        }
        if (compound.hasKey("ForcedDraftTicks")) {
            this.forcedDraftTicks = Math.max(0, Math.min(100, compound.getInteger("ForcedDraftTicks")));
        }
        if (compound.hasKey("HearthWorkpiece", 10)) {
            this.hearthWorkpiece = ItemStack.loadItemStackFromNBT(compound.getCompoundTag("HearthWorkpiece"));
        } else {
            this.hearthWorkpiece = null;
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
