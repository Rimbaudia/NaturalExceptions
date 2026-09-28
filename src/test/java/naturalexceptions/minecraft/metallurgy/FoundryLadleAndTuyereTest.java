package naturalexceptions.minecraft.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentGeometry;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.item.ItemFoundryLadle;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

final class FoundryLadleAndTuyereTest {

    @BeforeAll
    static void init() {
        try {
            net.minecraft.init.Bootstrap.func_151354_b();
        } catch (Throwable ignored) {}
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    @Test
    void testLadleScoopsMoltenMetalPreservingMassCompositionAndEnergy() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        MaterialQuantity initialCuMass = MaterialQuantity.ofGrams(800.0);
        ActiveThermalState hotThermal = ActiveThermalState
            .atTemperature(1450.0, 1.0, initialCuMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        double expectedInitialEnergy = hotThermal.energyJoules();
        AggregateLayer cuLayer = new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper")),
            initialCuMass,
            hotThermal,
            AggregatePhaseType.MOLTEN_METAL);
        smelter.vesselState()
            .addLayer(cuLayer);

        ItemStack ladle = new ItemStack(NEItems.foundryLadle);
        assertFalse(ItemFoundryLadle.hasMetal(ladle));

        assertTrue(smelter.pourIntoLadle(ladle));
        assertTrue(ItemFoundryLadle.hasMetal(ladle));

        // Mass in ladle equals initial mass since initialCuMass (800g) <= CAPACITY (4000g)
        assertEquals(initialCuMass, ItemFoundryLadle.getFilledMass(ladle));
        assertEquals(BuiltinMaterials.COPPER.ref(), ItemFoundryLadle.getContainedMaterialRef(ladle));

        ActiveThermalState ladleThermal = ItemFoundryLadle.getThermalState(ladle);
        assertNotNull(ladleThermal);
        assertEquals(1450.0, ladleThermal.temperatureKelvin(), 1.0);
        assertEquals(expectedInitialEnergy, ladleThermal.energyJoules(), 1e-3);

        // Smelter vessel layer was completely extracted into the ladle
        assertEquals(
            0,
            smelter.vesselState()
                .layers()
                .size());
        assertEquals(
            MaterialQuantity.ZERO,
            smelter.vesselState()
                .totalMass());
    }

    @Test
    void testLadlePoursIntoCastingMoldPreservingMassAndProportionalEnergy() {
        ItemStack ladle = new ItemStack(NEItems.foundryLadle);
        MaterialQuantity ladleMass = MaterialQuantity.ofGrams(1200.0);
        ActiveThermalState hotThermal = ActiveThermalState
            .atTemperature(1400.0, 1.0, ladleMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        AggregateLayer initialLayer = new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            BuiltinMaterials.COPPER_COMPOSITION,
            ladleMass,
            hotThermal,
            AggregatePhaseType.MOLTEN_METAL);
        ItemFoundryLadle.fillLadle(ladle, initialLayer);

        ItemStack mold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        MaterialQuantity requiredMoldMass = ComponentGeometry.pickHead()
            .requiredMass(BuiltinMaterials.COPPER_PROPERTIES.density());

        assertTrue(ItemFoundryLadle.pourIntoMold(ladle, mold));
        assertTrue(ItemCastingMold.isFilled(mold));
        assertEquals(requiredMoldMass, ItemCastingMold.getFilledMass(mold));

        // Remaining ladle mass exactly matches subtraction
        MaterialQuantity expectedLadleRemaining = ladleMass.subtract(requiredMoldMass);
        assertEquals(expectedLadleRemaining, ItemFoundryLadle.getFilledMass(ladle));

        // Mold received its exact proportional energy
        ActiveThermalState moldThermal = ItemCastingMold.getThermalState(mold);
        assertNotNull(moldThermal);
        assertEquals(1400.0, moldThermal.temperatureKelvin(), 1.0);
    }

