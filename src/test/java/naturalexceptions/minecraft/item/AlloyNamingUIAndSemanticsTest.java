package naturalexceptions.minecraft.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentGeometry;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialNameRegistry;
import naturalexceptions.core.material.MaterialNameResolver;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.gui.ContainerMaterialNaming;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

public final class AlloyNamingUIAndSemanticsTest {

    @BeforeAll
    static void init() {
        try {
            net.minecraft.init.Bootstrap.func_151354_b();
        } catch (Throwable ignored) {}
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    @BeforeEach
    void reset() {
        MaterialNameRegistry.reset();
    }

    @Test
    void directMoldExtractionProducesAsCastState() {
        ItemStack moldStack = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        MaterialQuantity pickMass = ComponentGeometry.pickHead()
            .requiredMass(BuiltinMaterials.COPPER_PROPERTIES);

        ActiveThermalState moltenThermal = ActiveThermalState
            .atTemperature(1400.0, 1.0, pickMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        AggregateLayer layer = new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            BuiltinMaterials.COPPER_COMPOSITION,
            pickMass,
            moltenThermal,
            AggregatePhaseType.MOLTEN_METAL);

        assertTrue(ItemCastingMold.fillMold(moldStack, layer));

        // Cool to solid
        ActiveThermalState solidThermal = ActiveThermalState
            .atTemperature(293.15, 0.0, pickMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        ItemCastingMold.saveThermalState(moldStack, solidThermal);
        assertTrue(ItemCastingMold.isSolidified(moldStack));

        // Extract
        ItemStack extracted = ItemCastingMold.extractComponent(moldStack);
        assertNotNull(extracted);
        ComponentInstance comp = ItemGenericComponent.getComponent(extracted);
        assertNotNull(comp);
        assertEquals(GeometryId.PICK_HEAD, comp.geometryId());
        assertEquals(
            ManufacturingState.AS_CAST,
            comp.manufacturingState(),
            "Direct mold extraction must produce AS_CAST manufacturing state");
    }

    @Test
    void componentTooltipReflectsAssemblyReadinessAndManufacturingState() {
        ItemGenericComponent compItem = (ItemGenericComponent) NEItems.genericComponent;

        // 1. Pick Head (AS_CAST) -> is assembly ready
        ComponentInstance pickHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.AS_CAST);
        assertTrue(ItemGenericComponent.isAssemblyReady(pickHead), "Pick head must be assembly ready");

        ItemStack pickStack = ItemGenericComponent.createStack(pickHead);
        List<String> pickTooltip = new ArrayList<>();
        compItem.addInformation(pickStack, null, pickTooltip, false);

        boolean foundPickReady = false;
        for (String line : pickTooltip) {
            if (line.contains("State: As-Cast (Ready for assembly)")) {
                foundPickReady = true;
            }
        }
        assertTrue(foundPickReady, "Pick head tooltip must indicate As-Cast (Ready for assembly)");

        // 2. Blank (AS_CAST) -> is NOT assembly ready
        ComponentInstance blank = new ComponentInstance(
            GeometryId.BLANK,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            MaterialQuantity.ofKilograms(1.0),
            ManufacturingState.AS_CAST);
        assertFalse(ItemGenericComponent.isAssemblyReady(blank), "Blank geometry must NOT be assembly ready");

        ItemStack blankStack = ItemGenericComponent.createStack(blank);
        List<String> blankTooltip = new ArrayList<>();
        compItem.addInformation(blankStack, null, blankTooltip, false);

        boolean foundBlankState = false;
        boolean falselyReady = false;
        for (String line : blankTooltip) {
            if (line.contains("State: As-Cast")) {
                foundBlankState = true;
            }
            if (line.contains("(Ready for assembly)")) {
                falselyReady = true;
            }
        }
        assertTrue(foundBlankState, "Blank tooltip must indicate State: As-Cast");
        assertFalse(falselyReady, "Blank stock must NEVER claim to be ready for assembly");
    }

    @Test
    void playerAlloyNamingPropagatesAcrossAllMaterialCarriers() {
        CanonicalComposition redsteelComp = CanonicalComposition.builder()
            .add(BuiltinMaterials.IRON_RESOURCE_ID, 0.70)
            .add(BuiltinMaterials.COPPER_RESOURCE_ID, 0.30)
            .build();
        CanonicalMaterialKey key = CanonicalMaterialKey.of(redsteelComp);
        MaterialRef ref = MaterialRef.generatedFrom(redsteelComp);

        // Before naming: fallback composition format
        String defaultName = MaterialNameResolver.resolveDisplayName(ref, redsteelComp);
        assertTrue(defaultName.contains("Iron") || defaultName.contains("Fe") || defaultName.contains("Alloy"));

        // Register custom name
        MaterialNameRegistry.global()
            .registerCustomName(redsteelComp, "Redsteel");

        // After naming: resolves to "Redsteel"
        assertEquals("Redsteel", MaterialNameResolver.resolveDisplayName(ref, redsteelComp));
        assertEquals("Redsteel", MaterialNameResolver.resolveMaterialName(ref, key));

        // Component name
        ComponentInstance comp = new ComponentInstance(
            GeometryId.PICK_HEAD,
            ref,
            key,
            ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.IRON_PROPERTIES),
            ManufacturingState.AS_CAST);
        ItemStack compStack = ItemGenericComponent.createStack(comp);
        assertEquals("Pick Head", compStack.getDisplayName());

        // Assembly name
        ComponentInstance handle = new ComponentInstance(
            GeometryId.HANDLE,
            BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.WOOD_COMPOSITION),
            ComponentGeometry.handle()
                .requiredMass(BuiltinMaterials.WOOD_PROPERTIES),
            ManufacturingState.FINISHED);
        AssemblyGraph graph = AssemblyGraph.ofTool(comp, handle);
        ItemStack toolStack = ItemGenericAssembly.createStack(graph);
        assertEquals("Pickaxe", toolStack.getDisplayName());

