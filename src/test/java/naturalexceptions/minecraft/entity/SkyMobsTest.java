package naturalexceptions.minecraft.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.minecraft.item.ItemCitadelHalberd;
import naturalexceptions.minecraft.recipe.SkystoneRecipes;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/** The sky's creatures behave as they are meant to: the husk's leap, the archer's arrows, the guard's shield. */
final class SkyMobsTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
        SkystoneRecipes.register();
    }

    @Test
    void aGaleHuskLeapsFromAFewBlocksOffOnly() {
        assertFalse(EntityGaleHusk.inLeap(2.5), "too close: it just strikes");
        assertTrue(EntityGaleHusk.inLeap(3.0));
        assertTrue(EntityGaleHusk.inLeap(8.0));
        assertFalse(EntityGaleHusk.inLeap(9.0), "too far");
        boolean feathers = false;
        Random random = new Random(1);
        for (int i = 0; i < 50; i++) for (ItemStack s : EntityGaleHusk.drops(random, 0)) feathers |= s.getItem() == Items.feather;
        assertTrue(feathers, "feathers in its wrappings");
    }

    @Test
    void aStormboneArchersArrowsFlyFastAndShock() {
        assertTrue(EntityStormArrow.SPEED > 1.6F, "faster than a skeleton's: " + EntityStormArrow.SPEED);
        assertTrue(EntityStormArrow.SPREAD < 6.0F, "straighter");
        assertTrue(EntityStormArrow.SHOCK_TICKS >= 20 && EntityStormArrow.SHOCK_AMPLIFIER >= 1, "a real slow");
        assertTrue(EntityStormboneArcher.RANGE > 15.0F, "shoots from further than a skeleton");
        int shards = 0;
        Random random = new Random(2);
        for (int i = 0; i < 400; i++) for (ItemStack s : EntityStormboneArcher.drops(random, 0))
            if (s.getItem() == NEItems.stormglassShard) shards++;
        assertTrue(shards > 10 && shards < 160, "now and then a shard: " + shards);
    }

    @Test
    void aCitadelGuardsShieldTurnsWhatComesFromTheFront() {
        // Facing south (+z): yaw 0.
        assertEquals(0.0F, EntityCitadelGuard.through(0.0F, 0, 0, 0, 5, true, true), "an arrow from in front");
        assertEquals(
            EntityCitadelGuard.MELEE_THROUGH,
            EntityCitadelGuard.through(0.0F, 0, 0, 0.5, 2, false, true),
            "a blow from in front, mostly");
        assertEquals(1.0F, EntityCitadelGuard.through(0.0F, 0, 0, 5, 0, false, true), "from his side, all of it");
        assertEquals(1.0F, EntityCitadelGuard.through(0.0F, 0, 0, 0, -5, true, true), "from behind, all of it");
        assertEquals(1.0F, EntityCitadelGuard.through(0.0F, 0, 0, 0, 5, true, false), "shield down, all of it");
        assertTrue(EntityCitadelGuard.REACH > 3.0, "a halberd's reach");
    }

    @Test
    void aCloudweaverSpitsFromAfarAndItsSilkBinds() {
        assertFalse(EntityCloudweaver.inSpit(3.0), "close in it bites");
        assertTrue(EntityCloudweaver.inSpit(4.0));
        assertTrue(EntityCloudweaver.inSpit(14.0));
        assertFalse(EntityCloudweaver.inSpit(15.0));
        assertTrue(EntityCloudweaver.SPIT_COOLDOWN >= 40, "not a hail of it");
        assertTrue(EntityCloudsilk.BIND_TICKS >= 20 && EntityCloudsilk.BIND_AMPLIFIER >= 3, "it pins you");
    }

    @Test
    void aCloudweaverLeavesItsSilkAndNowAndThenMore() {
        Random random = new Random(3);
        int clouds = 0, spinners = 0;
        for (int i = 0; i < 2000; i++) {
            boolean silk = false;
            for (ItemStack st : EntityCloudweaver.drops(random, 0)) {
                silk |= st.getItem() == NEItems.cloudsilk && st.stackSize >= 1;
                if (st.getItem() == net.minecraft.item.Item.getItemFromBlock(NEBlocks.cloud)) clouds++;
                if (st.getItem() == NEItems.silkspinner) spinners++;
            }
            assertTrue(silk, "Cloudsilk every time");
        }
        assertTrue(clouds > 100 && clouds < 320, "a Cloud now and then: " + clouds);
        assertTrue(spinners > 10 && spinners < 90, "its spinneret once in a long while: " + spinners);
    }

    @Test
    void cloudsilkRopeUnrollsAsFarAsTheAirAndTheRopeGo() {
        assertEquals(5, naturalexceptions.minecraft.block.BlockCloudsilkRope.unroll(64, k -> k < 5), "to the ground");
        assertEquals(3, naturalexceptions.minecraft.block.BlockCloudsilkRope.unroll(3, k -> true), "as far as it goes");
        assertEquals(
            naturalexceptions.minecraft.block.BlockCloudsilkRope.MAX_DROP,
            naturalexceptions.minecraft.block.BlockCloudsilkRope.unroll(64, k -> true),
            "no further than it can hang");
    }

    @Test
    void cloudsilkMakesRopeAndDressings() {
        boolean rope = false;
        for (Object entry : CraftingManager.getInstance()
            .getRecipeList()) {
            ItemStack out = ((IRecipe) entry).getRecipeOutput();
            rope |= out != null && out.getItem() == net.minecraft.item.Item.getItemFromBlock(NEBlocks.cloudsilkRope)
                && out.stackSize == 6;
        }
        assertTrue(rope, "three silk twist into six rope");
        ItemStack dressings = null;
        for (naturalexceptions.minecraft.furniture.CuttingRecipes.Recipe r : naturalexceptions.minecraft.furniture.CuttingRecipes
            .forInput(naturalexceptions.minecraft.furniture.CuttingRecipes.Kind.LOOM, new ItemStack(NEItems.cloudsilk)))
            if (r.output()
                .getItem() == NEItems.bandage) dressings = r.output();
        assertTrue(dressings != null && dressings.stackSize == 4, "four dressings a silk at the loom");
    }

    @Test
    void aCitadelHalberdHitsHarderThanASwordAndCanBeMade() {
        assertTrue(ItemCitadelHalberd.DAMAGE > 6.0);
        boolean found = false;
        for (Object entry : CraftingManager.getInstance()
            .getRecipeList()) {
            ItemStack out = ((IRecipe) entry).getRecipeOutput();
            found |= out != null && out.getItem() == NEItems.citadelHalberd;
        }
        assertTrue(found, "it can be made");
    }
}
