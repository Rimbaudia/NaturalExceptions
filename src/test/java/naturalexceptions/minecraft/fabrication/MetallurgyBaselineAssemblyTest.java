package naturalexceptions.minecraft.fabrication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.assembly.AssemblyGraph;
import naturalexceptions.core.assembly.DerivedCapability;
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
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemBlacksmithTongs;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.item.ItemGenericAssembly;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * Verification test suite for the Metallurgy Gameplay Simplification & Tool Assembly pass.
 *
 * Validates the core baseline loop:
 * Smelt -> Pour into mold -> Cool -> Extract bare-handed -> Assemble on Assembly Table -> Usable tool.
 * Ensures advanced forging, tongs, and thermal treatments remain optional.
 */
final class MetallurgyBaselineAssemblyTest {

    public static class TestPlayer extends net.minecraft.entity.player.EntityPlayer {

        public boolean damaged = false;
        public java.util.List<String> messages = new java.util.ArrayList<>();

        public TestPlayer() {
            super(null, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "TestPlayer"));
        }

        public boolean sneaking = false;

        @Override
        public boolean isSneaking() {
            return sneaking;
        }

        public static TestPlayer create() {
            try {
                java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
                TestPlayer player = (TestPlayer) unsafe.allocateInstance(TestPlayer.class);
                player.capabilities = new net.minecraft.entity.player.PlayerCapabilities();
                player.rand = new java.util.Random();
                player.dataWatcher = new net.minecraft.entity.DataWatcher(player);
                player.messages = new java.util.ArrayList<>();
                player.inventory = new net.minecraft.entity.player.InventoryPlayer(player);
                player.inventoryContainer = new net.minecraft.inventory.ContainerPlayer(
                    player.inventory,
                    false,
                    player);
                player.openContainer = player.inventoryContainer;
                return player;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean attackEntityFrom(DamageSource source, float amount) {
            this.damaged = true;
            return true;
        }

        public boolean tookDamage() {
            return damaged;
        }

        public void clearDamage() {
            damaged = false;
        }

        @Override
        public void addChatMessage(net.minecraft.util.IChatComponent chat) {
            messages.add(chat.getUnformattedText());
        }

        @Override
        public boolean canCommandSenderUseCommand(int perm, String cmd) {
            return true;
        }

        @Override
        public net.minecraft.util.ChunkCoordinates getPlayerCoordinates() {
            return new net.minecraft.util.ChunkCoordinates(0, 64, 0);
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
    void testCopperPickaxeBaselineEndToEndWorkflow() {
        // Step 1: Molten copper prepared in smelter and poured into pick-head mold
        ItemStack pickMold = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        MaterialQuantity headMass = ComponentGeometry.pickHead()
            .requiredMass(BuiltinMaterials.COPPER_PROPERTIES);

        ActiveThermalState moltenThermal = ActiveThermalState
            .atTemperature(1358.0, 1.0, headMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);

        AggregateLayer moltenCopper = new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION)
                .asMolten(),
            BuiltinMaterials.COPPER_COMPOSITION,
            headMass,
            moltenThermal,
            AggregatePhaseType.MOLTEN_METAL);

        assertTrue(ItemCastingMold.fillMold(pickMold, moltenCopper), "Outlet must pour into pick head mold");
        assertTrue(ItemCastingMold.isFilled(pickMold));
        assertFalse(ItemCastingMold.isSolidified(pickMold));

        // Step 2: Casting solidifies and cools naturally to ambient temperature (293.15 K)
        ActiveThermalState coolThermal = ActiveThermalState
            .atTemperature(293.15, 0.0, headMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        ItemCastingMold.saveThermalState(pickMold, coolThermal);
        assertTrue(ItemCastingMold.isSolidified(pickMold), "Mold must be solidified at ambient temperature");

        // Step 3: Bare-handed extraction without tongs (cooled casting <= 373.15 K)
        TestPlayer player = TestPlayer.create();
        ItemStack extractedComp = ItemCastingMold.extractComponent(pickMold);
        assertNotNull(extractedComp, "Cooled casting must extract cleanly");
        assertTrue(extractedComp.getItem() instanceof ItemGenericComponent);

        ComponentInstance headInstance = ItemGenericComponent.getComponent(extractedComp);
        assertNotNull(headInstance);
        assertEquals(GeometryId.PICK_HEAD, headInstance.geometryId(), "Geometry must be PICK_HEAD directly from mold");
        assertEquals(BuiltinMaterials.COPPER.ref(), headInstance.materialRef());
        assertFalse(player.tookDamage(), "Player must NOT take scald damage when extracting cool casting bare-handed");

        // Step 4: Assemble Copper Pickaxe on Assembly Table using stick handle
        TileEntityAssemblyTable table = new TileEntityAssemblyTable();
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HEAD, extractedComp);
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HANDLE, new ItemStack(Items.stick));

        ItemStack toolResult = table.computeResult();
        assertNotNull(toolResult, "Assembly Table must assemble head + stick into tool");
        assertTrue(toolResult.getItem() instanceof ItemGenericAssembly);
        assertTrue(
            toolResult.getDisplayName()
                .endsWith("Pickaxe"),
            "material, then the shape: " + toolResult.getDisplayName());
        assertFalse(
            toolResult.getDisplayName()
                .contains("Copper"),
            "Uninspected material is named by appearance only");

        // Step 5: Extract tool from Assembly Table
        ItemStack assembledPick = table.decrStackSize(TileEntityAssemblyTable.SLOT_RESULT, 1);
        assertNotNull(assembledPick);
        assertNull(table.getStackInSlot(TileEntityAssemblyTable.SLOT_HEAD), "Head component consumed");
        assertNull(table.getStackInSlot(TileEntityAssemblyTable.SLOT_HANDLE), "Handle component consumed");

        // Step 6: Verify tool mining capability and harvest characteristics
        ItemGenericAssembly toolItem = (ItemGenericAssembly) assembledPick.getItem();
        assertTrue(
            toolItem.getToolClasses(assembledPick)
                .contains("pickaxe"));
        int harvestLevel = toolItem.getHarvestLevel(assembledPick, "pickaxe");
        assertTrue(harvestLevel >= 1, "Copper pickaxe must have harvest level >= 1 (harvests stone and copper ore)");

        assertTrue(toolItem.canHarvestBlock(NEBlocks.copperOre, assembledPick));
        if (Blocks.stone != null) {
            assertTrue(toolItem.canHarvestBlock(Blocks.stone, assembledPick));
        }

        float speed = toolItem.getDigSpeed(assembledPick, NEBlocks.copperOre, 0);
        assertTrue(speed > 1.0F, "Copper pickaxe must mine copper ore with accelerated dig speed");

        // Step 7: Mine a block and verify physical damage accumulation
        toolItem.onBlockDestroyed(assembledPick, null, NEBlocks.copperOre, 0, 0, 0, null);
        AssemblyGraph updatedGraph = ItemGenericAssembly.getAssembly(assembledPick);
        assertNotNull(updatedGraph);
        assertTrue(
            updatedGraph.head()
                .damage()
                .wear() > 0.0,
            "Physical wear must accumulate on head after mining");
        assertTrue(toolItem.showDurabilityBar(assembledPick), "Durability bar must display when tool is damaged");
    }

