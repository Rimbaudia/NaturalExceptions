package naturalexceptions.minecraft.fabrication;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

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
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemBlacksmithTongs;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

public class TargetDrivenForgingTest {

    @BeforeAll
    static void setUp() {
        try {
            net.minecraft.init.Bootstrap.func_151354_b();
        } catch (Throwable ignored) {}
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    @Test
    void testAnvilTargetGeometryCyclingAndPersistence() {
        TileEntityForgingAnvil anvil = new TileEntityForgingAnvil();
        assertEquals(GeometryId.PLATE, anvil.getTargetGeometry(), "Default target geometry should be PLATE");

        // Cycle through targets
        GeometryId target1 = anvil.cycleTargetGeometry(null);
        assertNotNull(target1);
        assertNotEquals(GeometryId.PLATE, target1);

        anvil.setTargetGeometry(GeometryId.PICK_HEAD);
        assertEquals(GeometryId.PICK_HEAD, anvil.getTargetGeometry());

        // Test NBT persistence
        NBTTagCompound nbt = new NBTTagCompound();
        anvil.writeToNBT(nbt);
        assertEquals("PICK_HEAD", nbt.getString("TargetGeometry"));

        TileEntityForgingAnvil loadedAnvil = new TileEntityForgingAnvil();
        loadedAnvil.readFromNBT(nbt);
        assertEquals(GeometryId.PICK_HEAD, loadedAnvil.getTargetGeometry());
    }

    @Test
    void testTargetDrivenForgingPickHeadProgression() {
        TileEntityForgingAnvil anvil = new TileEntityForgingAnvil();
        anvil.setTargetGeometry(GeometryId.PICK_HEAD);

        MaterialQuantity mass = MaterialQuantity.ofGrams(300.0);
        // Copper hot forging heat at 950 K (> 611 K T_rx)
        ActiveThermalState hotThermal = ActiveThermalState
            .atTemperature(950.0, 0.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);

        ComponentInstance blankComp = new ComponentInstance(
            GeometryId.BLANK,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            mass,
            ManufacturingState.AS_CAST);
        ItemStack workpiece = ItemGenericComponent.createStack(blankComp);
        ItemGenericComponent.saveThermalState(workpiece, hotThermal);
        anvil.setWorkpiece(workpiece);

        // Plain hammer without manual mode tags
        ItemStack hammer = new ItemStack(NEItems.blacksmithHammer);

        // STRIKE 1: Target PICK_HEAD drives BLANK -> ROD (Drawing out)
        assertTrue(anvil.hammerStrike(null, hammer));
        ComponentInstance comp1 = ItemGenericComponent.getComponent(anvil.getWorkpiece());
        assertNotNull(comp1);
        assertEquals(GeometryId.ROD, comp1.geometryId(), "Strike 1 should draw out Blank into Rod");
        assertEquals(
            Microstructure.ANNEALED,
            comp1.stateKey()
                .microstructure());
        assertEquals(
            ConditioningHistory.HOT_ROLLED,
            comp1.stateKey()
                .conditioning());
        assertEquals(ManufacturingState.WORKED, comp1.manufacturingState());

        // STRIKE 2: Target PICK_HEAD drives ROD -> PICK_HEAD (Beveling / Shaping)
        assertTrue(anvil.hammerStrike(null, hammer));
        ComponentInstance comp2 = ItemGenericComponent.getComponent(anvil.getWorkpiece());
        assertNotNull(comp2);
        assertEquals(GeometryId.PICK_HEAD, comp2.geometryId(), "Strike 2 should bevel Rod into Pick Head");
        assertEquals(ManufacturingState.WORKED, comp2.manufacturingState());

        // STRIKE 3: Target PICK_HEAD planishes PICK_HEAD -> FINISHED
        assertTrue(anvil.hammerStrike(null, hammer));
        ComponentInstance comp3 = ItemGenericComponent.getComponent(anvil.getWorkpiece());
        assertNotNull(comp3);
        assertEquals(GeometryId.PICK_HEAD, comp3.geometryId());
        assertEquals(ManufacturingState.FINISHED, comp3.manufacturingState(), "Strike 3 should finish the component");

        // STRIKE 4: Subsequent strike on finished piece recognizes completion cleanly
        assertTrue(anvil.hammerStrike(null, hammer));
        ComponentInstance comp4 = ItemGenericComponent.getComponent(anvil.getWorkpiece());
        assertEquals(ManufacturingState.FINISHED, comp4.manufacturingState());
        assertFalse(
            comp4.damage()
                .isDamaged(),
            "Hot forging must not damage finished piece");
    }

    @Test
    void testTargetDrivenForgingBladeProgression() {
        TileEntityForgingAnvil anvil = new TileEntityForgingAnvil();
        anvil.setTargetGeometry(GeometryId.BLADE);

        MaterialQuantity mass = MaterialQuantity.ofGrams(250.0);
        ActiveThermalState hotThermal = ActiveThermalState
            .atTemperature(950.0, 0.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);

        ComponentInstance blankComp = new ComponentInstance(
            GeometryId.BLANK,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            mass,
            ManufacturingState.AS_CAST);
        ItemStack workpiece = ItemGenericComponent.createStack(blankComp);
        ItemGenericComponent.saveThermalState(workpiece, hotThermal);
        anvil.setWorkpiece(workpiece);

        ItemStack hammer = new ItemStack(NEItems.blacksmithHammer);

        // Strike 1: BLANK -> PLATE (Flattening)
        assertTrue(anvil.hammerStrike(null, hammer));
        ComponentInstance comp1 = ItemGenericComponent.getComponent(anvil.getWorkpiece());
        assertEquals(GeometryId.PLATE, comp1.geometryId());

        // Strike 2: PLATE -> BLADE (Beveling)
        assertTrue(anvil.hammerStrike(null, hammer));
        ComponentInstance comp2 = ItemGenericComponent.getComponent(anvil.getWorkpiece());
        assertEquals(GeometryId.BLADE, comp2.geometryId());

        // Strike 3: BLADE -> FINISHED (Planishing)
        assertTrue(anvil.hammerStrike(null, hammer));
        ComponentInstance comp3 = ItemGenericComponent.getComponent(anvil.getWorkpiece());
        assertEquals(GeometryId.BLADE, comp3.geometryId());
        assertEquals(ManufacturingState.FINISHED, comp3.manufacturingState());
    }

    @Test
    void testColdWorkingTargetDrivenForgingAccumulatesStrainAndHardening() {
        TileEntityForgingAnvil anvil = new TileEntityForgingAnvil();
        anvil.setTargetGeometry(GeometryId.PICK_HEAD);

        MaterialQuantity mass = MaterialQuantity.ofGrams(300.0);
        // Cold metal at ambient 293.15 K (< 611 K T_rx)
        ActiveThermalState ambientThermal = ActiveThermalState
            .atTemperature(293.15, 0.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);

        ComponentInstance blankComp = new ComponentInstance(
            GeometryId.BLANK,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            mass,
            ManufacturingState.AS_CAST);
        ItemStack workpiece = ItemGenericComponent.createStack(blankComp);
        ItemGenericComponent.saveThermalState(workpiece, ambientThermal);
        anvil.setWorkpiece(workpiece);

        ItemStack hammer = new ItemStack(NEItems.blacksmithHammer);

        // Strike cold metal
        assertTrue(anvil.hammerStrike(null, hammer));
        ComponentInstance coldComp = ItemGenericComponent.getComponent(anvil.getWorkpiece());
        assertNotNull(coldComp);
        assertEquals(GeometryId.ROD, coldComp.geometryId());

        // Physical simulation asserts: cold work induces strain hardening
        assertEquals(
            Microstructure.WORK_HARDENED,
            coldComp.stateKey()
                .microstructure());
        assertEquals(
            ConditioningHistory.COLD_WORKED,
            coldComp.stateKey()
                .conditioning());
        assertEquals(ManufacturingState.COLD_WORKED, coldComp.manufacturingState());
        assertTrue(
            coldComp.damage()
                .deformation() > 0.0,
            "Cold working must accumulate deformation");
        assertTrue(
            coldComp.damage()
                .cracking() > 0.0,
            "Cold working must accumulate microcracks");
    }

    @Test
    void testThermalConditionBands() {
        var props = BuiltinMaterials.COPPER_PROPERTIES;
        // T_rx = ~611 K, T_melt = ~1357.77 K

        assertEquals(
            ItemGenericComponent.ThermalCondition.COLD,
            ItemGenericComponent.getThermalCondition(293.15, props));
        assertEquals(
            ItemGenericComponent.ThermalCondition.WARM,
            ItemGenericComponent.getThermalCondition(330.0, props));
        assertEquals(
            ItemGenericComponent.ThermalCondition.STIFF_COLD_WORK,
            ItemGenericComponent.getThermalCondition(450.0, props));
        assertEquals(
            ItemGenericComponent.ThermalCondition.WORKABLE,
            ItemGenericComponent.getThermalCondition(580.0, props));
        assertEquals(
            ItemGenericComponent.ThermalCondition.IDEAL_FORGE,
            ItemGenericComponent.getThermalCondition(900.0, props));
        assertEquals(
            ItemGenericComponent.ThermalCondition.NEAR_MELTING,
            ItemGenericComponent.getThermalCondition(1300.0, props));
        assertEquals(
            ItemGenericComponent.ThermalCondition.MOLTEN,
            ItemGenericComponent.getThermalCondition(1400.0, props));
    }

    @Test
    void testTooltipProgressiveDisclosure() {
        ComponentInstance comp = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            MaterialQuantity.ofGrams(250.0),
            ManufacturingState.FINISHED);
        ItemStack stack = ItemGenericComponent.createStack(comp);

        ActiveThermalState hotThermal = ActiveThermalState
            .atTemperature(920.0, 0.0, 0.25, BuiltinMaterials.COPPER_PROPERTIES);
        ItemGenericComponent.saveThermalState(stack, hotThermal);

        // Standard tooltip (advanced = false)
        List<String> normalInfo = new ArrayList<>();
        stack.getItem()
            .addInformation(stack, null, normalInfo, false);

        String fullNormal = String.join("\n", normalInfo);
        assertTrue(fullNormal.contains("Pick Head"), "Should include readable geometry name");
        assertTrue(fullNormal.contains("material"), "Should include coarse material description");
        assertTrue(fullNormal.contains("Ideal Forge Heat"), "Should include qualitative heat label");
        assertTrue(fullNormal.contains("Planished"), "Should include finish label");
        assertFalse(
            fullNormal.contains("--- Physical Diagnostics ---"),
            "Should not show advanced diagnostics by default");
        assertTrue(fullNormal.contains("[Hold SHIFT for physical diagnostics]"), "Should show shift prompt");

        // Advanced tooltip (advanced = true)
        List<String> advancedInfo = new ArrayList<>();
        stack.getItem()
            .addInformation(stack, null, advancedInfo, true);

        String fullAdvanced = String.join("\n", advancedInfo);
        assertTrue(
            fullAdvanced.contains("--- Developer diagnostics"),
            "Advanced view should include diagnostics header");
        assertTrue(fullAdvanced.contains("Temperature: 920.0 K"), "Advanced view should include exact Kelvin");
        assertTrue(fullAdvanced.contains("Thresholds: T_rx"), "Advanced view should include physical thresholds");
    }

