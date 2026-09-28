package naturalexceptions.core.fluid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.thermal.ActiveThermalState;

final class FluidWorldStateTest {
    private static FluidParcel water(long mg) {
        CanonicalComposition composition = CanonicalComposition.of(ResourceId.of("minecraft", "water"));
        CanonicalMaterialKey key = CanonicalMaterialKey.of(composition, MaterialPhase.LIQUID, null, null);
        MaterialProperties properties = MaterialProperties.builder().density(1.0).specificHeatSolid(4180.0)
            .specificHeatLiquid(4180.0).meltingPointKelvin(273.15).latentHeatOfFusion(334000.0).build();
        MaterialQuantity mass = MaterialQuantity.ofMilligrams(mg);
        return new FluidParcel(MaterialRef.authored("minecraft", "water"), key, composition, mass,
            ActiveThermalState.ambient(mass.toKilograms(), properties));
    }

    @Test
    void connectedCellsRebuildWithBudgetAndSleepUntilDisturbed() {
        FluidWorldState world = new FluidWorldState(16);
        FluidCellKey a = new FluidCellKey(0, 1, 64, 1);
        FluidCellKey b = new FluidCellKey(0, 2, 64, 1);
        world.put(a, water(100000L));
        world.put(b, water(100000L));

        assertTrue(world.processDirty(1) <= 1);
        assertTrue(world.dirtyCellCount() > 0);
        world.processDirty(1);
        assertEquals(1, world.regionCount());
        assertEquals(2, world.regionAt(a).cellCount());
        assertEquals(0, world.processDirty(1));
        assertEquals(0, world.processDirty(1));
        assertEquals(1, world.sleepingRegionCount());

        world.disturb(b);
        assertTrue(world.isDirty(a));
        assertFalse(world.isDirty(new FluidCellKey(0, 99, 64, 99)));
        assertTrue(world.processDirty(1) <= 1);
    }

    @Test
    void removingCellSplitsConnectedBodyIntoSleepingRegions() {
        FluidWorldState world = new FluidWorldState(16);
        FluidCellKey a = new FluidCellKey(0, 0, 64, 0);
        FluidCellKey b = new FluidCellKey(0, 1, 64, 0);
        FluidCellKey c = new FluidCellKey(0, 2, 64, 0);
        world.put(a, water(100000L)); world.put(b, water(100000L)); world.put(c, water(100000L));
        while (world.processDirty(16) > 0) { }
        assertEquals(1, world.regionCount());

        world.remove(b);
        while (world.processDirty(1) > 0) { }
        assertEquals(2, world.regionCount());
        assertEquals(2, world.sleepingRegionCount());
        assertEquals(200000L, world.totalMass().milligrams());
    }

    @Test
    void movementAndSpillConserveMatterWhileReservoirRemainsDeclared() {
        FluidWorldState world = new FluidWorldState(32);
        FluidCellKey source = new FluidCellKey(0, 4, 64, 4);
        FluidCellKey destination = new FluidCellKey(0, 4, 63, 4);
        FluidVessel vessel = new FluidVessel(300000L, FluidInteractionRules.sameMaterialOnly());
        FluidParcel parcel = water(200000L);
        vessel.fill(parcel);
        world.spillFrom(vessel, source, MaterialQuantity.ofMilligrams(100000L));
        assertEquals(100000L, world.totalMass().milligrams());
        assertTrue(world.move(source, destination));
        assertEquals(100000L, world.totalMass().milligrams());
        assertEquals(MaterialRef.authored("minecraft", "water"), world.parcelAt(destination).materialRef());

        FluidSourceBoundary reservoir = FluidSourceBoundary.infinite("spring-1", world.parcelAt(destination).materialRef());
        world.registerSourceBoundary(destination, reservoir);
        assertTrue(world.sourceAt(destination).infinite());
        assertEquals(100000L, world.totalMass().milligrams());
    }