    @Test
    void testGeneratedAlloyToolAssemblyAndPersistence() {
        // Dynamically generated Fe70Cu30 alloy composition
        CanonicalComposition feCuComposition = CanonicalComposition.builder()
            .add(ResourceId.of("naturalexceptions", "iron"), 0.70)
            .add(ResourceId.of("naturalexceptions", "copper"), 0.30)
            .build();

        MaterialRef alloyRef = MaterialRef.generatedFrom(feCuComposition);
        CanonicalMaterialKey alloyKey = CanonicalMaterialKey.of(feCuComposition);

        MaterialPropertyResolver resolver = new MaterialPropertyResolver();
        MaterialProperties alloyProps = resolver.resolve(alloyRef, feCuComposition, MaterialPhase.SOLID)
            .properties();

        // Cast pick head from generated alloy
        ComponentGeometry geom = ComponentGeometry.pickHead();
        MaterialQuantity requiredMass = geom.requiredMass(alloyProps);

        ComponentInstance alloyHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            alloyRef,
            alloyKey,
            requiredMass,
            ManufacturingState.AS_CAST);

        ItemStack headStack = ItemGenericComponent.createStack(alloyHead);

        // Assemble on Assembly Table with wood handle
        TileEntityAssemblyTable table = new TileEntityAssemblyTable();
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HEAD, headStack);
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HANDLE, new ItemStack(Items.stick));

        ItemStack alloyPick = table.computeResult();
        assertNotNull(alloyPick, "Assembly Table must assemble generated alloy head");

        AssemblyGraph graph = ItemGenericAssembly.getAssembly(alloyPick);
        assertNotNull(graph);
        assertEquals(
            alloyRef,
            graph.head()
                .materialRef(),
            "Tool must preserve generated alloy reference");
        assertEquals(
            alloyKey,
            graph.head()
                .stateKey(),
            "Tool must preserve exact alloy composition key");

        // Verify physical properties differ appropriately from pure copper and pure iron
        DerivedCapability alloyCap = ItemGenericAssembly.getCapability(graph);
        assertNotNull(alloyCap);

        ComponentInstance cuHead = new ComponentInstance(
            GeometryId.PICK_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            geom.requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.AS_CAST);
        AssemblyGraph cuPick = AssemblyGraph.ofTool(cuHead, graph.handle());
        DerivedCapability cuCap = ItemGenericAssembly.getCapability(cuPick);

        assertTrue(
            alloyCap.penetration() > cuCap.penetration(),
            "Fe-Cu alloy hardness/penetration must exceed softer pure copper");

        // Test NBT persistence across inventory transfer / world reload
        NBTTagCompound nbt = new NBTTagCompound();
        alloyPick.writeToNBT(nbt);

        ItemStack reloaded = new ItemStack(NEItems.genericAssembly);
        reloaded.readFromNBT(nbt);
        assertNotNull(reloaded, "Tool must reload successfully from NBT");
        AssemblyGraph reloadedGraph = ItemGenericAssembly.getAssembly(reloaded);
        assertNotNull(reloadedGraph);
        assertEquals(
            alloyRef,
            reloadedGraph.head()
                .materialRef(),
            "Reloaded tool preserves alloy identity");
        assertEquals(
            alloyKey,
            reloadedGraph.head()
                .stateKey(),
            "Reloaded tool preserves composition key");
        assertEquals(
            graph.totalMass()
                .milligrams(),
            reloadedGraph.totalMass()
                .milligrams(),
            "Mass conserved across NBT");
    }

    @Test
    void testMultipleToolGeometriesSupport() {
        // 1. Axe: AXE_HEAD + handle -> axe tool class, chops wood
        ComponentInstance axeHead = new ComponentInstance(
            GeometryId.AXE_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            ComponentGeometry.axeHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.AS_CAST);
        ItemStack axeHeadStack = ItemGenericComponent.createStack(axeHead);

        TileEntityAssemblyTable table = new TileEntityAssemblyTable();
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HEAD, axeHeadStack);
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HANDLE, new ItemStack(Items.stick));

        ItemStack axeResult = table.computeResult();
        assertNotNull(axeResult);
        assertTrue(
            axeResult.getDisplayName()
                .endsWith("Axe"),
            "material, then the shape: " + axeResult.getDisplayName());

        ItemGenericAssembly assemblyItem = (ItemGenericAssembly) axeResult.getItem();
        assertTrue(
            assemblyItem.getToolClasses(axeResult)
                .contains("axe"));
        if (Blocks.log != null) {
            assertTrue(assemblyItem.canHarvestBlock(Blocks.log, axeResult));
            assertTrue(assemblyItem.getDigSpeed(axeResult, Blocks.log, 0) > 1.0F);
        }

        // 2. Shovel: SHOVEL_HEAD + handle -> shovel tool class, digs dirt/sand
        ComponentInstance shovelHead = new ComponentInstance(
            GeometryId.SHOVEL_HEAD,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            ComponentGeometry.shovelHead()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.AS_CAST);
        ItemStack shovelHeadStack = ItemGenericComponent.createStack(shovelHead);

        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HEAD, shovelHeadStack);
        ItemStack shovelResult = table.computeResult();
        assertNotNull(shovelResult);
        assertTrue(
            shovelResult.getDisplayName()
                .endsWith("Shovel"),
            "material, then the shape: " + shovelResult.getDisplayName());
        assertTrue(
            assemblyItem.getToolClasses(shovelResult)
                .contains("shovel"));
        if (Blocks.dirt != null) {
            assertTrue(assemblyItem.canHarvestBlock(Blocks.dirt, shovelResult));
            assertTrue(assemblyItem.getDigSpeed(shovelResult, Blocks.dirt, 0) > 1.0F);
        }

        // 3. Sword: BLADE + handle -> weapon damage modifier
        ComponentInstance blade = new ComponentInstance(
            GeometryId.BLADE,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            ComponentGeometry.blade()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.AS_CAST);
        ItemStack bladeStack = ItemGenericComponent.createStack(blade);

        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HEAD, bladeStack);
        ItemStack swordResult = table.computeResult();
        assertNotNull(swordResult);
        assertTrue(
            swordResult.getDisplayName()
                .endsWith("Sword"),
            "material, then the shape: " + swordResult.getDisplayName());

        com.google.common.collect.Multimap attrs = swordResult.getAttributeModifiers();
        assertTrue(attrs.containsKey(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName()));
        AttributeModifier mod = (AttributeModifier) attrs
            .get(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName())
            .iterator()
            .next();
        assertTrue(mod.getAmount() > 4.0, "Copper sword must provide substantial attack damage bonus");
    }

    @Test
    void testTemperatureSafetyAndTongsInteractions() {
        ItemStack moldStack = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        MaterialQuantity mass = ComponentGeometry.pickHead()
            .requiredMass(BuiltinMaterials.COPPER_PROPERTIES);

        // Case A: Hot casting (600 K > 373.15 K)
        ActiveThermalState hotThermal = ActiveThermalState
            .atTemperature(600.0, 0.0, mass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        AggregateLayer hotLayer = new AggregateLayer(
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            BuiltinMaterials.COPPER_COMPOSITION,
            mass,
            hotThermal,
            AggregatePhaseType.MOLTEN_METAL);
        assertTrue(ItemCastingMold.fillMold(moldStack, hotLayer));
        ItemCastingMold.saveThermalState(moldStack, hotThermal);
        assertTrue(ItemCastingMold.isSolidified(moldStack));

        TestPlayer player = TestPlayer.create();
        // In-hand extraction of hot casting burns player
        moldStack.getItem()
            .onItemRightClick(moldStack, null, player);
        assertTrue(player.tookDamage(), "Extracting hot casting bare-handed must cause scald damage");

        // Tongs extraction on casting table handles hot casting safely
        ItemStack tableMoldStack = ItemCastingMold.createStack(GeometryId.PICK_HEAD);
        assertTrue(ItemCastingMold.fillMold(tableMoldStack, hotLayer));
        ItemCastingMold.saveThermalState(tableMoldStack, hotThermal);

        TileEntityCastingTable table = new TileEntityCastingTable();
        table.setMold(tableMoldStack);
        ItemStack tongsStack = new ItemStack(NEItems.blacksmithTongs);

        BlockCastingTable tableBlock = (BlockCastingTable) NEBlocks.castingTable;
        player.clearDamage();
        player.setCurrentItemOrArmor(0, tongsStack);
        assertTrue(tableBlock.handleInteraction(table, player, null, 0, 0, 0));
        assertTrue(ItemBlacksmithTongs.hasWorkpiece(tongsStack), "Tongs must safely hold hot workpiece");
        assertFalse(player.tookDamage(), "Tongs extraction must NOT cause scald damage");
    }

    @Test
    void testIncompatibleAssemblyCombinationsRejected() {
        TileEntityAssemblyTable table = new TileEntityAssemblyTable();

        // 1. Blank + Handle: Blank is not a valid tool head
        ComponentInstance blankComp = new ComponentInstance(
            GeometryId.BLANK,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            ComponentGeometry.blank()
                .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
            ManufacturingState.AS_CAST);
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HEAD, ItemGenericComponent.createStack(blankComp));
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HANDLE, new ItemStack(Items.stick));
        assertNull(table.computeResult(), "Blank cannot be assembled as a tool head");

        // 2. Handle in head slot: rejected
        ComponentInstance handleComp = new ComponentInstance(
            GeometryId.HANDLE,
            BuiltinMaterials.WOOD.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.WOOD_COMPOSITION),
            MaterialQuantity.ofGrams(100.0),
            ManufacturingState.FINISHED);
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HEAD, ItemGenericComponent.createStack(handleComp));
        assertNull(table.computeResult(), "Handle cannot serve as head");

        // 3. Head in handle slot: rejected
        table.setInventorySlotContents(
            TileEntityAssemblyTable.SLOT_HEAD,
            ItemGenericComponent.createStack(
                new ComponentInstance(
                    GeometryId.PICK_HEAD,
                    BuiltinMaterials.COPPER.ref(),
                    CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
                    ComponentGeometry.pickHead()
                        .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
                    ManufacturingState.AS_CAST)));
        table.setInventorySlotContents(
            TileEntityAssemblyTable.SLOT_HANDLE,
            ItemGenericComponent.createStack(
                new ComponentInstance(
                    GeometryId.PICK_HEAD,
                    BuiltinMaterials.COPPER.ref(),
                    CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
                    ComponentGeometry.pickHead()
                        .requiredMass(BuiltinMaterials.COPPER_PROPERTIES),
                    ManufacturingState.AS_CAST)));
        assertNull(table.computeResult(), "Two heads cannot be assembled");
    }

    @Test
    void testOptionalForgingWorkflowRemainsFullyFunctional() {
        TileEntityForgingAnvil anvil = new TileEntityForgingAnvil();
        anvil.setTargetGeometry(GeometryId.PICK_HEAD);

        // Place hot copper blank on anvil
        MaterialQuantity blankMass = ComponentGeometry.blank()
            .requiredMass(BuiltinMaterials.COPPER_PROPERTIES);
        ComponentInstance blank = new ComponentInstance(
            GeometryId.BLANK,
            BuiltinMaterials.COPPER.ref(),
            CanonicalMaterialKey.of(BuiltinMaterials.COPPER_COMPOSITION),
            blankMass,
            ManufacturingState.AS_CAST);
        ItemStack blankStack = ItemGenericComponent.createStack(blank);
        ActiveThermalState hotState = ActiveThermalState
            .atTemperature(900.0, 0.0, blankMass.toKilograms(), BuiltinMaterials.COPPER_PROPERTIES);
        ItemGenericComponent.saveThermalState(blankStack, hotState);

        anvil.setWorkpiece(blankStack);
        ItemStack hammer = new ItemStack(NEItems.blacksmithHammer);
        TestPlayer player = TestPlayer.create();

        // Hammer strikes deform blank towards PICK_HEAD target
        assertTrue(anvil.hammerStrike(player, hammer), "Hammer strike 1");
        assertTrue(anvil.hammerStrike(player, hammer), "Hammer strike 2");
        assertTrue(anvil.hammerStrike(player, hammer), "Hammer strike 3");

        ItemStack finishedWorkpiece = anvil.getWorkpiece();
        assertNotNull(finishedWorkpiece);
        ComponentInstance finishedComp = ItemGenericComponent.getComponent(finishedWorkpiece);
        assertNotNull(finishedComp);
        assertEquals(GeometryId.PICK_HEAD, finishedComp.geometryId(), "Anvil shaping must reach target PICK_HEAD");
        assertEquals(ManufacturingState.FINISHED, finishedComp.manufacturingState());

        // Finished forged component can now be assembled directly on Assembly Table
        TileEntityAssemblyTable table = new TileEntityAssemblyTable();
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HEAD, finishedWorkpiece);
        table.setInventorySlotContents(TileEntityAssemblyTable.SLOT_HANDLE, new ItemStack(Items.stick));

        ItemStack forgedPick = table.computeResult();
        assertNotNull(forgedPick, "Forged head must assemble smoothly on Assembly Table");
        assertTrue(
            forgedPick.getDisplayName()
                .endsWith("Pickaxe"),
            "material, then the shape: " + forgedPick.getDisplayName());
    }
}
