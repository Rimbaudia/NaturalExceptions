package naturalexceptions.minecraft.recipe;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentGeometry;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialAppearance;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

final class ToolAssemblyAndCastingPassTest {

    private static final class DummyContainer extends Container {

        @Override
        public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) {
            return true;
        }
    }

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
    void testMaterialAppearanceColors() {
        // Authored solid colors
        assertEquals(0xD36C4C, MaterialAppearance.getSolidColor(BuiltinMaterials.COPPER.ref(), null));
        assertEquals(0xD0D8DE, MaterialAppearance.getSolidColor(BuiltinMaterials.TIN.ref(), null));
        assertEquals(0x646672, MaterialAppearance.getSolidColor(BuiltinMaterials.LEAD.ref(), null));
        assertEquals(0xC48443, MaterialAppearance.getSolidColor(BuiltinMaterials.BRONZE.ref(), null));
        assertEquals(0x3D352E, MaterialAppearance.getSolidColor(BuiltinMaterials.SLAG.ref(), null));

        // Molten colors (Planckian glow + metal tint)
        int molten1400 = MaterialAppearance.getMoltenColor(BuiltinMaterials.COPPER.ref(), null, 1400.0);
        int molten2000 = MaterialAppearance.getMoltenColor(BuiltinMaterials.COPPER.ref(), null, 2000.0);
        assertTrue(molten1400 != 0);
        assertTrue(molten2000 != 0);

        // Composition color mixing for arbitrary alloys
        CanonicalComposition alloy = CanonicalComposition.builder()
            .add(ResourceId.of("naturalexceptions", "copper"), 0.88)
            .add(ResourceId.of("naturalexceptions", "tin"), 0.12)
            .build();
        int compColor = MaterialAppearance.getCompositionColor(alloy);
        assertTrue(compColor != 0);
    }

    @Test
    void testCastingMoldMultiPassRenderingAndExtraction() {
        ItemCastingMold moldItem = (ItemCastingMold) NEItems.castingMold;
        ItemStack moldStack = ItemCastingMold.createStack(GeometryId.PICK_HEAD);

        assertTrue(moldItem.requiresMultipleRenderPasses());
        assertEquals(2, moldItem.getRenderPasses(0));

        // Pass 0 (empty mold): clay color untinted
        assertEquals(0xFFFFFF, moldItem.getColorFromItemStack(moldStack, 0));
        assertEquals(0xFFFFFF, moldItem.getColorFromItemStack(moldStack, 1));
        assertFalse(ItemCastingMold.isFilled(moldStack));

        // Pour molten Bronze into pick head mold
        ActiveThermalState moltenThermal = ActiveThermalState.atTemperature(
            1300.0,
            1.0,
            ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.BRONZE_PROPERTIES)
                .toKilograms(),
            BuiltinMaterials.BRONZE_PROPERTIES);

        AggregateLayer bronzeLayer = new AggregateLayer(
            BuiltinMaterials.BRONZE.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.BRONZE_COMPOSITION),
            BuiltinMaterials.BRONZE_COMPOSITION,
            ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.BRONZE_PROPERTIES),
            moltenThermal,
            AggregatePhaseType.MOLTEN_METAL);

        assertTrue(ItemCastingMold.fillMold(moldStack, bronzeLayer));
        assertTrue(ItemCastingMold.isFilled(moldStack));
        assertFalse(ItemCastingMold.isSolidified(moldStack));

        // Molten pass 1 has glowing molten color
        int moltenColor = moldItem.getColorFromItemStack(moldStack, 1);
        assertEquals(MaterialAppearance.getMoltenColor(BuiltinMaterials.BRONZE.ref(), null, 1300.0), moltenColor);

        // Cool to room temperature -> Solidified
        ActiveThermalState solidThermal = ActiveThermalState.atTemperature(
            293.15,
            0.0,
            ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.BRONZE_PROPERTIES)
                .toKilograms(),
            BuiltinMaterials.BRONZE_PROPERTIES);
        ItemCastingMold.saveThermalState(moldStack, solidThermal);
        assertTrue(ItemCastingMold.isSolidified(moldStack));

        // Solidified pass 1 has true Bronze solid color
        int solidColor = moldItem.getColorFromItemStack(moldStack, 1);
        assertEquals(0xC48443, solidColor);

        // Extract completed component
        ItemStack componentStack = ItemCastingMold.extractComponent(moldStack);
        assertNotNull(componentStack);
        assertTrue(componentStack.getItem() instanceof ItemGenericComponent);
        ComponentInstance comp = ItemGenericComponent.getComponent(componentStack);
        assertNotNull(comp);
        assertEquals(GeometryId.PICK_HEAD, comp.geometryId());
        assertEquals(BuiltinMaterials.BRONZE.ref(), comp.materialRef());
        assertFalse(ItemCastingMold.isFilled(moldStack));
    }

    @Test
    void filledMoldColorFollowsContainedComposition() {
        ItemCastingMold moldItem = (ItemCastingMold) NEItems.castingMold;
        ItemStack copperMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        ItemStack bronzeMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        ItemStack alloyMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);

        fillAtAmbient(
            copperMold,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            BuiltinMaterials.COPPER_PROPERTIES);
        fillAtAmbient(
            bronzeMold,
            BuiltinMaterials.BRONZE.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.BRONZE_COMPOSITION),
            BuiltinMaterials.BRONZE_PROPERTIES);
        CanonicalComposition alloy = CanonicalComposition.builder()
            .add(ResourceId.of("naturalexceptions", "copper"), 0.57)
            .add(ResourceId.of("naturalexceptions", "tin"), 0.23)
            .add(ResourceId.of("naturalexceptions", "lead"), 0.20)
            .build();
        fillAtAmbient(
            alloyMold,
            MaterialRef.generatedFrom(alloy),
            CanonicalMaterialKey.of(alloy),
            new naturalexceptions.core.property.MaterialPropertyResolver()
                .resolve(MaterialRef.generatedFrom(alloy), alloy, naturalexceptions.core.material.MaterialPhase.SOLID)
                .properties());

        int copperColor = moldItem.getColorFromItemStack(copperMold, 1);
        int bronzeColor = moldItem.getColorFromItemStack(bronzeMold, 1);
        int alloyColor = moldItem.getColorFromItemStack(alloyMold, 1);
        assertNotEquals(copperColor, bronzeColor);
        assertNotEquals(copperColor, alloyColor);
        assertNotEquals(bronzeColor, alloyColor);

        ItemStack reloadedAlloyMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        reloadedAlloyMold.setTagCompound(
            (NBTTagCompound) alloyMold.getTagCompound()
                .copy());
        assertEquals(MaterialRef.generatedFrom(alloy), ItemCastingMold.getContainedMaterialRef(reloadedAlloyMold));
        assertEquals(alloyColor, moldItem.getColorFromItemStack(reloadedAlloyMold, 1));
    }

    @Test
    void extractingFilledMoldClearsAllContainedPayload() {
        ItemStack moldStack = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        fillAtAmbient(
            moldStack,
            BuiltinMaterials.BRONZE.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.BRONZE_COMPOSITION),
            BuiltinMaterials.BRONZE_PROPERTIES);

        assertNotNull(ItemCastingMold.extractComponent(moldStack));
        assertFalse(ItemCastingMold.isFilled(moldStack));
        assertFalse(
            moldStack.getTagCompound()
                .hasKey(ItemCastingMold.KEY_DENSITY));
        assertFalse(
            moldStack.getTagCompound()
                .hasKey(ItemCastingMold.KEY_SPECIFIC_HEAT));
        assertFalse(
            moldStack.getTagCompound()
                .hasKey(ItemCastingMold.KEY_SPECIFIC_HEAT_LIQUID));
        assertFalse(
            moldStack.getTagCompound()
                .hasKey(ItemCastingMold.KEY_MELT_POINT));
        assertFalse(
            moldStack.getTagCompound()
                .hasKey(ItemCastingMold.KEY_LATENT_HEAT));
    }

    @Test
    void legacyMoldPayloadReportsThermalRecoveryInTooltip() {
        ItemStack moldStack = ItemCastingMold.createStack(GeometryId.BLANK);
        fillAtAmbient(
            moldStack,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            BuiltinMaterials.COPPER_PROPERTIES);
        NBTTagCompound nbt = moldStack.getTagCompound();
        nbt.removeTag(ItemCastingMold.KEY_DENSITY);
        nbt.removeTag(ItemCastingMold.KEY_SPECIFIC_HEAT);
        nbt.removeTag(ItemCastingMold.KEY_SPECIFIC_HEAT_LIQUID);
        nbt.removeTag(ItemCastingMold.KEY_MELT_POINT);
        nbt.removeTag(ItemCastingMold.KEY_LATENT_HEAT);

        ArrayList<String> tooltip = new ArrayList<>();
        NEItems.castingMold.addInformation(moldStack, null, tooltip, false);

        assertTrue(
            tooltip.toString()
                .contains("thermal properties reconstructed"));
        assertTrue(ItemCastingMold.getThermalState(moldStack) != null);
    }

    @Test
    void malformedMoldIdentityIsSafeForTooltipAndDynamicTint() {
        ItemStack moldStack = ItemCastingMold.createStack(GeometryId.BLANK);
        fillAtAmbient(
            moldStack,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            BuiltinMaterials.COPPER_PROPERTIES);
        moldStack.getTagCompound()
            .setString(ItemCastingMold.KEY_STATE_KEY, "malformed-state-key");

        ArrayList<String> tooltip = new ArrayList<>();
        assertDoesNotThrow(() -> NEItems.castingMold.addInformation(moldStack, null, tooltip, false));
        assertDoesNotThrow(() -> NEItems.castingMold.getColorFromItemStack(moldStack, 1));
        assertEquals(
            "Mold data is incomplete; extraction is blocked.",
            ItemCastingMold.getPayloadDiagnostic(moldStack));
        assertTrue(
            tooltip.toString()
                .contains("extraction is blocked"),
            tooltip.toString());
    }

    @Test
    void moldReconstructsMissingThermalPropertiesFromMaterialIdentity() {
        ItemStack moldStack = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        CanonicalComposition alloy = CanonicalComposition.builder()
            .add(ResourceId.of("naturalexceptions", "copper"), 0.57)
            .add(ResourceId.of("naturalexceptions", "tin"), 0.23)
            .add(ResourceId.of("naturalexceptions", "lead"), 0.20)
            .build();
        MaterialRef alloyRef = MaterialRef.generatedFrom(alloy);
        CanonicalMaterialKey alloyKey = CanonicalMaterialKey.of(alloy);
        naturalexceptions.core.property.MaterialProperties alloyProperties = new naturalexceptions.core.property.MaterialPropertyResolver()
            .resolve(alloyRef, alloy, naturalexceptions.core.material.MaterialPhase.SOLID)
            .properties();
        fillAtAmbient(moldStack, alloyRef, alloyKey, alloyProperties);
        moldStack.getTagCompound()
            .removeTag(ItemCastingMold.KEY_DENSITY);
        moldStack.getTagCompound()
            .removeTag(ItemCastingMold.KEY_SPECIFIC_HEAT);
        moldStack.getTagCompound()
            .removeTag(ItemCastingMold.KEY_SPECIFIC_HEAT_LIQUID);
        moldStack.getTagCompound()
            .removeTag(ItemCastingMold.KEY_MELT_POINT);
        moldStack.getTagCompound()
            .removeTag(ItemCastingMold.KEY_LATENT_HEAT);

        ActiveThermalState restored = ItemCastingMold.getThermalState(moldStack);
        assertEquals(
            alloyProperties.density(),
            restored.properties()
                .density(),
            0.0001);
        assertEquals(
            alloyProperties.meltingPointKelvin(),
            restored.properties()
                .meltingPointKelvin(),
            0.0001);
    }

    private static void fillAtAmbient(ItemStack mold, MaterialRef ref, CanonicalMaterialKey key,
        naturalexceptions.core.property.MaterialProperties properties) {
        MaterialQuantity mass = ComponentGeometry.pickHead()
            .requiredMass(properties);
        ItemCastingMold.fillMold(
            mold,
            new AggregateLayer(
                ref,
                key.asMolten(),
                key.composition(),
                mass,
                ActiveThermalState.atTemperature(293.15, 0.0, mass.toKilograms(), properties),
                AggregatePhaseType.MOLTEN_METAL));
    }

    @Test
    void testToolAssemblyAndDisassemblyRecipe() {
        RecipeToolAssembly recipe = new RecipeToolAssembly();
        DummyContainer container = new DummyContainer();
        InventoryCrafting inv = new InventoryCrafting(container, 3, 3);

        // Create Bronze Pick Head
        ComponentInstance bronzeHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.BRONZE.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.BRONZE_COMPOSITION),
            ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.BRONZE_PROPERTIES),
            ManufacturingState.FINISHED);
        ItemStack headStack = ItemGenericComponent.createStack(bronzeHead);

        // Create Wood Handle component
        ComponentInstance woodHandle = new ComponentInstance(
            GeometryId.HANDLE,
            BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.WOOD_COMPOSITION),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);
        ItemStack handleStack = ItemGenericComponent.createStack(woodHandle);

        // Place in crafting grid
        inv.setInventorySlotContents(0, headStack);
        inv.setInventorySlotContents(1, handleStack);

        assertTrue(recipe.matches(inv, null));
        ItemStack toolResult = recipe.getCraftingResult(inv);
        assertNotNull(toolResult);
        assertTrue(toolResult.getItem() instanceof ItemGenericAssembly);

        AssemblyGraph assembly = ItemGenericAssembly.getAssembly(toolResult);
        assertNotNull(assembly);
        assertEquals(
            GeometryId.PICK_HEAD,
            assembly.head()
                .geometryId());
        assertEquals(
            BuiltinMaterials.BRONZE.ref(),
            assembly.head()
                .materialRef());
        assertEquals(
            GeometryId.HANDLE,
            assembly.handle()
                .geometryId());

        // Test stick fallback in crafting
        inv.setInventorySlotContents(1, new ItemStack(Items.stick));
        assertTrue(recipe.matches(inv, null));
        ItemStack stickToolResult = recipe.getCraftingResult(inv);
        assertNotNull(stickToolResult);
        AssemblyGraph stickAssembly = ItemGenericAssembly.getAssembly(stickToolResult);
        assertNotNull(stickAssembly);
        assertEquals(
            BuiltinMaterials.WOOD.ref(),
            stickAssembly.handle()
                .materialRef());

        // Test Disassembly
        InventoryCrafting disInv = new InventoryCrafting(container, 3, 3);
        disInv.setInventorySlotContents(4, toolResult);

        assertFalse(recipe.matches(disInv, null));
        assertNull(recipe.getCraftingResult(disInv));
        // Use the Assembly Table for disassembly: its interaction returns both historical parts.
    }

    @Test
    void testToolMiningCapabilitiesAndHarvesting() {
        ItemGenericAssembly assemblyItem = (ItemGenericAssembly) NEItems.genericAssembly;

        ComponentInstance bronzeHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.BRONZE.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.BRONZE_COMPOSITION),
            ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.BRONZE_PROPERTIES),
            ManufacturingState.FINISHED);
        ComponentInstance woodHandle = new ComponentInstance(
            GeometryId.HANDLE,
            BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.WOOD_COMPOSITION),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);
        AssemblyGraph toolAssembly = AssemblyGraph.ofTool(bronzeHead, woodHandle);
        ItemStack toolStack = ItemGenericAssembly.createStack(toolAssembly);

        // Tool classes & harvest level
        assertTrue(
            assemblyItem.getToolClasses(toolStack)
                .contains("pickaxe"));
        int harvestLevel = assemblyItem.getHarvestLevel(toolStack, "pickaxe");
        assertTrue(harvestLevel >= 1); // Bronze penetrates well above stone

        // Can harvest copper ore (and stone if initialized)
        assertTrue(assemblyItem.canHarvestBlock(NEBlocks.copperOre, toolStack));
        if (Blocks.stone != null) {
            assertTrue(assemblyItem.canHarvestBlock(Blocks.stone, toolStack));
        }

        // Dig speed is accelerated
        float speed = assemblyItem.getDigSpeed(toolStack, NEBlocks.copperOre, 0);
        assertTrue(speed > 1.0F);

        // Original vanilla wood pixels are untinted; the bronze head remains material-tinted.
        assertEquals(0xFFFFFF, assemblyItem.getColorFromItemStack(toolStack, 0));
        assertEquals(0xC48443, assemblyItem.getColorFromItemStack(toolStack, 1));
    }

    @Test
    void testSmelterGeneralizedRemelting() {
        TileEntityStarterSmelter smelter = new TileEntityStarterSmelter();

        // 1. Remelt a Tin Component
        ComponentInstance tinPlate = new ComponentInstance(
            GeometryId.PLATE,
            BuiltinMaterials.TIN.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.TIN_COMPOSITION),
            ComponentGeometry.plate()
                .requiredMass(BuiltinMaterials.TIN_PROPERTIES),
            ManufacturingState.FINISHED);
        ItemStack tinPlateStack = ItemGenericComponent.createStack(tinPlate);
        assertTrue(smelter.insertFeed(tinPlateStack));
        assertEquals(
            1,
            smelter.vesselState()
                .layers()
                .size());
        assertEquals(
            BuiltinMaterials.TIN.ref(),
            smelter.vesselState()
                .layers()
                .get(0)
                .materialRef());

        // 2. Remelt a filled Mold
        ItemStack moldStack = ItemCastingMold.createStack(GeometryId.ROD);
        ActiveThermalState leadThermal = ActiveThermalState
            .atTemperature(700.0, 1.0, 0.5, BuiltinMaterials.LEAD_PROPERTIES);
        AggregateLayer leadLayer = new AggregateLayer(
            BuiltinMaterials.LEAD.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.LEAD_COMPOSITION),
            BuiltinMaterials.LEAD_COMPOSITION,
            MaterialQuantity.ofKilograms(0.5),
            leadThermal,
            AggregatePhaseType.MOLTEN_METAL);
        ItemCastingMold.fillMold(moldStack, leadLayer);

        assertTrue(smelter.insertFeed(moldStack));
        assertFalse(ItemCastingMold.isFilled(moldStack));
        assertEquals(
            2,
            smelter.vesselState()
                .layers()
                .size());
        // Lead is denser (~11.34 g/cm³) than Tin (~7.3 g/cm³), settling at bottom (index 0)
        assertEquals(
            BuiltinMaterials.LEAD.ref(),
            smelter.vesselState()
                .layers()
                .get(0)
                .materialRef());
        assertEquals(
            BuiltinMaterials.TIN.ref(),
            smelter.vesselState()
                .layers()
                .get(1)
                .materialRef());
    }
}