    @Test
    void refusedSpillLeavesSourceVesselUntouched() {
        FluidWorldState world = new FluidWorldState(1);
        world.put(new FluidCellKey(0, 40, 64, 40), water(1000L));
        FluidVessel vessel = new FluidVessel(300000L, FluidInteractionRules.sameMaterialOnly());
        vessel.fill(water(200000L));
        double energyBefore = vessel.totalThermalEnergyJoules();

        // The bounded rebuild queue is full, so the destination cannot accept matter.
        assertFalse(world.spillFrom(vessel, new FluidCellKey(0, 4, 64, 4), MaterialQuantity.ofMilligrams(100000L)));
        assertEquals(200000L, vessel.totalMass().milligrams());
        assertEquals(1, vessel.contents().size());
        assertEquals(energyBefore, vessel.totalThermalEnergyJoules(), 1.0e-9);
        assertEquals(1000L, world.totalMass().milligrams());

        // Occupied destinations are refused before draining too.
        FluidWorldState roomy = new FluidWorldState(32);
        FluidCellKey occupied = new FluidCellKey(0, 1, 64, 1);
        roomy.put(occupied, water(1000L));
        assertFalse(roomy.spillFrom(vessel, occupied, MaterialQuantity.ofMilligrams(1000L)));
        assertEquals(200000L, vessel.totalMass().milligrams());
    }

    @Test
    void hotSpatialFluidUsesSharedThermalStateThenSleepsAfterSolidifying() {
        FluidWorldState world = new FluidWorldState(16);
        FluidCellKey key = new FluidCellKey(0, 8, 70, 8);
        CanonicalComposition c = CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"));
        CanonicalMaterialKey k = CanonicalMaterialKey.of(c, MaterialPhase.LIQUID, null, null);
        MaterialProperties p = MaterialProperties.builder().density(8.96).specificHeatSolid(385).specificHeatLiquid(410)
            .meltingPointKelvin(1357.77).latentHeatOfFusion(205000).build();
        MaterialQuantity q = MaterialQuantity.ofGrams(100);
        FluidParcel hot = new FluidParcel(MaterialRef.authored("naturalexceptions", "copper"), k, c, q,
            ActiveThermalState.atTemperature(1400, 1, q.toKilograms(), p));
        double initialEnergy = hot.thermalState().energyJoules();
        world.put(key, hot);

        assertEquals(1, world.activeThermalCellCount());
        assertTrue(world.processThermalWork(86400, 1) > 0);
        assertTrue(hot.thermalState().energyJoules() < initialEnergy);
        assertEquals(MaterialPhase.SOLID, hot.phase());
        assertEquals(MaterialPhase.SOLID, hot.stateKey().phase());
        assertEquals(0, world.activeThermalCellCount());
        assertEquals(0, world.processThermalWork(86400, 1));
    }

    @Test
    void phaseChangeQueuesOnlyTheHostThatNeedsARepresentationRefresh() {
        FluidWorldState world = new FluidWorldState(16);
        FluidCellKey key = new FluidCellKey(0, 8, 70, 8);
        CanonicalComposition c = CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"));
        CanonicalMaterialKey k = CanonicalMaterialKey.of(c, MaterialPhase.LIQUID, null, null);
        MaterialProperties p = MaterialProperties.builder().density(8.96).specificHeatSolid(385).specificHeatLiquid(410)
            .meltingPointKelvin(1357.77).latentHeatOfFusion(205000).build();
        MaterialQuantity q = MaterialQuantity.ofGrams(100);
        world.put(key, new FluidParcel(MaterialRef.authored("naturalexceptions", "copper"), k, c, q,
            ActiveThermalState.atTemperature(1400, 1, q.toKilograms(), p)));

        world.processThermalWork(86400, 1);

        assertEquals(java.util.Collections.singletonList(key), world.drainHostPhaseChanges(4));
        assertTrue(world.drainHostPhaseChanges(4).isEmpty());
    }