    @Test
    void testLadlePoursBackIntoSmelter() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        ItemStack ladle = new ItemStack(NEItems.foundryLadle);
        MaterialQuantity mass = MaterialQuantity.ofGrams(650.0);
        ActiveThermalState thermal = ActiveThermalState
            .atTemperature(1380.0, 1.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        AggregateLayer layer = new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            BuiltinMaterials.COPPER_COMPOSITION,
            mass,
            thermal,
            AggregatePhaseType.MOLTEN_METAL);
        ItemFoundryLadle.fillLadle(ladle, layer);

        assertTrue(smelter.insertFeed(ladle));
        assertFalse(ItemFoundryLadle.hasMetal(ladle), "Ladle must be emptied when poured back into smelter");
        assertEquals(
            1,
            smelter.vesselState()
                .layers()
                .size());
        assertEquals(
            mass,
            smelter.vesselState()
                .totalMass());
    }

    @Test
    void testLadleEnvironmentalCoolingAndSolidification() {
        ItemStack ladle = new ItemStack(NEItems.foundryLadle);
        MaterialQuantity mass = MaterialQuantity.ofGrams(400.0);
        // Copper barely molten at 1358 K
        ActiveThermalState hotThermal = ActiveThermalState
            .atTemperature(1358.0, 0.5, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        ItemFoundryLadle.fillLadle(
            ladle,
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
                BuiltinMaterials.COPPER_COMPOSITION,
                mass,
                hotThermal,
                AggregatePhaseType.MOLTEN_METAL));

        assertFalse(ItemFoundryLadle.isSolidified(ladle));

        // Simulate 400 ticks of cooling (20 seconds in ambient air)
        ItemFoundryLadle item = (ItemFoundryLadle) ladle.getItem();
        for (int i = 0; i < 400; i++) {
            item.onUpdate(ladle, null, null, 0, true);
        }

        ActiveThermalState cooledThermal = ItemFoundryLadle.getThermalState(ladle);
        assertNotNull(cooledThermal);
        assertTrue(cooledThermal.temperatureKelvin() < 1357.77, "Temperature must fall below melting point");
        assertTrue(ItemFoundryLadle.isSolidified(ladle), "Ladle metal must solidify");

        // Solidified ladle must reject pouring into mold
        ItemStack mold = ItemCastingMold.createStack(GeometryId.PLATE);
        assertFalse(ItemFoundryLadle.pourIntoMold(ladle, mold), "Solidified ladle cannot pour into molds");
    }

    @Test
    void testLadleAutomatedFillingInSmelterSlot() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        MaterialQuantity chargeMass = MaterialQuantity.ofGrams(500.0);
        ActiveThermalState hotThermal = ActiveThermalState
            .atTemperature(1420.0, 1.0, chargeMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        smelter.vesselState()
            .addLayer(
                new AggregateLayer(
                    BuiltinMaterials.COPPER.ref(),
                    CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
                    BuiltinMaterials.COPPER_COMPOSITION,
                    chargeMass,
                    hotThermal,
                    AggregatePhaseType.MOLTEN_METAL));

        ItemStack emptyLadle = new ItemStack(NEItems.foundryLadle);
        smelter.setInventorySlotContents(TileEntityStarterSmelter.SLOT_MOLD, emptyLadle);

        // Update entity does NOT perform magic ladle filling in mold slot
        smelter.updateEntity();

        ItemStack slotLadle = smelter.getStackInSlot(TileEntityStarterSmelter.SLOT_MOLD);
        assertNotNull(slotLadle);
        assertFalse(ItemFoundryLadle.hasMetal(slotLadle), "Ladle in mold slot must not be magically auto-filled");

        // Manual player scooping works directly
        assertTrue(smelter.pourIntoLadle(slotLadle), "Direct manual ladle pouring must succeed");
        assertTrue(ItemFoundryLadle.hasMetal(slotLadle));
        assertEquals(chargeMass, ItemFoundryLadle.getFilledMass(slotLadle));
    }

    @Test
    void testTuyereDraftBoostAndAirMultiplier() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        assertEquals(0, smelter.getForcedDraftTicks());

        smelter.boostForcedDraft(200);
        assertEquals(100, smelter.getForcedDraftTicks());

        // Repeated strokes refresh a short window; they cannot bank minutes of draft.
        smelter.boostForcedDraft(2000);
        assertEquals(100, smelter.getForcedDraftTicks());

        // Test VesselState step with forced draft
        naturalexceptions.core.metallurgy.VesselState vessel = new naturalexceptions.core.metallurgy.VesselState();
        vessel.addFuel(500);
        MaterialQuantity cuMass = MaterialQuantity.ofGrams(300.0);
        vessel.addLayer(
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
                BuiltinMaterials.COPPER_COMPOSITION,
                cuMass,
                ActiveThermalState.ambient(cuMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES),
                AggregatePhaseType.SOLID_CHARGE));

        // Step 100 ticks under standard draft (1.0)
        for (int i = 0; i < 100; i++) {
            vessel.step(1, 293.15, 1.0);
        }
        double tempNatural = vessel.layers()
            .get(0)
            .temperatureKelvin();

        // Step a fresh vessel 100 ticks under boosted draft (1.6)
        naturalexceptions.core.metallurgy.VesselState boostedVessel = new naturalexceptions.core.metallurgy.VesselState();
        boostedVessel.addFuel(500);
        boostedVessel.addLayer(
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
                BuiltinMaterials.COPPER_COMPOSITION,
                cuMass,
                ActiveThermalState.ambient(cuMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES),
                AggregatePhaseType.SOLID_CHARGE));

        for (int i = 0; i < 100; i++) {
            boostedVessel.step(1, 293.15, 1.6);
        }
        double tempBoosted = boostedVessel.layers()
            .get(0)
            .temperatureKelvin();

        assertTrue(
            tempBoosted > tempNatural,
            "Forced draft must heat charge faster: " + tempBoosted + " > " + tempNatural);
        assertTrue(
            boostedVessel.fuelState()
                .remainingTicks()
                < vessel.fuelState()
                    .remainingTicks(),
            "Forced draft must accelerate fuel burn rate (burn more fuel ticks): " + boostedVessel.fuelState()
                .remainingTicks()
                + " < "
                + vessel.fuelState()
                    .remainingTicks());
    }

    @Test
    void testBellowsDraftExpiresWithoutFuelAndDoesNotCreateHeat() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        smelter.boostForcedDraft(100);
        for (int i = 0; i < 100; i++) smelter.updateEntity();
        assertEquals(0, smelter.getForcedDraftTicks());
        assertEquals(293.15, smelter.getHearthTemperature(), 0.001);
        assertFalse(
            smelter.vesselState()
                .fuelState()
                .isBurning());
    }

    @Test
    void testBellowsAcceleratesButConservesTotalFuelEnergy() {
        naturalexceptions.core.metallurgy.FuelBurnState natural = new naturalexceptions.core.metallurgy.FuelBurnState();
        naturalexceptions.core.metallurgy.FuelBurnState forced = new naturalexceptions.core.metallurgy.FuelBurnState();
        natural.addFuel(500);
        forced.addFuel(500);
        double naturalEnergy = 0.0;
        double forcedEnergy = 0.0;
        int naturalTicks = 0;
        int forcedTicks = 0;
        while (natural.isBurning()) {
            naturalEnergy += natural.step(1, 1.0);
            naturalTicks++;
        }
        while (forced.isBurning()) {
            forcedEnergy += forced.step(1, 1.6);
            forcedTicks++;
        }
        assertTrue(forcedTicks < naturalTicks);
        assertEquals(naturalEnergy, forcedEnergy, 0.001);
    }

    @Test
    void testTuyerePlacementPointsBackAtClickedVessel() {
        BlockSmelterTuyere nozzle = new BlockSmelterTuyere();
        for (int side = 2; side <= 5; side++) {
            assertEquals(side ^ 1, nozzle.onBlockPlaced(null, 0, 0, 0, side, 0, 0, 0, 0));
        }
    }

    @Test
    void testSidedInventoryAutomationForLadlesAndMolds() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        ItemStack emptyLadle = new ItemStack(NEItems.foundryLadle);
        ItemStack filledLadle = new ItemStack(NEItems.foundryLadle);
        MaterialQuantity mass = MaterialQuantity.ofGrams(500.0);
        ItemFoundryLadle.fillLadle(
            filledLadle,
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
                BuiltinMaterials.COPPER_COMPOSITION,
                mass,
                ActiveThermalState.atTemperature(1400.0, 1.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES),
                AggregatePhaseType.MOLTEN_METAL));

        ItemStack fullLadle = new ItemStack(NEItems.foundryLadle);
        ItemFoundryLadle.fillLadle(
            fullLadle,
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
                BuiltinMaterials.COPPER_COMPOSITION,
                ItemFoundryLadle.CAPACITY,
                ActiveThermalState.atTemperature(
                    1400.0,
                    1.0,
                    ItemFoundryLadle.CAPACITY.toKilograms(),
                    BuiltinMaterials.COPPER_PROPERTIES),
                AggregatePhaseType.MOLTEN_METAL));

        // Top slot accepts feed
        int[] topSlots = smelter.getAccessibleSlotsFromSide(1);
        assertEquals(1, topSlots.length);
        assertEquals(TileEntityStarterSmelter.SLOT_FEED, topSlots[0]);

        // Bottom slots extract output
        int[] bottomSlots = smelter.getAccessibleSlotsFromSide(0);
        assertTrue(bottomSlots.length >= 2);

        // Ladles cannot be inserted into or extracted from mold slot (automation does not treat ladle as a mold)
        assertFalse(smelter.canInsertItem(TileEntityStarterSmelter.SLOT_MOLD, emptyLadle, 0));
        assertFalse(smelter.canExtractItem(TileEntityStarterSmelter.SLOT_MOLD, emptyLadle, 0));
        assertFalse(smelter.canInsertItem(TileEntityStarterSmelter.SLOT_MOLD, filledLadle, 0));
        assertFalse(smelter.canExtractItem(TileEntityStarterSmelter.SLOT_MOLD, filledLadle, 0));

        // Casting molds are no longer inserted into the smelter: transfer is physical via outlet or ladle.
        ItemStack emptyMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        assertFalse(smelter.canInsertItem(TileEntityStarterSmelter.SLOT_MOLD, emptyMold, 0));
    }

    @Test
    void testLadleCoolingNeverDropsBelowAmbientEquilibrium() {
        ItemStack ladle = new ItemStack(NEItems.foundryLadle);
        MaterialQuantity mass = MaterialQuantity.ofGrams(500.0);
        ActiveThermalState nearAmbient = ActiveThermalState
            .atTemperature(293.20, 0.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        ItemFoundryLadle.fillLadle(
            ladle,
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
                BuiltinMaterials.COPPER_COMPOSITION,
                mass,
                nearAmbient,
                AggregatePhaseType.MOLTEN_METAL));

        double ambientEquilibriumJoules = naturalexceptions.core.thermal.ThermodynamicModel
            .energyForTemperature(293.15, 0.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);

        ItemFoundryLadle item = (ItemFoundryLadle) ladle.getItem();
        for (int i = 0; i < 2000; i++) {
            item.onUpdate(ladle, null, null, 0, true);
        }

        ActiveThermalState cooled = ItemFoundryLadle.getThermalState(ladle);
        assertNotNull(cooled);
        assertTrue(
            cooled.energyJoules() >= ambientEquilibriumJoules - 1e-4,
            "Ladle energy (" + cooled.energyJoules()
                + " J) must not drop below ambient floor ("
                + ambientEquilibriumJoules
                + " J)");
        assertTrue(
            cooled.temperatureKelvin() >= 293.15 - 0.01,
            "Ladle temperature (" + cooled.temperatureKelvin() + " K) must not drop below 293.15 K");
    }
}
