package naturalexceptions.minecraft.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Assumptions;
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
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.multiblock.MultiblockDefinition;
import naturalexceptions.minecraft.multiblock.MultiblockPattern;
import naturalexceptions.minecraft.multiblock.MultiblockRole;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

class KnowledgeBrowserRegistryTest {

    @BeforeAll
    static void setup() {
        BuiltinContent.register();
        NEItems.ensureBootstrapped();
        NEBlocks.register();
        NEItems.register();
        KnowledgeRegistry.instance()
            .init();
    }

    @Test
    void testRegistryBootstrappingAndIntegrity() {
        List<KnowledgeEntry> allEntries = KnowledgeRegistry.instance()
            .getAllEntries();
        assertFalse(allEntries.isEmpty(), "Registry should contain registered entries");

        for (KnowledgeEntry entry : allEntries) {
            assertNotNull(entry.id(), "Entry id must not be null");
            assertNotNull(entry.title(), "Entry title must not be null: " + entry.id());
            assertNotNull(entry.category(), "Entry category must not be null: " + entry.id());
            assertNotNull(entry.iconStack(), "Entry icon must not be null: " + entry.id());
            assertFalse(
                entry.summary()
                    .isEmpty(),
                "Summary should not be empty: " + entry.id());
            assertFalse(
                entry.description()
                    .isEmpty(),
                "Description should not be empty: " + entry.id());
            assertNotNull(entry.maturity(), "Maturity should not be null: " + entry.id());
        }

        // Verify categories
        for (KnowledgeCategory cat : KnowledgeCategory.values()) {
            List<KnowledgeEntry> catEntries = KnowledgeRegistry.instance()
                .getEntriesByCategory(cat);
            assertFalse(catEntries.isEmpty(), "Category " + cat.name() + " should contain entries");
        }
    }

    @Test
    void testMultiFieldSearch() {
        // Multi-word search
        List<KnowledgeEntry> smelterResults = KnowledgeRegistry.instance()
            .search("starter smelter");
        assertFalse(smelterResults.isEmpty());
        assertTrue(
            smelterResults.stream()
                .anyMatch(
                    e -> e.id()
                        .equals("starter_smelter")));

        // Search by category
        List<KnowledgeEntry> castingResults = KnowledgeRegistry.instance()
            .search("casting");
        assertFalse(castingResults.isEmpty());
        assertTrue(
            castingResults.stream()
                .anyMatch(
                    e -> e.id()
                        .equals("casting_table")));

        // Search by property
        List<KnowledgeEntry> tempResults = KnowledgeRegistry.instance()
            .search("1538");
        assertFalse(tempResults.isEmpty());
        assertTrue(
            tempResults.stream()
                .anyMatch(
                    e -> e.id()
                        .equals("iron_ore")
                        || e.id()
                            .equals("material_iron")));

        // Empty query returns all entries
        assertEquals(
            KnowledgeRegistry.instance()
                .getAllEntries()
                .size(),
            KnowledgeRegistry.instance()
                .search("")
                .size());
        assertEquals(
            KnowledgeRegistry.instance()
                .getAllEntries()
                .size(),
            KnowledgeRegistry.instance()
                .search("   ")
                .size());
    }

