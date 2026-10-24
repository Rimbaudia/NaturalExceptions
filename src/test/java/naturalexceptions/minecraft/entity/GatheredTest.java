package naturalexceptions.minecraft.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.minecraft.gear.Treasure;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * The Gathered: blows pass through it unless every candle burns; it tears at half health and puts out more candles
 * then; laid to rest it leaves the Mourner's Veil and one other relic, with the wind it held.
 */
final class GatheredTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    @Test
    void theCandlesAreTheWayToStrikeIt() {
        assertTrue(GatheredRules.through(false) < 0.25F, "standing, blows mostly pass through the dead");
        assertEquals(1.0F, GatheredRules.through(true), "kneeling before the candles, every blow tells");
        assertFalse(GatheredRules.kneels(4, 3), "not while a candle is out");
        assertTrue(GatheredRules.kneels(4, 4));
        assertFalse(GatheredRules.kneels(0, 0), "no candles, no kneeling");
        assertTrue(
            GatheredRules.KNEEL_TICKS >= 60 && GatheredRules.KNEEL_TICKS <= 140,
            "a few seconds, not a free kill");
        assertFalse(GatheredRules.torn(GatheredRules.MAX_HEALTH));
        assertTrue(GatheredRules.torn(GatheredRules.MAX_HEALTH / 2.0F));
        assertEquals(1, GatheredRules.snuffs(false));
        assertEquals(2, GatheredRules.snuffs(true), "torn, it puts out more");
        assertTrue(GatheredRules.WAIL_WINDUP >= 20 && GatheredRules.GRASP_WINDUP >= 15, "every blow shown first");
    }

    @Test
    void itLeavesTheVeilAndOneMoreRelic() {
        Item veil = NEItems.TREASURES.get(Treasure.MOURNERS_VEIL);
        Set<Item> others = new HashSet<>();
        others.add(NEItems.bellOfRest);
        others.add(NEItems.reliquaryLantern);
        others.add(NEItems.TREASURES.get(Treasure.GATHERED_HEART));
        Set<Item> seen = new HashSet<>();
        Random random = new Random(9);
        boolean wind = false;
        for (int i = 0; i < 200; i++) {
            List<ItemStack> drops = EntityGathered.drops(random);
            assertEquals(
                veil,
                drops.get(0)
                    .getItem(),
                "the veil, always");
            int relics = 0;
            for (ItemStack stack : drops) {
                if (others.contains(stack.getItem())) {
                    relics++;
                    seen.add(stack.getItem());
                }
                if (stack.getItem() == NEItems.galeCore) wind = true;
            }
            assertEquals(1, relics, "and one more of its relics");
        }
        assertEquals(others, seen, "each of them turns up");
        assertTrue(wind, "and the wind it held: Gale Cores");
        assertEquals(Treasure.Source.GATHERED, Treasure.MOURNERS_VEIL.source());
        assertFalse(Treasure.MOURNERS_VEIL.crafted());
    }
}
