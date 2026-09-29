package naturalexceptions.core.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.metallurgy.VesselState;
import naturalexceptions.core.persistence.SaveSchemaVersion;
import naturalexceptions.core.persistence.WorldMetadata;
import naturalexceptions.core.persistence.WorldgenEpoch;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.item.ItemMaterialSample;
import naturalexceptions.minecraft.metallurgy.FeedstockItemRegistry;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

public final class MaterialNamingHierarchyTest {

    @BeforeAll
    static void initAll() {
        NEItems.ensureBootstrapped();
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
        FeedstockItemRegistry.registerBuiltins();
    }

    @BeforeEach
    void setUp() {
        NEItems.ensureBootstrapped();
        MaterialNameRegistry.reset();
    }

    @Test
    void copperResolvesAsCopperNeverGeneratedAlloy() {
        // 1. Ingot item display name
        ItemStack copperIngot = new ItemStack(NEItems.copperIngot);
        assertEquals("Copper Ingot", copperIngot.getDisplayName());

        // 2. Component display name
        ComponentInstance copperHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);
        ItemStack headStack = ItemGenericComponent.createStack(copperHead);
        assertTrue(
            headStack.getDisplayName()
                .endsWith("Pick Head"),
            "material, then the shape: " + headStack.getDisplayName());
        assertFalse(
            headStack.getDisplayName()
                .contains("Generated"));

