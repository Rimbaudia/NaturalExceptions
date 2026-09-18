package naturalexceptions.minecraft.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.ConditioningHistory;
import naturalexceptions.core.material.GeneratedMaterial;
import naturalexceptions.core.material.MaterialCatalogue;
import naturalexceptions.core.material.MaterialDefinition;
import naturalexceptions.core.material.MaterialPhase;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.material.Microstructure;
import naturalexceptions.core.property.MaterialPropertyResolver;
import naturalexceptions.core.property.ResolvedProperties;
import naturalexceptions.minecraft.item.ItemMaterialSample;

final class GeneratedMaterialPersistenceTest {

    private static final ResourceId CU = ResourceId.of("naturalexceptions", "copper");
    private static final ResourceId SN = ResourceId.of("naturalexceptions", "tin");
    private static final ResourceId ZN = ResourceId.of("naturalexceptions", "zinc");

    @BeforeAll
    static void initBuiltin() {
        BuiltinContent.register();
    }

    @Test
    void generatedMaterialPersistsThroughItemStackNbtAndReconstructsIdentically() {
        // Step 1: Define ternary alloy (Cu 70%, Sn 20%, Zn 10%) with phase and microstructure
        CanonicalComposition composition = CanonicalComposition.builder()
            .add(CU, 70)
            .add(SN, 20)
            .add(ZN, 10)
            .build();
        CanonicalMaterialKey stateKey = CanonicalMaterialKey
            .of(composition, MaterialPhase.SOLID, Microstructure.ANNEALED, ConditioningHistory.COLD_WORKED);
        MaterialRef originalRef = MaterialRef.generatedFrom(stateKey);

        // Step 2: Register in catalogue 1 and resolve properties
        MaterialCatalogue catalogue1 = new MaterialCatalogue();
        catalogue1.register(BuiltinMaterials.COPPER);
        GeneratedMaterial genMat1 = catalogue1.getOrRegisterGenerated(stateKey);
        assertEquals(originalRef, genMat1.ref());

        MaterialPropertyResolver resolver1 = new MaterialPropertyResolver();
        ResolvedProperties resolvedProps1 = resolver1.resolve(genMat1);
        assertNotNull(resolvedProps1);

        // Step 3: Package into Minecraft ItemStack carrier
        ItemStack stack = ItemMaterialSample.createStack(originalRef, stateKey);
        assertNotNull(stack.getTagCompound());

        // Step 4: Simulate world save/reload by deep-copying the NBT compound
        NBTTagCompound serializedNbt = (NBTTagCompound) stack.getTagCompound()
            .copy();
        ItemStack reloadedStack = new ItemStack(new ItemMaterialSample());
        reloadedStack.setTagCompound(serializedNbt);

        // Discard catalogue1 and resolver1 (clear in-memory state)
        catalogue1 = null;
        resolver1 = null;

        // Step 5: Read from reloaded stack
        MaterialRef reloadedRef = ItemMaterialSample.getMaterialRef(reloadedStack);
        CanonicalMaterialKey reloadedKey = ItemMaterialSample.getStateKey(reloadedStack);
        CanonicalComposition reloadedComp = ItemMaterialSample.getComposition(reloadedStack);

        assertNotNull(reloadedRef);
        assertNotNull(reloadedKey);
        assertNotNull(reloadedComp);
        assertEquals(originalRef, reloadedRef);
        assertEquals(stateKey, reloadedKey);
        assertEquals(composition, reloadedComp);
        assertEquals(MaterialPhase.SOLID, reloadedKey.phase());
        assertEquals(Microstructure.ANNEALED, reloadedKey.microstructure());
        assertEquals(ConditioningHistory.COLD_WORKED, reloadedKey.conditioning());

        // Step 6: Reconstruct in a fresh catalogue instance
        MaterialCatalogue catalogue2 = new MaterialCatalogue();
        catalogue2.register(BuiltinMaterials.COPPER);
        GeneratedMaterial genMat2 = catalogue2.getOrRegisterGenerated(reloadedKey);

        assertEquals(originalRef, genMat2.ref());
        assertEquals(genMat1.id(), genMat2.id());
        assertEquals(composition, genMat2.composition());

        // Step 7: Resolve in a fresh resolver instance and verify identical properties
        MaterialPropertyResolver resolver2 = new MaterialPropertyResolver();
        ResolvedProperties resolvedProps2 = resolver2.resolve(genMat2);

        assertEquals(resolvedProps1.properties(), resolvedProps2.properties());
        assertEquals(
            resolvedProps1.trace()
                .appliedLayers(),
            resolvedProps2.trace()
                .appliedLayers());
        assertEquals(
            resolvedProps1.properties()
                .density(),
            resolvedProps2.properties()
                .density(),
            1e-9);
        assertEquals(
            resolvedProps1.properties()
                .hardness(),
            resolvedProps2.properties()
                .hardness(),
            1e-9);
        assertEquals(
            resolvedProps1.properties()
                .conductivity(),
            resolvedProps2.properties()
                .conductivity(),
            1e-9);
    }

