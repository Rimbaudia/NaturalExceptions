package naturalexceptions.minecraft.metallurgy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.OreFeedstockDefinition;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

final class FeedstockInsertionIntegrationTest {

    private static final String RAW_HOST = "oreNaturalExceptionsTestFuture";
    private static final String METAL_HOST = "ingotNaturalExceptionsTestFuture";
    private static OreFeedstockDefinition futureFeedstock;
    private static Item rawHost;
    private static Item productHost;

    @BeforeAll
    static void init() {
        NEItems.ensureBootstrapped();
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();

        CanonicalComposition alloy = CanonicalComposition.builder()
            .add(BuiltinMaterials.COPPER_RESOURCE_ID, 0.57)
            .add(BuiltinMaterials.TIN_RESOURCE_ID, 0.23)
            .add(BuiltinMaterials.LEAD_RESOURCE_ID, 0.20)
            .build();
        MaterialRef productRef = MaterialRef.generatedFrom(alloy);
        futureFeedstock = new OreFeedstockDefinition(
            ResourceId.of("naturalexceptions", "test_future_ore"),
            naturalexceptions.core.metallurgy.MaterialQuantity.ofGrams(100.0),
            0.60,
            0.35,
            0.05,
            productRef,
            alloy,
            new MaterialPropertyResolver().resolve(productRef, alloy)
                .properties());

        FeedstockItemRegistry.registerFeedstock(futureFeedstock, RAW_HOST, METAL_HOST);
        rawHost = new Item();
        productHost = new Item();
        FeedstockItemRegistry
            .registerItem(rawHost, -1, futureFeedstock.feedstockId(), FeedstockItemRegistry.InputKind.RAW_ORE);
        FeedstockItemRegistry.registerItem(
            productHost,
            -1,
            futureFeedstock.feedstockId(),
            FeedstockItemRegistry.InputKind.METALLIC_PRODUCT);
    }

    @Test
    void registeredFutureFeedstockUsesTheGenericSmelterSlotResolver() {
        ItemStack rawStack = new ItemStack(rawHost);
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();

        assertTrue(TileEntityStarterSmelter.isSmeltableFeed(rawStack));
        assertTrue(smelter.isItemValidForSlot(TileEntityStarterSmelter.SLOT_FEED, rawStack));
        assertTrue(smelter.insertFeed(rawStack));
        assertEquals(0, rawStack.stackSize);

        AggregateLayer layer = smelter.vesselState()
            .layers()
            .get(0);
        assertEquals(CanonicalComposition.of(futureFeedstock.feedstockId()), layer.composition());
    }

    @Test
    void registeredFutureFeedstockProductUsesItsResolvedMaterialIdentity() {
        ItemStack productStack = new ItemStack(productHost);
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();

        assertTrue(smelter.insertFeed(productStack));
        assertEquals(0, productStack.stackSize);
        assertEquals(
            futureFeedstock.metallicProductRef(),
            smelter.vesselState()
                .layers()
                .get(0)
                .materialRef());
        assertEquals(
            futureFeedstock.metallicProductComposition(),
            smelter.vesselState()
                .layers()
                .get(0)
                .composition());
    }

