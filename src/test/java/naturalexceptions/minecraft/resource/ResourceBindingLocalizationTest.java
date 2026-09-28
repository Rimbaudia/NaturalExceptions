package naturalexceptions.minecraft.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.item.ItemMaterialSample;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NECreativeTab;
import naturalexceptions.minecraft.registry.NEItems;

final class ResourceBindingLocalizationTest {

    private static Properties langProps = new Properties();
    private static final File ASSETS_DIR = new File("src/main/resources/assets/naturalexceptions");

    @BeforeAll
    static void init() throws Exception {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();

        File langFile = new File(ASSETS_DIR, "lang/en_US.lang");
        assertTrue(langFile.exists(), "en_US.lang must exist in assets");
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(langFile), StandardCharsets.UTF_8)) {
            langProps.load(reader);
        }
    }

    @Test
    void allRegisteredBlocksHaveLocalization() {
        List<Block> blocks = Arrays.asList(
            NEBlocks.copperOre,
            NEBlocks.copperBlock,
            NEBlocks.refractoryBrick,
            NEBlocks.starterSmelter,
            NEBlocks.castingTable,
            NEBlocks.smelterOutlet,
            NEBlocks.smelterTuyere,
            NEBlocks.forgingAnvil);

        for (Block b : blocks) {
            assertNotNull(b, "Registered block must not be null");
            String unlocalized = b.getUnlocalizedName();
            assertNotNull(unlocalized, "Block unlocalized name must not be null");
            String key = unlocalized + ".name";
            String localized = langProps.getProperty(key);
            assertNotNull(localized, "Missing localization for block key: " + key);
            assertFalse(
                localized.trim()
                    .isEmpty(),
                "Empty localization for block key: " + key);
            assertFalse(localized.contains("null"), "Localized name must not contain 'null': " + localized);
            assertFalse(localized.contains(".name"), "Localized name must not contain raw '.name': " + localized);
        }
    }

    @Test
    void allRegisteredItemsHaveLocalization() {
        List<Item> items = Arrays.asList(
            NEItems.bellows,
            NEItems.copperIngot,
            NEItems.slagLump,
            NEItems.materialSample,
            NEItems.castingMold,
            NEItems.genericComponent,
            NEItems.genericAssembly,
            NEItems.foundryLadle,
            NEItems.blacksmithHammer,
            NEItems.blacksmithTongs,
            NEItems.refractoryMix,
            NEItems.refractoryBrick);

        for (Item item : items) {
            assertNotNull(item, "Registered item must not be null");
            String unlocalized = item.getUnlocalizedName();
            assertNotNull(unlocalized, "Item unlocalized name must not be null");
            String key = unlocalized + ".name";
            String localized = langProps.getProperty(key);
            assertNotNull(localized, "Missing localization for item key: " + key);
            assertFalse(
                localized.trim()
                    .isEmpty(),
                "Empty localization for item key: " + key);
            assertFalse(localized.contains("null"), "Localized name must not contain 'null': " + localized);
            assertFalse(localized.contains(".name"), "Localized name must not contain raw '.name': " + localized);
        }
    }

    @Test
    void creativeTabHasLocalization() {
        String localized = langProps.getProperty("itemGroup.naturalExceptions");
        assertNotNull(localized, "Missing localization for itemGroup.naturalExceptions");
        assertEquals("Natural Exceptions", localized);
    }

    @Test
    void allRegisteredBlocksAndItemsAssignedToNECreativeTab() {
        List<Block> blocks = Arrays.asList(
            NEBlocks.copperOre,
            NEBlocks.copperBlock,
            NEBlocks.refractoryBrick,
            NEBlocks.starterSmelter,
            NEBlocks.castingTable,
            NEBlocks.smelterOutlet,
            NEBlocks.smelterTuyere,
            NEBlocks.forgingAnvil);

        for (Block b : blocks) {
            assertEquals(
                NECreativeTab.INSTANCE,
                b.getCreativeTabToDisplayOn(),
                "Block " + b.getUnlocalizedName() + " must be in NECreativeTab");
        }

        List<Item> items = Arrays.asList(
            NEItems.bellows,
            NEItems.copperIngot,
            NEItems.slagLump,
            NEItems.materialSample,
            NEItems.castingMold,
            NEItems.genericComponent,
            NEItems.genericAssembly,
            NEItems.foundryLadle,
            NEItems.blacksmithHammer,
            NEItems.blacksmithTongs,
            NEItems.refractoryMix,
            NEItems.refractoryBrick);

        for (Item item : items) {
            assertEquals(
                NECreativeTab.INSTANCE,
                item.getCreativeTab(),
                "Item " + item.getUnlocalizedName() + " must be in NECreativeTab");
        }
    }

    @Test
    void declaredBlockTexturesExistInAssets() {
        String[] blockTextures = { "copper_ore.png", "copper_block.png", "refractory_brick_block.png",
            "starter_smelter_front.png", "starter_smelter_front_lit.png", "starter_smelter_side.png",
            "starter_smelter_top.png", "starter_smelter_base.png", "casting_table_side.png", "casting_table_top.png" };

        File blockDir = new File(ASSETS_DIR, "textures/blocks");
        assertTrue(blockDir.exists() && blockDir.isDirectory(), "Block texture directory must exist");

        for (String tex : blockTextures) {
            File texFile = new File(blockDir, tex);
            assertTrue(texFile.exists(), "Block texture missing from assets: " + tex);
            assertTrue(texFile.length() > 0, "Block texture must not be empty: " + tex);
        }
    }

    @Test
    void declaredItemTexturesExistInAssets() throws Exception {
        String[] itemTextures = { "bellows.png", "copper_ingot.png", "molten_slag_lump.png", "material_sample.png",
            "ingot_casting_mold.png", "hammered_copper_plate.png", "blacksmith_hammer.png", "tool_handle.png",
            "tool_handle_pickaxe.png", "tool_handle_axe.png", "tool_handle_shovel.png", "tool_handle_hoe.png",
            "tool_handle_sword.png", "tool_pickaxe_head.png", "tool_axe_head.png", "tool_shovel_head.png",
            "tool_hoe_head.png", "tool_blade.png", "component_handle.png", "component_pick_head.png",
            "component_axe_head.png", "component_shovel_head.png", "component_hoe_head.png", "component_blade.png" };

        File itemDir = new File(ASSETS_DIR, "textures/items");
        assertTrue(itemDir.exists() && itemDir.isDirectory(), "Item texture directory must exist");

        for (String tex : itemTextures) {
            File texFile = new File(itemDir, tex);
            assertTrue(texFile.exists(), "Item texture missing from assets: " + tex);
            assertTrue(texFile.length() > 0, "Item texture must not be empty: " + tex);

            if (tex.startsWith("tool_") || tex.startsWith("component_")) {
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(texFile);
                assertNotNull(img, "Must read image: " + tex);
                assertEquals(16, img.getWidth(), "Tool item must be 16x16: " + tex);
                assertEquals(16, img.getHeight(), "Tool item must be 16x16: " + tex);
            }
        }
    }

    @Test
    void genericCarriersSafeWithEmptyOrCorruptedNbt() {
        // 1. Material sample
        ItemStack emptyMat = new ItemStack(NEItems.materialSample);
        assertEquals("Material Sample (Empty)", emptyMat.getDisplayName());

        ItemStack corruptMat = new ItemStack(NEItems.materialSample);
        NBTTagCompound badTag = new NBTTagCompound();
        badTag.setInteger("Format", 999);
        corruptMat.setTagCompound(badTag);
        assertEquals("\u00a7cMaterial Sample (Corrupted)", corruptMat.getDisplayName());

        // 2. Generic component
        ItemStack emptyComp = new ItemStack(NEItems.genericComponent);
        assertEquals("Component", emptyComp.getDisplayName());

        ItemStack corruptComp = new ItemStack(NEItems.genericComponent);
        corruptComp.setTagCompound(new NBTTagCompound());
        assertEquals("Component", corruptComp.getDisplayName());

        // 3. Casting mold
        ItemStack emptyMold = new ItemStack(NEItems.castingMold);
        assertEquals("Unshaped Blank Mold", emptyMold.getDisplayName());

        ItemStack corruptMold = new ItemStack(NEItems.castingMold);
        corruptMold.setTagCompound(new NBTTagCompound());
        assertEquals("Unshaped Blank Mold", corruptMold.getDisplayName());

        // 4. Generic assembly
        ItemStack emptyTool = new ItemStack(NEItems.genericAssembly);
        assertEquals("Tool Assembly", emptyTool.getDisplayName());

        ItemStack corruptTool = new ItemStack(NEItems.genericAssembly);
        corruptTool.setTagCompound(new NBTTagCompound());
        assertEquals("Tool Assembly", corruptTool.getDisplayName());
    }

    @Test
    void genericCarrierNamesDoNotRevealUnrecordedMaterials() {
        // Authored solid material sample
        ItemStack cuSample = ItemMaterialSample.createStack(BuiltinMaterials.COPPER.ref());
        assertEquals("Material Sample", cuSample.getDisplayName());

        // Authored liquid material sample
        ActiveThermalState hotCu = ActiveThermalState
            .atTemperature(1400.0, 1.0, 0.1, BuiltinMaterials.COPPER_PROPERTIES);
        ItemStack hotCuSample = ItemMaterialSample.createStack(BuiltinMaterials.COPPER.ref(), null, hotCu);
        assertEquals("Molten Material Sample", hotCuSample.getDisplayName());

        // Generated material sample
        CanonicalComposition genComp = CanonicalComposition.builder()
            .add(ResourceId.of("naturalexceptions", "copper"), 0.57)
            .add(ResourceId.of("naturalexceptions", "tin"), 0.23)
            .add(ResourceId.of("naturalexceptions", "lead"), 0.20)
            .build();
        MaterialRef genRef = MaterialRef.generatedFrom(genComp);
        ItemStack genSample = ItemMaterialSample.createStack(genRef, genComp);
        assertEquals("Material Sample", genSample.getDisplayName());
        assertFalse(
            genSample.getDisplayName()
                .contains("generated_"),
            "Must not leak raw generated SHA ID");

        // Component: Copper pick head
        ComponentInstance cuHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);
        ItemStack headStack = ItemGenericComponent.createStack(cuHead);
        assertEquals("Pick Head", headStack.getDisplayName());

        // Component: Wood handle
        ComponentInstance woodHandle = new ComponentInstance(
            GeometryId.HANDLE,
            BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "wood"))),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);
        ItemStack handleStack = ItemGenericComponent.createStack(woodHandle);
        assertEquals("Handle", handleStack.getDisplayName());

        // Casting mold: Pick Head
        ItemStack pickMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        assertEquals("Casting Mold (Pick Head)", pickMold.getDisplayName());

        // Assembly: Copper + Wood Pick
        AssemblyGraph tool = AssemblyGraph.ofTool(cuHead, woodHandle);
        ItemStack toolStack = ItemGenericAssembly.createStack(tool);
        assertEquals("Pickaxe", toolStack.getDisplayName());
        assertFalse(
            toolStack.getDisplayName()
                .contains("itemAssembly"),
            "Must not leak internal variable name");

        // Broken assembly
        cuHead.damage()
            .fracture();
        ItemStack brokenTool = ItemGenericAssembly.createStack(tool);
        assertEquals("Pickaxe", brokenTool.getDisplayName());
    }

    @Test
    void creativeSubItemsArePopulatedWithMeaningfulContent() {
        List<ItemStack> subItems = new ArrayList<>();
        NEItems.genericComponent.getSubItems(NEItems.genericComponent, NECreativeTab.INSTANCE, subItems);
        assertEquals(10, subItems.size(), "One material-free component per geometry");

        List<ItemStack> moldSubItems = new ArrayList<>();
        NEItems.castingMold.getSubItems(NEItems.castingMold, NECreativeTab.INSTANCE, moldSubItems);
        assertTrue(moldSubItems.size() >= 2, "CastingMold must provide representative mold variants in creative");

        List<ItemStack> toolSubItems = new ArrayList<>();
        NEItems.genericAssembly.getSubItems(NEItems.genericAssembly, NECreativeTab.INSTANCE, toolSubItems);
        assertEquals(5, toolSubItems.size(), "One material-free carrier per tool geometry");
    }
}