    @Test
    void authoredMaterialPersistsThroughItemStackNbt() {
        MaterialRef authoredRef = MaterialRef.authored(BuiltinMaterials.COPPER.id());

        ItemStack stack = ItemMaterialSample.createStack(authoredRef);
        NBTTagCompound savedNbt = (NBTTagCompound) stack.getTagCompound()
            .copy();

        ItemStack reloadedStack = new ItemStack(new ItemMaterialSample());
        reloadedStack.setTagCompound(savedNbt);

        MaterialRef reloadedRef = ItemMaterialSample.getMaterialRef(reloadedStack);
        assertEquals(authoredRef, reloadedRef);
        assertNull(ItemMaterialSample.getComposition(reloadedStack));

        MaterialCatalogue catalogue = new MaterialCatalogue();
        catalogue.register(BuiltinMaterials.COPPER);
        MaterialDefinition def = catalogue.getByRef(reloadedRef);
        assertNotNull(def);
        assertEquals(BuiltinMaterials.COPPER.id(), def.id());

        MaterialPropertyResolver resolver = new MaterialPropertyResolver();
        ResolvedProperties resolved = resolver.resolve(def);
        assertEquals(
            8.96,
            resolved.properties()
                .density(),
            1e-3);
        assertEquals(
            3.0,
            resolved.properties()
                .hardness(),
            1e-3);
        assertEquals(
            100.0,
            resolved.properties()
                .conductivity(),
            1e-3);
    }

    @Test
    void carrierSurvivesMinecraftContainerInventorySaveAndLoad() {
        // Build sample
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 85)
            .add(SN, 15)
            .build();
        CanonicalMaterialKey key = CanonicalMaterialKey
            .of(comp, MaterialPhase.SOLID, Microstructure.AS_CAST, ConditioningHistory.NONE);
        MaterialRef ref = MaterialRef.generatedFrom(key);
        ItemStack stack = ItemMaterialSample.createStack(ref, key);

        // Simulate Minecraft container / chest inventory save:
        // Container serializes to NBTTagList of slot compounds
        NBTTagCompound containerNbt = new NBTTagCompound();
        NBTTagList itemsList = new NBTTagList();

        NBTTagCompound slot0Tag = new NBTTagCompound();
        slot0Tag.setByte("Slot", (byte) 0);
        // stack writes its state (id, Count, Damage, tag)
        slot0Tag.setShort("id", (short) 1000); // simulated host numeric id
        slot0Tag.setByte("Count", (byte) 1);
        slot0Tag.setShort("Damage", (short) 0);
        if (stack.getTagCompound() != null) {
            slot0Tag.setTag(
                "tag",
                stack.getTagCompound()
                    .copy());
        }
        itemsList.appendTag(slot0Tag);
        containerNbt.setTag("Items", itemsList);

        // Deep copy simulating disk write / read
        NBTTagCompound diskNbt = (NBTTagCompound) containerNbt.copy();

        // Simulate container restore
        NBTTagList reloadedItems = diskNbt.getTagList("Items", 10);
        assertEquals(1, reloadedItems.tagCount());
        NBTTagCompound restoredSlot0 = reloadedItems.getCompoundTagAt(0);

        ItemStack restoredStack = new ItemStack(new ItemMaterialSample());
        if (restoredSlot0.hasKey("tag", 10)) {
            restoredStack.setTagCompound(restoredSlot0.getCompoundTag("tag"));
        }

        MaterialRef restoredRef = ItemMaterialSample.getMaterialRef(restoredStack);
        CanonicalMaterialKey restoredKey = ItemMaterialSample.getStateKey(restoredStack);