        // 3. Dynamic assembly display name
        ComponentInstance woodHandle = new ComponentInstance(
            GeometryId.HANDLE,
            BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.WOOD_COMPOSITION),
            MaterialQuantity.ofGrams(80.0),
            ManufacturingState.FINISHED);
        AssemblyGraph assembly = AssemblyGraph.ofTool(copperHead, woodHandle);
        ItemStack toolStack = ItemGenericAssembly.createStack(assembly);
        assertTrue(
            toolStack.getDisplayName()
                .endsWith("Pickaxe"),
            "material, then the shape: " + toolStack.getDisplayName());
        assertFalse(
            toolStack.getDisplayName()
                .contains("Generated"));

        // 4. Casting mold display name
        ItemStack mold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        AggregateLayer layer = new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            BuiltinMaterials.COPPER_COMPOSITION,
            MaterialQuantity.ofGrams(268.8),
            ActiveThermalState.atTemperature(300.0, 0.0, 0.2688, BuiltinMaterials.COPPER_PROPERTIES),
            AggregatePhaseType.MOLTEN_METAL);
        ItemCastingMold.fillMold(mold, layer);
        assertEquals("Solidified Casting Mold (Pick Head)", mold.getDisplayName());
        assertFalse(
            mold.getDisplayName()
                .contains("Generated"));

        // 5. Molten layer mixing in VesselState: two pure copper melts must not become GENERATED
        VesselState vessel = new VesselState();
        AggregateLayer melt1 = new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)
                .asMolten(),
            BuiltinMaterials.COPPER_COMPOSITION,
            MaterialQuantity.ofKilograms(1.0),
            ActiveThermalState.atTemperature(1400.0, 1.0, 1.0, BuiltinMaterials.COPPER_PROPERTIES),
            AggregatePhaseType.MOLTEN_METAL);
        AggregateLayer melt2 = new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)
                .asMolten(),
            BuiltinMaterials.COPPER_COMPOSITION,
            MaterialQuantity.ofKilograms(1.0),
            ActiveThermalState.atTemperature(1400.0, 1.0, 1.0, BuiltinMaterials.COPPER_PROPERTIES),
            AggregatePhaseType.MOLTEN_METAL);
        vessel.addLayer(melt1);
        vessel.addLayer(melt2);
        vessel.step(20, 300.0, 1.0);

        assertEquals(
            1,
            vessel.layers()
                .size());
        AggregateLayer mixed = vessel.layers()
            .get(0);
        assertTrue(
            mixed.materialRef()
                .isAuthored(),
            "Mixed pure copper layer must retain authored ref");
        assertEquals(BuiltinMaterials.COPPER.ref(), mixed.materialRef());
        assertFalse(
            mixed.materialRef()
                .isGenerated());
    }

    @Test
    void ironResolvesAsIron() {
        ComponentInstance ironHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.IRON.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.IRON_COMPOSITION),
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);
        ComponentInstance woodHandle = new ComponentInstance(
            GeometryId.HANDLE,
            BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.WOOD_COMPOSITION),
            MaterialQuantity.ofGrams(80.0),
            ManufacturingState.FINISHED);

        AssemblyGraph tool = AssemblyGraph.ofTool(ironHead, woodHandle);
        assertEquals("Iron Pickaxe", MaterialNameResolver.resolveToolDisplayName(tool));
        assertEquals("Iron Pick Head", MaterialNameResolver.resolveComponentDisplayName(ironHead));
        assertEquals(
            "Iron",
            MaterialNameResolver.resolveDisplayName(BuiltinMaterials.IRON.ref(), BuiltinMaterials.IRON_COMPOSITION));
    }

    @Test
    void goldResolvesAsGold() {
        ComponentInstance goldHead = new ComponentInstance(
            GeometryId.BLADE,
            BuiltinMaterials.GOLD.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.GOLD_COMPOSITION),
            MaterialQuantity.ofGrams(500.0),
            ManufacturingState.FINISHED);
        ComponentInstance woodHandle = new ComponentInstance(
            GeometryId.HANDLE,
            BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.WOOD_COMPOSITION),
            MaterialQuantity.ofGrams(80.0),
            ManufacturingState.FINISHED);

        AssemblyGraph sword = AssemblyGraph.ofTool(goldHead, woodHandle);
        assertEquals("Gold Sword", MaterialNameResolver.resolveToolDisplayName(sword));
        assertEquals("Gold Blade", MaterialNameResolver.resolveComponentDisplayName(goldHead));
        assertEquals(
            "Gold",
            MaterialNameResolver.resolveDisplayName(BuiltinMaterials.GOLD.ref(), BuiltinMaterials.GOLD_COMPOSITION));
    }

    @Test
    void bronzeResolvesAsBronze() {
        ComponentInstance bronzeHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.BRONZE.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.BRONZE_COMPOSITION),
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);
        ComponentInstance woodHandle = new ComponentInstance(
            GeometryId.HANDLE,
            BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.WOOD_COMPOSITION),
            MaterialQuantity.ofGrams(80.0),
            ManufacturingState.FINISHED);

        AssemblyGraph pick = AssemblyGraph.ofTool(bronzeHead, woodHandle);
        assertEquals("Bronze Pickaxe", MaterialNameResolver.resolveToolDisplayName(pick));
        assertEquals("Bronze Pick Head", MaterialNameResolver.resolveComponentDisplayName(bronzeHead));
        assertEquals(
            "Bronze",
            MaterialNameResolver
                .resolveDisplayName(BuiltinMaterials.BRONZE.ref(), BuiltinMaterials.BRONZE_COMPOSITION));
    }

    @Test
    void unnamedGeneratedAlloyReceivesDeterministicFallbackName() {
        CanonicalComposition feCuAlloy = CanonicalComposition.builder()
            .add(ResourceId.of("naturalexceptions", "iron"), 0.70)
            .add(ResourceId.of("naturalexceptions", "copper"), 0.30)
            .build();
        MaterialRef alloyRef = MaterialRef.generatedFrom(feCuAlloy);

        assertEquals("Fe70Cu30 Alloy", MaterialNameResolver.formatCompositionFallback(feCuAlloy));
        assertEquals("Fe70Cu30 Alloy", MaterialNameResolver.resolveDisplayName(alloyRef, feCuAlloy));

        ComponentInstance alloyHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            alloyRef,
            CanonicalMaterialKey.of(feCuAlloy),
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);
        ComponentInstance woodHandle = new ComponentInstance(
            GeometryId.HANDLE,
            BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.WOOD_COMPOSITION),
            MaterialQuantity.ofGrams(80.0),
            ManufacturingState.FINISHED);

        AssemblyGraph tool = AssemblyGraph.ofTool(alloyHead, woodHandle);
        assertEquals("Fe70Cu30 Alloy Pickaxe", MaterialNameResolver.resolveToolDisplayName(tool));
        assertEquals("Fe70Cu30 Alloy Pick Head", MaterialNameResolver.resolveComponentDisplayName(alloyHead));
    }

    @Test
    void legacyGlobalNameDoesNotLeakIntoUninformedCarrierNames() {
        CanonicalComposition feCuAlloy = CanonicalComposition.builder()
            .add(ResourceId.of("naturalexceptions", "iron"), 0.70)
            .add(ResourceId.of("naturalexceptions", "copper"), 0.30)
            .build();
        MaterialRef alloyRef = MaterialRef.generatedFrom(feCuAlloy);

        // Before renaming: deterministic fallback
        assertEquals("Fe70Cu30 Alloy", MaterialNameResolver.resolveDisplayName(alloyRef, feCuAlloy));

        // Assign player custom alloy name
        MaterialNameRegistry.register(feCuAlloy, "Redsteel");
        assertEquals("Redsteel", MaterialNameRegistry.get(feCuAlloy));
        assertEquals("Redsteel", MaterialNameRegistry.get(alloyRef));

        // 1. Resolves as Redsteel in MaterialNameResolver
        assertEquals("Redsteel", MaterialNameResolver.resolveDisplayName(alloyRef, feCuAlloy));

        // Player-facing carrier labels have no player context here and must not expose canonical or global names.
        ComponentInstance head = new ComponentInstance(
            GeometryId.PICK_HEAD,
            alloyRef,
            CanonicalMaterialKey.of(feCuAlloy),
            MaterialQuantity.ofGrams(268.8),
            ManufacturingState.FINISHED);
        ItemStack headStack = ItemGenericComponent.createStack(head);
        assertTrue(
            headStack.getDisplayName()
                .endsWith("Pick Head"),
            "material, then the shape: " + headStack.getDisplayName());

        // 3. Dynamic Tool derives custom name
        ComponentInstance woodHandle = new ComponentInstance(
            GeometryId.HANDLE,
            BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.WOOD_COMPOSITION),
            MaterialQuantity.ofGrams(80.0),
            ManufacturingState.FINISHED);
        AssemblyGraph assembly = AssemblyGraph.ofTool(head, woodHandle);
        ItemStack toolStack = ItemGenericAssembly.createStack(assembly);
        assertTrue(
            toolStack.getDisplayName()
                .endsWith("Pickaxe"),
            "material, then the shape: " + toolStack.getDisplayName());

        // 4. Material Sample derives custom name
        ItemStack sample = ItemMaterialSample.createStack(alloyRef, feCuAlloy);
        assertEquals("Material Sample", sample.getDisplayName());

        // 5. Mold derives custom name
        ItemStack mold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        AggregateLayer layer = new AggregateLayer(
            alloyRef,
            CanonicalMaterialKey.of(feCuAlloy),
            feCuAlloy,
            MaterialQuantity.ofGrams(268.8),
            ActiveThermalState.atTemperature(300.0, 0.0, 0.2688, BuiltinMaterials.IRON_PROPERTIES),
            AggregatePhaseType.MOLTEN_METAL);
        ItemCastingMold.fillMold(mold, layer);
        assertEquals("Solidified Casting Mold (Pick Head)", mold.getDisplayName());
    }

    @Test
    void worldSaveDoesNotPersistAPlayerPersonalAliasGlobally() {
        CanonicalComposition alloy = CanonicalComposition.builder()
            .add(ResourceId.of("naturalexceptions", "iron"), 0.60)
            .add(ResourceId.of("naturalexceptions", "gold"), 0.40)
            .build();
        MaterialNameRegistry.register(alloy, "Sunsteel");

        // Save to NBT via NEWorldSavedData
        NEWorldSavedData savedData = new NEWorldSavedData("naturalexceptions");
        savedData.setMetadata(
            WorldMetadata.create(
                UUID.randomUUID()
                    .toString(),
                SaveSchemaVersion.CURRENT,
                WorldgenEpoch.INITIAL,
                BuiltinContent.CONTENT_VERSION,
                0L));
        NBTTagCompound nbt = new NBTTagCompound();
        savedData.writeToNBT(nbt);

        // Wipe memory registry
        MaterialNameRegistry.reset();
        assertEquals(null, MaterialNameRegistry.get(alloy));

        // Load from NBT
        NEWorldSavedData loadedData = new NEWorldSavedData("naturalexceptions");
        loadedData.readFromNBT(nbt);

        assertNull(MaterialNameRegistry.get(alloy));
        assertEquals(
            "Fe60Au40 Alloy",
            MaterialNameResolver.resolveDisplayName(MaterialRef.generatedFrom(alloy), alloy));
    }

    @Test
    void renamingDoesNotAlterMaterialPhysicsOrComposition() {
        CanonicalComposition feCuAlloy = CanonicalComposition.builder()
            .add(ResourceId.of("naturalexceptions", "iron"), 0.70)
            .add(ResourceId.of("naturalexceptions", "copper"), 0.30)
            .build();
        MaterialRef alloyRef = MaterialRef.generatedFrom(feCuAlloy);

        MaterialPropertyResolver resolver = new MaterialPropertyResolver();
        MaterialProperties initialProps = resolver.resolve(CanonicalMaterialKey.of(feCuAlloy))
            .properties();
        double initialDensity = initialProps.density();
        double initialMeltingPoint = initialProps.meltingPointKelvin();
        double initialHardness = initialProps.hardness();

        // Assign name
        MaterialNameRegistry.register(feCuAlloy, "Redsteel");

        // Composition must be strictly identical
        assertEquals(0.70, feCuAlloy.fraction(ResourceId.of("naturalexceptions", "iron")), 1e-6);
        assertEquals(0.30, feCuAlloy.fraction(ResourceId.of("naturalexceptions", "copper")), 1e-6);

        // Properties must be strictly unchanged
        MaterialProperties afterProps = resolver.resolve(CanonicalMaterialKey.of(feCuAlloy))
            .properties();
        assertEquals(initialDensity, afterProps.density(), 1e-6);
        assertEquals(initialMeltingPoint, afterProps.meltingPointKelvin(), 1e-6);
        assertEquals(initialHardness, afterProps.hardness(), 1e-6);
    }

    @Test
    void vanillaFeedstockBindingResolvesIronAndGoldInputsCorrectly() {
        // Vanilla Iron Ingot
        FeedstockItemRegistry.ResolvedInput ironIngot = FeedstockItemRegistry.resolve(new ItemStack(Items.iron_ingot));
        assertNotNull(ironIngot, "Items.iron_ingot must resolve");
        assertEquals(BuiltinMaterials.IRON.ref(), ironIngot.materialRef());
        assertEquals(FeedstockItemRegistry.InputKind.METALLIC_PRODUCT, ironIngot.kind());
        assertEquals(MaterialQuantity.ofKilograms(1.0), ironIngot.inputMass());

        // Vanilla Iron Ore Block
        FeedstockItemRegistry.ResolvedInput ironOre = FeedstockItemRegistry.resolve(new ItemStack(Blocks.iron_ore));
        assertNotNull(ironOre, "Blocks.iron_ore must resolve");
        assertEquals(BuiltinMaterials.IRON.ref(), ironOre.materialRef());
        assertEquals(FeedstockItemRegistry.InputKind.RAW_ORE, ironOre.kind());

        // Vanilla Iron Block
        FeedstockItemRegistry.ResolvedInput ironBlock = FeedstockItemRegistry.resolve(new ItemStack(Blocks.iron_block));
        assertNotNull(ironBlock, "Blocks.iron_block must resolve");
        assertEquals(BuiltinMaterials.IRON.ref(), ironBlock.materialRef());
        assertEquals(MaterialQuantity.ofKilograms(9.0), ironBlock.inputMass());

        // Vanilla Gold Ingot
        FeedstockItemRegistry.ResolvedInput goldIngot = FeedstockItemRegistry.resolve(new ItemStack(Items.gold_ingot));
        assertNotNull(goldIngot, "Items.gold_ingot must resolve");
        assertEquals(BuiltinMaterials.GOLD.ref(), goldIngot.materialRef());

        // Vanilla Gold Ore Block
        FeedstockItemRegistry.ResolvedInput goldOre = FeedstockItemRegistry.resolve(new ItemStack(Blocks.gold_ore));
        assertNotNull(goldOre, "Blocks.gold_ore must resolve");
        assertEquals(BuiltinMaterials.GOLD.ref(), goldOre.materialRef());
        assertEquals(FeedstockItemRegistry.InputKind.RAW_ORE, goldOre.kind());

        // Vanilla Gold Nugget
        FeedstockItemRegistry.ResolvedInput goldNugget = FeedstockItemRegistry
            .resolve(new ItemStack(Items.gold_nugget));
        assertNotNull(goldNugget, "Items.gold_nugget must resolve");
        assertEquals(BuiltinMaterials.GOLD.ref(), goldNugget.materialRef());
        assertEquals(MaterialQuantity.ofMilligrams(111111), goldNugget.inputMass());
    }
}
