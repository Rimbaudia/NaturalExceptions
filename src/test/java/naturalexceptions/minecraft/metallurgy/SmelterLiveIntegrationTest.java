package naturalexceptions.minecraft.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentGeometry;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.metallurgy.OreFeedstockDefinition;
import naturalexceptions.core.metallurgy.VesselState;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.persistence.VesselStateNbtCodec;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

final class SmelterLiveIntegrationTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    @Test
    void normalCopperChargeDoesNotRunAwayThermally() {
        VesselState vessel = new VesselState();
        // Add realistic copper feed: 540g copper
        MaterialQuantity cuMass = MaterialQuantity.ofGrams(540.0);
        ActiveThermalState ambientThermal = ActiveThermalState
            .ambient(cuMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        AggregateLayer cuLayer = new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper")),
            cuMass,
            ambientThermal,
            AggregatePhaseType.SOLID_CHARGE);
        vessel.addLayer(cuLayer);

        // Burn fuel for 2000 continuous ticks (100 seconds)
        vessel.addFuel(2000);
        for (int i = 0; i < 2000; i++) {
            vessel.step(1, 293.15);
        }

        AggregateLayer metal = vessel.layers()
            .get(0);
        assertEquals(AggregatePhaseType.MOLTEN_METAL, metal.phaseType());
        assertEquals(MaterialPhase.LIQUID, metal.physicalPhase());

        double finalTemp = metal.temperatureKelvin();
        // Operating temperature must be well above melting point (1357.8 K)
        assertTrue(finalTemp > 1357.77, "Copper must be fully molten: T = " + finalTemp);
        // Operating temperature must NOT run away to absurd values (previously ~32,000 K)
        // Must stabilize bounded by flame temperature (~1750 K) and radiant equilibrium ~1450-1500 K
        assertTrue(finalTemp < 1550.0, "Copper must not run away: T = " + finalTemp + " K (must be < 1550 K)");
        assertTrue(finalTemp >= 1400.0, "Copper must reach normal working superheat: T = " + finalTemp + " K");
    }

    @Test
    void smelterEnvironmentRemovesHeatWhenHot() {
        VesselState vessel = new VesselState();
        MaterialQuantity cuMass = MaterialQuantity.ofGrams(100.0);
        ActiveThermalState hotThermal = ActiveThermalState
            .atTemperature(1450.0, 1.0, cuMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        AggregateLayer cuLayer = new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper")))
                .asMolten(),
            CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper")),
            cuMass,
            hotThermal,
            AggregatePhaseType.MOLTEN_METAL);
        vessel.addLayer(cuLayer);

        double initialEnergy = cuLayer.thermalState()
            .energyJoules();
        // Step with 0 fuel for 100 ticks (5 seconds)
        vessel.step(100, 293.15);

        double finalEnergy = cuLayer.thermalState()
            .energyJoules();
        assertTrue(finalEnergy < initialEnergy, "Hot vessel must dissipate energy to environment");
        assertTrue(cuLayer.temperatureKelvin() < 1450.0, "Vessel temperature must decrease during cooling");
    }

    @Test
    void outletTransfersExactMaterialMassAndThermalEnergy() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        // Setup molten copper in smelter
        MaterialQuantity cuMass = MaterialQuantity.ofGrams(500.0);
        ActiveThermalState moltenThermal = ActiveThermalState
            .atTemperature(1450.0, 1.0, cuMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        AggregateLayer cuLayer = new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper")))
                .asMolten(),
            CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper")),
            cuMass,
            moltenThermal,
            AggregatePhaseType.MOLTEN_METAL);
        smelter.vesselState()
            .addLayer(cuLayer);

        // Prepare empty Pick Head mold (requires 30 cm³ * 8.96 g/cm³ = 268.8 g)
        ItemStack pickMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        MaterialQuantity vesselMassBefore = smelter.vesselState()
            .totalMass();

        boolean success = smelter.pourIntoMold(pickMold);
        assertTrue(success, "Pouring into pick head mold must succeed");

        // Verify mold state
        assertTrue(ItemCastingMold.isFilled(pickMold), "Mold must be marked filled");
        MaterialQuantity pouredMass = ItemCastingMold.getFilledMass(pickMold);
        assertEquals(268.8, pouredMass.toGrams(), 0.5);

        // Vessel mass decreased by exactly poured mass
        assertEquals(
            vesselMassBefore.subtract(pouredMass),
            smelter.vesselState()
                .totalMass());

        // Thermal state carried into mold
        ActiveThermalState moldThermal = ItemCastingMold.getThermalState(pickMold);
        assertNotNull(moldThermal);
        assertEquals(1450.0, moldThermal.temperatureKelvin(), 1.0);
        assertEquals(MaterialPhase.LIQUID, moldThermal.phase());
    }

    @Test
    void outletCannotPourNonLiquidLayer() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        // Cold unmelted solid charge in smelter
        smelter.vesselState()
            .addOreChunk(OreFeedstockDefinition.copperOreChunk());

        ItemStack pickMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        boolean success = smelter.pourIntoMold(pickMold);
        assertFalse(success, "Cannot pour when only solid charge exists in smelter");
        assertFalse(ItemCastingMold.isFilled(pickMold), "Mold must remain empty");
    }

    @Test
    void skimRemovesActualSlagMass() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        // Setup molten metal and slag
        smelter.vesselState()
            .addOreChunk(OreFeedstockDefinition.copperOreChunk());
        smelter.vesselState()
            .addFuel(1000);
        for (int i = 0; i < 400; i++) {
            smelter.vesselState()
                .step(1, 293.15);
        }

        // Verify slag exists
        assertTrue(
            smelter.vesselState()
                .slagLayers()
                .size() > 0,
            "Smelting ore must produce slag");
        MaterialQuantity initialMass = smelter.vesselState()
            .totalMass();

        ItemStack skimmed = smelter.skimSlag();
        assertNotNull(skimmed, "Skimming must return slag item");
        assertTrue(
            smelter.vesselState()
                .totalMass()
                .compareTo(initialMass) < 0,
            "Vessel mass must decrease after skimming");
    }

    @Test
    void moldSolidifiesThroughThermodynamicsAndYieldsComponent() {
        ItemStack mold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);

        // Pour 268.8g of 1450K molten copper into mold
        MaterialQuantity mass = MaterialQuantity.ofGrams(268.8);
        ActiveThermalState moltenThermal = ActiveThermalState
            .atTemperature(1450.0, 1.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        AggregateLayer poured = new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper")))
                .asMolten(),
            CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper")),
            mass,
            moltenThermal,
            AggregatePhaseType.MOLTEN_METAL);

        ItemCastingMold.fillMold(mold, poured);
        assertFalse(ItemCastingMold.isSolidified(mold), "Mold must be initially molten");

        // Simulate natural cooling ticks until solidified
        for (int tick = 0; tick < 1000; tick++) {
            ((ItemCastingMold) mold.getItem()).onUpdate(mold, null, null, 0, false);
            if (ItemCastingMold.isSolidified(mold)) {
                break;
            }
        }

        assertTrue(ItemCastingMold.isSolidified(mold), "Mold must solidify through cooling");

        // Extract component
        ItemStack component = ItemCastingMold.extractComponent(mold);
        assertNotNull(component, "Extracting from solidified mold must yield component");
        assertEquals("Copper Pick Head", component.getDisplayName());

        ComponentInstance instance = ItemGenericComponent.getComponent(component);
        assertNotNull(instance);
        assertEquals(GeometryId.PICK_HEAD, instance.geometryId());
        assertEquals(BuiltinMaterials.COPPER.ref(), instance.materialRef());
        assertEquals(ManufacturingState.FINISHED, instance.manufacturingState());

        // Mold should be empty again
        assertFalse(ItemCastingMold.isFilled(mold), "Mold must be reset to empty after extraction");
    }

    @Test
    void ingotAndPickHeadAreVisuallyAndMechanicallyDistinct() {
        ComponentInstance cuBlank = new ComponentInstance(
            GeometryId.BLANK,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            ComponentGeometry.blank()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        ItemStack blankStack = ItemGenericComponent.createStack(cuBlank);

        ComponentInstance cuPickHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        ItemStack pickHeadStack = ItemGenericComponent.createStack(cuPickHead);

        // Display names must differ
        assertNotEquals(blankStack.getDisplayName(), pickHeadStack.getDisplayName());
        assertEquals("Copper Blank", blankStack.getDisplayName());
        assertEquals("Copper Pick Head", pickHeadStack.getDisplayName());

        // Geometries must differ
        assertNotEquals(
            ItemGenericComponent.getComponent(blankStack)
                .geometryId(),
            ItemGenericComponent.getComponent(pickHeadStack)
                .geometryId());

        // Masses must differ
        assertNotEquals(
            ItemGenericComponent.getComponent(blankStack)
                .mass(),
            ItemGenericComponent.getComponent(pickHeadStack)
                .mass());

        // Material identities must match (both canonical Copper)
        assertEquals(
            ItemGenericComponent.getComponent(blankStack)
                .materialRef(),
            ItemGenericComponent.getComponent(pickHeadStack)
                .materialRef());
    }

    @Test
    void saveReloadPreservesActiveVesselAndFilledMold() {
        // 1. Vessel state save/reload
        VesselState vessel = new VesselState();
        vessel.addFuel(850);
        MaterialQuantity cuMass = MaterialQuantity.ofGrams(300.0);
        ActiveThermalState cuThermal = ActiveThermalState
            .atTemperature(1450.0, 1.0, cuMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        vessel.addLayer(
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper")))
                    .asMolten(),
                CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper")),
                cuMass,
                cuThermal,
                AggregatePhaseType.MOLTEN_METAL));

        NBTTagCompound nbt = new NBTTagCompound();
        VesselStateNbtCodec.writeToNbt(vessel, nbt);
        VesselState reloaded = VesselStateNbtCodec.readFromNbt(nbt, new MaterialPropertyResolver());

        assertEquals(vessel.totalMass(), reloaded.totalMass());
        assertEquals(
            850,
            reloaded.fuelState()
                .remainingTicks());
        assertEquals(
            1,
            reloaded.layers()
                .size());
        assertEquals(
            1450.0,
            reloaded.layers()
                .get(0)
                .temperatureKelvin(),
            1.0);

        // 2. Filled mold save/reload
        ItemStack mold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        ItemCastingMold.fillMold(
            mold,
            vessel.layers()
                .get(0));

        NBTTagCompound moldNbt = (NBTTagCompound) mold.getTagCompound()
            .copy();
        ItemStack reloadedMold = new ItemStack(new ItemCastingMold());
        reloadedMold.setTagCompound(moldNbt);

        assertTrue(ItemCastingMold.isFilled(reloadedMold));
        assertEquals(GeometryId.PICK_HEAD, ItemCastingMold.getGeometry(reloadedMold));
        assertEquals(BuiltinMaterials.COPPER.ref(), ItemCastingMold.getContainedMaterialRef(reloadedMold));
        assertEquals(
            300.0,
            ItemCastingMold.getFilledMass(reloadedMold)
                .toGrams(),
            0.1);
    }
}