    @Test
    void stalePersistedPhaseQueuesOneHostRefreshOnStateReconstruction() {
        FluidWorldState world = new FluidWorldState(16);
        FluidCellKey key = new FluidCellKey(0, 10, 70, 10);
        CanonicalComposition c = CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"));
        CanonicalMaterialKey staleLiquidKey = CanonicalMaterialKey.of(c, MaterialPhase.LIQUID, null, null);
        MaterialProperties p = MaterialProperties.builder().density(8.96).specificHeatSolid(385).specificHeatLiquid(410)
            .meltingPointKelvin(1357.77).latentHeatOfFusion(205000).build();
        MaterialQuantity q = MaterialQuantity.ofGrams(100);
        FluidParcel savedSolid = new FluidParcel(MaterialRef.authored("naturalexceptions", "copper"), staleLiquidKey, c, q,
            ActiveThermalState.atTemperature(1100.0, 1.0, q.toKilograms(), p));

        world.put(key, savedSolid);

        assertEquals(java.util.Collections.singletonList(key), world.drainHostPhaseChanges(1));
        assertEquals(MaterialPhase.SOLID, savedSolid.stateKey().phase());
    }

    @Test
    void unloadedThermalCellsPauseAndPhaseRefreshWaitsForLoadedHost() {
        FluidWorldState world = new FluidWorldState(16);
        FluidCellKey key = new FluidCellKey(0, 20, 70, 20);
        CanonicalComposition c = CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"));
        CanonicalMaterialKey k = CanonicalMaterialKey.of(c, MaterialPhase.LIQUID, null, null);
        MaterialProperties p = MaterialProperties.builder().density(8.96).specificHeatSolid(385).specificHeatLiquid(410)
            .meltingPointKelvin(1357.77).latentHeatOfFusion(205000).build();
        MaterialQuantity q = MaterialQuantity.ofGrams(100);
        FluidParcel hot = new FluidParcel(MaterialRef.authored("naturalexceptions", "copper"), k, c, q,
            ActiveThermalState.atTemperature(1400.0, 1.0, q.toKilograms(), p));
        double energy = hot.thermalState().energyJoules();
        world.put(key, hot);

        assertEquals(0, world.processThermalWork(86400.0, 4, candidate -> false));
        assertEquals(energy, hot.thermalState().energyJoules(), 1.0e-8);
        assertTrue(world.drainHostPhaseChanges(4, candidate -> false).isEmpty());
        assertEquals(1, world.processThermalWork(86400.0, 4, candidate -> true));
        assertEquals(java.util.Collections.singletonList(key), world.drainHostPhaseChanges(4, candidate -> true));
    }

    @Test
    void smelterMeltCanTransferThroughVesselIntoSpatialFluidAndSolidify() {
        CanonicalComposition copper = CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"));
        CanonicalMaterialKey liquidKey = CanonicalMaterialKey.of(copper, MaterialPhase.LIQUID, null, null);
        MaterialProperties properties = MaterialProperties.builder().density(8.96).specificHeatSolid(385)
            .specificHeatLiquid(410).meltingPointKelvin(1357.77).latentHeatOfFusion(205000).build();
        MaterialQuantity mass = MaterialQuantity.ofGrams(100);
        AggregateLayer smelterMelt = new AggregateLayer(MaterialRef.authored("naturalexceptions", "copper"),
            liquidKey, copper, mass, ActiveThermalState.atTemperature(1400, 1, mass.toKilograms(), properties),
            AggregatePhaseType.MOLTEN_METAL);
        FluidParcel extracted = FluidParcel.fromAggregateLayer(smelterMelt);
        FluidVessel vessel = new FluidVessel(20_000, FluidInteractionRules.sameMaterialOnly());
        assertEquals(mass, vessel.fill(extracted));
        FluidParcel poured = vessel.drain(mass);
        FluidCellKey cell = new FluidCellKey(0, 5, 65, 5);
        FluidWorldState world = new FluidWorldState();
        world.put(cell, poured);

        assertEquals(100_000L, world.totalMass().milligrams());
        assertEquals(MaterialPhase.LIQUID, world.parcelAt(cell).phase());
        world.processThermalWork(86400, 1);
        assertEquals(MaterialPhase.SOLID, world.parcelAt(cell).phase());
        assertEquals(100_000L, world.totalMass().milligrams());
        assertEquals(0, world.activeThermalCellCount());
    }
}