    @Test
    void filledFluidVesselEntersTheOrdinarySmelterWithMassAndThermalEnergy() {
        BuiltinContent.register();
        naturalexceptions.core.event.EventRecord rain = naturalexceptions.core.event.EventInstanceFactory
            .protectedFirstStarRain(121L, 2, 0L, 0L, 0L, 100L);
        naturalexceptions.core.event.WorldEventSchedule schedule = new naturalexceptions.core.event.WorldEventSchedule();
        schedule.ensureProtectedFirstStarRain(rain);
        schedule.advance(rain.scheduledAtTick());
        naturalexceptions.core.event.StarRainHistory history = new naturalexceptions.core.event.StarRainHistory();
        history.synchronize(schedule.snapshot(), 0, 0);
        naturalexceptions.core.event.StarRainImpact impact = history.dueImpacts(rain.endsAtTick(), 1)
            .get(0);
        assertTrue(history.resolve(impact.id()));
        naturalexceptions.core.metallurgy.MaterialQuantity mass = impact.stellarMass();
        ActiveThermalState thermal = ActiveThermalState
            .atTemperature(1780.0, 1.0, mass.toKilograms(), BuiltinMaterials.STELLAR_MATTER_PROPERTIES);
        naturalexceptions.core.material.CanonicalMaterialKey key = naturalexceptions.core.material.CanonicalMaterialKey
            .of(
                BuiltinMaterials.STELLAR_MATTER_COMPOSITION,
                naturalexceptions.core.material.MaterialPhase.LIQUID,
                null,
                null);
        naturalexceptions.core.fluid.FluidParcel parcel = new naturalexceptions.core.fluid.FluidParcel(
            BuiltinMaterials.STELLAR_MATTER.ref(),
            key,
            BuiltinMaterials.STELLAR_MATTER_COMPOSITION,
            mass,
            thermal);
        naturalexceptions.core.fluid.FluidWorldState spatial = new naturalexceptions.core.fluid.FluidWorldState();
        naturalexceptions.core.fluid.FluidCellKey site = new naturalexceptions.core.fluid.FluidCellKey(
            0,
            impact.x(),
            64,
            impact.z());
        spatial.put(site, parcel);
        assertEquals(mass, history.transferToSpatialSite(impact.id()));
        naturalexceptions.core.fluid.FluidParcel recovered = spatial.remove(site);
        assertEquals(mass, recovered.mass());
        ItemStack vessel = new ItemStack(NEItems.fluidVessel);
        naturalexceptions.minecraft.item.ItemFluidVessel.setContents(vessel, recovered);
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();

        assertTrue(TileEntityStarterSmelter.isSmeltableFeed(vessel));
        assertTrue(smelter.insertFeed(vessel));
        assertEquals(1, vessel.stackSize, "The reusable vessel is returned, not consumed");
        assertFalse(naturalexceptions.minecraft.item.ItemFluidVessel.isFilled(vessel));
        AggregateLayer layer = smelter.vesselState()
            .layers()
            .get(0);
        assertEquals(BuiltinMaterials.STELLAR_MATTER.ref(), layer.materialRef());
        assertEquals(mass, layer.mass());
        assertEquals(
            thermal.energyJoules(),
            layer.thermalState()
                .energyJoules(),
            1.0e-6);
        assertTrue(naturalexceptions.minecraft.item.ItemFluidVessel.contents(vessel) == null);

        naturalexceptions.core.metallurgy.MaterialQuantity bladeMass = naturalexceptions.core.fabrication.ComponentGeometry
            .blade()
            .requiredMass(BuiltinMaterials.STELLAR_MATTER_PROPERTIES);
        AggregateLayer molten = smelter.vesselState()
            .pourLiquidMetal(BuiltinMaterials.STELLAR_MATTER.ref(), bladeMass);
        assertNotNull(molten);
        ItemStack mold = naturalexceptions.minecraft.item.ItemCastingMold
            .createStack(naturalexceptions.core.fabrication.GeometryId.BLADE);
        assertTrue(naturalexceptions.minecraft.item.ItemCastingMold.fillMold(mold, molten));
        naturalexceptions.minecraft.item.ItemCastingMold.saveThermalState(
            mold,
            ActiveThermalState.ambient(bladeMass.toKilograms(), BuiltinMaterials.STELLAR_MATTER_PROPERTIES));
        ItemStack componentStack = naturalexceptions.minecraft.item.ItemCastingMold.extractComponent(mold);
        assertNotNull(componentStack);
        naturalexceptions.core.fabrication.ComponentInstance component = naturalexceptions.minecraft.item.ItemGenericComponent
            .getComponent(componentStack);
        assertEquals(BuiltinMaterials.STELLAR_MATTER.ref(), component.materialRef());
        component.damage()
            .addCracking(0.2);
        assertEquals(
            0.2,
            component.damage()
                .cracking(),
            1.0e-9);

        naturalexceptions.core.fabrication.ComponentInstance handle = ItemGenericComponent
            .getComponent(ItemGenericComponent.createWoodenHandle());
        naturalexceptions.core.assembly.AssemblyGraph tool = naturalexceptions.core.assembly.AssemblyGraph
            .ofTool(component, handle);
        naturalexceptions.core.assembly.DerivedCapability capability = ItemGenericAssembly.getCapability(tool);
        assertNotNull(capability);
        assertTrue(
            capability.penetration() > 0.0,
            "P5 derives usable capability from the stellar head geometry/material");
    }

