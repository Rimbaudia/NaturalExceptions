package naturalexceptions.minecraft.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

public final class MoldVisualStatePipelineTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    private static AggregateLayer createMoltenLayer(MaterialRef ref, CanonicalComposition comp,
        MaterialProperties props, double massKg, double tempK) {
        ActiveThermalState thermal = ActiveThermalState.ambient(massKg, props);
        double energyNeeded = props.specificHeatSolid() * massKg * (props.meltingPointKelvin() - 293.15)
            + props.latentHeatOfFusion() * massKg
            + props.specificHeatLiquid() * massKg * (tempK - props.meltingPointKelvin());
        thermal.addEnergyJoules(energyNeeded);
        return new AggregateLayer(
            ref,
            CanonicalMaterialKey.of(comp)
                .asMolten(),
            comp,
            MaterialQuantity.ofKilograms(massKg),
            thermal,
            AggregatePhaseType.MOLTEN_METAL);
    }

    @Test
    void emptyMoldHasNeutralPass0AndPass1Colors() {
        ItemStack emptyMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        ItemCastingMold item = (ItemCastingMold) emptyMold.getItem();

        assertFalse(ItemCastingMold.isFilled(emptyMold));
        assertEquals(0xFFFFFF, item.getColorFromItemStack(emptyMold, 0));
        assertEquals(0xFFFFFF, item.getColorFromItemStack(emptyMold, 1));
        assertNull(ItemCastingMold.getPayloadDiagnostic(emptyMold));
    }

    @Test
    void filledMoldPass0IsAlwaysNeutralWhilePass1IsMaterialDerived() {
        ItemStack copperMold = ItemCastingMold.createStack(GeometryId.BLANK);
        AggregateLayer layer = createMoltenLayer(
            BuiltinMaterials.COPPER.ref(),
            BuiltinMaterials.COPPER_COMPOSITION,
            BuiltinMaterials.COPPER_PROPERTIES,
            1.0,
            1400.0);
        ItemCastingMold.fillMold(copperMold, layer);

        ItemCastingMold item = (ItemCastingMold) copperMold.getItem();
        assertTrue(ItemCastingMold.isFilled(copperMold));

        // Pass 0 (base clay body) must always be neutral white
        assertEquals(0xFFFFFF, item.getColorFromItemStack(copperMold, 0));

        // Pass 1 (molten metal fill) must have a non-neutral color
        int moltenColor = item.getColorFromItemStack(copperMold, 1);
        assertNotEquals(0xFFFFFF, moltenColor);
    }

    @Test
    void distinctMaterialsProduceDistinctDynamicTintColors() {
        ItemCastingMold item = NEItems.castingMold;

        // 1. Copper
        ItemStack cuMold = ItemCastingMold.createStack(GeometryId.BLANK);
        AggregateLayer cuLayer = createMoltenLayer(
            BuiltinMaterials.COPPER.ref(),
            BuiltinMaterials.COPPER_COMPOSITION,
            BuiltinMaterials.COPPER_PROPERTIES,
            1.0,
            300.0);
        cuLayer.thermalState()
            .removeEnergyJoules(
                cuLayer.thermalState()
                    .energyJoules()); // solidify
        ItemCastingMold.fillMold(cuMold, cuLayer);
        int cuColor = item.getColorFromItemStack(cuMold, 1);

        // 2. Tin
        ItemStack snMold = ItemCastingMold.createStack(GeometryId.BLANK);
        AggregateLayer snLayer = createMoltenLayer(
            BuiltinMaterials.TIN.ref(),
            BuiltinMaterials.TIN_COMPOSITION,
            BuiltinMaterials.TIN_PROPERTIES,
            1.0,
            300.0);
        snLayer.thermalState()
            .removeEnergyJoules(
                snLayer.thermalState()
                    .energyJoules());
        ItemCastingMold.fillMold(snMold, snLayer);
        int snColor = item.getColorFromItemStack(snMold, 1);

        // 3. Lead
        ItemStack pbMold = ItemCastingMold.createStack(GeometryId.BLANK);
        AggregateLayer pbLayer = createMoltenLayer(
            BuiltinMaterials.LEAD.ref(),
            BuiltinMaterials.LEAD_COMPOSITION,
            BuiltinMaterials.LEAD_PROPERTIES,
            1.0,
            300.0);
        pbLayer.thermalState()
            .removeEnergyJoules(
                pbLayer.thermalState()
                    .energyJoules());
        ItemCastingMold.fillMold(pbMold, pbLayer);
        int pbColor = item.getColorFromItemStack(pbMold, 1);

        // 4. Bronze (Cu 88%, Sn 12%)
        ItemStack bronzeMold = ItemCastingMold.createStack(GeometryId.BLANK);
        AggregateLayer bronzeLayer = createMoltenLayer(
            BuiltinMaterials.BRONZE.ref(),
            BuiltinMaterials.BRONZE_COMPOSITION,
            BuiltinMaterials.BRONZE_PROPERTIES,
            1.0,
            300.0);
        bronzeLayer.thermalState()
            .removeEnergyJoules(
                bronzeLayer.thermalState()
                    .energyJoules());
        ItemCastingMold.fillMold(bronzeMold, bronzeLayer);
        int bronzeColor = item.getColorFromItemStack(bronzeMold, 1);

        // 5. Custom 50/50 Cu-Sn alloy
        CanonicalComposition alloy5050 = CanonicalComposition.builder()
            .add(BuiltinMaterials.COPPER_RESOURCE_ID, 0.5)
            .add(BuiltinMaterials.TIN_RESOURCE_ID, 0.5)
            .build();
        MaterialRef alloyRef = MaterialRef.generatedFrom(alloy5050);
        MaterialProperties alloyProps = new MaterialPropertyResolver().resolve(alloyRef, alloy5050)
            .properties();
        ItemStack alloyMold = ItemCastingMold.createStack(GeometryId.BLANK);
        AggregateLayer alloyLayer = createMoltenLayer(alloyRef, alloy5050, alloyProps, 1.0, 300.0);
        alloyLayer.thermalState()
            .removeEnergyJoules(
                alloyLayer.thermalState()
                    .energyJoules());
        ItemCastingMold.fillMold(alloyMold, alloyLayer);
        int alloyColor = item.getColorFromItemStack(alloyMold, 1);

        // Verify all 5 colors are strictly distinct from each other
        assertNotEquals(cuColor, snColor, "Copper and Tin colors must differ");
        assertNotEquals(cuColor, pbColor, "Copper and Lead colors must differ");
        assertNotEquals(snColor, pbColor, "Tin and Lead colors must differ");
        assertNotEquals(cuColor, bronzeColor, "Copper and Bronze colors must differ");
        assertNotEquals(alloyColor, cuColor, "50/50 Alloy and Copper colors must differ");
        assertNotEquals(alloyColor, snColor, "50/50 Alloy and Tin colors must differ");

        // Verify exact known baseline solid colors
        assertEquals(0xD36C4C, cuColor, "Solid copper color must match canonical identity");
        assertEquals(0xD0D8DE, snColor, "Solid tin color must match canonical identity");
        assertEquals(0x646672, pbColor, "Solid lead color must match canonical identity");
    }

    @Test
    void thermalCoolingTransitionsColorFromMoltenToSolid() {
        ItemCastingMold item = NEItems.castingMold;
        ItemStack mold = ItemCastingMold.createStack(GeometryId.BLADE);

        // Pour liquid copper at 1500 K (well above melting point 1358 K)
        AggregateLayer hotLayer = createMoltenLayer(
            BuiltinMaterials.COPPER.ref(),
            BuiltinMaterials.COPPER_COMPOSITION,
            BuiltinMaterials.COPPER_PROPERTIES,
            1.0,
            1500.0);
        ItemCastingMold.fillMold(mold, hotLayer);

        assertFalse(ItemCastingMold.isSolidified(mold));
        int moltenColor = item.getColorFromItemStack(mold, 1);

        // Cool mold until fully solidified at 300 K
        ActiveThermalState thermal = ItemCastingMold.getThermalState(mold);
        thermal.removeEnergyJoules(thermal.energyJoules()); // Remove all heat down to 0 K then set to 300 K
        ActiveThermalState ambient = ActiveThermalState.ambient(1.0, BuiltinMaterials.COPPER_PROPERTIES);
        ItemCastingMold.saveThermalState(mold, ambient);

        assertTrue(ItemCastingMold.isSolidified(mold));
        int solidColor = item.getColorFromItemStack(mold, 1);

        // Molten color and solid color must be distinctly different
        assertNotEquals(moltenColor, solidColor);
        assertEquals(0xD36C4C, solidColor);
    }

    @Test
    void visualStateSurvivesNbtSerializationRoundTrip() {
        ItemStack mold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        AggregateLayer layer = createMoltenLayer(
            BuiltinMaterials.COPPER.ref(),
            BuiltinMaterials.COPPER_COMPOSITION,
            BuiltinMaterials.COPPER_PROPERTIES,
            0.5,
            1400.0);
        ItemCastingMold.fillMold(mold, layer);

        // Simulate network / container NBT save and reload
        NBTTagCompound tag = (NBTTagCompound) mold.getTagCompound()
            .copy();
        ItemStack loaded = new ItemStack(NEItems.castingMold);
        loaded.setTagCompound(tag);
        assertNotNull(loaded);
        assertTrue(ItemCastingMold.isFilled(loaded));
        assertEquals(GeometryId.PICK_HEAD, ItemCastingMold.getGeometry(loaded));
        assertEquals(BuiltinMaterials.COPPER.ref(), ItemCastingMold.getContainedMaterialRef(loaded));
        assertEquals(
            ((ItemCastingMold) mold.getItem()).getColorFromItemStack(mold, 1),
            ((ItemCastingMold) loaded.getItem()).getColorFromItemStack(loaded, 1));
    }

    @Test
    void malformedLegacyPayloadRecoversGracefullyWithPlayerFacingDiagnostics() {
        // Create an old/legacy mold payload missing StateKey and thermal properties
        ItemStack legacyMold = ItemCastingMold.createStack(GeometryId.BLANK);
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setBoolean(ItemCastingMold.KEY_FILLED, true);
        nbt.setString(
            ItemCastingMold.KEY_MATERIAL_REF,
            BuiltinMaterials.COPPER.ref()
                .asString());
        nbt.setLong(ItemCastingMold.KEY_MASS_MG, 1000000L);
        nbt.setDouble(ItemCastingMold.KEY_ENERGY, 0.0); // Ambient / cold
        legacyMold.setTagCompound(nbt);

        // Verify legacy recovery logic
        CanonicalMaterialKey recoveredKey = ItemCastingMold.getContainedStateKey(legacyMold);
        assertNotNull(recoveredKey, "StateKey should be reconstructed from authored MaterialRef");
        assertTrue(ItemCastingMold.isRecovered(legacyMold), "Mold should be flagged as recovered");

        // Verify player-facing diagnostic string
        String diag = ItemCastingMold.getPayloadDiagnostic(legacyMold);
        assertNotNull(diag);
        assertTrue(diag.contains("Recovered") || diag.contains("reconstructed"));

        // Verify successful extraction from recovered payload
        assertTrue(ItemCastingMold.isSolidified(legacyMold));
        ItemStack extracted = ItemCastingMold.extractComponent(legacyMold);
        assertNotNull(extracted, "Extraction must succeed on recovered legacy mold");
        assertEquals(
            BuiltinMaterials.COPPER.ref(),
            ItemGenericComponent.getComponent(extracted)
                .materialRef());

        // Verify empty mold is cleanly restored
        assertFalse(ItemCastingMold.isFilled(legacyMold));
        assertNull(ItemCastingMold.getPayloadDiagnostic(legacyMold));
    }

    @Test
    void corruptedMoldPayloadProvidesClearDiagnosticAndCanBeScrapedClean() {
        ItemStack corrupted = ItemCastingMold.createStack(GeometryId.PLATE);
        NBTTagCompound nbt = corrupted.getTagCompound();
        nbt.setBoolean(ItemCastingMold.KEY_FILLED, true);
        nbt.setString(ItemCastingMold.KEY_MATERIAL_REF, "corrupted:gibberish$@#!");

        // StateKey cannot be parsed or reconstructed
        assertNull(ItemCastingMold.getContainedStateKey(corrupted));

        // Diagnostic identifies corrupt payload
        String diag = ItemCastingMold.getPayloadDiagnostic(corrupted);
        assertNotNull(diag);
        assertTrue(
            diag.toLowerCase()
                .contains("corrupted")
                || diag.toLowerCase()
                    .contains("incomplete"));

        // Extraction is safely blocked
        assertNull(ItemCastingMold.extractComponent(corrupted));

        // Scraping clean (simulating sneak right-click) clears corrupted payload
        ItemCastingMold.clearContainedPayload(corrupted);
        assertFalse(ItemCastingMold.isFilled(corrupted));
        assertEquals(GeometryId.PLATE, ItemCastingMold.getGeometry(corrupted));
        assertNull(ItemCastingMold.getPayloadDiagnostic(corrupted));
    }

    @Test
    void negativeOrCorruptedMassDoesNotCrashThermalQueryOrTooltip() {
        ItemStack mold = ItemCastingMold.createStack(GeometryId.BLANK);
        NBTTagCompound nbt = mold.getTagCompound();
        nbt.setBoolean(ItemCastingMold.KEY_FILLED, true);
        nbt.setString(
            ItemCastingMold.KEY_MATERIAL_REF,
            BuiltinMaterials.COPPER.ref()
                .asString());
        nbt.setLong(ItemCastingMold.KEY_MASS_MG, -5000L); // Corrupted negative mass

        assertNull(ItemCastingMold.getThermalState(mold), "Negative mass must return null thermal state");
        assertEquals(MaterialQuantity.ZERO, ItemCastingMold.getFilledMass(mold), "Negative mass must return ZERO mass");

        java.util.ArrayList<String> tooltip = new java.util.ArrayList<>();
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
            () -> NEItems.castingMold.addInformation(mold, null, tooltip, false),
            "Tooltip rendering must not throw on negative mass");
    }

    @Test
    void malformedStateKeyMoldIsMarkedScrapableAndReclaimsCleanly() {
        ItemStack mold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        NBTTagCompound nbt = mold.getTagCompound();
        nbt.setBoolean(ItemCastingMold.KEY_FILLED, true);
        nbt.setString(
            ItemCastingMold.KEY_MATERIAL_REF,
            BuiltinMaterials.COPPER.ref()
                .asString());
        nbt.setString(ItemCastingMold.KEY_STATE_KEY, "invalid_state_identity!@#");
        nbt.setLong(ItemCastingMold.KEY_MASS_MG, 100000L);

        assertTrue(ItemCastingMold.isScrapable(mold), "Mold with corrupted state key must be scrapable");
        assertNull(ItemCastingMold.extractComponent(mold), "Extraction must be blocked for corrupted state key");

        java.util.ArrayList<String> tooltip = new java.util.ArrayList<>();
        NEItems.castingMold.addInformation(mold, null, tooltip, false);
        assertTrue(
            tooltip.toString()
                .contains("scrape clean"),
            "Tooltip must instruct player to scrape clean");

        ItemCastingMold.clearContainedPayload(mold);
        assertFalse(ItemCastingMold.isFilled(mold));
        assertEquals(GeometryId.PICK_HEAD, ItemCastingMold.getGeometry(mold));
    }
}