    @Test
    void testFindAcquisitionsForKeyR() {
        // 1. Refractory Brick
        ItemStack brick = new ItemStack(NEBlocks.refractoryBrick);
        Assumptions.assumeTrue(
            brick.getItem() != null,
            "The headless Forge test bootstrap does not create ItemBlock mappings; runtime client covers this lookup");
        List<KnowledgeEntry> brickAcq = KnowledgeRegistry.instance()
            .findAcquisitions(brick);
        assertFalse(brickAcq.isEmpty());
        assertTrue(
            brickAcq.stream()
                .anyMatch(
                    e -> e.id()
                        .equals("refractory_brick")));

        // 2. Tuyere
        ItemStack tuyere = new ItemStack(NEBlocks.smelterTuyere);
        List<KnowledgeEntry> tuyereAcq = KnowledgeRegistry.instance()
            .findAcquisitions(tuyere);
        assertFalse(tuyereAcq.isEmpty());
        assertTrue(
            tuyereAcq.stream()
                .anyMatch(
                    e -> e.id()
                        .equals("smelter_tuyere")));

        // 3. Hand Bellows
        ItemStack bellows = new ItemStack(NEItems.bellows);
        List<KnowledgeEntry> bellowsAcq = KnowledgeRegistry.instance()
            .findAcquisitions(bellows);
        assertFalse(bellowsAcq.isEmpty());
        assertTrue(
            bellowsAcq.stream()
                .anyMatch(
                    e -> e.id()
                        .equals("bellows")));

        // 4. Wooden Handle
        ItemStack handle = ItemGenericComponent.createWoodenHandle();
        List<KnowledgeEntry> handleAcq = KnowledgeRegistry.instance()
            .findAcquisitions(handle);
        assertFalse(handleAcq.isEmpty());
        assertTrue(
            handleAcq.stream()
                .anyMatch(
                    e -> e.id()
                        .equals("wooden_handle")));

        // 5. Dynamic Copper Pick Head
        ComponentInstance copperPickHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "copper"))),
            MaterialQuantity.ofGrams(30.0 * 8.96),
            ManufacturingState.AS_CAST);
        ItemStack pickHead = ItemGenericComponent.createStack(copperPickHead);
        List<KnowledgeEntry> pickHeadAcq = KnowledgeRegistry.instance()
            .findAcquisitions(pickHead);
        assertFalse(pickHeadAcq.isEmpty());
        assertTrue(
            pickHeadAcq.stream()
                .anyMatch(
                    e -> e.id()
                        .equals("mold_pick_head")
                        || e.id()
                            .equals("casting_table")));

        // 6. Dynamic Assembled Pickaxe
        ComponentInstance woodHandle = new ComponentInstance(
            GeometryId.HANDLE,
            BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey.of(CanonicalComposition.of(ResourceId.of("naturalexceptions", "wood"))),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);
        ItemStack pickaxe = ItemGenericAssembly.createStack(AssemblyGraph.ofTool(copperPickHead, woodHandle));
        List<KnowledgeEntry> pickaxeAcq = KnowledgeRegistry.instance()
            .findAcquisitions(pickaxe);
        assertFalse(pickaxeAcq.isEmpty());
        assertTrue(
            pickaxeAcq.stream()
                .anyMatch(
                    e -> e.id()
                        .equals("tool_assembly")));
    }

    @Test
    void testFindUsagesForKeyU() {
        // 1. Copper Ore -> Smelter
        ItemStack copperOre = new ItemStack(NEBlocks.copperOre);
        List<KnowledgeEntry> oreUsages = KnowledgeRegistry.instance()
            .findUsages(copperOre);
        assertFalse(oreUsages.isEmpty());
        assertTrue(
            oreUsages.stream()
                .anyMatch(
                    e -> e.id()
                        .equals("starter_smelter")
                        || e.id()
                            .equals("copper_ore")));

        // 2. Copper Ingot -> Tuyere / Remelting
        ItemStack copperIngot = new ItemStack(NEItems.copperIngot);
        List<KnowledgeEntry> ingotUsages = KnowledgeRegistry.instance()
            .findUsages(copperIngot);
        assertFalse(ingotUsages.isEmpty());
        assertTrue(
            ingotUsages.stream()
                .anyMatch(
                    e -> e.id()
                        .equals("smelter_tuyere")
                        || e.id()
                            .equals("starter_smelter")));

        // 3. Fuel (Coal) -> Starter Smelter
        ItemStack coal = new ItemStack(Items.coal);
        List<KnowledgeEntry> coalUsages = KnowledgeRegistry.instance()
            .findUsages(coal);
        assertFalse(coalUsages.isEmpty());
        assertTrue(
            coalUsages.stream()
                .anyMatch(
                    e -> e.id()
                        .equals("starter_smelter")));

        // 4. Hand Bellows -> Blast Smelting
        ItemStack bellows = new ItemStack(NEItems.bellows);
        List<KnowledgeEntry> bellowsUsages = KnowledgeRegistry.instance()
            .findUsages(bellows);
        assertFalse(bellowsUsages.isEmpty());
        assertTrue(
            bellowsUsages.stream()
                .anyMatch(
                    e -> e.id()
                        .equals("smelter_tuyere")
                        || e.id()
                            .equals("starter_smelter")));

        // 5. Wooden Handle -> Tool Assembly
        ItemStack handle = ItemGenericComponent.createWoodenHandle();
        List<KnowledgeEntry> handleUsages = KnowledgeRegistry.instance()
            .findUsages(handle);
        assertFalse(handleUsages.isEmpty());
        assertTrue(
            handleUsages.stream()
                .anyMatch(
                    e -> e.id()
                        .equals("tool_assembly")));

        // 6. Mold -> Casting Table
        ItemStack mold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        List<KnowledgeEntry> moldUsages = KnowledgeRegistry.instance()
            .findUsages(mold);
        assertFalse(moldUsages.isEmpty());
        assertTrue(
            moldUsages.stream()
                .anyMatch(
                    e -> e.id()
                        .equals("casting_table")));
    }

    @Test
    void testAuthoritativeMultiblockStructureDetailsInBrowser() {
        KnowledgeEntry smelterEntry = KnowledgeRegistry.instance()
            .getEntry("starter_smelter");
        assertNotNull(smelterEntry);
        assertTrue(smelterEntry.hasMultiblock());

        MultiblockDefinition mb = smelterEntry.multiblock();
        List<Integer> layers = mb.getLayerNumbers();
        assertEquals(3, layers.size());
        assertEquals(
            -1,
            layers.get(0)
                .intValue());
        assertEquals(
            0,
            layers.get(1)
                .intValue());
        assertEquals(
            1,
            layers.get(2)
                .intValue());

        // Base layer: 9 required bricks + 1 related external Casting Table (dy=-1, dz=-1)
        List<MultiblockPattern> basePatterns = mb.getPatternsInLayer(-1);
        assertEquals(10, basePatterns.size());
        long baseBricks = basePatterns.stream()
            .filter(p -> p.role() == MultiblockRole.REQUIRED_BLOCK)
            .count();
        assertEquals(9, baseBricks);
        assertTrue(
            basePatterns.stream()
                .anyMatch(p -> p.role() == MultiblockRole.RELATED_EXTERNAL));

        // Crucible chamber layer: 1 controller, 1 cavity, 7 walls, 2 tuyere ports, 1 outlet, 1 table
        List<MultiblockPattern> chamberPatterns = mb.getPatternsInLayer(0);
        assertTrue(
            chamberPatterns.stream()
                .anyMatch(p -> p.role() == MultiblockRole.REQUIRED_CONTROLLER));
        assertTrue(
            chamberPatterns.stream()
                .anyMatch(p -> p.role() == MultiblockRole.REQUIRED_AIR));
        assertTrue(
            chamberPatterns.stream()
                .anyMatch(p -> p.role() == MultiblockRole.OPTIONAL_ATTACHMENT));
        assertTrue(
            chamberPatterns.stream()
                .anyMatch(p -> p.role() == MultiblockRole.RELATED_EXTERNAL));

        // Upper rim: 8 bricks + 1 air flue
        List<MultiblockPattern> rimPatterns = mb.getPatternsInLayer(1);
        assertEquals(9, rimPatterns.size());
        long brickCount = rimPatterns.stream()
            .filter(p -> p.role() == MultiblockRole.REQUIRED_BLOCK)
            .count();
        long flueCount = rimPatterns.stream()
            .filter(p -> p.role() == MultiblockRole.REQUIRED_AIR)
            .count();
        assertEquals(8, brickCount);
        assertEquals(1, flueCount);
    }
}
