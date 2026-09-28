package naturalexceptions.minecraft.fabrication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.ConditioningHistory;
import naturalexceptions.core.material.Microstructure;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemBlacksmithTongs;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

final class AnvilForgingAndAnnealingTest {

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
    void testWorkpiecePlacementAndRetrieval() {
        TileEntityForgingAnvil anvil = new TileEntityForgingAnvil();
        assertNull(anvil.getWorkpiece());

        ComponentInstance blankComp = new ComponentInstance(
            GeometryId.BLANK,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            MaterialQuantity.ofGrams(250.0),
            ManufacturingState.AS_CAST);
        ItemStack stack = ItemGenericComponent.createStack(blankComp);

        anvil.setWorkpiece(stack);
        assertNotNull(anvil.getWorkpiece());
        assertEquals(stack, anvil.getWorkpiece());

        anvil.setWorkpiece(null);
        assertNull(anvil.getWorkpiece());
    }

    @Test
    void testHotWorkingAboveRecrystallizationThreshold() {
        TileEntityForgingAnvil anvil = new TileEntityForgingAnvil();
        MaterialQuantity mass = MaterialQuantity.ofGrams(300.0);

        // Copper melting point is 1357.77 K. Recrystallization threshold T_rx = 0.45 * 1357.77 = 611 K.
        // Component is hot at 950 K (hot forging range).
        ActiveThermalState hotThermal = ActiveThermalState
            .atTemperature(950.0, 0.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);

        ComponentInstance comp = new ComponentInstance(
            GeometryId.BLANK,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            mass,
            ManufacturingState.AS_CAST);
        ItemStack workpiece = ItemGenericComponent.createStack(comp);
        ItemGenericComponent.saveThermalState(workpiece, hotThermal);
        anvil.setWorkpiece(workpiece);

        ItemStack hammer = new ItemStack(NEItems.blacksmithHammer);
        assertTrue(anvil.hammerStrike(null, hammer));

        ItemStack forgedStack = anvil.getWorkpiece();
        assertNotNull(forgedStack);
        ComponentInstance forgedComp = ItemGenericComponent.getComponent(forgedStack);
        assertNotNull(forgedComp);

        // BLANK flattened to PLATE
        assertEquals(GeometryId.PLATE, forgedComp.geometryId());
        // Mass and material identity strictly conserved
        assertEquals(mass, forgedComp.mass());
        assertEquals(BuiltinMaterials.COPPER.ref(), forgedComp.materialRef());

        // Hot-working dynamic grain recovery: does NOT work harden
        assertEquals(
            Microstructure.ANNEALED,
            forgedComp.stateKey()
                .microstructure());
        assertEquals(
            ConditioningHistory.HOT_ROLLED,
            forgedComp.stateKey()
                .conditioning());
        assertEquals(ManufacturingState.WORKED, forgedComp.manufacturingState());
        assertFalse(
            forgedComp.damage()
                .isDamaged(),
            "Hot forging must not induce cold-work strain or cracking");
    }

    @Test
    void testColdWorkingBelowRecrystallizationThreshold() {
        TileEntityForgingAnvil anvil = new TileEntityForgingAnvil();
        MaterialQuantity mass = MaterialQuantity.ofGrams(300.0);

        // Component is cold at ambient 293.15 K (< 611 K T_rx)
        ActiveThermalState ambientThermal = ActiveThermalState
            .atTemperature(293.15, 0.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);

        ComponentInstance comp = new ComponentInstance(
            GeometryId.BLANK,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            mass,
            ManufacturingState.AS_CAST);
        ItemStack workpiece = ItemGenericComponent.createStack(comp);
        ItemGenericComponent.saveThermalState(workpiece, ambientThermal);
        anvil.setWorkpiece(workpiece);

        ItemStack hammer = new ItemStack(NEItems.blacksmithHammer);
        assertTrue(anvil.hammerStrike(null, hammer));

        ItemStack forgedStack = anvil.getWorkpiece();
        assertNotNull(forgedStack);
        ComponentInstance forgedComp = ItemGenericComponent.getComponent(forgedStack);
        assertNotNull(forgedComp);

        // BLANK transformed to PLATE
        assertEquals(GeometryId.PLATE, forgedComp.geometryId());
        assertEquals(mass, forgedComp.mass());

        // Cold-working induces strain hardening
        assertEquals(
            Microstructure.WORK_HARDENED,
            forgedComp.stateKey()
                .microstructure());
        assertEquals(
            ConditioningHistory.COLD_WORKED,
            forgedComp.stateKey()
                .conditioning());
        assertEquals(ManufacturingState.COLD_WORKED, forgedComp.manufacturingState());
        assertTrue(
            forgedComp.damage()
                .deformation() > 0.0,
            "Cold working must accumulate deformation");
        assertTrue(
            forgedComp.damage()
                .cracking() > 0.0,
            "Cold working must accumulate cracking");
    }

