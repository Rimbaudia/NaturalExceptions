package naturalexceptions.minecraft.metallurgy;

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
import naturalexceptions.core.fabrication.ComponentGeometry;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.metallurgy.OreFeedstockDefinition;
import naturalexceptions.core.metallurgy.VesselState;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.fabrication.TileEntityCastingTable;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.persistence.VesselStateNbtCodec;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

final class SmelterLiveIntegrationTest {

    private static net.minecraft.item.Item testFuelItem;
    private static net.minecraft.item.Item testEmptyCanister;
    private static net.minecraft.item.Item testFuelCanister;

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();

        testFuelItem = new net.minecraft.item.Item().setUnlocalizedName("testFuelItem");
        testEmptyCanister = new net.minecraft.item.Item().setUnlocalizedName("testEmptyCanister");
        testFuelCanister = new net.minecraft.item.Item().setUnlocalizedName("testFuelCanister")
            .setContainerItem(testEmptyCanister);

        cpw.mods.fml.common.registry.GameRegistry.registerFuelHandler(stack -> {
            if (stack == null || stack.getItem() == null) return 0;
            if (stack.getItem() == testFuelItem) return 1600;
            if (stack.getItem() == testFuelCanister) return 20000;
            return 0;
        });
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
        // Must stabilize bounded by flame temperature (~1950 K) and radiant equilibrium ~1750-1800 K
        assertTrue(finalTemp < 1950.0, "Copper must not run away: T = " + finalTemp + " K (must be < 1950 K)");
        assertTrue(finalTemp >= 1400.0, "Copper must reach normal working superheat: T = " + finalTemp + " K");
    }

    @Test
    void generatedAlloyComponentRemeltsWithResolvedProperties() {
        CanonicalComposition alloyComposition = CanonicalComposition.builder()
            .add(ResourceId.of("naturalexceptions", "copper"), 0.57)
            .add(ResourceId.of("naturalexceptions", "tin"), 0.23)
            .add(ResourceId.of("naturalexceptions", "lead"), 0.20)
            .build();
        CanonicalMaterialKey alloyKey = CanonicalMaterialKey.of(alloyComposition);
        MaterialRef alloyRef = MaterialRef.generatedFrom(alloyKey);
        ComponentInstance component = new ComponentInstance(
            GeometryId.BLANK,
            alloyRef,
            alloyKey,
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);
        ItemStack componentStack = ItemGenericComponent.createStack(component);
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();

        assertTrue(smelter.insertFeed(componentStack));
        assertEquals(0, componentStack.stackSize);
        assertEquals(
            1,
            smelter.vesselState()
                .layers()
                .size());

        AggregateLayer layer = smelter.vesselState()
            .layers()
            .get(0);
        assertEquals(alloyRef, layer.materialRef());
        assertEquals(
            new MaterialPropertyResolver().resolve(alloyRef, alloyComposition, MaterialPhase.SOLID)
                .properties(),
            layer.thermalState()
                .properties());
    }

    @Test
    void remeltingFilledMoldClearsContainedPayload() {
        ItemStack mold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        MaterialQuantity mass = ComponentGeometry.pickHead()
            .requiredMass(BuiltinMaterials.COPPER_PROPERTIES);
        ItemCastingMold.fillMold(
            mold,
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)
                    .asMolten(),
                BuiltinMaterials.COPPER_COMPOSITION,
                mass,
                ActiveThermalState.atTemperature(1450.0, 1.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES),
                AggregatePhaseType.MOLTEN_METAL));

        assertTrue(new TileEntityStarterSmelter().insertFeed(mold));
        assertFalse(ItemCastingMold.isFilled(mold));
        assertFalse(
            mold.getTagCompound()
                .hasKey(ItemCastingMold.KEY_DENSITY));
        assertFalse(
            mold.getTagCompound()
                .hasKey(ItemCastingMold.KEY_SPECIFIC_HEAT));
        assertFalse(
            mold.getTagCompound()
                .hasKey(ItemCastingMold.KEY_SPECIFIC_HEAT_LIQUID));
        assertFalse(
            mold.getTagCompound()
                .hasKey(ItemCastingMold.KEY_MELT_POINT));
        assertFalse(
            mold.getTagCompound()
                .hasKey(ItemCastingMold.KEY_LATENT_HEAT));
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
            .addRawOre(OreFeedstockDefinition.copperOre());

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
            .addRawOre(OreFeedstockDefinition.copperOre());
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
        assertTrue(
            component.getDisplayName()
                .endsWith("Pick Head"),
            "material, then the shape: " + component.getDisplayName());

        ComponentInstance instance = ItemGenericComponent.getComponent(component);
        assertNotNull(instance);
        assertEquals(GeometryId.PICK_HEAD, instance.geometryId());
        assertEquals(BuiltinMaterials.COPPER.ref(), instance.materialRef());
        assertEquals(ManufacturingState.AS_CAST, instance.manufacturingState());

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
        assertTrue(
            blankStack.getDisplayName()
                .endsWith("Blank"),
            "material, then the shape: " + blankStack.getDisplayName());
        assertTrue(
            pickHeadStack.getDisplayName()
                .endsWith("Pick Head"),
            "material, then the shape: " + pickHeadStack.getDisplayName());

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

    @Test
    void castingTableAcceptsMoldAndPersistsState() {
        naturalexceptions.minecraft.fabrication.TileEntityCastingTable table = new naturalexceptions.minecraft.fabrication.TileEntityCastingTable();
        ItemStack pickMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);

        // Place mold
        table.setMold(pickMold);
        assertNotNull(table.getMold());
        assertEquals(GeometryId.PICK_HEAD, ItemCastingMold.getGeometry(table.getMold()));

        // NBT round-trip
        NBTTagCompound tag = new NBTTagCompound();
        table.writeToNBT(tag);

        naturalexceptions.minecraft.fabrication.TileEntityCastingTable reloadedTable = new naturalexceptions.minecraft.fabrication.TileEntityCastingTable();
        reloadedTable.readFromNBT(tag);

        assertNotNull(reloadedTable.getMold());
        assertEquals(GeometryId.PICK_HEAD, ItemCastingMold.getGeometry(reloadedTable.getMold()));
    }

    @Test
    void castingTableCoolsHotMoldAndExtractsComponent() {
        naturalexceptions.minecraft.fabrication.TileEntityCastingTable table = new naturalexceptions.minecraft.fabrication.TileEntityCastingTable();
        ItemStack pickMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);

        // Create molten copper layer
        ActiveThermalState hotThermal = ActiveThermalState
            .atTemperature(1450.0, 1.0, 0.300, BuiltinMaterials.COPPER_PROPERTIES);
        AggregateLayer hotCu = new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper")),
            MaterialQuantity.ofGrams(300.0),
            hotThermal,
            AggregatePhaseType.MOLTEN_METAL);

        ItemCastingMold.fillMold(pickMold, hotCu);
        table.setMold(pickMold);

        assertTrue(ItemCastingMold.isFilled(table.getMold()));
        assertFalse(ItemCastingMold.isSolidified(table.getMold()), "Mold must initially be molten");

        // Simulate cooling ticks
        for (int tick = 0; tick < 1000; tick++) {
            ((ItemCastingMold) table.getMold()
                .getItem()).onUpdate(table.getMold(), null, null, 0, false);
            if (ItemCastingMold.isSolidified(table.getMold())) {
                break;
            }
        }

        assertTrue(ItemCastingMold.isSolidified(table.getMold()), "Mold on table must solidify through cooling");

        // Extract component
        ItemStack component = ItemCastingMold.extractComponent(table.getMold());
        assertNotNull(component);
        assertEquals(GeometryId.PICK_HEAD, ItemGenericComponent.getGeometry(component));
        assertFalse(ItemCastingMold.isFilled(table.getMold()), "Mold on table resets to empty after extraction");
    }

    @Test
    void testFuelFeedingAndPacketSync() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        ItemStack fuel = new ItemStack(testFuelItem, 64);
        assertTrue(smelter.isItemValidForSlot(TileEntityStarterSmelter.SLOT_FUEL, fuel));
        smelter.setInventorySlotContents(TileEntityStarterSmelter.SLOT_FUEL, fuel);

        smelter.updateEntity();
        assertTrue(
            smelter.vesselState()
                .fuelState()
                .isBurning());
        assertEquals(63, smelter.getStackInSlot(TileEntityStarterSmelter.SLOT_FUEL).stackSize);

        // Verify description packet has action ID 0 (custom mod tile entity, not mob spawner)
        net.minecraft.network.play.server.S35PacketUpdateTileEntity pkt = (net.minecraft.network.play.server.S35PacketUpdateTileEntity) smelter
            .getDescriptionPacket();
        assertNotNull(pkt);
        assertEquals(
            0,
            pkt.func_148853_f(),
            "Description packet must use action ID 0 so Forge client executes onDataPacket");
    }

    @Test
    void testContainerFuelLeavesEmptyContainer() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        ItemStack fuelCanister = new ItemStack(testFuelCanister, 1);
        smelter.setInventorySlotContents(TileEntityStarterSmelter.SLOT_FUEL, fuelCanister);

        smelter.updateEntity();
        assertTrue(
            smelter.vesselState()
                .fuelState()
                .isBurning());
        ItemStack remaining = smelter.getStackInSlot(TileEntityStarterSmelter.SLOT_FUEL);
        assertNotNull(remaining);
        assertEquals(
            testEmptyCanister,
            remaining.getItem(),
            "Container fuel consumption must leave empty container item");
    }

    @Test
    void testFeedSlotAcceptsCopperIngotAndOre() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        ItemStack ingot = new ItemStack(NEItems.copperIngot, 1);
        assertTrue(smelter.isItemValidForSlot(TileEntityStarterSmelter.SLOT_FEED, ingot));
        smelter.setInventorySlotContents(TileEntityStarterSmelter.SLOT_FEED, ingot);

        smelter.updateEntity();
        assertEquals(
            1,
            smelter.vesselState()
                .layers()
                .size());
        assertEquals(
            AggregatePhaseType.SOLID_CHARGE,
            smelter.vesselState()
                .layers()
                .get(0)
                .phaseType());
    }

    @Test
    void testMultiFuelBufferingAllowsContinuousBurning() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        // Give 5 fuel items
        ItemStack fuelStack = new ItemStack(testFuelItem, 5);
        smelter.setInventorySlotContents(TileEntityStarterSmelter.SLOT_FUEL, fuelStack);

        // First update consumes 1 fuel (1600 ticks) and steps 1 tick -> 1599 ticks
        smelter.updateEntity();
        assertEquals(4, smelter.getStackInSlot(TileEntityStarterSmelter.SLOT_FUEL).stackSize);
        assertEquals(
            1599,
            smelter.vesselState()
                .fuelState()
                .remainingTicks());

        // Second update consumes another fuel because remainingTicks (1599) <= 1600 (buffering up to 2 items)
        smelter.updateEntity();
        assertEquals(3, smelter.getStackInSlot(TileEntityStarterSmelter.SLOT_FUEL).stackSize);
        assertEquals(
            3198,
            smelter.vesselState()
                .fuelState()
                .remainingTicks());

        // Third update does NOT consume because remainingTicks is 3198 > 1600 (buffer full)
        smelter.updateEntity();
        assertEquals(3, smelter.getStackInSlot(TileEntityStarterSmelter.SLOT_FUEL).stackSize);
        assertEquals(
            3197,
            smelter.vesselState()
                .fuelState()
                .remainingTicks());
    }

    @Test
    void testSmelterOutletPhysicalPouringProcess() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        // Charge with 300g molten copper at 1450 K
        MaterialQuantity mass = MaterialQuantity.ofGrams(300.0);
        ActiveThermalState hotThermal = ActiveThermalState
            .atTemperature(1450.0, 1.0, 0.300, BuiltinMaterials.COPPER_PROPERTIES);
        AggregateLayer hotCu = new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper")),
            mass,
            hotThermal,
            AggregatePhaseType.MOLTEN_METAL);
        smelter.vesselState()
            .addLayer(hotCu);

        TileEntitySmelterOutlet outlet = new TileEntitySmelterOutlet();
        outlet.setFacing(2); // North
        assertEquals(2, outlet.getFacing());
        assertFalse(outlet.isPouring());

        // Test outlet NBT serialization
        NBTTagCompound tag = new NBTTagCompound();
        outlet.writeToNBT(tag);
        TileEntitySmelterOutlet loadedOutlet = new TileEntitySmelterOutlet();
        loadedOutlet.readFromNBT(tag);
        assertEquals(2, loadedOutlet.getFacing());
        assertFalse(loadedOutlet.isPouring());

        // Pouring directly into empty mold stack
        ItemStack mold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        assertFalse(ItemCastingMold.isFilled(mold));
        boolean poured = smelter.pourIntoMold(mold);
        assertTrue(poured, "Smelter must pour into mold");
        assertTrue(ItemCastingMold.isFilled(mold), "Mold must be filled after pouring");
        assertFalse(ItemCastingMold.isSolidified(mold), "Freshly poured mold must be molten");
    }

    @Test
    void testPourDiagnosticSkipsSlagMeltingPoint() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();

        // 1. Add molten slag at 1280 K (melting point 1250 K)
        MaterialQuantity slagMass = MaterialQuantity.ofGrams(50.0);
        ActiveThermalState slagThermal = ActiveThermalState
            .atTemperature(1280.0, 1.0, slagMass.toKilograms(), OreFeedstockDefinition.SLAG_PROPERTIES);
        AggregateLayer slagLayer = new AggregateLayer(
            OreFeedstockDefinition.SLAG_REF,
            CanonicalMaterialKey.of(CanonicalComposition.of(OreFeedstockDefinition.SLAG_RESOURCE_ID)),
            CanonicalComposition.of(OreFeedstockDefinition.SLAG_RESOURCE_ID),
            slagMass,
            slagThermal,
            AggregatePhaseType.SLAG);
        smelter.vesselState()
            .addLayer(slagLayer);

        // 2. Add solid copper at 1280 K (melting point 1358 K)
        MaterialQuantity cuMass = MaterialQuantity.ofGrams(200.0);
        ActiveThermalState cuThermal = ActiveThermalState
            .atTemperature(1280.0, 0.0, cuMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        AggregateLayer cuLayer = new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            BuiltinMaterials.COPPER_COMPOSITION,
            cuMass,
            cuThermal,
            AggregatePhaseType.SOLID_CHARGE);
        smelter.vesselState()
            .addLayer(cuLayer);

        // 3. Pour into mold fails because copper is solid
        ItemStack mold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        assertFalse(smelter.pourIntoMold(mold), "Cannot pour solid metal");

        // Smelting UI no longer offers a duplicate mold-pour path; physical transfer is through outlet/ladle.
        CommandSmelterTest.TestCommandPlayer player = CommandSmelterTest.TestCommandPlayer.create();

        smelter.setInventorySlotContents(TileEntityStarterSmelter.SLOT_MOLD, mold);
        ContainerStarterSmelter container = new ContainerStarterSmelter(player.inventory, smelter);
        boolean pouredInContainer = container.enchantItem(player, 0);
        assertFalse(pouredInContainer);
        assertEquals(0, player.messages.size());
        // No dead output slot: only fuel and feed plus the player's inventory.
        assertEquals(2 + 36, container.inventorySlots.size());
        assertEquals(SmelterOutletState.STRUCTURE_INCOMPLETE.canPour(), false);
    }

    private static class EnvironmentalTestCastingTable extends TileEntityCastingTable {

        double mockAmbient = 293.15;
        boolean mockSubmerged = false;
        boolean mockRaining = false;
        int mockAdjacentWater = 0;

        @Override
        public double calculateAmbientTemperature() {
            return mockAmbient;
        }

        @Override
        public boolean isSubmergedInWater() {
            return mockSubmerged;
        }

        @Override
        public boolean isExposedToRain() {
            return mockRaining;
        }

        @Override
        public int countAdjacentWaterBlocks() {
            return mockAdjacentWater;
        }
    }

    @Test
    void castingTableEnvironmentalThermalDissipationAndQuenching() {
        MaterialQuantity cuMass = MaterialQuantity.ofGrams(268.0);

        // 1. Dry air cooling
        EnvironmentalTestCastingTable dryTable = new EnvironmentalTestCastingTable();
        ItemStack dryMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        ActiveThermalState dryThermal = ActiveThermalState
            .atTemperature(1400.0, 1.0, cuMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        double initialJoules = dryThermal.energyJoules();
        ItemCastingMold.fillMold(
            dryMold,
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)
                    .asMolten(),
                BuiltinMaterials.COPPER_COMPOSITION,
                cuMass,
                dryThermal,
                AggregatePhaseType.MOLTEN_METAL));
        dryTable.setMold(dryMold);
        dryTable.coolMold(1.0); // 1 second cooling
        double dryLoss = initialJoules - ItemCastingMold.getThermalState(dryTable.getMold())
            .energyJoules();

        // 2. Rain exposed cooling
        EnvironmentalTestCastingTable rainTable = new EnvironmentalTestCastingTable();
        rainTable.mockRaining = true;
        ItemStack rainMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        ActiveThermalState rainThermal = ActiveThermalState
            .atTemperature(1400.0, 1.0, cuMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        ItemCastingMold.fillMold(
            rainMold,
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)
                    .asMolten(),
                BuiltinMaterials.COPPER_COMPOSITION,
                cuMass,
                rainThermal,
                AggregatePhaseType.MOLTEN_METAL));
        rainTable.setMold(rainMold);
        rainTable.coolMold(1.0);
        double rainLoss = initialJoules - ItemCastingMold.getThermalState(rainTable.getMold())
            .energyJoules();
        assertTrue(
            rainLoss > dryLoss,
            "Rain exposure must cool faster than dry air (Rain: " + rainLoss + " > Dry: " + dryLoss + ")");

        // 3. Adjacent water blocks quenching
        EnvironmentalTestCastingTable adjacentWaterTable = new EnvironmentalTestCastingTable();
        adjacentWaterTable.mockAdjacentWater = 2; // 2 water blocks adjacent
        ItemStack adjMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        ActiveThermalState adjThermal = ActiveThermalState
            .atTemperature(1400.0, 1.0, cuMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        ItemCastingMold.fillMold(
            adjMold,
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)
                    .asMolten(),
                BuiltinMaterials.COPPER_COMPOSITION,
                cuMass,
                adjThermal,
                AggregatePhaseType.MOLTEN_METAL));
        adjacentWaterTable.setMold(adjMold);
        adjacentWaterTable.coolMold(1.0);
        double adjLoss = initialJoules - ItemCastingMold.getThermalState(adjacentWaterTable.getMold())
            .energyJoules();
        assertTrue(adjLoss > dryLoss, "Adjacent water must cool faster than dry air");

        // 4. Submerged water immersion quenching
        EnvironmentalTestCastingTable submergedTable = new EnvironmentalTestCastingTable();
        submergedTable.mockSubmerged = true;
        ItemStack subMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        ActiveThermalState subThermal = ActiveThermalState
            .atTemperature(1400.0, 1.0, cuMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        ItemCastingMold.fillMold(
            subMold,
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)
                    .asMolten(),
                BuiltinMaterials.COPPER_COMPOSITION,
                cuMass,
                subThermal,
                AggregatePhaseType.MOLTEN_METAL));
        submergedTable.setMold(subMold);
        submergedTable.coolMold(1.0);
        double subLoss = initialJoules - ItemCastingMold.getThermalState(submergedTable.getMold())
            .energyJoules();
        assertTrue(
            subLoss > adjLoss * 1.5,
            "Submerged quenching must remove heat much faster than adjacent blocks (Sub: " + subLoss
                + " vs Adj: "
                + adjLoss
                + ")");

        // 5. Thermal equilibrium floor: mold at ambient cannot cool below ambient
        dryTable.mockAmbient = 350.0;
        ItemStack floorMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        ActiveThermalState floorThermal = ActiveThermalState
            .atTemperature(350.0, 0.0, cuMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        ItemCastingMold.fillMold(
            floorMold,
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
                BuiltinMaterials.COPPER_COMPOSITION,
                cuMass,
                floorThermal,
                AggregatePhaseType.MOLTEN_METAL));
        dryTable.setMold(floorMold);
        double energyBefore = floorThermal.energyJoules();
        dryTable.coolMold(10.0);
        assertEquals(
            energyBefore,
            ItemCastingMold.getThermalState(dryTable.getMold())
                .energyJoules(),
            1e-4,
            "Mold at or below ambient temperature must not lose energy");
    }

    @Test
    void fractionalMultiStepFillAndIncompatibleMetalRejection() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        // Smelter has only 100g of molten copper (less than pick head requirement of 268g)
        MaterialQuantity cuMass = MaterialQuantity.ofGrams(100.0);
        ActiveThermalState cuThermal = ActiveThermalState
            .atTemperature(1400.0, 1.0, cuMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        smelter.vesselState()
            .addLayer(
                new AggregateLayer(
                    BuiltinMaterials.COPPER.ref(),
                    CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)
                        .asMolten(),
                    BuiltinMaterials.COPPER_COMPOSITION,
                    cuMass,
                    cuThermal,
                    AggregatePhaseType.MOLTEN_METAL));

        TileEntityCastingTable table = new TileEntityCastingTable();
        ItemStack pickMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        table.setMold(pickMold);

        // Step 1: Pour available 100g
        boolean poured1 = smelter.pourIntoMold(table.getMold());
        assertTrue(poured1);
        assertEquals(MaterialQuantity.ofGrams(100.0), ItemCastingMold.getFilledMass(table.getMold()));
        assertFalse(ItemCastingMold.isFilled(table.getMold()), "Mold is only partially filled");
        assertTrue(ItemCastingMold.hasMetal(table.getMold()), "Mold has metal");
        assertEquals(
            MaterialQuantity.ZERO,
            smelter.vesselState()
                .totalMass(),
            "Smelter copper was completely transferred");

        // Step 2: Smelter adds molten tin (incompatible with contained copper in mold)
        MaterialQuantity tinMass = MaterialQuantity.ofGrams(200.0);
        ActiveThermalState tinThermal = ActiveThermalState
            .atTemperature(600.0, 1.0, tinMass.toKilograms(), BuiltinMaterials.TIN_PROPERTIES);
        smelter.vesselState()
            .addLayer(
                new AggregateLayer(
                    BuiltinMaterials.TIN.ref(),
                    CanonicalMaterialKey.of(BuiltinMaterials.TIN_COMPOSITION)
                        .asMolten(),
                    BuiltinMaterials.TIN_COMPOSITION,
                    tinMass,
                    tinThermal,
                    AggregatePhaseType.MOLTEN_METAL));

        // Pouring tin into partially copper-filled mold must be rejected
        boolean tinPoured = smelter.pourIntoMold(table.getMold());
        assertFalse(tinPoured, "Cannot pour incompatible metal into partially filled mold");
        assertEquals(
            tinMass,
            smelter.vesselState()
                .totalMass(),
            "Tin in smelter is untouched");
        assertEquals(
            MaterialQuantity.ofGrams(100.0),
            ItemCastingMold.getFilledMass(table.getMold()),
            "Mold metal is untouched");

        // Step 3: Remove tin, add 300g more copper
        smelter.vesselState()
            .pourLiquidMetal(BuiltinMaterials.TIN.ref(), tinMass);
        MaterialQuantity cu2Mass = MaterialQuantity.ofGrams(300.0);
        ActiveThermalState cu2Thermal = ActiveThermalState
            .atTemperature(1400.0, 1.0, cu2Mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        smelter.vesselState()
            .addLayer(
                new AggregateLayer(
                    BuiltinMaterials.COPPER.ref(),
                    CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)
                        .asMolten(),
                    BuiltinMaterials.COPPER_COMPOSITION,
                    cu2Mass,
                    cu2Thermal,
                    AggregatePhaseType.MOLTEN_METAL));

        // Step 4: Pour remaining required copper to finish filling
        boolean poured2 = smelter.pourIntoMold(table.getMold());
        assertTrue(poured2);
        assertTrue(ItemCastingMold.isFilled(table.getMold()), "Mold is now full");
        MaterialQuantity req = ItemCastingMold.getRequiredMass(table.getMold());
        assertEquals(req, ItemCastingMold.getFilledMass(table.getMold()));
        MaterialQuantity transferredSecond = req.subtract(MaterialQuantity.ofGrams(100.0));
        assertEquals(
            cu2Mass.subtract(transferredSecond),
            smelter.vesselState()
                .totalMass());

        // Step 5: Cool and extract
        for (int i = 0; i < 500; i++) {
            table.coolMold(0.1);
            if (ItemCastingMold.isSolidified(table.getMold())) break;
        }
        assertTrue(ItemCastingMold.isSolidified(table.getMold()));
        ItemStack comp = ItemCastingMold.extractComponent(table.getMold());
        assertNotNull(comp);
        assertEquals(GeometryId.PICK_HEAD, ItemGenericComponent.getGeometry(comp));
    }

    @Test
    void partiallyFilledMoldCanBeRemeltedInSmelter() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        ItemStack pickMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        MaterialQuantity partialMass = MaterialQuantity.ofGrams(120.0);
        ActiveThermalState thermal = ActiveThermalState
            .atTemperature(1360.0, 1.0, partialMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        ItemCastingMold.fillMold(
            pickMold,
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)
                    .asMolten(),
                BuiltinMaterials.COPPER_COMPOSITION,
                partialMass,
                thermal,
                AggregatePhaseType.MOLTEN_METAL));

        assertFalse(ItemCastingMold.isFilled(pickMold));
        assertTrue(ItemCastingMold.hasMetal(pickMold));

        // Mold can be placed in feed slot, but rejected in mold slot
        assertTrue(TileEntityStarterSmelter.isSmeltableFeed(pickMold));
        assertTrue(smelter.isItemValidForSlot(TileEntityStarterSmelter.SLOT_FEED, pickMold));
        assertFalse(smelter.isItemValidForSlot(TileEntityStarterSmelter.SLOT_MOLD, pickMold));

        // Remelt into smelter
        boolean inserted = smelter.insertFeed(pickMold);
        assertTrue(inserted);
        assertFalse(ItemCastingMold.hasMetal(pickMold), "Mold is cleansed of metal after remelt");
        assertEquals(
            partialMass,
            smelter.vesselState()
                .totalMass(),
            "Metal mass recovered in smelter");
    }

    @Test
    void smelterOutletStreamColorAndStateLifecycle() {
        TileEntitySmelterOutlet outlet = new TileEntitySmelterOutlet();
        outlet.setFacing(3);
        assertEquals(3, outlet.getFacing());

        // Test stream color calculation from copper
        AggregateLayer cuLayer = new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)
                .asMolten(),
            BuiltinMaterials.COPPER_COMPOSITION,
            MaterialQuantity.ofGrams(300.0),
            ActiveThermalState.atTemperature(1400.0, 1.0, 0.3, BuiltinMaterials.COPPER_PROPERTIES),
            AggregatePhaseType.MOLTEN_METAL);
        int color = naturalexceptions.core.material.MaterialAppearance
            .getMoltenColor(cuLayer.materialRef(), cuLayer.stateKey(), cuLayer.temperatureKelvin());
        assertNotEquals(0, color);

        NBTTagCompound tag = new NBTTagCompound();
        outlet.writeToNBT(tag);
        tag.setInteger("StreamColor", color);
        tag.setBoolean("IsPouring", true);

        TileEntitySmelterOutlet loaded = new TileEntitySmelterOutlet();
        loaded.readFromNBT(tag);
        assertEquals(3, loaded.getFacing());
        assertTrue(loaded.isPouring());
        assertEquals(color, loaded.getStreamColor());
    }

    @Test
    void partiallyFilledSolidifiedMoldNotErasedWhenClickedOnCastingTable() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        TileEntityCastingTable table = new TileEntityCastingTable();
        ItemStack pickMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        table.setMold(pickMold);

        // 1. Smelter starts with 100g molten copper
        MaterialQuantity partialCu = MaterialQuantity.ofGrams(100.0);
        smelter.vesselState()
            .addLayer(
                new AggregateLayer(
                    BuiltinMaterials.COPPER.ref(),
                    CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)
                        .asMolten(),
                    BuiltinMaterials.COPPER_COMPOSITION,
                    partialCu,
                    ActiveThermalState
                        .atTemperature(1400.0, 1.0, partialCu.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES),
                    AggregatePhaseType.MOLTEN_METAL));

        // Pour 100g into mold
        assertTrue(smelter.pourIntoMold(table.getMold()));
        assertEquals(partialCu, ItemCastingMold.getFilledMass(table.getMold()));
        assertFalse(ItemCastingMold.isFilled(table.getMold()));

        // 2. Simulate cooling until completely solidified
        for (int i = 0; i < 500; i++) {
            table.coolMold(0.1);
            if (ItemCastingMold.isSolidified(table.getMold())) break;
        }
        assertTrue(ItemCastingMold.isSolidified(table.getMold()), "Mold metal must be solidified");

        // 3. Verify that the mold is NOT scrapable, nor does extractComponent erase it
        assertFalse(ItemCastingMold.isScrapable(table.getMold()), "Valid partial fill must not be scrapable");
        assertNull(ItemCastingMold.extractComponent(table.getMold()), "Incomplete mold cannot extract component");
        assertTrue(ItemCastingMold.hasMetal(table.getMold()), "Mold metal must remain completely intact");
        assertEquals(partialCu, ItemCastingMold.getFilledMass(table.getMold()));

        // 4. Smelter melts remaining copper needed
        MaterialQuantity req = ItemCastingMold.getRequiredMass(table.getMold());
        MaterialQuantity needed = req.subtract(partialCu);
        smelter.vesselState()
            .addLayer(
                new AggregateLayer(
                    BuiltinMaterials.COPPER.ref(),
                    CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)
                        .asMolten(),
                    BuiltinMaterials.COPPER_COMPOSITION,
                    needed,
                    ActiveThermalState
                        .atTemperature(1400.0, 1.0, needed.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES),
                    AggregatePhaseType.MOLTEN_METAL));

        // 5. Complete pouring into mold
        assertTrue(smelter.pourIntoMold(table.getMold()));
        assertTrue(ItemCastingMold.isFilled(table.getMold()));
        assertEquals(req, ItemCastingMold.getFilledMass(table.getMold()));

        // Cool to solid and extract
        for (int i = 0; i < 500; i++) {
            table.coolMold(0.1);
            if (ItemCastingMold.isSolidified(table.getMold())) break;
        }
        assertTrue(ItemCastingMold.isSolidified(table.getMold()));
        ItemStack component = ItemCastingMold.extractComponent(table.getMold());
        assertNotNull(component);
        assertEquals(GeometryId.PICK_HEAD, ItemGenericComponent.getGeometry(component));
    }

    @Test
    void moldCoolingNeverDropsBelowEnvironmentalAmbientEquilibriumEnergy() {
        EnvironmentalTestCastingTable table = new EnvironmentalTestCastingTable();
        table.mockAmbient = 320.0; // Hot biome ambient (e.g. desert / savanna)
        table.mockSubmerged = true; // Aggressive water immersion quenching

        ItemStack mold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        MaterialQuantity cuMass = MaterialQuantity.ofGrams(268.0);
        // Mold starts just 0.5 Kelvin above ambient
        ActiveThermalState nearAmbient = ActiveThermalState
            .atTemperature(320.5, 0.0, cuMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        ItemCastingMold.fillMold(
            mold,
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
                BuiltinMaterials.COPPER_COMPOSITION,
                cuMass,
                nearAmbient,
                AggregatePhaseType.MOLTEN_METAL));
        table.setMold(mold);

        double ambientEquilibriumJoules = naturalexceptions.core.thermal.ThermodynamicModel
            .energyForTemperature(320.0, 0.0, cuMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);

        // Cool with an aggressively large time step (e.g. 60 seconds of quenching)
        table.coolMold(60.0);

        ActiveThermalState cooledThermal = ItemCastingMold.getThermalState(table.getMold());
        assertNotNull(cooledThermal);
        assertTrue(
            cooledThermal.energyJoules() >= ambientEquilibriumJoules - 1e-4,
            "Mold energy (" + cooledThermal.energyJoules()
                + " J) must not drop below environmental equilibrium floor ("
                + ambientEquilibriumJoules
                + " J)");
        assertTrue(
            cooledThermal.temperatureKelvin() >= 320.0 - 0.01,
            "Mold temperature (" + cooledThermal.temperatureKelvin()
                + " K) must never cool below ambient ("
                + 320.0
                + " K)");
    }

    @Test
    void smelterOutletPouringAbortsImmediatelyWhenTargetOrSourceDestroyed() {
        class MockOutlet extends TileEntitySmelterOutlet {

            TileEntityStarterSmelter mockSmelter;
            TileEntityCastingTable mockTable;

            @Override
            public TileEntityStarterSmelter findConnectedSmelter() {
                return mockSmelter;
            }

            @Override
            public TileEntityCastingTable findTargetCastingTable() {
                return mockTable;
            }
        }

        MockOutlet outlet = new MockOutlet();
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        TileEntityCastingTable table = new TileEntityCastingTable();
        ItemStack pickMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        table.setMold(pickMold);

        outlet.mockSmelter = smelter;
        outlet.mockTable = table;

        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("IsPouring", true);
        tag.setInteger("PourTicks", 25);
        outlet.readFromNBT(tag);
        assertTrue(outlet.isPouring());

        // 1. With valid smelter and table with empty mold, pouring continues
        outlet.updateEntity();
        assertTrue(outlet.isPouring());

        // 2. Break casting table (target becomes null)
        outlet.mockTable = null;
        outlet.updateEntity();
        assertFalse(outlet.isPouring(), "Pouring must abort immediately when casting table is broken");

        // 3. Reset and test removing mold mid-pour
        outlet.readFromNBT(tag);
        assertTrue(outlet.isPouring());
        outlet.mockTable = table;
        table.setMold(null); // Mold removed by player mid-pour
        outlet.updateEntity();
        assertFalse(outlet.isPouring(), "Pouring must abort immediately when mold is removed mid-pour");
    }

    @Test
    void healthyPartiallyFilledMoldIsNotScrapableAndTooltipDoesNotAdviseScraping() {
        ItemStack mold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        MaterialQuantity mass = MaterialQuantity.ofGrams(120.0);
        ActiveThermalState thermal = ActiveThermalState
            .atTemperature(1300.0, 0.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        ItemCastingMold.fillMold(
            mold,
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
                BuiltinMaterials.COPPER_COMPOSITION,
                mass,
                thermal,
                AggregatePhaseType.MOLTEN_METAL));

        assertFalse(ItemCastingMold.isFilled(mold));
        assertTrue(ItemCastingMold.hasMetal(mold));
        assertFalse(ItemCastingMold.isScrapable(mold), "Healthy partially filled mold must NOT be scrapable");

        java.util.ArrayList<String> tooltip = new java.util.ArrayList<>();
        NEItems.castingMold.addInformation(mold, null, tooltip, false);
        String tt = tooltip.toString();
        assertTrue(tt.contains("Partially filled"), tt);
        assertFalse(tt.contains("scrape clean"), tt);
        assertFalse(tt.contains("extraction is blocked"), tt);
    }
}