    @Test
    void testTongsFluentTransfersAcrossStations() {
        TileEntityForgingAnvil anvil = new TileEntityForgingAnvil();
        ItemStack tongs = new ItemStack(NEItems.blacksmithTongs);

        ComponentInstance comp = new ComponentInstance(
            GeometryId.BLANK,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            MaterialQuantity.ofGrams(250.0),
            ManufacturingState.AS_CAST);
        ItemStack workpiece = ItemGenericComponent.createStack(comp);

        // Put workpiece in tongs
        ItemBlacksmithTongs.setWorkpiece(tongs, workpiece);
        assertTrue(ItemBlacksmithTongs.hasWorkpiece(tongs));

        // Place onto empty anvil with tongs
        assertTrue(anvil.tongsInteract(null, tongs));
        assertFalse(ItemBlacksmithTongs.hasWorkpiece(tongs), "Tongs must be empty after placing on anvil");
        assertNotNull(anvil.getWorkpiece(), "Anvil must now hold workpiece");

        // Grab back from anvil with empty tongs
        assertTrue(anvil.tongsInteract(null, tongs));
        assertNull(anvil.getWorkpiece(), "Anvil must be empty after grab");
        assertTrue(ItemBlacksmithTongs.hasWorkpiece(tongs), "Tongs must now hold workpiece");
    }
}