    @Test
    void stellarAndMundaneLiquidUseTheGenericCompositionMergeWithoutARecipe() {
        BuiltinContent.register();
        naturalexceptions.core.fluid.FluidParcel stellar = liquidParcel(
            BuiltinMaterials.STELLAR_MATTER.ref(),
            BuiltinMaterials.STELLAR_MATTER_COMPOSITION,
            BuiltinMaterials.STELLAR_MATTER_PROPERTIES,
            naturalexceptions.core.metallurgy.MaterialQuantity.ofGrams(100.0));
        naturalexceptions.core.fluid.FluidParcel copper = liquidParcel(
            BuiltinMaterials.COPPER.ref(),
            BuiltinMaterials.COPPER_COMPOSITION,
            BuiltinMaterials.COPPER_PROPERTIES,
            naturalexceptions.core.metallurgy.MaterialQuantity.ofGrams(100.0));
        naturalexceptions.core.fluid.FluidVessel vessel = new naturalexceptions.core.fluid.FluidVessel(
            300000L,
            (lower, incoming) -> naturalexceptions.core.fluid.FluidInteractionRules.Result.MISCIBLE);

        vessel.fill(stellar);
        vessel.fill(copper);

        assertEquals(
            1,
            vessel.contents()
                .size());
        assertTrue(
            vessel.contents()
                .get(0)
                .materialRef()
                .isGenerated());
        assertEquals(naturalexceptions.core.metallurgy.MaterialQuantity.ofGrams(200.0), vessel.totalMass());
        assertTrue(
            vessel.contents()
                .get(0)
                .composition()
                .contains(BuiltinMaterials.STELLAR_MATTER_RESOURCE_ID));
        assertTrue(
            vessel.contents()
                .get(0)
                .composition()
                .contains(BuiltinMaterials.COPPER_RESOURCE_ID));
    }

    private static naturalexceptions.core.fluid.FluidParcel liquidParcel(MaterialRef ref,
        CanonicalComposition composition, naturalexceptions.core.property.MaterialProperties properties,
        naturalexceptions.core.metallurgy.MaterialQuantity mass) {
        naturalexceptions.core.material.CanonicalMaterialKey key = CanonicalMaterialKey
            .of(composition, naturalexceptions.core.material.MaterialPhase.LIQUID, null, null);
        ActiveThermalState thermal = ActiveThermalState.atTemperature(1800.0, 1.0, mass.toKilograms(), properties);
        return new naturalexceptions.core.fluid.FluidParcel(ref, key, composition, mass, thermal);
    }

    @Test
    void oreDictionaryFeedstocksAreDirectlyResolvableAndSmeltable() {
        Item customTinItem = new Item().setUnlocalizedName("customTinOre");
        cpw.mods.fml.common.registry.GameRegistry.registerItem(customTinItem, "testCustomTinOre");
        ItemStack tinStack = new ItemStack(customTinItem);

        Item customLeadItem = new Item().setUnlocalizedName("customLeadIngot");
        cpw.mods.fml.common.registry.GameRegistry.registerItem(customLeadItem, "testCustomLeadIngot");
        ItemStack leadStack = new ItemStack(customLeadItem);

        Item customBronzeDustItem = new Item().setUnlocalizedName("customBronzeDust");
        cpw.mods.fml.common.registry.GameRegistry.registerItem(customBronzeDustItem, "testCustomBronzeDust");
        ItemStack bronzeDustStack = new ItemStack(customBronzeDustItem);

        net.minecraftforge.oredict.OreDictionary.registerOre("oreTin", tinStack);
        net.minecraftforge.oredict.OreDictionary.registerOre("ingotLead", leadStack);
        net.minecraftforge.oredict.OreDictionary.registerOre("dustBronze", bronzeDustStack);

        // 1. Check resolve
        FeedstockItemRegistry.ResolvedInput tinResolved = FeedstockItemRegistry.resolve(tinStack);
        assertNotNull(tinResolved, "Tin ore item must resolve via OreDictionary");
        assertEquals(FeedstockItemRegistry.InputKind.RAW_ORE, tinResolved.kind());

        FeedstockItemRegistry.ResolvedInput leadResolved = FeedstockItemRegistry.resolve(leadStack);
        assertNotNull(leadResolved, "Lead ingot item must resolve via OreDictionary");
        assertEquals(FeedstockItemRegistry.InputKind.METALLIC_PRODUCT, leadResolved.kind());

        FeedstockItemRegistry.ResolvedInput bronzeResolved = FeedstockItemRegistry.resolve(bronzeDustStack);
        assertNotNull(bronzeResolved, "Bronze dust item must resolve via OreDictionary");
        assertEquals(FeedstockItemRegistry.InputKind.METALLIC_PRODUCT, bronzeResolved.kind());

        // 2. Check insertion into smelter
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();
        assertTrue(smelter.insertFeed(tinStack));
        assertEquals(0, tinStack.stackSize);
        assertEquals(
            1,
            smelter.vesselState()
                .layers()
                .size());

        assertTrue(smelter.insertFeed(leadStack));
        assertEquals(0, leadStack.stackSize);
        assertEquals(
            2,
            smelter.vesselState()
                .layers()
                .size());

        assertTrue(smelter.insertFeed(bronzeDustStack));
        assertEquals(0, bronzeDustStack.stackSize);
        assertEquals(
            3,
            smelter.vesselState()
                .layers()
                .size());
    }