        assertNotNull(restoredRef);
        assertNotNull(restoredKey);
        assertEquals(ref, restoredRef);
        assertEquals(key, restoredKey);
        assertEquals(
            8_500,
            restoredKey.composition()
                .parts(CU));
        assertEquals(
            1_500,
            restoredKey.composition()
                .parts(SN));
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void presentationAndTooltipsReflectCarrierState() {
        ItemMaterialSample item = new ItemMaterialSample();

        // 1. Authored carrier
        MaterialRef authoredRef = MaterialRef.authored(CU);
        ItemStack authoredStack = ItemMaterialSample.createStack(authoredRef);
        assertEquals("Material Sample (Copper)", item.getItemStackDisplayName(authoredStack));
        List<String> tooltip = new ArrayList<>();
        item.addInformation(authoredStack, null, (List) tooltip, false);
        assertTrue(tooltip.contains("\u00a78mat:authored:naturalexceptions:copper"));

        // 2. Generated carrier with state key
        CanonicalComposition comp = CanonicalComposition.builder()
            .add(CU, 80)
            .add(SN, 20)
            .build();
        CanonicalMaterialKey stateKey = CanonicalMaterialKey
            .of(comp, MaterialPhase.SOLID, Microstructure.ANNEALED, ConditioningHistory.COLD_WORKED);
        MaterialRef genRef = MaterialRef.generatedFrom(stateKey);
        ItemStack genStack = ItemMaterialSample.createStack(genRef, stateKey);
        assertTrue(
            item.getItemStackDisplayName(genStack)
                .startsWith("Material Sample (Generated "));
        List<String> genTooltip = new ArrayList<>();
        item.addInformation(genStack, null, (List) genTooltip, false);
        assertTrue(genTooltip.contains("\u00a78" + genRef.asString()));
        assertTrue(genTooltip.contains("\u00a77Phase: solid"));
        assertTrue(genTooltip.contains("\u00a77Microstructure: annealed"));
        assertTrue(genTooltip.contains("\u00a77Conditioning: cold_worked"));
        assertTrue(genTooltip.contains("\u00a77Composition:"));

        // 3. Empty carrier
        ItemStack emptyStack = new ItemStack(item);
        assertEquals("Material Sample (Empty)", item.getItemStackDisplayName(emptyStack));
        List<String> emptyTooltip = new ArrayList<>();
        item.addInformation(emptyStack, null, (List) emptyTooltip, false);
        assertTrue(emptyTooltip.contains("\u00a77Empty carrier"));

        // 4. Corrupted carrier
        ItemStack corruptStack = new ItemStack(item);
        NBTTagCompound corruptNbt = new NBTTagCompound();
        corruptNbt.setInteger(MaterialCarrierNbtCodec.KEY_FORMAT, 2);
        corruptNbt.setString(MaterialCarrierNbtCodec.KEY_MATERIAL_REF, "bad:ref");
        corruptStack.setTagCompound(corruptNbt);
        assertEquals("\u00a7cMaterial Sample (Corrupted)", item.getItemStackDisplayName(corruptStack));
        assertNull(ItemMaterialSample.getMaterialRef(corruptStack));

        // 5. Unresolved authored carrier fails safely and visibly
        MaterialRef unresolvedRef = MaterialRef.authored(ResourceId.of("naturalexceptions", "mythic_alloy_999"));
        ItemStack unresolvedStack = ItemMaterialSample.createStack(unresolvedRef);
        assertEquals(
            "\u00a7cMaterial Sample (Unresolved Authored: mythic_alloy_999)",
            item.getItemStackDisplayName(unresolvedStack));
        List<String> unresolvedTooltip = new ArrayList<>();
        item.addInformation(unresolvedStack, null, (List) unresolvedTooltip, false);
        assertTrue(
            unresolvedTooltip.contains("\u00a7cUnresolved authored material: naturalexceptions:mythic_alloy_999"));
    }

    @Test
    void carrierWithActiveThermalStateSurvivesContainerSaveAndLoad() {
        MaterialRef ref = MaterialRef.authored(CU);
        naturalexceptions.core.property.MaterialProperties props = BuiltinMaterials.COPPER_PROPERTIES;
        naturalexceptions.core.thermal.ActiveThermalState thermal = naturalexceptions.core.thermal.ActiveThermalState
            .atTemperature(850.0, 0.0, 1.5, props);

        ItemStack hotStack = ItemMaterialSample.createStack(ref, (CanonicalMaterialKey) null, thermal);
        assertTrue(MaterialCarrierNbtCodec.hasActiveThermalState(hotStack.getTagCompound()));

        // Simulate chest slot NBT save
        NBTTagList chestSlots = new NBTTagList();
        NBTTagCompound slotTag = new NBTTagCompound();
        slotTag.setByte("Slot", (byte) 0);
        slotTag.setShort("id", (short) 1000);
        slotTag.setByte("Count", (byte) 1);
        slotTag.setShort("Damage", (short) 0);
        if (hotStack.getTagCompound() != null) {
            slotTag.setTag(
                "tag",
                hotStack.getTagCompound()
                    .copy());
        }
        chestSlots.appendTag(slotTag);

        NBTTagCompound chestCompound = new NBTTagCompound();
        chestCompound.setTag("Items", chestSlots);

        // Simulate reload into fresh context
        NBTTagList loadedSlots = chestCompound.getTagList("Items", 10);
        assertEquals(1, loadedSlots.tagCount());
        NBTTagCompound loadedSlot0 = loadedSlots.getCompoundTagAt(0);

        ItemStack restoredStack = new ItemStack(new ItemMaterialSample());
        if (loadedSlot0.hasKey("tag", 10)) {
            restoredStack.setTagCompound(loadedSlot0.getCompoundTag("tag"));
        }

        assertNotNull(restoredStack);
        assertEquals(ref, ItemMaterialSample.getMaterialRef(restoredStack));
        assertTrue(MaterialCarrierNbtCodec.hasActiveThermalState(restoredStack.getTagCompound()));

        naturalexceptions.core.thermal.ActiveThermalState restoredThermal = ItemMaterialSample
            .getThermalState(restoredStack, props);
        assertNotNull(restoredThermal);
        assertEquals(thermal.energyJoules(), restoredThermal.energyJoules(), 0.001);
        assertEquals(thermal.massKg(), restoredThermal.massKg(), 0.001);
        assertEquals(850.0, restoredThermal.temperatureKelvin(), 0.01);
    }
}