        // Material Sample stack
        ItemStack sampleStack = ItemMaterialSample.createStack(ref, key);
        assertEquals("Material Sample", sampleStack.getDisplayName());
    }

    @Test
    void foundryLadleTooltipShowsGeneratedCompositionNameInsteadOfMaterialId() {
        CanonicalComposition composition = CanonicalComposition.builder()
            .add(BuiltinMaterials.IRON_RESOURCE_ID, 0.70)
            .add(BuiltinMaterials.COPPER_RESOURCE_ID, 0.30)
            .build();
        CanonicalMaterialKey key = CanonicalMaterialKey.of(composition)
            .asMolten();
        MaterialRef ref = MaterialRef.generatedFrom(composition);
        ItemStack ladle = new ItemStack(NEItems.foundryLadle);
        ItemFoundryLadle.fillLadle(
            ladle,
            new AggregateLayer(
                ref,
                key,
                composition,
                MaterialQuantity.ofGrams(250),
                ActiveThermalState.atTemperature(1600.0, 1.0, 0.25, BuiltinMaterials.IRON_PROPERTIES),
                AggregatePhaseType.MOLTEN_METAL));

        List<String> tooltip = new ArrayList<>();
        ((ItemFoundryLadle) NEItems.foundryLadle).addInformation(ladle, null, tooltip, false);

        assertTrue(tooltip.stream().anyMatch(line -> line.contains("Metal: ") && line.toLowerCase().contains("material")));
        assertFalse(
            tooltip.stream()
                .anyMatch(line -> line.contains(ref.asString()) || line.contains("Fe70Cu30")));
    }

    @Test
    void assembledToolNameUsesHeadMaterialWhileTooltipKeepsHandleMaterial() {
        ComponentInstance copperHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        ComponentInstance ironHandle = new ComponentInstance(
            GeometryId.HANDLE,
            BuiltinMaterials.IRON.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.IRON_COMPOSITION),
            ComponentGeometry.handle()
                .requiredMass(BuiltinMaterials.IRON_PROPERTIES),
            ManufacturingState.FINISHED);
        ItemStack pickaxe = ItemGenericAssembly.createStack(AssemblyGraph.ofTool(copperHead, ironHandle));

        assertEquals("Pickaxe", pickaxe.getDisplayName());

        List<String> tooltip = new ArrayList<>();
        ((ItemGenericAssembly) NEItems.genericAssembly).addInformation(pickaxe, null, tooltip, false);
        assertTrue(tooltip.stream().anyMatch(line -> line.contains("Handle: ") && line.contains("material")));
        assertFalse(tooltip.stream().anyMatch(line -> line.contains("Handle: ") && line.contains("Iron")));
    }

    @Test
    void sampleNamesAndColorsFollowTheirMaterial() {
        ItemStack copper = ItemMaterialSample.createStack(BuiltinMaterials.COPPER.ref());
        assertEquals("Material Sample", copper.getDisplayName());
        assertEquals(
            naturalexceptions.core.material.MaterialAppearance.getSolidColor(BuiltinMaterials.COPPER.ref(), null),
            copper.getItem()
                .getColorFromItemStack(copper, 0));
        CanonicalComposition composition = CanonicalComposition.builder()
            .add(BuiltinMaterials.GOLD_RESOURCE_ID, 0.67)
            .add(BuiltinMaterials.IRON_RESOURCE_ID, 0.33)
            .build();
        ItemStack sample = ItemMaterialSample
            .createStack(MaterialRef.generatedFrom(composition), CanonicalMaterialKey.of(composition));
        assertEquals("Material Sample", sample.getDisplayName());
    }

    @Test
    void namingTargetReadsFinishedToolHeadWithoutChangingNbt() {
        CanonicalComposition composition = CanonicalComposition.builder()
            .add(BuiltinMaterials.GOLD_RESOURCE_ID, 0.67)
            .add(BuiltinMaterials.IRON_RESOURCE_ID, 0.33)
            .build();
        CanonicalMaterialKey key = CanonicalMaterialKey.of(composition);
        ComponentInstance head = new ComponentInstance(
            GeometryId.PICK_HEAD,
            MaterialRef.generatedFrom(composition),
            key,
            MaterialQuantity.ofGrams(500),
            ManufacturingState.FINISHED);
        ComponentInstance handle = new ComponentInstance(
            GeometryId.HANDLE,
            BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.WOOD_COMPOSITION),
            MaterialQuantity.ofGrams(200),
            ManufacturingState.FINISHED);
        ItemStack tool = ItemGenericAssembly.createStack(AssemblyGraph.ofTool(head, handle));
        NBTTagCompound before = (NBTTagCompound) tool.getTagCompound()
            .copy();
        assertEquals(key, MaterialNamingTarget.getStateKey(tool));
        assertEquals(head.materialRef(), MaterialNamingTarget.getMaterialRef(tool));
        assertEquals(key, MaterialNamingTarget.getStateKey(ItemGenericComponent.createStack(head)));
        assertEquals(before, tool.getTagCompound());
    }

    @Test
    void ladleSamplePreservesIdentityMassAndEnergyAndCanBeRemelted() {
        ItemStack ladle = new ItemStack(NEItems.foundryLadle);
        CanonicalMaterialKey key = CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)
            .asMolten();
        ActiveThermalState thermal = ActiveThermalState
            .atTemperature(1450, 1, 0.25, BuiltinMaterials.COPPER_PROPERTIES);
        ItemFoundryLadle.fillLadle(
            ladle,
            new AggregateLayer(
                BuiltinMaterials.COPPER.ref(),
                key,
                key.composition(),
                MaterialQuantity.ofGrams(250),
                thermal,
                AggregatePhaseType.MOLTEN_METAL));
        ItemStack sample = ItemFoundryLadle.extractSample(ladle);
        assertNotNull(sample);
        assertEquals(key, ItemMaterialSample.getStateKey(sample));
        assertEquals(BuiltinMaterials.COPPER.ref(), ItemMaterialSample.getMaterialRef(sample));
        ActiveThermalState sampled = ItemMaterialSample.getThermalState(sample, BuiltinMaterials.COPPER_PROPERTIES);
        assertEquals(0.1, sampled.massKg(), 1e-9);
        assertEquals(MaterialQuantity.ofGrams(150), ItemFoundryLadle.getFilledMass(ladle));
        assertEquals(
            thermal.energyJoules(),
            sampled.energyJoules() + ItemFoundryLadle.getThermalState(ladle)
                .energyJoules(),
            1e-6);
        naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter smelter = new naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter();
        assertTrue(smelter.insertFeed(sample));
        assertEquals(
            MaterialQuantity.ofGrams(100),
            smelter.vesselState()
                .totalMass());
        ItemFoundryLadle.extractSample(ladle);
        ItemStack remainder = ItemFoundryLadle.extractSample(ladle);
        assertEquals(
            0.05,
            ItemMaterialSample.getThermalState(remainder, BuiltinMaterials.COPPER_PROPERTIES)
                .massKg(),
            1e-9);
        assertFalse(ItemFoundryLadle.hasMetal(ladle));
        assertNull(ItemFoundryLadle.extractSample(ladle));
    }

    @Test
    void alloySampleRetainsGeneratedIdentityAndFrozenLadlesCannotBeSampled() {
        CanonicalComposition composition = CanonicalComposition.builder()
            .add(BuiltinMaterials.GOLD_RESOURCE_ID, 0.67)
            .add(BuiltinMaterials.IRON_RESOURCE_ID, 0.33)
            .build();
        CanonicalMaterialKey key = CanonicalMaterialKey.of(composition)
            .asMolten();
        MaterialRef ref = MaterialRef.generatedFrom(key);
        naturalexceptions.core.property.MaterialProperties properties = new naturalexceptions.core.property.MaterialPropertyResolver()
            .resolve(key)
            .properties();
        ItemStack ladle = new ItemStack(NEItems.foundryLadle);
        ActiveThermalState hot = ActiveThermalState.atTemperature(2500, 1, 0.2, properties);
        ItemFoundryLadle.fillLadle(
            ladle,
            new AggregateLayer(
                ref,
                key,
                composition,
                MaterialQuantity.ofGrams(200),
                hot,
                AggregatePhaseType.MOLTEN_METAL));
        ItemStack sample = ItemFoundryLadle.extractSample(ladle);
        assertEquals(ref, ItemMaterialSample.getMaterialRef(sample));
        assertEquals(key, ItemMaterialSample.getStateKey(sample));
        ActiveThermalState cold = ActiveThermalState.ambient(0.1, properties);
        ItemFoundryLadle.saveThermalState(ladle, cold);
        NBTTagCompound before = (NBTTagCompound) ladle.getTagCompound()
            .copy();
        assertNull(ItemFoundryLadle.extractSample(ladle));
        assertEquals(before, ladle.getTagCompound());
    }

    @Test
    void alloyNamePersistenceAcrossNbtRoundTrip() {
        CanonicalComposition electrumComp = CanonicalComposition.builder()
            .add(BuiltinMaterials.GOLD_RESOURCE_ID, 0.50)
            .add(BuiltinMaterials.COPPER_RESOURCE_ID, 0.50)
            .build();

        MaterialNameRegistry.global()
            .registerCustomName(electrumComp, "Electrum Plus");
        assertEquals("Electrum Plus", MaterialNameRegistry.get(electrumComp));

        NBTTagCompound nbt = new NBTTagCompound();
        MaterialNameRegistry.global()
            .writeToNbt(nbt);

        // Clear registry
        MaterialNameRegistry.reset();
        assertNull(MaterialNameRegistry.get(electrumComp));

        // Restore from NBT
        MaterialNameRegistry.global()
            .readFromNbt(nbt);
        assertEquals("Electrum Plus", MaterialNameRegistry.get(electrumComp));
    }

    @Test
    void cleanNormalTooltipsWithoutRawHashesOrInternalIds() {
        ComponentInstance pickHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.AS_CAST);
        ItemStack pickStack = ItemGenericComponent.createStack(pickHead);

        List<String> tooltip = new ArrayList<>();
        ((ItemGenericComponent) NEItems.genericComponent).addInformation(pickStack, null, tooltip, false);

        for (String line : tooltip) {
            assertFalse(line.contains("mat:authored:"), "Normal tooltip must not contain raw material URI: " + line);
            assertFalse(line.contains("mat:generated:"), "Normal tooltip must not contain raw generated URI: " + line);
            assertFalse(line.contains("comp="), "Normal tooltip must not contain serialized state key: " + line);
            assertFalse(line.contains("phase="), "Normal tooltip must not contain serialized state key: " + line);
            assertFalse(line.contains("micro="), "Normal tooltip must not contain serialized state key: " + line);
            assertFalse(line.contains("cond="), "Normal tooltip must not contain serialized state key: " + line);
        }
    }

    public static class TestNamingPlayer extends net.minecraft.entity.player.EntityPlayer {

        public boolean sneaking = false;
        public ItemStack heldItem = null;
        public boolean alive = true;

        public TestNamingPlayer() {
            super(null, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "TestPlayer"));
        }

        public static TestNamingPlayer create(boolean sneaking) {
            try {
                java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
                TestNamingPlayer player = (TestNamingPlayer) unsafe.allocateInstance(TestNamingPlayer.class);
                player.sneaking = sneaking;
                player.alive = true;
                return player;
            } catch (Exception e) {
                throw new RuntimeException("Failed to allocate TestNamingPlayer", e);
            }
        }

        @Override
        public ItemStack getHeldItem() {
            return heldItem;
        }

        @Override
        public boolean isEntityAlive() {
            return alive;
        }

        @Override
        public boolean isSneaking() {
            return sneaking;
        }

        @Override
        public void addChatMessage(net.minecraft.util.IChatComponent chat) {}

        @Override
        public boolean canCommandSenderUseCommand(int permLevel, String commandName) {
            return false;
        }

        @Override
        public net.minecraft.util.ChunkCoordinates getPlayerCoordinates() {
            return new net.minecraft.util.ChunkCoordinates(0, 0, 0);
        }
    }

    @Test
    void testSneakRightClickNamingAffordanceAndTooltips() {
        TestNamingPlayer sneakingPlayer = TestNamingPlayer.create(true);
        TestNamingPlayer standingPlayer = TestNamingPlayer.create(false);

        CanonicalComposition comp = CanonicalComposition.builder()
            .add(BuiltinMaterials.COPPER_RESOURCE_ID, 0.70)
            .add(BuiltinMaterials.TIN_RESOURCE_ID, 0.30)
            .build();
        CanonicalMaterialKey genKey = CanonicalMaterialKey.of(comp);
        MaterialRef genRef = MaterialRef.generatedFrom(genKey);

        // 1. Component
        ComponentInstance genComponent = new ComponentInstance(
            GeometryId.PICK_HEAD,
            genRef,
            genKey,
            MaterialQuantity.ofGrams(300),
            ManufacturingState.FINISHED);
        ItemStack compStack = ItemGenericComponent.createStack(genComponent);

        ComponentInstance copperComponent = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            MaterialQuantity.ofGrams(300),
            ManufacturingState.FINISHED);
        ItemStack copperCompStack = ItemGenericComponent.createStack(copperComponent);

        assertTrue(MaterialNamingTarget.openIfSneaking(compStack, null, sneakingPlayer));
        assertFalse(MaterialNamingTarget.openIfSneaking(compStack, null, standingPlayer));
        assertFalse(MaterialNamingTarget.openIfSneaking(copperCompStack, null, sneakingPlayer));

        List<String> compTooltip = new ArrayList<>();
        ((ItemGenericComponent) compStack.getItem()).addInformation(compStack, null, compTooltip, false);
        assertTrue(
            compTooltip.stream()
                .anyMatch(l -> l.contains("Sneak-right-click to name generated material")));

        List<String> cuCompTooltip = new ArrayList<>();
        ((ItemGenericComponent) copperCompStack.getItem()).addInformation(copperCompStack, null, cuCompTooltip, false);
        assertFalse(
            cuCompTooltip.stream()
                .anyMatch(l -> l.contains("Sneak-right-click to name generated material")));

        // 2. Assembled Tool (targets head, not handle)
        ComponentInstance handle = new ComponentInstance(
            GeometryId.HANDLE,
            BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.WOOD_COMPOSITION),
            MaterialQuantity.ofGrams(100),
            ManufacturingState.FINISHED);
        ItemStack toolStack = ItemGenericAssembly.createStack(AssemblyGraph.ofTool(genComponent, handle));
        ItemStack copperToolStack = ItemGenericAssembly.createStack(AssemblyGraph.ofTool(copperComponent, handle));

        assertEquals(genRef, MaterialNamingTarget.getMaterialRef(toolStack));
        assertEquals(genKey, MaterialNamingTarget.getStateKey(toolStack));
        assertTrue(MaterialNamingTarget.openIfSneaking(toolStack, null, sneakingPlayer));
        assertFalse(MaterialNamingTarget.openIfSneaking(toolStack, null, standingPlayer));
        assertFalse(MaterialNamingTarget.openIfSneaking(copperToolStack, null, sneakingPlayer));

        List<String> toolTooltip = new ArrayList<>();
        ((ItemGenericAssembly) toolStack.getItem()).addInformation(toolStack, null, toolTooltip, false);
        assertTrue(
            toolTooltip.stream()
                .anyMatch(l -> l.contains("Sneak-right-click to name generated material")));

        List<String> cuToolTooltip = new ArrayList<>();
        ((ItemGenericAssembly) copperToolStack.getItem()).addInformation(copperToolStack, null, cuToolTooltip, false);
        assertFalse(
            cuToolTooltip.stream()
                .anyMatch(l -> l.contains("Sneak-right-click to name generated material")));

        // 3. Material Sample
        ItemStack sampleStack = ItemMaterialSample.createStack(genRef, genKey);
        ItemStack copperSampleStack = ItemMaterialSample.createStack(BuiltinMaterials.COPPER.ref());

        assertTrue(MaterialNamingTarget.openIfSneaking(sampleStack, null, sneakingPlayer));
        assertFalse(MaterialNamingTarget.openIfSneaking(sampleStack, null, standingPlayer));
        assertFalse(MaterialNamingTarget.openIfSneaking(copperSampleStack, null, sneakingPlayer));

        List<String> sampleTooltip = new ArrayList<>();
        ((ItemMaterialSample) sampleStack.getItem()).addInformation(sampleStack, null, sampleTooltip, false);
        assertTrue(
            sampleTooltip.stream()
                .anyMatch(l -> l.contains("Sneak-right-click to name generated material")));

        List<String> cuSampleTooltip = new ArrayList<>();
        ((ItemMaterialSample) copperSampleStack.getItem())
            .addInformation(copperSampleStack, null, cuSampleTooltip, false);
        assertFalse(
            cuSampleTooltip.stream()
                .anyMatch(l -> l.contains("Sneak-right-click to name generated material")));
    }

    @Test
    void testMoltenVsSolidSampleDynamicColor() {
        MaterialRef ref = BuiltinMaterials.COPPER.ref();
        naturalexceptions.core.property.MaterialProperties props = BuiltinMaterials.COPPER_PROPERTIES;

        // Solid sample
        ItemStack solidSample = ItemMaterialSample.createStack(ref);
        int solidColor = solidSample.getItem()
            .getColorFromItemStack(solidSample, 0);
        int expectedSolid = naturalexceptions.core.material.MaterialAppearance.getSolidColor(ref, null);
        assertEquals(expectedSolid, solidColor);

        // Molten sample
        ActiveThermalState moltenThermal = ActiveThermalState.atTemperature(1450.0, 1.0, 0.1, props);
        CanonicalMaterialKey moltenKey = CanonicalMaterialKey
            .of(CanonicalComposition.of(BuiltinMaterials.COPPER_RESOURCE_ID))
            .asMolten();
        ItemStack moltenSample = ItemMaterialSample.createStack(ref, moltenKey, moltenThermal);
        int moltenColor = moltenSample.getItem()
            .getColorFromItemStack(moltenSample, 0);
        int expectedMolten = naturalexceptions.core.material.MaterialAppearance.getMoltenColor(ref, moltenKey, 1450.0);
        assertEquals(expectedMolten, moltenColor);
        assertNotEquals(
            solidColor,
            moltenColor,
            "Molten sample color must differ from solid color due to thermal glow");
    }

    @Test
    void testShiftRightClickOnBlocksTriggersOnItemUseFirst() {
        TestNamingPlayer sneakingPlayer = TestNamingPlayer.create(true);
        TestNamingPlayer standingPlayer = TestNamingPlayer.create(false);

        CanonicalComposition comp = CanonicalComposition.builder()
            .add(BuiltinMaterials.COPPER_RESOURCE_ID, 0.70)
            .add(BuiltinMaterials.TIN_RESOURCE_ID, 0.30)
            .build();
        CanonicalMaterialKey genKey = CanonicalMaterialKey.of(comp);
        MaterialRef genRef = MaterialRef.generatedFrom(genKey);

        // Component
        ComponentInstance genComponent = new ComponentInstance(
            GeometryId.PICK_HEAD,
            genRef,
            genKey,
            MaterialQuantity.ofGrams(300),
            ManufacturingState.FINISHED);
        ItemStack compStack = ItemGenericComponent.createStack(genComponent);

        ComponentInstance copperComponent = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            MaterialQuantity.ofGrams(300),
            ManufacturingState.FINISHED);
        ItemStack copperCompStack = ItemGenericComponent.createStack(copperComponent);

        // Assembly
        ComponentInstance handle = new ComponentInstance(
            GeometryId.HANDLE,
            BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.WOOD_COMPOSITION),
            MaterialQuantity.ofGrams(100),
            ManufacturingState.FINISHED);
        ItemStack toolStack = ItemGenericAssembly.createStack(AssemblyGraph.ofTool(genComponent, handle));
        ItemStack copperToolStack = ItemGenericAssembly.createStack(AssemblyGraph.ofTool(copperComponent, handle));

        // Sample
        ItemStack sampleStack = ItemMaterialSample.createStack(genRef, genKey);
        ItemStack copperSampleStack = ItemMaterialSample.createStack(BuiltinMaterials.COPPER.ref());

        // Sneaking on block calls onItemUseFirst -> true for alloys
        assertTrue(
            compStack.getItem()
                .onItemUseFirst(compStack, sneakingPlayer, null, 0, 64, 0, 1, 0.5f, 1.0f, 0.5f));
        assertTrue(
            toolStack.getItem()
                .onItemUseFirst(toolStack, sneakingPlayer, null, 0, 64, 0, 1, 0.5f, 1.0f, 0.5f));
        assertTrue(
            sampleStack.getItem()
                .onItemUseFirst(sampleStack, sneakingPlayer, null, 0, 64, 0, 1, 0.5f, 1.0f, 0.5f));

        // onItemUse also returns true when sneaking on blocks
        assertTrue(
            compStack.getItem()
                .onItemUse(compStack, sneakingPlayer, null, 0, 64, 0, 1, 0.5f, 1.0f, 0.5f));
        assertTrue(
            toolStack.getItem()
                .onItemUse(toolStack, sneakingPlayer, null, 0, 64, 0, 1, 0.5f, 1.0f, 0.5f));
        assertTrue(
            sampleStack.getItem()
                .onItemUse(sampleStack, sneakingPlayer, null, 0, 64, 0, 1, 0.5f, 1.0f, 0.5f));

        // Standing player on block does not trigger naming
        assertFalse(
            compStack.getItem()
                .onItemUseFirst(compStack, standingPlayer, null, 0, 64, 0, 1, 0.5f, 1.0f, 0.5f));
        assertFalse(
            toolStack.getItem()
                .onItemUseFirst(toolStack, standingPlayer, null, 0, 64, 0, 1, 0.5f, 1.0f, 0.5f));
        assertFalse(
            sampleStack.getItem()
                .onItemUseFirst(sampleStack, standingPlayer, null, 0, 64, 0, 1, 0.5f, 1.0f, 0.5f));

        // Pure copper does not trigger naming even when sneaking
        assertFalse(
            copperCompStack.getItem()
                .onItemUseFirst(copperCompStack, sneakingPlayer, null, 0, 64, 0, 1, 0.5f, 1.0f, 0.5f));
        assertFalse(
            copperToolStack.getItem()
                .onItemUseFirst(copperToolStack, sneakingPlayer, null, 0, 64, 0, 1, 0.5f, 1.0f, 0.5f));
        assertFalse(
            copperSampleStack.getItem()
                .onItemUseFirst(copperSampleStack, sneakingPlayer, null, 0, 64, 0, 1, 0.5f, 1.0f, 0.5f));
    }

    @Test
    void testBronzeAuthoredAlloyNamingAffordance() {
        TestNamingPlayer sneakingPlayer = TestNamingPlayer.create(true);
        TestNamingPlayer standingPlayer = TestNamingPlayer.create(false);

        // Bronze is an authored alloy (88% Cu, 12% Sn)
        MaterialRef bronzeRef = BuiltinMaterials.BRONZE.ref();
        CanonicalMaterialKey bronzeKey = CanonicalMaterialKey.of(BuiltinMaterials.BRONZE_COMPOSITION);

        ComponentInstance bronzeHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            bronzeRef,
            bronzeKey,
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);
        ItemStack bronzeComp = ItemGenericComponent.createStack(bronzeHead);

        ComponentInstance handle = new ComponentInstance(
            GeometryId.HANDLE,
            BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.WOOD_COMPOSITION),
            MaterialQuantity.ofGrams(80.0),
            ManufacturingState.FINISHED);
        ItemStack bronzeTool = ItemGenericAssembly.createStack(AssemblyGraph.ofTool(bronzeHead, handle));

        ItemStack bronzeSample = ItemMaterialSample.createStack(bronzeRef, bronzeKey);

        // canOpenNaming recognizes authored multi-element alloy
        assertTrue(MaterialNamingTarget.canOpenNaming(bronzeComp));
        assertTrue(MaterialNamingTarget.canOpenNaming(bronzeTool));
        assertTrue(MaterialNamingTarget.canOpenNaming(bronzeSample));

        // Sneak-right-click opens naming
        assertTrue(MaterialNamingTarget.openIfSneaking(bronzeComp, null, sneakingPlayer));
        assertTrue(MaterialNamingTarget.openIfSneaking(bronzeTool, null, sneakingPlayer));
        assertTrue(MaterialNamingTarget.openIfSneaking(bronzeSample, null, sneakingPlayer));

        // Standing player does not
        assertFalse(MaterialNamingTarget.openIfSneaking(bronzeComp, null, standingPlayer));
        assertFalse(MaterialNamingTarget.openIfSneaking(bronzeTool, null, standingPlayer));
        assertFalse(MaterialNamingTarget.openIfSneaking(bronzeSample, null, standingPlayer));

        // Tooltips display naming prompt for Bronze alloy
        List<String> compTooltip = new ArrayList<>();
        ((ItemGenericComponent) bronzeComp.getItem()).addInformation(bronzeComp, null, compTooltip, false);
        assertTrue(
            compTooltip.stream()
                .anyMatch(l -> l.contains("Sneak-right-click to name generated material")));

        List<String> toolTooltip = new ArrayList<>();
        ((ItemGenericAssembly) bronzeTool.getItem()).addInformation(bronzeTool, null, toolTooltip, false);
        assertTrue(
            toolTooltip.stream()
                .anyMatch(l -> l.contains("Sneak-right-click to name generated material")));

        List<String> sampleTooltip = new ArrayList<>();
        ((ItemMaterialSample) bronzeSample.getItem()).addInformation(bronzeSample, null, sampleTooltip, false);
        assertTrue(
            sampleTooltip.stream()
                .anyMatch(l -> l.contains("Sneak-right-click to name generated material")));
    }

    @Test
    void testContainerMaterialNamingCanInteractWithStability() {
        TestNamingPlayer player = TestNamingPlayer.create(true);

        CanonicalComposition comp = CanonicalComposition.builder()
            .add(BuiltinMaterials.COPPER_RESOURCE_ID, 0.70)
            .add(BuiltinMaterials.TIN_RESOURCE_ID, 0.30)
            .build();
        ItemStack alloyStack = ItemMaterialSample.createStack(MaterialRef.generatedFrom(comp), comp);

        player.heldItem = alloyStack;
        ContainerMaterialNaming container = new ContainerMaterialNaming(player, alloyStack);

        // 1. Initial stack reference equality holds
        assertTrue(container.canInteractWith(player));

        // 2. Deserialized / cloned stack (simulating packet reserialization or inventory tick)
        ItemStack deserializedStack = alloyStack.copy();
        assertNotSame(alloyStack, deserializedStack);
        player.heldItem = deserializedStack;
        assertTrue(
            container.canInteractWith(player),
            "Container must survive ItemStack reference changes when holding the same alloy");

        // 3. Dropped item / empty hand closes container
        player.heldItem = null;
        assertFalse(container.canInteractWith(player));

        // 4. Switching to unnameable item closes container
        player.heldItem = ItemMaterialSample.createStack(BuiltinMaterials.COPPER.ref());
        assertFalse(container.canInteractWith(player));

        // 5. Dead player closes container
        player.heldItem = alloyStack;
        player.alive = false;
        assertFalse(container.canInteractWith(player));

        // 6. Different player cannot interact
        TestNamingPlayer intruder = TestNamingPlayer.create(true);
        intruder.heldItem = alloyStack;
        assertFalse(container.canInteractWith(intruder));
    }
}
