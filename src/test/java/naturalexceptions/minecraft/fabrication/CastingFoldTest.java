package naturalexceptions.minecraft.fabrication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.material.BuiltinMaterials;
import naturalexceptions.core.composition.CanonicalComposition;
import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.fabrication.ManufacturingState;
import naturalexceptions.core.material.CanonicalMaterialKey;
import naturalexceptions.core.material.MaterialDefinition;
import naturalexceptions.core.metallurgy.MaterialQuantity;
import naturalexceptions.core.thermal.ActiveThermalState;
import naturalexceptions.core.thermal.ThermodynamicModel;
import naturalexceptions.minecraft.item.ItemGenericComponent;
import naturalexceptions.minecraft.registry.NEItems;

/** Five castings piled on the anvil fold into one ingot, keeping their mass; hot in one blow, cold in a few. */
final class CastingFoldTest {

    @BeforeAll
    static void setUp() {
        NEItems.ensureBootstrapped();
    }

    private static ItemStack casting(MaterialDefinition metal, double grams, double kelvin) {
        CanonicalComposition comp = metal.standardComposition();
        ComponentInstance part = new ComponentInstance(
            GeometryId.INGOT,
            metal.ref(),
            CanonicalMaterialKey.of(comp),
            MaterialQuantity.ofGrams(grams),
            ManufacturingState.AS_CAST);
        ItemStack stack = ItemGenericComponent.createStack(part);
        ItemGenericComponent.saveThermalState(
            stack,
            new ActiveThermalState(
                ThermodynamicModel.energyForTemperature(kelvin, 0.0, grams / 1000.0, metal.properties()),
                grams / 1000.0,
                metal.properties()));
        return stack;
    }

    private static ItemStack pileOf(MaterialDefinition metal, int castings, double kelvin) {
        ItemStack pile = casting(metal, 220.0, kelvin);
        for (int i = 1; i < castings; i++) {
            ItemStack next = casting(metal, 220.0, kelvin);
            assertTrue(CastingFold.joins(pile, next));
            pile = CastingFold.join(pile, next);
        }
        return pile;
    }

    @Test
    void fiveCopperCastingsFoldIntoACopperIngot() {
        ItemStack pile = pileOf(BuiltinMaterials.COPPER, 5, 293.15);
        assertTrue(CastingFold.ready(pile), "1.1 kg is an ingot's worth");
        assertTrue(!CastingFold.ready(pileOf(BuiltinMaterials.COPPER, 4, 293.15)), "0.88 kg is not");
        ItemStack[] out = CastingFold.fold(pile);
        assertEquals(NEItems.copperIngot, out[0].getItem());
        assertEquals(
            100.0,
            ItemGenericComponent.getComponent(out[1])
                .mass()
                .toKilograms() * 1000.0,
            0.5,
            "what is over an ingot stays on the anvil");
    }

    @Test
    void ironAndGoldFoldIntoTheirOwnIngotsButMetalsDoNotMix() {
        assertEquals(Items.iron_ingot, CastingFold.fold(pileOf(BuiltinMaterials.IRON, 5, 293.15))[0].getItem());
        assertEquals(Items.gold_ingot, CastingFold.fold(pileOf(BuiltinMaterials.GOLD, 5, 293.15))[0].getItem());
        assertTrue(
            !CastingFold.joins(casting(BuiltinMaterials.COPPER, 220, 293.15), casting(BuiltinMaterials.IRON, 220, 293.15)),
            "copper and iron castings are not one pile");
        assertNull(CastingFold.ingotFor(BuiltinMaterials.BRONZE_COMPOSITION), "only the pure metals fold");
    }

    @Test
    void hotItFoldsAtOneBlowColdItTakesMore() {
        assertEquals(1, CastingFold.blows(pileOf(BuiltinMaterials.COPPER, 5, 900.0)));
        assertEquals(CastingFold.COLD_BLOWS, CastingFold.blows(pileOf(BuiltinMaterials.COPPER, 5, 293.15)));
        assertTrue(
            !CastingFold.joins(casting(BuiltinMaterials.COPPER, 220, 1500.0), casting(BuiltinMaterials.COPPER, 220, 293.15)),
            "a molten casting is not laid on a pile");
    }
}
