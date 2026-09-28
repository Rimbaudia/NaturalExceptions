package naturalexceptions.minecraft.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.event.EventInstanceFactory;
import naturalexceptions.core.event.EventRecord;
import naturalexceptions.core.event.WorldEventSchedule;
import naturalexceptions.core.electrical.CircuitNetwork;
import naturalexceptions.core.electrical.ResistiveLoad;
import naturalexceptions.core.electrical.SwitchElement;
import naturalexceptions.core.electrical.VoltageSource;
import naturalexceptions.core.fabrication.ComponentDamage;
import naturalexceptions.core.fluid.FluidCellKey;
import naturalexceptions.core.fluid.FluidParcel;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.persistence.WorldMetadata;
import naturalexceptions.core.persistence.SaveSchemaVersion;
import naturalexceptions.core.persistence.WorldgenEpoch;
import naturalexceptions.core.thermal.ActiveThermalState;

final class NEWorldSavedDataP9Test {
    @Test
    void worldSaveRetainsP9FluidAndCircuitPhysicalTruth() {
        MaterialProperties properties = MaterialProperties.builder().density(1).specificHeatSolid(4180)
            .specificHeatLiquid(4180).meltingPointKelvin(273).latentHeatOfFusion(334000)
            .electricalResistivity(1e-6).build();
        CanonicalComposition water = CanonicalComposition.of(ResourceId.of("minecraft", "water"));
        MaterialQuantity quantity = MaterialQuantity.ofGrams(500);
        FluidParcel parcel = new FluidParcel(MaterialRef.authored("minecraft", "water"),
            CanonicalMaterialKey.of(water, MaterialPhase.LIQUID, null, null), water, quantity,
            ActiveThermalState.ambient(quantity.toKilograms(), properties));
        NEWorldSavedData original = new NEWorldSavedData();
        original.setMetadata(WorldMetadata.create("p9-test-world", SaveSchemaVersion.CURRENT,
            WorldgenEpoch.CURRENT, "p9-test", 0L));
        FluidCellKey cell = new FluidCellKey(0, 3, 65, -8);
        original.fluids().put(cell, parcel);
        original.electrical().register(new CircuitNetwork("saved", new VoltageSource("battery", 9),
            new SwitchElement("switch", true), new ResistiveLoad("heater", 10,
                ActiveThermalState.ambient(.01, properties), new ComponentDamage(), 900)));
        original.markDirty();

        NBTTagCompound saved = new NBTTagCompound(); original.writeToNBT(saved);
        NEWorldSavedData restored = new NEWorldSavedData(); restored.readFromNBT(saved);
        assertEquals(500000L, restored.fluids().totalMass().milligrams());
        assertTrue(restored.electrical().network("saved").switchElement().closed());
        assertEquals(0.9, restored.electrical().network("saved").solve().currentAmps(), 1.0e-12);
    }

    @Test
    void worldSaveRetainsImpactOwnershipBesideItsMaterializedP9Matter() {
        BuiltinContent.register();
        NEWorldSavedData original = new NEWorldSavedData();
        original.setMetadata(WorldMetadata.create("p11-test-world", SaveSchemaVersion.CURRENT,
            WorldgenEpoch.CURRENT, "p11-test", 0L));
        EventRecord rain = EventInstanceFactory.protectedFirstStarRain(221L, 2, 0L, 0L, 0L, 100L);
        WorldEventSchedule schedule = new WorldEventSchedule();
        schedule.ensureProtectedFirstStarRain(rain);
        original.p11().synchronize(schedule.snapshot(), 0, 0);
        naturalexceptions.core.event.StarRainImpact impact = original.p11().snapshot().get(0);
        original.p11().resolve(impact.id());
        MaterialQuantity mass = impact.stellarMass();
        ActiveThermalState hot = ActiveThermalState.atTemperature(1780.0, 1.0, mass.toKilograms(),
            BuiltinMaterials.STELLAR_MATTER_PROPERTIES);
        FluidParcel star = new FluidParcel(BuiltinMaterials.STELLAR_MATTER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.STELLAR_MATTER_COMPOSITION, MaterialPhase.LIQUID, null, null),
            BuiltinMaterials.STELLAR_MATTER_COMPOSITION, mass, hot, impact.id());
        FluidCellKey site = new FluidCellKey(0, impact.x(), 64, impact.z());
        original.fluids().put(site, star);
        assertEquals(mass, original.p11().transferToSpatialSite(impact.id()));
        original.markDirty();

        NBTTagCompound saved = new NBTTagCompound();
        original.writeToNBT(saved);
        NEWorldSavedData restored = new NEWorldSavedData();
        restored.readFromNBT(saved);
        naturalexceptions.core.event.StarRainImpact restoredImpact = restored.p11().get(impact.id());
        assertTrue(restoredImpact.isMaterialized());
        assertEquals(MaterialQuantity.ZERO, restoredImpact.stellarMass());
        assertEquals(mass, restored.fluids().parcelAt(site).mass());
        assertEquals(impact.id(), restored.fluids().parcelAt(site).provenanceId());
        assertEquals(1780.0, restored.fluids().parcelAt(site).thermalState().temperatureKelvin(), 0.1);
    }
}
