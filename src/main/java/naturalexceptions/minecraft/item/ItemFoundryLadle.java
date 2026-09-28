package naturalexceptions.minecraft.item;

import java.util.List;
import java.util.Objects;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentGeometry;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.fabrication.TileEntityCastingTable;

/**
 * Foundry hand vessel for scooping, transporting, and pouring molten metal with exact mass,
 * composition, and thermal energy conservation.
 */
public final class ItemFoundryLadle extends Item {

    public static final MaterialQuantity CAPACITY = MaterialQuantity.ofKilograms(4.0);

    public static final String KEY_MATERIAL_REF = "MaterialRef";
    public static final String KEY_STATE_KEY = "StateKey";
    public static final String KEY_COMPOSITION = "Composition";
    public static final String KEY_MASS_MG = "MassMg";
    public static final String KEY_ENERGY = "Energy";
    public static final String KEY_DENSITY = "Density";
    public static final String KEY_SPECIFIC_HEAT = "SpecificHeatSolid";
    public static final String KEY_SPECIFIC_HEAT_LIQUID = "SpecificHeatLiquid";
    public static final String KEY_MELT_POINT = "MeltingPointKelvin";
    public static final String KEY_LATENT_HEAT = "LatentHeatOfFusion";

    public ItemFoundryLadle() {
        setMaxStackSize(1);
        setUnlocalizedName("foundryLadle");
        setTextureName("naturalexceptions:clay_crucible");
    }