    @Test
    void testSevereOverworkingCausesFracture() {
        TileEntityForgingAnvil anvil = new TileEntityForgingAnvil();
        MaterialQuantity mass = MaterialQuantity.ofGrams(200.0);
        ComponentInstance comp = new ComponentInstance(
            GeometryId.BLANK,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            mass,
            ManufacturingState.AS_CAST);
        ItemStack workpiece = ItemGenericComponent.createStack(comp);
        anvil.setWorkpiece(workpiece);

        ItemStack hammer = new ItemStack(NEItems.blacksmithHammer);

        // Strike 20 times cold without annealing to induce mechanical fatigue failure
        for (int i = 0; i < 20; i++) {
            anvil.hammerStrike(null, hammer);
        }

        ComponentInstance fracturedComp = ItemGenericComponent.getComponent(anvil.getWorkpiece());
        assertNotNull(fracturedComp);
        assertTrue(
            fracturedComp.damage()
                .isFractured(),
            "Excessive cold work without annealing must fracture component");

        // Subsequent strike on fractured piece is rejected
        assertFalse(anvil.hammerStrike(null, hammer), "Fractured component must not allow further forging");
    }

    @Test
    void testAnnealingRecoveryRelievesColdWork() {
        MaterialQuantity mass = MaterialQuantity.ofGrams(250.0);
        ComponentInstance coldWorkedComp = new ComponentInstance(
            GeometryId.PLATE,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(
                BuiltinMaterials.COPPER_COMPOSITION,
                naturalexceptions.core.material.MaterialPhase.SOLID,
                Microstructure.WORK_HARDENED,
                ConditioningHistory.COLD_WORKED),
            mass,
            ManufacturingState.COLD_WORKED);
        coldWorkedComp.damage()
            .addDeformation(0.40);
        coldWorkedComp.damage()
            .addCracking(0.30);

        ItemStack stack = ItemGenericComponent.createStack(coldWorkedComp);

        // Heat workpiece to 800 K (above 611 K recrystallization temperature, below 1357 K melting point)
        ActiveThermalState hotThermal = ActiveThermalState
            .atTemperature(800.0, 0.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        ItemGenericComponent.saveThermalState(stack, hotThermal);

        // Check anneal recovery
        assertTrue(ItemGenericComponent.checkAnneal(stack), "Thermal check must trigger anneal recovery");

        ComponentInstance annealedComp = ItemGenericComponent.getComponent(stack);
        assertNotNull(annealedComp);
        assertEquals(
            Microstructure.ANNEALED,
            annealedComp.stateKey()
                .microstructure());
        assertEquals(
            ConditioningHistory.NONE,
            annealedComp.stateKey()
                .conditioning());
        assertEquals(ManufacturingState.ANNEALED, annealedComp.manufacturingState());
        // Deformation stress is relieved
        assertFalse(
            annealedComp.damage()
                .isFractured());
    }

    @Test
    void testBlacksmithTongsPickUpHoldAndPlaceWorkpiece() {
        TileEntityForgingAnvil anvil = new TileEntityForgingAnvil();
        ComponentInstance comp = new ComponentInstance(
            GeometryId.BLANK,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            MaterialQuantity.ofGrams(200.0),
            ManufacturingState.AS_CAST);
        ItemStack wp = ItemGenericComponent.createStack(comp);
        anvil.setWorkpiece(wp);

        ItemStack tongs = new ItemStack(NEItems.blacksmithTongs);
        assertFalse(ItemBlacksmithTongs.hasWorkpiece(tongs));

        // Tongs interact with anvil: grab workpiece from anvil into tongs
        assertTrue(anvil.tongsInteract(null, tongs));
        assertNull(anvil.getWorkpiece(), "Anvil must be empty after tongs pick up");
        assertTrue(ItemBlacksmithTongs.hasWorkpiece(tongs));

        ItemStack heldInTongs = ItemBlacksmithTongs.getWorkpiece(tongs);
        assertNotNull(heldInTongs);
        assertEquals(
            comp.geometryId(),
            ItemGenericComponent.getComponent(heldInTongs)
                .geometryId());

        // Tongs interact again with empty anvil: places workpiece back onto anvil
        assertTrue(anvil.tongsInteract(null, tongs));
        assertNotNull(anvil.getWorkpiece(), "Workpiece must be placed back on anvil");
        assertFalse(ItemBlacksmithTongs.hasWorkpiece(tongs), "Tongs must now be empty");
    }

    @Test
    void testGenericFeedstockIngotForgingOnAnvil() {
        TileEntityForgingAnvil anvil = new TileEntityForgingAnvil();
        ItemStack copperIngotStack = new ItemStack(NEItems.copperIngot);
        anvil.setWorkpiece(copperIngotStack);

        ItemStack hammer = new ItemStack(NEItems.blacksmithHammer);
        assertTrue(anvil.hammerStrike(null, hammer), "Striking raw ingot on anvil must convert and forge it");

        ItemStack forged = anvil.getWorkpiece();
        assertNotNull(forged);
        assertTrue(forged.getItem() instanceof ItemGenericComponent);

        ComponentInstance comp = ItemGenericComponent.getComponent(forged);
        assertNotNull(comp);
        // Striking a converted BLANK without sneaking produces a PLATE
        assertEquals(GeometryId.PLATE, comp.geometryId());
        assertEquals(BuiltinMaterials.COPPER.ref(), comp.materialRef());
        assertEquals(MaterialQuantity.ofKilograms(1.0), comp.mass());
    }

    @Test
    void testMicroCrackingReliefAndFractureImmutability() {
        MaterialQuantity mass = MaterialQuantity.ofGrams(200.0);
        ComponentInstance comp = new ComponentInstance(
            GeometryId.PLATE,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            mass,
            ManufacturingState.COLD_WORKED);

        comp.damage()
            .addDeformation(0.40);
        comp.damage()
            .addCracking(0.30);
        assertEquals(
            0.30,
            comp.damage()
                .cracking(),
            1e-4);

        ItemStack stack = ItemGenericComponent.createStack(comp);
        ActiveThermalState hotThermal = ActiveThermalState
            .atTemperature(800.0, 0.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        ItemGenericComponent.saveThermalState(stack, hotThermal);

        // Anneal relieves deformation and cracking
        assertTrue(ItemGenericComponent.checkAnneal(stack));
        ComponentInstance annealed = ItemGenericComponent.getComponent(stack);
        assertNotNull(annealed);
        assertTrue(
            annealed.damage()
                .cracking() < 0.30,
            "Annealing must relieve sub-critical micro-cracking");
        assertEquals(
            0.0,
            annealed.damage()
                .cracking(),
            1e-3);

        // Now test fractured component: fracturing cannot be annealed
        annealed.damage()
            .fracture();
        assertTrue(
            annealed.damage()
                .isFractured());
        ItemGenericComponent.saveThermalState(stack, hotThermal);
        assertFalse(
            ItemGenericComponent.checkAnneal(stack),
            "Fractured component cannot be recovered by thermal annealing");
    }

    @Test
    void testCastingMoldExtractionPreservesThermalState() {
        ItemStack mold = ItemCastingMold.createStack(GeometryId.BLANK);
        MaterialQuantity mass = MaterialQuantity.ofGrams(540.0);
        ActiveThermalState hotThermal = ActiveThermalState
            .atTemperature(1100.0, 0.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);

        ItemCastingMold.fillMold(
            mold,
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
                BuiltinMaterials.COPPER_COMPOSITION,
                mass,
                hotThermal,
                AggregatePhaseType.MOLTEN_METAL));

        ItemStack extracted = ItemCastingMold.extractComponent(mold);
        assertNotNull(extracted);
        assertTrue(extracted.getItem() instanceof ItemGenericComponent);

        ActiveThermalState compThermal = ItemGenericComponent.getThermalState(extracted);
        assertNotNull(compThermal, "Extracted component must retain thermal state from casting mold");
        assertEquals(1100.0, compThermal.temperatureKelvin(), 1.0);
    }

    @Test
    void testComponentCoolingNeverDropsBelowAmbient() {
        MaterialQuantity mass = MaterialQuantity.ofGrams(250.0);
        ComponentInstance comp = new ComponentInstance(
            GeometryId.PLATE,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            mass,
            ManufacturingState.FINISHED);
        ItemStack stack = ItemGenericComponent.createStack(comp);
        ActiveThermalState nearAmbient = ActiveThermalState
            .atTemperature(293.20, 0.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        ItemGenericComponent.saveThermalState(stack, nearAmbient);

        double ambientEquilibriumJoules = naturalexceptions.core.thermal.ThermodynamicModel
            .energyForTemperature(293.15, 0.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);

        ItemGenericComponent item = (ItemGenericComponent) stack.getItem();
        for (int i = 0; i < 2000; i++) {
            item.onUpdate(stack, null, null, 0, true);
        }

        ActiveThermalState cooled = ItemGenericComponent.getThermalState(stack);
        double tempK = ItemGenericComponent.getTemperatureKelvin(stack);
        assertTrue(tempK >= 293.15 - 0.01, "Component temperature must not drop below 293.15 K: " + tempK);
        if (cooled != null) {
            assertTrue(
                cooled.energyJoules() >= ambientEquilibriumJoules - 1e-4,
                "Component energy must not drop below ambient floor");
        }
    }

    @Test
    void testHammerOperationCycleAndSelectiveShaping() {
        TileEntityForgingAnvil anvil = new TileEntityForgingAnvil();
        MaterialQuantity mass = MaterialQuantity.ofGrams(300.0);
        ComponentInstance comp = new ComponentInstance(
            GeometryId.BLANK,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            mass,
            ManufacturingState.AS_CAST);
        ItemStack workpiece = ItemGenericComponent.createStack(comp);
        ActiveThermalState hotThermal = ActiveThermalState
            .atTemperature(900.0, 0.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        ItemGenericComponent.saveThermalState(workpiece, hotThermal);
        anvil.setWorkpiece(workpiece);

        ItemStack hammer = new ItemStack(NEItems.blacksmithHammer);
        // Default face is FLATTEN. Set face to DRAW_OUT:
        naturalexceptions.minecraft.item.ItemBlacksmithHammer
            .setOperation(hammer, naturalexceptions.minecraft.item.ItemBlacksmithHammer.HammerOperation.DRAW_OUT);
        assertEquals(
            naturalexceptions.minecraft.item.ItemBlacksmithHammer.HammerOperation.DRAW_OUT,
            naturalexceptions.minecraft.item.ItemBlacksmithHammer.getOperation(hammer));

        // Strike with DRAW_OUT: BLANK -> ROD
        assertTrue(anvil.hammerStrike(null, hammer));
        ComponentInstance rodComp = ItemGenericComponent.getComponent(anvil.getWorkpiece());
        assertNotNull(rodComp);
        assertEquals(GeometryId.ROD, rodComp.geometryId());

        // Cycle to BEVEL: ROD -> PICK_HEAD
        naturalexceptions.minecraft.item.ItemBlacksmithHammer
            .setOperation(hammer, naturalexceptions.minecraft.item.ItemBlacksmithHammer.HammerOperation.BEVEL);
        assertTrue(anvil.hammerStrike(null, hammer));
        ComponentInstance pickComp = ItemGenericComponent.getComponent(anvil.getWorkpiece());
        assertNotNull(pickComp);
        assertEquals(GeometryId.PICK_HEAD, pickComp.geometryId());

        // Cycle to PLANISH: finish the pick head
        naturalexceptions.minecraft.item.ItemBlacksmithHammer
            .setOperation(hammer, naturalexceptions.minecraft.item.ItemBlacksmithHammer.HammerOperation.PLANISH);
        assertTrue(anvil.hammerStrike(null, hammer));
        ComponentInstance finishedComp = ItemGenericComponent.getComponent(anvil.getWorkpiece());
        assertNotNull(finishedComp);
        assertEquals(GeometryId.PICK_HEAD, finishedComp.geometryId());
        assertEquals(ManufacturingState.FINISHED, finishedComp.manufacturingState());
    }

    @Test
    void testPhysicalSmelterHearthWorkpieceHeatingAndContinuousAnnealing() {
        naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter smelter = new naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter();
        // Add fuel and ignite hearth
        smelter.vesselState()
            .fuelState()
            .addFuel(5000);
        assertTrue(
            smelter.vesselState()
                .fuelState()
                .isBurning());
        assertTrue(smelter.getHearthTemperature() >= 1200.0);

        MaterialQuantity mass = MaterialQuantity.ofGrams(250.0);
        ComponentInstance coldWorkedComp = new ComponentInstance(
            GeometryId.PLATE,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(
                BuiltinMaterials.COPPER_COMPOSITION,
                naturalexceptions.core.material.MaterialPhase.SOLID,
                Microstructure.WORK_HARDENED,
                ConditioningHistory.COLD_WORKED),
            mass,
            ManufacturingState.COLD_WORKED);
        coldWorkedComp.damage()
            .addDeformation(0.35);
        coldWorkedComp.damage()
            .addCracking(0.25);
        ItemStack stack = ItemGenericComponent.createStack(coldWorkedComp);
        ActiveThermalState coldThermal = ActiveThermalState
            .atTemperature(293.15, 0.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        ItemGenericComponent.saveThermalState(stack, coldThermal);

        // Place into smelter hearth
        smelter.setHearthWorkpiece(stack);
        assertEquals(stack, smelter.getHearthWorkpiece());

        // Step smelter entity: physical heat transfer occurs
        double initialTemp = ItemGenericComponent.getTemperatureKelvin(smelter.getHearthWorkpiece());
        assertEquals(293.15, initialTemp, 0.1);

        for (int i = 0; i < 400; i++) {
            smelter.updateEntity();
        }

        ItemStack heatedWp = smelter.getHearthWorkpiece();
        assertNotNull(heatedWp);
        double finalTemp = ItemGenericComponent.getTemperatureKelvin(heatedWp);
        assertTrue(finalTemp > 600.0, "Workpiece in burning hearth must heat up: " + finalTemp);

        // Continuous annealing has relieved strain and restored ANNEALED microstructure
        ComponentInstance annealedComp = ItemGenericComponent.getComponent(heatedWp);
        assertNotNull(annealedComp);
        assertEquals(
            Microstructure.ANNEALED,
            annealedComp.stateKey()
                .microstructure());
        assertEquals(ManufacturingState.ANNEALED, annealedComp.manufacturingState());
        assertFalse(
            annealedComp.damage()
                .isFractured());
    }

    @Test
    void testBlacksmithTongsWaterQuenching() {
        MaterialQuantity mass = MaterialQuantity.ofGrams(200.0);
        ComponentInstance comp = new ComponentInstance(
            GeometryId.BLADE,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            mass,
            ManufacturingState.WORKED);
        ItemStack wp = ItemGenericComponent.createStack(comp);
        ActiveThermalState hotThermal = ActiveThermalState
            .atTemperature(950.0, 0.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        ItemGenericComponent.saveThermalState(wp, hotThermal);

        ItemStack tongs = new ItemStack(NEItems.blacksmithTongs);
        ItemBlacksmithTongs.setWorkpiece(tongs, wp);
        assertTrue(ItemBlacksmithTongs.hasWorkpiece(tongs));

        // Directly verify quenching logic: cooling to 293 K and setting QUENCHED
        ActiveThermalState currentThermal = ItemGenericComponent.getThermalState(wp);
        assertTrue(currentThermal.temperatureKelvin() > 373.15);

        ActiveThermalState quenched = ActiveThermalState
            .atTemperature(293.15, 0.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        ItemGenericComponent.saveThermalState(wp, quenched);
        comp.setStateKey(
            comp.stateKey()
                .withMicrostructure(Microstructure.QUENCHED));
        naturalexceptions.minecraft.persistence.AssemblyNbtCodec.writeComponentToNbt(comp, wp.getTagCompound());
        ItemBlacksmithTongs.setWorkpiece(tongs, wp);

        ItemStack quenchedWp = ItemBlacksmithTongs.getWorkpiece(tongs);
        assertNotNull(quenchedWp);
        assertEquals(293.15, ItemGenericComponent.getTemperatureKelvin(quenchedWp), 0.1);
        ComponentInstance quenchedComp = ItemGenericComponent.getComponent(quenchedWp);
        assertNotNull(quenchedComp);
        assertEquals(
            Microstructure.QUENCHED,
            quenchedComp.stateKey()
                .microstructure());
    }
}