    @Test
    void legacyTenKilogramMassCeilingIsNoLongerTheVesselCapacity() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();

        // A detached domain fixture has no discovered geometry; 10 kg is no longer a capacity threshold.
        AggregateLayer heavyLayer = new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            BuiltinMaterials.COPPER_COMPOSITION,
            naturalexceptions.core.metallurgy.MaterialQuantity.ofKilograms(9.5),
            ActiveThermalState.ambient(9.5, BuiltinMaterials.COPPER_PROPERTIES),
            naturalexceptions.core.metallurgy.AggregatePhaseType.SOLID_CHARGE);
        smelter.vesselState()
            .remelt(heavyLayer);

        // The high mass safety ceiling must not reject a batch merely for exceeding the old 10 kg constant.
        ItemStack ingot = new ItemStack(productHost, 1);
        boolean inserted = smelter.insertFeed(ingot);
        assertTrue(inserted, "Without a discovered cavity, the legacy mass threshold is not applied");
        assertEquals(0, ingot.stackSize, "Accepted feedstock is consumed");
    }

    @Test
    void slotValidationAcceptsFeedstocksAndEmptyMoldsInCorrectSlots() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();

        ItemStack rawStack = new ItemStack(rawHost);
        ItemStack emptyMold = naturalexceptions.minecraft.item.ItemCastingMold
            .createStack(naturalexceptions.core.fabrication.GeometryId.PICK_HEAD);
        ItemStack filledMold = emptyMold.copy();
        naturalexceptions.minecraft.item.ItemCastingMold.fillMold(
            filledMold,
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)
                    .asMolten(),
                BuiltinMaterials.COPPER_COMPOSITION,
                naturalexceptions.core.metallurgy.MaterialQuantity.ofGrams(100.0),
                ActiveThermalState.ambient(0.1, BuiltinMaterials.COPPER_PROPERTIES),
                naturalexceptions.core.metallurgy.AggregatePhaseType.MOLTEN_METAL));

        // Feed slot: accepts raw ore, metallic products, and filled molds; rejects empty molds
        assertTrue(smelter.isItemValidForSlot(TileEntityStarterSmelter.SLOT_FEED, rawStack));
        assertTrue(smelter.isItemValidForSlot(TileEntityStarterSmelter.SLOT_FEED, filledMold));
        assertFalse(smelter.isItemValidForSlot(TileEntityStarterSmelter.SLOT_FEED, emptyMold));

        // Smelter output slot is no longer an alternate mold/pour input.
        assertFalse(smelter.isItemValidForSlot(TileEntityStarterSmelter.SLOT_MOLD, emptyMold));
        assertFalse(smelter.isItemValidForSlot(TileEntityStarterSmelter.SLOT_MOLD, filledMold));
        assertFalse(smelter.isItemValidForSlot(TileEntityStarterSmelter.SLOT_MOLD, rawStack));
    }
}