    public static boolean hasMetal(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return false;
        }
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt.hasKey(KEY_MASS_MG) && nbt.getLong(KEY_MASS_MG) > 0L;
    }

    public static MaterialQuantity getFilledMass(ItemStack stack) {
        if (!hasMetal(stack)) {
            return MaterialQuantity.ZERO;
        }
        return MaterialQuantity.ofMilligrams(
            stack.getTagCompound()
                .getLong(KEY_MASS_MG));
    }

    public static MaterialRef getContainedMaterialRef(ItemStack stack) {
        if (!hasMetal(stack)) {
            return null;
        }
        try {
            return MaterialRef.parse(
                stack.getTagCompound()
                    .getString(KEY_MATERIAL_REF));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static CanonicalMaterialKey getContainedStateKey(ItemStack stack) {
        if (!hasMetal(stack)) {
            return null;
        }
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt.hasKey(KEY_STATE_KEY)) {
            try {
                return CanonicalMaterialKey.parse(nbt.getString(KEY_STATE_KEY));
            } catch (RuntimeException ignored) {}
        }
        if (nbt.hasKey(KEY_COMPOSITION)) {
            try {
                return CanonicalMaterialKey.of(CanonicalComposition.parse(nbt.getString(KEY_COMPOSITION)));
            } catch (RuntimeException ignored) {}
        }
        MaterialRef ref = getContainedMaterialRef(stack);
        if (ref != null && ref.isAuthored()) {
            return CanonicalMaterialKey.of(CanonicalComposition.of(ref.authoredId()));
        }
        return null;
    }

    public static ActiveThermalState getThermalState(ItemStack stack) {
        if (!hasMetal(stack)) {
            return null;
        }
        NBTTagCompound nbt = stack.getTagCompound();
        double energy = nbt.getDouble(KEY_ENERGY);
        long massMg = nbt.getLong(KEY_MASS_MG);
        if (massMg <= 0L) {
            return null;
        }
        double massKg = (double) massMg / MaterialQuantity.MILLIGRAMS_PER_KILOGRAM;
        MaterialProperties props = resolveProperties(stack);
        return new ActiveThermalState(energy, massKg, props);
    }

    public static void saveThermalState(ItemStack stack, ActiveThermalState thermal) {
        if (stack == null || thermal == null) {
            return;
        }
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound nbt = stack.getTagCompound();
        nbt.setDouble(KEY_ENERGY, thermal.energyJoules());
        nbt.setLong(
            KEY_MASS_MG,
            MaterialQuantity.ofKilograms(thermal.massKg())
                .milligrams());
    }

    public static boolean isSolidified(ItemStack stack) {
        if (!hasMetal(stack)) {
            return false;
        }
        ActiveThermalState thermal = getThermalState(stack);
        if (thermal == null) {
            return true;
        }
        return thermal.phase() == MaterialPhase.SOLID && thermal.meltFraction() <= 0.0;
    }

    public static boolean fillLadle(ItemStack stack, AggregateLayer pouredLayer) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(pouredLayer, "pouredLayer");

        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound nbt = stack.getTagCompound();
        MaterialQuantity incomingMass = pouredLayer.mass();
        ActiveThermalState incomingThermal = pouredLayer.thermalState();
        MaterialProperties props = incomingThermal.properties();

        if (hasMetal(stack)) {
            MaterialRef existingRef = getContainedMaterialRef(stack);
            if (existingRef != null && !existingRef.equals(pouredLayer.materialRef())) {
                return false; // Incompatible metals
            }
            MaterialQuantity currentMass = getFilledMass(stack);
            MaterialQuantity newMass = currentMass.add(incomingMass);
            if (newMass.compareTo(CAPACITY) > 0) {
                return false;
            }
            double totalEnergy = nbt.getDouble(KEY_ENERGY) + incomingThermal.energyJoules();
            nbt.setLong(KEY_MASS_MG, newMass.milligrams());
            nbt.setDouble(KEY_ENERGY, totalEnergy);
        } else {
            if (incomingMass.compareTo(CAPACITY) > 0) {
                return false;
            }
            nbt.setString(
                KEY_MATERIAL_REF,
                pouredLayer.materialRef()
                    .asString());
            nbt.setString(
                KEY_STATE_KEY,
                pouredLayer.stateKey()
                    .canonicalString());
            nbt.setString(
                KEY_COMPOSITION,
                pouredLayer.composition()
                    .canonicalString());
            nbt.setLong(KEY_MASS_MG, incomingMass.milligrams());
            nbt.setDouble(KEY_ENERGY, incomingThermal.energyJoules());

            nbt.setDouble(KEY_DENSITY, props.density());
            nbt.setDouble(KEY_SPECIFIC_HEAT, props.specificHeatSolid());
            nbt.setDouble(KEY_SPECIFIC_HEAT_LIQUID, props.specificHeatLiquid());
            nbt.setDouble(KEY_MELT_POINT, props.meltingPointKelvin());
            nbt.setDouble(KEY_LATENT_HEAT, props.latentHeatOfFusion());
        }
        return true;
    }

    public static AggregateLayer drainMetal(ItemStack stack, MaterialQuantity amount) {
        if (!hasMetal(stack) || amount == null || amount.isZero()) {
            return null;
        }
        NBTTagCompound nbt = stack.getTagCompound();
        MaterialQuantity currentMass = getFilledMass(stack);
        MaterialQuantity transferMass = currentMass.compareTo(amount) <= 0 ? currentMass : amount;

        double currentEnergy = nbt.getDouble(KEY_ENERGY);
        double fraction = (double) transferMass.milligrams() / (double) currentMass.milligrams();
        double transferEnergy = currentEnergy * fraction;
        double remainingEnergy = Math.max(0.0, currentEnergy - transferEnergy);

        MaterialQuantity remainingMass = currentMass.subtract(transferMass);

        MaterialRef ref = getContainedMaterialRef(stack);
        CanonicalMaterialKey key = getContainedStateKey(stack);
        CanonicalComposition comp = key != null ? key.composition() : CanonicalComposition.of(ref.authoredId());
        MaterialProperties props = resolveProperties(stack);

        ActiveThermalState drainedThermal = new ActiveThermalState(transferEnergy, transferMass.toKilograms(), props);
        AggregatePhaseType phaseType = drainedThermal.phase() == MaterialPhase.LIQUID ? AggregatePhaseType.MOLTEN_METAL
            : AggregatePhaseType.SOLID_CHARGE;

        if (remainingMass.isZero()) {
            clearLadle(stack);
        } else {
            nbt.setLong(KEY_MASS_MG, remainingMass.milligrams());
            nbt.setDouble(KEY_ENERGY, remainingEnergy);
        }

        return new AggregateLayer(ref, key, comp, transferMass, drainedThermal, phaseType);
    }

    public static void clearLadle(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return;
        }
        NBTTagCompound nbt = stack.getTagCompound();
        nbt.removeTag(KEY_MATERIAL_REF);
        nbt.removeTag(KEY_STATE_KEY);
        nbt.removeTag(KEY_COMPOSITION);
        nbt.removeTag(KEY_MASS_MG);
        nbt.removeTag(KEY_ENERGY);
        nbt.removeTag(KEY_DENSITY);
        nbt.removeTag(KEY_SPECIFIC_HEAT);
        nbt.removeTag(KEY_SPECIFIC_HEAT_LIQUID);
        nbt.removeTag(KEY_MELT_POINT);
        nbt.removeTag(KEY_LATENT_HEAT);
    }

    /** Withdraw a real portion of the ladle charge, retaining its identity and thermal energy. */
    public static ItemStack extractSample(ItemStack stack) {
        if (!hasMetal(stack) || isSolidified(stack)) return null;
        AggregateLayer portion = drainMetal(stack, MaterialQuantity.ofGrams(100.0));
        return portion == null ? null
            : ItemMaterialSample.createStack(portion.materialRef(), portion.stateKey(), portion.thermalState());
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote && player.isSneaking()) {
            ItemStack sample = extractSample(stack);
            if (sample != null) {
                if (!player.inventory.addItemStackToInventory(sample)) {
                    player.dropPlayerItemWithRandomChoice(sample, false);
                }
                player.inventoryContainer.detectAndSendChanges();
            }
        }
        return stack;
    }

    public static boolean pourIntoMold(ItemStack ladleStack, ItemStack moldStack) {
        if (!hasMetal(ladleStack) || moldStack == null || ItemCastingMold.isFilled(moldStack)) {
            return false;
        }
        if (isSolidified(ladleStack)) {
            return false; // Cannot pour solidified metal
        }

        GeometryId geom = ItemCastingMold.getGeometry(moldStack);
        if (geom == null) {
            geom = GeometryId.BLANK;
        }
        ComponentGeometry geomDef = ComponentGeometry.forId(geom);
        MaterialRef ladleRef = getContainedMaterialRef(ladleStack);
        MaterialRef moldRef = ItemCastingMold.getContainedMaterialRef(moldStack);

        if (moldRef != null && !moldRef.equals(ladleRef)) {
            return false; // Incompatible metal
        }

        MaterialProperties props = resolveProperties(ladleStack);
        MaterialQuantity required = geomDef.requiredMass(props.density());
        MaterialQuantity currentInMold = ItemCastingMold.getFilledMass(moldStack);
        MaterialQuantity needed = required.compareTo(currentInMold) > 0 ? required.subtract(currentInMold)
            : MaterialQuantity.ZERO;

        if (needed.isZero()) {
            return false;
        }

        MaterialQuantity ladleMass = getFilledMass(ladleStack);
        MaterialQuantity transferAmount = ladleMass.compareTo(needed) <= 0 ? ladleMass : needed;

        AggregateLayer drained = drainMetal(ladleStack, transferAmount);
        if (drained != null) {
            ItemCastingMold.fillMold(moldStack, drained);
            return true;
        }
        return false;
    }

    public static boolean pourIntoCastingTable(ItemStack ladleStack, TileEntityCastingTable table) {
        if (table == null || table.getMold() == null) {
            return false;
        }
        ItemStack tableMold = table.getMold();
        boolean poured = pourIntoMold(ladleStack, tableMold);
        if (poured) {
            table.markDirty();
            if (table.getWorldObj() != null) {
                table.getWorldObj()
                    .markBlockForUpdate(table.xCoord, table.yCoord, table.zCoord);
                table.getWorldObj()
                    .playSoundEffect(
                        table.xCoord + 0.5D,
                        table.yCoord + 0.5D,
                        table.zCoord + 0.5D,
                        "liquid.lavapop",
                        1.0F,
                        1.0F);
            }
        }
        return poured;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean isCurrentItem) {
        if (hasMetal(stack)) {
            ActiveThermalState thermal = getThermalState(stack);
            if (thermal != null && thermal.temperatureKelvin() > 293.15) {
                double deltaSeconds = 0.05; // 1 tick
                double temp = thermal.temperatureKelvin();
                double surfaceArea = 0.05; // Ladle open surface area
                double qRad = 0.80 * 5.670374419e-8
                    * surfaceArea
                    * (Math.pow(temp, 4.0) - Math.pow(293.15, 4.0))
                    * deltaSeconds;
                double qConv = 15.0 * surfaceArea * (temp - 293.15) * deltaSeconds;
                double totalLoss = Math.max(0.0, qRad + qConv);
                double ambientEnergy = naturalexceptions.core.thermal.ThermodynamicModel
                    .energyForTemperature(293.15, 0.0, thermal.massKg(), thermal.properties());
                if (thermal.energyJoules() - totalLoss < ambientEnergy) {
                    thermal.setEnergyJoules(ambientEnergy);
                } else {
                    thermal.removeEnergyJoules(totalLoss);
                }
                saveThermalState(stack, thermal);
            }
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        if (!hasMetal(stack)) {
            list.add("\u00a77Empty ladle (Capacity: " + CAPACITY + ")");
            list.add("\u00a78Right-click smelter or outlet to scoop molten metal.");
            list.add("\u00a78Then sneak-right-click in air to take a material sample.");
            return;
        }

        MaterialRef ref = getContainedMaterialRef(stack);
        CanonicalMaterialKey key = getContainedStateKey(stack);
        list.add("\u00a78Sneak-right-click in air to take up to 100 g as a sample.");
        ActiveThermalState thermal = getThermalState(stack);

        String matName = naturalexceptions.minecraft.knowledge.KnowledgePresentation.materialName(player, ref, key);
        list.add("\u00a76Metal: \u00a7f" + matName);

        if (thermal != null) {
            double tempK = thermal.temperatureKelvin();
            double tempC = tempK - 273.15;
            double meltK = thermal.properties()
                .meltingPointKelvin();
            ItemGenericComponent.ThermalCondition cond = ItemGenericComponent
                .getThermalCondition(tempK, thermal.properties());

            list.add("  \u00a77Heat: " + cond.label());

            if (tempK >= meltK) {
                list.add("  \u00a7eState: Molten Liquid (Ready to Pour)");
            } else if (thermal.phase() == MaterialPhase.LIQUID || thermal.meltFraction() > 0.0) {
                list.add("  \u00a76State: Partly solidified");
            } else {
                list.add("  \u00a7cState: Solidified (Frozen in Ladle)");
                list.add("\u00a78Frozen in ladle; place in smelter feed to remelt.");
            }

            boolean showAdvanced = advanced;
            try {
                if (org.lwjgl.input.Keyboard.isCreated()) {
                    showAdvanced |= org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT)
                        || org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT);
                }
            } catch (Throwable ignored) {}

            if (advanced) {
                list.add("\u00a78--- Developer diagnostics ---");
                list.add(
                    String
                        .format("  \u00a78Temperature: %.1f K (%.1f \u00b0C) | T_melt = %.0f K", tempK, tempC, meltK));
            }
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (!hasMetal(stack)) {
            return "Foundry Ladle";
        }
        MaterialRef ref = getContainedMaterialRef(stack);
        CanonicalMaterialKey key = getContainedStateKey(stack);
        if (isSolidified(stack)) {
            return "Solidified Foundry Ladle";
        }
        return "Filled Foundry Ladle";
    }

    private static MaterialProperties resolveProperties(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null && nbt.hasKey(KEY_DENSITY)) {
            java.util.Map<String, Double> map = new java.util.HashMap<>();
            map.put(MaterialProperties.KEY_DENSITY, nbt.getDouble(KEY_DENSITY));
            map.put(MaterialProperties.KEY_SPECIFIC_HEAT_SOLID, nbt.getDouble(KEY_SPECIFIC_HEAT));
            map.put(MaterialProperties.KEY_SPECIFIC_HEAT_LIQUID, nbt.getDouble(KEY_SPECIFIC_HEAT_LIQUID));
            map.put(MaterialProperties.KEY_MELTING_POINT, nbt.getDouble(KEY_MELT_POINT));
            map.put(MaterialProperties.KEY_LATENT_HEAT_FUSION, nbt.getDouble(KEY_LATENT_HEAT));
            map.put(MaterialProperties.KEY_HARDNESS, 3.0);
            return new MaterialProperties(map);
        }

        MaterialRef ref = getContainedMaterialRef(stack);
        CanonicalMaterialKey key = getContainedStateKey(stack);
        if (ref != null) {
            MaterialRef effectiveRef = ref;
            CanonicalComposition comp = key != null ? key.composition() : CanonicalComposition.of(ref.authoredId());
            MaterialPhase phase = key != null ? key.phase() : MaterialPhase.LIQUID;
            try {
                return new MaterialPropertyResolver().resolve(effectiveRef, comp, phase)
                    .properties();
            } catch (Exception ignored) {}
        }
        return BuiltinMaterials.COPPER_PROPERTIES;
    }
}
