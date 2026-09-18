package naturalexceptions.minecraft.persistence;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
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

/**
 * NBT serialization codec for VesselState domain records.
 */
public final class VesselStateNbtCodec {

    public static final String KEY_CAPACITY_MG = "CapacityMg";
    public static final String KEY_LINING_DAMAGE = "LiningDamage";
    public static final String KEY_LINING_MAX_TEMP = "LiningMaxTemp";
    public static final String KEY_FUEL_REMAINING = "FuelRemaining";
    public static final String KEY_FUEL_TOTAL = "FuelTotal";
    public static final String KEY_FUEL_POWER = "FuelPower";
    public static final String KEY_EXHAUST_TOTAL_MG = "ExhaustTotalMg";
    public static final String KEY_EXHAUST_CONSTITUENTS = "ExhaustConstituents";
    public static final String KEY_LAYERS = "Layers";

    public static final String KEY_LAYER_REF = "MaterialRef";
    public static final String KEY_LAYER_STATE_KEY = "StateKey";
    public static final String KEY_LAYER_COMPOSITION = "Composition";
    public static final String KEY_LAYER_MASS_MG = "MassMg";
    public static final String KEY_LAYER_PHASE_TYPE = "PhaseType";
    public static final String KEY_LAYER_ENERGY = "Energy";

    private VesselStateNbtCodec() {}

    public static void writeToNbt(VesselState state, NBTTagCompound nbt) {
        if (state == null || nbt == null) return;

        nbt.setLong(
            KEY_CAPACITY_MG,
            state.capacity()
                .milligrams());
        nbt.setDouble(
            KEY_LINING_DAMAGE,
            state.lining()
                .damage());
        nbt.setDouble(
            KEY_LINING_MAX_TEMP,
            state.lining()
                .maxSafeTemperatureK());

        nbt.setInteger(
            KEY_FUEL_REMAINING,
            state.fuelState()
                .remainingTicks());
        nbt.setInteger(
            KEY_FUEL_TOTAL,
            state.fuelState()
                .totalBurnTicks());
        nbt.setDouble(
            KEY_FUEL_POWER,
            state.fuelState()
                .powerWatts());

        nbt.setLong(
            KEY_EXHAUST_TOTAL_MG,
            state.exhaustSink()
                .totalVentedMass()
                .milligrams());

        NBTTagList exhaustList = new NBTTagList();
        for (java.util.Map.Entry<ResourceId, MaterialQuantity> entry : state.exhaustSink()
            .ventedByConstituent()
            .entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString(
                "Id",
                entry.getKey()
                    .toString());
            tag.setLong(
                "Mg",
                entry.getValue()
                    .milligrams());
            exhaustList.appendTag(tag);
        }
        nbt.setTag(KEY_EXHAUST_CONSTITUENTS, exhaustList);

        NBTTagList layersList = new NBTTagList();
        for (AggregateLayer layer : state.layers()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString(
                KEY_LAYER_REF,
                layer.materialRef()
                    .asString());
            tag.setString(
                KEY_LAYER_STATE_KEY,
                layer.stateKey()
                    .canonicalString());
            tag.setString(
                KEY_LAYER_COMPOSITION,
                layer.composition()
                    .canonicalString());
            tag.setLong(
                KEY_LAYER_MASS_MG,
                layer.mass()
                    .milligrams());
            tag.setString(
                KEY_LAYER_PHASE_TYPE,
                layer.phaseType()
                    .name());
            tag.setDouble(
                KEY_LAYER_ENERGY,
                layer.thermalState()
                    .energyJoules());
            layersList.appendTag(tag);
        }
        nbt.setTag(KEY_LAYERS, layersList);
    }

    public static VesselState readFromNbt(NBTTagCompound nbt, MaterialPropertyResolver resolver) {
        if (nbt == null) {
            return new VesselState();
        }

        MaterialQuantity capacity = nbt.hasKey(KEY_CAPACITY_MG, 99)
            ? MaterialQuantity.ofMilligrams(nbt.getLong(KEY_CAPACITY_MG))
            : VesselState.DEFAULT_CAPACITY;

        double liningDamage = nbt.getDouble(KEY_LINING_DAMAGE);
        double liningMaxTemp = nbt.hasKey(KEY_LINING_MAX_TEMP, 99) ? nbt.getDouble(KEY_LINING_MAX_TEMP)
            : VesselLining.DEFAULT_MAX_SAFE_TEMP_K;
        VesselLining lining = new VesselLining(liningMaxTemp, liningDamage);

        int fuelRemaining = nbt.getInteger(KEY_FUEL_REMAINING);
        int fuelTotal = nbt.getInteger(KEY_FUEL_TOTAL);
        double fuelPower = nbt.hasKey(KEY_FUEL_POWER, 99) ? nbt.getDouble(KEY_FUEL_POWER)
            : (FuelBurnState.JOULES_PER_BURN_TICK * FuelBurnState.TICKS_PER_SECOND);
        FuelBurnState fuelState = new FuelBurnState(fuelRemaining, fuelTotal, fuelPower);

        ExhaustSink exhaustSink = new ExhaustSink();
        if (nbt.hasKey(KEY_EXHAUST_CONSTITUENTS, 9)) {
            NBTTagList exhaustList = nbt.getTagList(KEY_EXHAUST_CONSTITUENTS, 10);
            for (int i = 0; i < exhaustList.tagCount(); i++) {
                NBTTagCompound tag = exhaustList.getCompoundTagAt(i);
                ResourceId id = ResourceId.parse(tag.getString("Id"));
                long mg = tag.getLong("Mg");
                exhaustSink.vent(id, MaterialQuantity.ofMilligrams(mg));
            }
        }

        VesselState vessel = new VesselState(capacity, lining, fuelState, exhaustSink, resolver);

        if (nbt.hasKey(KEY_LAYERS, 9)) {
            NBTTagList layersList = nbt.getTagList(KEY_LAYERS, 10);
            for (int i = 0; i < layersList.tagCount(); i++) {
                NBTTagCompound tag = layersList.getCompoundTagAt(i);
                MaterialRef ref = MaterialRef.parse(tag.getString(KEY_LAYER_REF));
                CanonicalMaterialKey key = CanonicalMaterialKey.parse(tag.getString(KEY_LAYER_STATE_KEY));
                CanonicalComposition comp = CanonicalComposition.parse(tag.getString(KEY_LAYER_COMPOSITION));
                MaterialQuantity mass = MaterialQuantity.ofMilligrams(tag.getLong(KEY_LAYER_MASS_MG));
                AggregatePhaseType phaseType = AggregatePhaseType.valueOf(tag.getString(KEY_LAYER_PHASE_TYPE));
                double energy = tag.getDouble(KEY_LAYER_ENERGY);

                MaterialProperties props = resolver.resolve(ref, comp, key.phase())
                    .properties();
                ActiveThermalState thermal = new ActiveThermalState(energy, mass.toKilograms(), props);

                AggregateLayer layer = new AggregateLayer(ref, key, comp, mass, thermal, phaseType);
                vessel.addLayer(layer);
            }
        }

        return vessel;
    }
}
