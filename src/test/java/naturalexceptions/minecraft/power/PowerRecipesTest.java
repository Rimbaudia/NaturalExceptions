package naturalexceptions.minecraft.power;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.core.power.Cable;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/** Every kind of cable and the first machines can be made; and an arc lamp is rated for a crank dynamo's volts. */
final class PowerRecipesTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    private static boolean craftable(Item item, int meta) {
        for (Object entry : CraftingManager.getInstance()
            .getRecipeList()) {
            ItemStack out = ((IRecipe) entry).getRecipeOutput();
            if (out != null && out.getItem() == item && (meta < 0 || out.getItemDamage() == meta)) return true;
        }
        return false;
    }

    @Test
    void everyCableAndMachineCanBeMade() {
        Item cable = Item.getItemFromBlock(NEBlocks.cable);
        for (Cable kind : Cable.values()) assertTrue(craftable(cable, kind.ordinal()), kind + " can be made");
        assertTrue(craftable(Item.getItemFromBlock(NEBlocks.knifeSwitch), -1));
        assertTrue(craftable(Item.getItemFromBlock(NEBlocks.stormglassRelay), -1));
        assertTrue(craftable(Item.getItemFromBlock(NEBlocks.crankDynamo), -1));
        assertTrue(craftable(Item.getItemFromBlock(NEBlocks.arcLamp), -1));
        assertTrue(craftable(NEItems.multimeter, -1));
        assertTrue(craftable(Item.getItemFromBlock(NEBlocks.windmill), -1));
        assertTrue(craftable(Item.getItemFromBlock(NEBlocks.heartSocket), -1));
        assertTrue(craftable(Item.getItemFromBlock(NEBlocks.stormCell), -1));
        assertTrue(craftable(Item.getItemFromBlock(NEBlocks.lightningSpire), -1));
    }

    @Test
    void aLampIsRatedForADynamo() {
        assertTrue(TileArcLamp.RATED_VOLTS == TileCrankDynamo.EMF, "a dynamo turned flat out lights a lamp fully");
        assertTrue(TileArcLamp.brightness(0.0) == 0 && TileArcLamp.brightness(TileArcLamp.RATED_WATTS) == 3);
        double shortCircuitAmps = TileCrankDynamo.EMF / TileCrankDynamo.INTERNAL_OHMS;
        assertTrue(shortCircuitAmps > Cable.LIGHT_INSULATED.ratedAmps(), "a dead short can overheat light wire");
    }
}
