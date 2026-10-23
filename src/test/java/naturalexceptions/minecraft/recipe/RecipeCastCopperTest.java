package naturalexceptions.minecraft.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.fabrication.ComponentGeometry;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialRef;
import naturalexceptions.core.metallurgy.AggregateLayer;
import naturalexceptions.core.metallurgy.AggregatePhaseType;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.property.MaterialProperties;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.minecraft.item.ItemCastingMold;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/** The copper recipes take the ingots a player casts, not only the ingots found in loot. */
final class RecipeCastCopperTest {

    @BeforeAll
    static void init() {
        try {
            net.minecraft.init.Bootstrap.func_151354_b();
        } catch (Throwable ignored) {}
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    /** An ingot cast in an ingot mold from molten metal, set, taken out, at the given temperature. */
    private static ItemStack castIngot(MaterialRef ref, naturalexceptions.core.composition.CanonicalComposition comp,
        MaterialProperties props, double kelvin) {
        ItemStack mold = ItemCastingMold.createStack(GeometryId.INGOT);
        MaterialQuantity mass = ComponentGeometry.forId(GeometryId.INGOT)
            .requiredMass(props.density());
        ItemCastingMold.fillMold(
            mold,
            new AggregateLayer(
                ref,
                CanonicalMaterialKey.of(comp)
                    .asMolten(),
                comp,
                mass,
                ActiveThermalState.atTemperature(1450.0, 1.0, mass.toKilograms(), props),
                AggregatePhaseType.MOLTEN_METAL));
        ItemCastingMold
            .saveThermalState(mold, ActiveThermalState.atTemperature(kelvin, 1.0, mass.toKilograms(), props));
        assertTrue(ItemCastingMold.isSolidified(mold), "the casting has set");
        ItemStack ingot = ItemCastingMold.extractComponent(mold);
        assertNotNull(ingot, "the casting comes out of the mold");
        return ingot;
    }

    private static ItemStack copper(double kelvin) {
        return castIngot(
            BuiltinMaterials.COPPER.ref(),
            BuiltinMaterials.COPPER_COMPOSITION,
            BuiltinMaterials.COPPER_PROPERTIES,
            kelvin);
    }

    private static final Container NONE = new Container() {

        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return false;
        }
    };

    /** The Shardcaster's grid: " LL", " CL", "C ", with the given stack in both copper places. */
    private static InventoryCrafting shardcasterGrid(ItemStack copper) {
        InventoryCrafting grid = new InventoryCrafting(NONE, 3, 3);
        grid.setInventorySlotContents(1, new ItemStack(NEItems.stormglassShard));
        grid.setInventorySlotContents(2, new ItemStack(NEItems.stormglassShard));
        grid.setInventorySlotContents(4, copper.copy());
        grid.setInventorySlotContents(5, new ItemStack(NEItems.stormglassShard));
        grid.setInventorySlotContents(6, copper.copy());
        return grid;
    }

    private static RecipeCastCopper shardcaster() {
        return new RecipeCastCopper(
            new ItemStack(NEItems.shardcaster),
            " LL",
            " CL",
            "C  ",
            'L',
            NEItems.stormglassShard,
            'C',
            NEItems.copperIngot);
    }

    @Test
    void aCoolCastCopperIngotCountsAsCopper() {
        ItemStack ingot = copper(300.0);
        assertTrue(RecipeCastCopper.isCastCopperIngot(ingot));
        assertTrue(shardcaster().matches(shardcasterGrid(ingot), null), "cast copper makes a Shardcaster");
        assertEquals(
            NEItems.shardcaster,
            shardcaster().getCraftingResult(shardcasterGrid(ingot))
                .getItem());
    }

    @Test
    void aFoundCopperIngotStillWorks() {
        assertTrue(shardcaster().matches(shardcasterGrid(new ItemStack(NEItems.copperIngot)), null));
    }

    @Test
    void aCastingStillHotIsNotTakenYet() {
        ItemStack hot = copper(900.0);
        assertTrue(ItemGenericComponent.getTemperatureKelvin(hot) > RecipeCastCopper.HANDLEABLE_KELVIN);
        assertFalse(RecipeCastCopper.isCastCopperIngot(hot));
        assertFalse(shardcaster().matches(shardcasterGrid(hot), null));
    }

    @Test
    void anotherMetalIsNotCopper() {
        ItemStack iron = castIngot(
            BuiltinMaterials.IRON.ref(),
            BuiltinMaterials.IRON_COMPOSITION,
            BuiltinMaterials.IRON_PROPERTIES,
            300.0);
        assertFalse(RecipeCastCopper.isCastCopperIngot(iron));
        assertFalse(shardcaster().matches(shardcasterGrid(iron), null));
    }
}
