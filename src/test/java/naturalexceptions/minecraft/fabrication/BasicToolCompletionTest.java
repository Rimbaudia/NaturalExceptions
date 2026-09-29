package naturalexceptions.minecraft.fabrication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.assembly.DerivedCapability;
import naturalexceptions.core.assembly.TargetMaterialProfile;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentGeometry;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.persistence.AssemblyNbtCodec;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

public class BasicToolCompletionTest {

    private static final class DummyContainer extends Container {

        @Override
        public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) {
            return true;
        }
    }

    @BeforeAll
    public static void setUp() {
        NEItems.ensureBootstrapped();
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    @Test
    public void survivalCraftingOfHandlesProducesWoodHandle() {
        DummyContainer container = new DummyContainer();
        InventoryCrafting inv = new InventoryCrafting(container, 3, 3);

        // Place 2 sticks in a vertical line in slots 0 and 3
        inv.setInventorySlotContents(0, new ItemStack(Items.stick));
        inv.setInventorySlotContents(3, new ItemStack(Items.stick));

        ItemStack result = null;
        @SuppressWarnings("unchecked")
        List<IRecipe> recipes = CraftingManager.getInstance()
            .getRecipeList();
        for (IRecipe r : recipes) {
            if (r.matches(inv, null)) {
                result = r.getCraftingResult(inv);
                break;
            }
        }

        assertNotNull(result, "2 vertical sticks must craft into a Wood Handle in survival");
        assertTrue(result.getItem() instanceof ItemGenericComponent, "Result must be an ItemGenericComponent");
        ComponentInstance comp = ItemGenericComponent.getComponent(result);
        assertNotNull(comp, "Handle item must contain component metadata");
        assertEquals(GeometryId.HANDLE, comp.geometryId());
        assertEquals(BuiltinMaterials.WOOD.ref(), comp.materialRef());
        assertEquals("Wood Handle", result.getDisplayName(), "wood is common knowledge");
    }

    @Test
    public void assemblyTableAcceptsWoodHandleAndAllToolHeads() {
        TileEntityAssemblyTable table = new TileEntityAssemblyTable();
        ItemStack woodHandle = ItemGenericComponent.createWoodenHandle();

        // 1. Pickaxe
        ComponentInstance pickHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HEAD, ItemGenericComponent.createStack(pickHead));
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HANDLE, woodHandle.copy());

        ItemStack pickResult = table.getStackInSlot(TileEntityAssemblyTable.SLOT_RESULT);
        assertNotNull(pickResult);
        assertTrue(
            pickResult.getDisplayName()
                .endsWith("Pickaxe"),
            "material, then the shape: " + pickResult.getDisplayName());

        // 2. Axe
        ComponentInstance axeHead = new ComponentInstance(
            GeometryId.AXE_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            ComponentGeometry.axeHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HEAD, ItemGenericComponent.createStack(axeHead));

        ItemStack axeResult = table.getStackInSlot(TileEntityAssemblyTable.SLOT_RESULT);
        assertNotNull(axeResult);
        assertTrue(
            axeResult.getDisplayName()
                .endsWith("Axe"),
            "material, then the shape: " + axeResult.getDisplayName());

        // 3. Shovel
        ComponentInstance shovelHead = new ComponentInstance(
            GeometryId.SHOVEL_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            ComponentGeometry.shovelHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HEAD, ItemGenericComponent.createStack(shovelHead));

        ItemStack shovelResult = table.getStackInSlot(TileEntityAssemblyTable.SLOT_RESULT);
        assertNotNull(shovelResult);
        assertTrue(
            shovelResult.getDisplayName()
                .endsWith("Shovel"),
            "material, then the shape: " + shovelResult.getDisplayName());

        // 4. Hoe
        ComponentInstance hoeHead = new ComponentInstance(
            GeometryId.HOE_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            ComponentGeometry.hoeHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HEAD, ItemGenericComponent.createStack(hoeHead));

        ItemStack hoeResult = table.getStackInSlot(TileEntityAssemblyTable.SLOT_RESULT);
        assertNotNull(hoeResult);
        assertTrue(
            hoeResult.getDisplayName()
                .endsWith("Hoe"),
            "material, then the shape: " + hoeResult.getDisplayName());

        // 5. Vanilla stick fallback compatibility
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HANDLE, new ItemStack(Items.stick));
        ItemStack stickHoeResult = table.getStackInSlot(TileEntityAssemblyTable.SLOT_RESULT);
        assertNotNull(stickHoeResult);
        assertTrue(
            stickHoeResult.getDisplayName()
                .endsWith("Hoe"),
            "material, then the shape: " + stickHoeResult.getDisplayName());
    }

    @Test
    public void allFourToolTypesFunctionCorrectly() {
        ItemGenericAssembly assemblyItem = (ItemGenericAssembly) NEItems.genericAssembly;
        ComponentInstance woodHandle = ItemGenericComponent.getComponent(ItemGenericComponent.createWoodenHandle());

        // 1. Pickaxe checks
        ComponentInstance pickHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        ItemStack pickStack = ItemGenericAssembly.createStack(AssemblyGraph.ofTool(pickHead, woodHandle));
        assertTrue(
            assemblyItem.getToolClasses(pickStack)
                .contains("pickaxe"));
        assertTrue(assemblyItem.getHarvestLevel(pickStack, "pickaxe") >= 1);
        assertTrue(assemblyItem.canHarvestBlock(NEBlocks.copperOre, pickStack));
        assertTrue(assemblyItem.getDigSpeed(pickStack, NEBlocks.copperOre, 0) > 1.0F);

        // 2. Axe checks
        ComponentInstance axeHead = new ComponentInstance(
            GeometryId.AXE_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            ComponentGeometry.axeHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        ItemStack axeStack = ItemGenericAssembly.createStack(AssemblyGraph.ofTool(axeHead, woodHandle));
        assertTrue(
            assemblyItem.getToolClasses(axeStack)
                .contains("axe"));
        assertTrue(assemblyItem.getHarvestLevel(axeStack, "axe") >= 1);
        assertTrue(assemblyItem.getDigSpeed(axeStack, Blocks.log, 0) > 1.0F);

        // 3. Shovel checks
        ComponentInstance shovelHead = new ComponentInstance(
            GeometryId.SHOVEL_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            ComponentGeometry.shovelHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        ItemStack shovelStack = ItemGenericAssembly.createStack(AssemblyGraph.ofTool(shovelHead, woodHandle));
        assertTrue(
            assemblyItem.getToolClasses(shovelStack)
                .contains("shovel"));
        assertTrue(assemblyItem.getHarvestLevel(shovelStack, "shovel") >= 1);
        assertTrue(assemblyItem.getDigSpeed(shovelStack, Blocks.dirt, 0) > 1.0F);

        // 4. Hoe checks
        ComponentInstance hoeHead = new ComponentInstance(
            GeometryId.HOE_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            ComponentGeometry.hoeHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        ItemStack hoeStack = ItemGenericAssembly.createStack(AssemblyGraph.ofTool(hoeHead, woodHandle));
        assertTrue(
            assemblyItem.getToolClasses(hoeStack)
                .contains("hoe"));
        assertTrue(assemblyItem.getHarvestLevel(hoeStack, "hoe") >= 1);
        assertTrue(
            assemblyItem.getAttributeModifiers(hoeStack)
                .containsKey("generic.attackDamage"));
    }

    @Test
    public void materialPropertiesPropagateToDurabilityAndPerformance() {
        ComponentInstance woodHandle = ItemGenericComponent.getComponent(ItemGenericComponent.createWoodenHandle());

        // Copper Pick (Hardness 3.0, Density 8.96)
        ComponentInstance cuHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        AssemblyGraph cuTool = AssemblyGraph.ofTool(cuHead, woodHandle);
        DerivedCapability cuCap = ItemGenericAssembly.getCapability(cuTool);

        // Bronze Pick (Hardness 4.2, Density 8.75, Trait "durable")
        ComponentInstance bronzeHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.BRONZE.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.BRONZE_COMPOSITION),
            ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.BRONZE_PROPERTIES),
            ManufacturingState.FINISHED);
        AssemblyGraph bronzeTool = AssemblyGraph.ofTool(bronzeHead, woodHandle);
        DerivedCapability bronzeCap = ItemGenericAssembly.getCapability(bronzeTool);

        // Mining speed: Bronze penetrates better and mines faster than Copper
        assertTrue(bronzeCap.penetration() > cuCap.penetration(), "Bronze must have higher penetration than copper");
        assertTrue(
            bronzeCap.miningSpeed(TargetMaterialProfile.stone()) > cuCap.miningSpeed(TargetMaterialProfile.stone()),
            "Bronze must mine faster than copper");

        // Durability: Bronze wears noticeably slower per block than Copper
        double cuRate = cuCap.calculateWearRate(TargetMaterialProfile.stone());
        double bronzeRate = bronzeCap.calculateWearRate(TargetMaterialProfile.stone());
        assertTrue(cuRate > bronzeRate, "Copper must wear faster than bronze");
        assertTrue(bronzeRate / cuRate < 0.70, "Bronze should provide >= 40% greater lifespan than copper");

        // Estimated uses
        int cuUses = (int) Math.round(1.0 / cuRate);
        int bronzeUses = (int) Math.round(1.0 / bronzeRate);
        assertTrue(cuUses >= 200 && cuUses <= 300, "Copper lifespan should be ~200-300 uses, was: " + cuUses);
        assertTrue(
            bronzeUses >= 350 && bronzeUses <= 550,
            "Bronze lifespan should be ~350-550 uses, was: " + bronzeUses);
    }

    @Test
    public void progressiveWearAccumulationOnUse() {
        ComponentInstance woodHandle = ItemGenericComponent.getComponent(ItemGenericComponent.createWoodenHandle());
        ComponentInstance cuHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        AssemblyGraph cuTool = AssemblyGraph.ofTool(cuHead, woodHandle);
        ItemStack toolStack = ItemGenericAssembly.createStack(cuTool);
        ItemGenericAssembly assemblyItem = (ItemGenericAssembly) toolStack.getItem();

        assertFalse(assemblyItem.isDamaged(toolStack));
        assertEquals(0.0, assemblyItem.getDurabilityForDisplay(toolStack), 0.001);

        // Simulate 20 block destructions
        DerivedCapability cap = ItemGenericAssembly.getCapability(cuTool);
        for (int i = 0; i < 20; i++) {
            cap.interactWith(TargetMaterialProfile.stone());
        }
        ItemGenericAssembly.saveAssembly(toolStack, cuTool);

        assertTrue(assemblyItem.isDamaged(toolStack));
        assertTrue(assemblyItem.showDurabilityBar(toolStack));
        assertTrue(assemblyItem.getDurabilityForDisplay(toolStack) > 0.05);
        assertTrue(assemblyItem.getDamage(toolStack) > 0);
    }

    @Test
    public void terminalWearCausesCompleteToolBreakage() {
        ComponentInstance woodHandle = ItemGenericComponent.getComponent(ItemGenericComponent.createWoodenHandle());
        ComponentInstance cuHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        AssemblyGraph cuTool = AssemblyGraph.ofTool(cuHead, woodHandle);
        ItemStack toolStack = ItemGenericAssembly.createStack(cuTool);
        ItemGenericAssembly assemblyItem = (ItemGenericAssembly) toolStack.getItem();

        // Push wear to 1.0 (terminal failure)
        cuHead.damage()
            .addWear(1.0);
        ItemGenericAssembly.saveAssembly(toolStack, cuTool);

        assertTrue(cuTool.isBroken(), "Assembly must report isBroken when wear reaches 1.0");
        assertTrue(
            cuHead.damage()
                .isFractured(),
            "Component must be fractured at 1.0 wear");
        assertTrue(
            toolStack.getDisplayName()
                .endsWith("Pickaxe"),
            "material, then the shape: " + toolStack.getDisplayName());
        assertFalse(assemblyItem.canHarvestBlock(NEBlocks.copperOre, toolStack));
        assertEquals(1.0F, assemblyItem.getDigSpeed(toolStack, NEBlocks.copperOre, 0), 0.01F);
        assertEquals(-1, assemblyItem.getHarvestLevel(toolStack, "pickaxe"));
        assertTrue(
            assemblyItem.getAttributeModifiers(toolStack)
                .isEmpty());

        // Test checkToolBreak execution
        boolean broken = ItemGenericAssembly.checkToolBreak(toolStack, null, null);
        assertTrue(broken, "checkToolBreak must confirm tool breakage");
        assertEquals(0, toolStack.stackSize, "Tool stackSize must be zero after breaking");
    }

    @Test
    public void multiPassLayeredToolArtAndDynamicTinting() {
        ItemGenericAssembly assemblyItem = (ItemGenericAssembly) NEItems.genericAssembly;
        assertTrue(assemblyItem.requiresMultipleRenderPasses(), "Tool must require multiple render passes");
        assertEquals(2, assemblyItem.getRenderPasses(0), "Tool must use 2 render passes");

        ComponentInstance woodHandle = ItemGenericComponent.getComponent(ItemGenericComponent.createWoodenHandle());

        // 1. Copper Tool
        ComponentInstance cuHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        ItemStack cuTool = ItemGenericAssembly.createStack(AssemblyGraph.ofTool(cuHead, woodHandle));
        assertEquals(0xFFFFFF, assemblyItem.getColorFromItemStack(cuTool, 0), "Keep original vanilla wood colors");
        assertEquals(0xD36C4C, assemblyItem.getColorFromItemStack(cuTool, 1), "Pass 1 (head) must be Copper color");

        // 2. Bronze Tool
        ComponentInstance bronzeHead = new ComponentInstance(
            GeometryId.AXE_HEAD,
            BuiltinMaterials.BRONZE.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.BRONZE_COMPOSITION),
            ComponentGeometry.axeHead()
                .requiredMass(BuiltinMaterials.BRONZE_PROPERTIES),
            ManufacturingState.FINISHED);
        ItemStack bronzeTool = ItemGenericAssembly.createStack(AssemblyGraph.ofTool(bronzeHead, woodHandle));
        assertEquals(0xFFFFFF, assemblyItem.getColorFromItemStack(bronzeTool, 0), "Keep original vanilla wood colors");
        assertEquals(0xC48443, assemblyItem.getColorFromItemStack(bronzeTool, 1), "Pass 1 (head) must be Bronze color");

        // 3. Iron Tool: pure vanilla luminance (0xFFFFFF tint preserving vanilla iron shading curve)
        ComponentInstance ironHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            naturalexceptions.core.material.MaterialRef.authored(ResourceId.of("naturalexceptions", "iron")),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "iron"))),
            ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        ItemStack ironTool = ItemGenericAssembly.createStack(AssemblyGraph.ofTool(ironHead, woodHandle));
        assertEquals(0xFFFFFF, assemblyItem.getColorFromItemStack(ironTool, 0), "Keep original vanilla wood colors");
        assertEquals(
            0xFFFFFF,
            assemblyItem.getColorFromItemStack(ironTool, 1),
            "Pass 1 (head) must preserve vanilla iron highlights");

        // 4. Generated alloy: dynamically mixed composition
        CanonicalComposition genComp = CanonicalComposition.builder()
            .add(ResourceId.of("naturalexceptions", "copper"), 0.60)
            .add(ResourceId.of("naturalexceptions", "tin"), 0.40)
            .build();
        ComponentInstance alloyHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            naturalexceptions.core.material.MaterialRef.generatedFrom(genComp),
            CanonicalMaterialKey.of(genComp),
            ComponentGeometry.pickHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        ItemStack alloyTool = ItemGenericAssembly.createStack(AssemblyGraph.ofTool(alloyHead, woodHandle));
        assertEquals(0xFFFFFF, assemblyItem.getColorFromItemStack(alloyTool, 0), "Handle colors remain independent");
        int alloyColor = assemblyItem.getColorFromItemStack(alloyTool, 1);
        assertTrue(alloyColor > 0, "Generated alloy must resolve valid non-zero RGB tint");
        assertTrue(alloyColor != 0xD36C4C, "Generated alloy tint must differ from pure copper");

        // 5. Standalone components dynamic tinting
        ItemGenericComponent compItem = (ItemGenericComponent) NEItems.genericComponent;
        ItemStack handleStack = ItemGenericComponent.createWoodenHandle();
        assertEquals(
            0xFFFFFF,
            compItem.getColorFromItemStack(handleStack, 0),
            "Wood Handle is drawn in its own stick colours, untinted");

        ItemStack cuHeadStack = ItemGenericComponent.createStack(cuHead);
        assertEquals(0xD36C4C, compItem.getColorFromItemStack(cuHeadStack, 0), "Copper head item must tint Copper");

        ItemStack ironHeadStack = ItemGenericComponent.createStack(ironHead);
        assertEquals(0xFFFFFF, compItem.getColorFromItemStack(ironHeadStack, 0), "Iron head item must tint 0xFFFFFF");
    }

    @Test
    public void vanillaToolGeometrySelectionAndPassSeparation() {
        ItemGenericAssembly assemblyItem = (ItemGenericAssembly) NEItems.genericAssembly;
        ItemGenericComponent compItem = (ItemGenericComponent) NEItems.genericComponent;

        // Register icons using a mock IIconRegister
        IIconRegister dummyReg = new IIconRegister() {

            @Override
            public IIcon registerIcon(final String name) {
                return new IIcon() {

                    @Override
                    public int getIconWidth() {
                        return 16;
                    }

                    @Override
                    public int getIconHeight() {
                        return 16;
                    }

                    @Override
                    public float getMinU() {
                        return 0;
                    }

                    @Override
                    public float getMaxU() {
                        return 1;
                    }

                    @Override
                    public float getInterpolatedU(double d) {
                        return (float) d;
                    }

                    @Override
                    public float getMinV() {
                        return 0;
                    }

                    @Override
                    public float getMaxV() {
                        return 1;
                    }

                    @Override
                    public float getInterpolatedV(double d) {
                        return (float) d;
                    }

                    @Override
                    public String getIconName() {
                        return name;
                    }
                };
            }
        };

        assemblyItem.registerIcons(dummyReg);
        compItem.registerIcons(dummyReg);

        ComponentInstance woodHandle = ItemGenericComponent.getComponent(ItemGenericComponent.createWoodenHandle());

        // Verify each tool selects its exact vanilla geometry
        GeometryId[] toolHeads = { GeometryId.PICK_HEAD, GeometryId.AXE_HEAD, GeometryId.SHOVEL_HEAD,
            GeometryId.HOE_HEAD, GeometryId.BLADE };
        String[] expectedHandleIcons = { "naturalexceptions:tool_handle_pickaxe_wood",
            "naturalexceptions:tool_handle_axe_wood", "naturalexceptions:tool_handle_shovel_wood",
            "naturalexceptions:tool_handle_hoe_wood", "naturalexceptions:tool_handle_sword_wood" };
        String[] expectedHeadIcons = { "naturalexceptions:tool_pickaxe_head", "naturalexceptions:tool_axe_head",
            "naturalexceptions:tool_shovel_head", "naturalexceptions:tool_hoe_head", "naturalexceptions:tool_blade" };
        String[] expectedComponentIcons = { "naturalexceptions:component_pick_head",
            "naturalexceptions:component_axe_head", "naturalexceptions:component_shovel_head",
            "naturalexceptions:component_hoe_head", "naturalexceptions:component_blade" };

        for (int i = 0; i < toolHeads.length; i++) {
            ComponentInstance head = new ComponentInstance(
                toolHeads[i],
                BuiltinMaterials.COPPER.ref(),
                CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
                ComponentGeometry.pickHead()
                    .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
                ManufacturingState.FINISHED);
            ItemStack toolStack = ItemGenericAssembly.createStack(AssemblyGraph.ofTool(head, woodHandle));

            IIcon pass0 = assemblyItem.getIcon(toolStack, 0);
            IIcon pass1 = assemblyItem.getIcon(toolStack, 1);

            assertNotNull(pass0, "Pass 0 (handle) icon must not be null for " + toolHeads[i]);
            assertNotNull(pass1, "Pass 1 (head) icon must not be null for " + toolHeads[i]);
            assertEquals(expectedHandleIcons[i], pass0.getIconName(), "Pass 0 must select tool-specific handle icon");
            assertEquals(expectedHeadIcons[i], pass1.getIconName(), "Pass 1 must select tool-specific head icon");
            assertFalse(
                pass0.getIconName()
                    .equals(pass1.getIconName()),
                "Handle and head passes must be distinct");

            // Check standalone component icon
            ItemStack headStack = ItemGenericComponent.createStack(head);
            IIcon compIcon = compItem.getIconIndex(headStack);
            assertNotNull(compIcon, "Component icon must not be null for " + toolHeads[i]);
            assertEquals(
                expectedComponentIcons[i],
                compIcon.getIconName(),
                "Component must select corresponding head icon");
        }

        // Check standalone handle component
        ItemStack handleStack = ItemGenericComponent.createWoodenHandle();
        IIcon handleIcon = compItem.getIconIndex(handleStack);
        assertNotNull(handleIcon);
        assertEquals("naturalexceptions:tool_handle_pickaxe_wood", handleIcon.getIconName());
    }

    @Test
    public void vanillaDerivedBrightnessAndShadingIntegrity() throws Exception {
        File assetsDir = new File("src/main/resources/assets/naturalexceptions/textures/items");
        assertTrue(assetsDir.exists(), "Item textures dir must exist");

        String[] toolFiles = { "tool_pickaxe_head.png", "tool_axe_head.png", "tool_shovel_head.png",
            "tool_hoe_head.png", "tool_blade.png", "tool_handle_pickaxe.png", "tool_handle_axe.png",
            "tool_handle_shovel.png", "tool_handle_hoe.png", "tool_handle_sword.png" };

        for (String fn : toolFiles) {
            File f = new File(assetsDir, fn);
            assertTrue(f.exists(), "Asset must exist: " + fn);
            BufferedImage img = ImageIO.read(f);
            assertNotNull(img, "ImageIO must load " + fn);

            // 1. Strict 16x16 native resolution (no 512x512, no blurring)
            assertEquals(16, img.getWidth(), "Width must be exactly 16 for " + fn);
            assertEquals(16, img.getHeight(), "Height must be exactly 16 for " + fn);

            Set<Integer> distinctLuminances = new HashSet<>();
            int visibleCount = 0;

            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int argb = img.getRGB(x, y);
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;

                    // 2. Binary alpha only (crisp, pixel-art, zero antialiasing)
                    assertTrue(a == 0 || a == 255, "Alpha must be 0 or 255 at (" + x + "," + y + ") in " + fn);

                    if (a == 255) {
                        visibleCount++;
                        // 3. Grayscale base for multiplicative tinting
                        assertEquals(r, g, "Texture must be grayscale R==G in " + fn);
                        assertEquals(g, b, "Texture must be grayscale G==B in " + fn);
                        distinctLuminances.add(r);
                    }
                }
            }

            assertTrue(visibleCount > 0, "Texture must have visible pixels: " + fn);
            // 4. Preserves vanilla shading curve (at least 3 distinct value steps: highlights, midtones, outlines)
            assertTrue(
                distinctLuminances.size() >= 3,
                "Texture must preserve multi-step vanilla shading curve in " + fn);
        }
    }

    @Test
    public void toolAssemblySurvivesNbtRoundtrip() {
        ComponentInstance woodHandle = ItemGenericComponent.getComponent(ItemGenericComponent.createWoodenHandle());
        ComponentInstance cuHead = new ComponentInstance(
            GeometryId.SHOVEL_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            ComponentGeometry.shovelHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.FINISHED);
        cuHead.damage()
            .addWear(0.25);
        AssemblyGraph originalTool = AssemblyGraph.ofTool(cuHead, woodHandle);

        NBTTagCompound nbt = new NBTTagCompound();
        AssemblyNbtCodec.writeToNbt(originalTool, nbt);

        AssemblyGraph restored = AssemblyNbtCodec.readFromNbt(nbt);
        assertNotNull(restored);
        assertEquals(
            GeometryId.SHOVEL_HEAD,
            restored.head()
                .geometryId());
        assertEquals(
            BuiltinMaterials.COPPER.ref(),
            restored.head()
                .materialRef());
        assertEquals(
            0.25,
            restored.head()
                .damage()
                .wear(),
            0.001);
        assertEquals(
            GeometryId.HANDLE,
            restored.handle()
                .geometryId());
        assertEquals(
            BuiltinMaterials.WOOD.ref(),
            restored.handle()
                .materialRef());
    }
}
