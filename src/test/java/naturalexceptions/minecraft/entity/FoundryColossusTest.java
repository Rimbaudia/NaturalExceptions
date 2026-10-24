package naturalexceptions.minecraft.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * The Foundry Colossus: a miniboss, not a wall: its hot shell turns most of a blow and a dousing opens it to them all;
 * its pour runs a straight line out from its feet; it always gives up its crucible.
 */
final class FoundryColossusTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    @Test
    void aMinibossLighterThanTheCastellan() {
        assertTrue(ColossusRules.MAX_HEALTH < WardenRules.MAX_HEALTH);
        assertTrue(ColossusRules.through(false) < 0.5F, "hot, its shell turns most of a blow");
        assertEquals(1.0F, ColossusRules.through(true), "doused, every blow tells");
        assertTrue(ColossusRules.QUENCH_TICKS >= 60, "long enough to land a few blows");
        assertTrue(ColossusRules.SLAM_WINDUP >= 15 && ColossusRules.POUR_WINDUP >= 20, "every blow shown first");
    }

    @Test
    void thePourRunsStraightOutFromItsFeet() {
        int[][] line = ColossusRules.pourLine(0.5, 0.5, 0.5, 20.5);
        assertEquals(ColossusRules.POUR_LENGTH, line.length);
        for (int i = 0; i < line.length; i++) {
            assertEquals(0, line[i][0], "straight along z");
            assertEquals(2 + i, line[i][1], "one block on each step, starting a block out");
        }
        int[][] diagonal = ColossusRules.pourLine(0.5, 0.5, 10.5, 10.5);
        assertTrue(diagonal[diagonal.length - 1][0] > 3 && diagonal[diagonal.length - 1][1] > 3, "towards the target");
    }

    @Test
    void itAlwaysGivesUpItsCrucible() {
        Item crucible = Item.getItemFromBlock(NEBlocks.colossusCrucible);
        Random random = new Random(1);
        for (int i = 0; i < 50; i++) {
            List<ItemStack> drops = EntityFoundryColossus.drops(random, 0);
            assertEquals(
                crucible,
                drops.get(0)
                    .getItem());
        }
    }
}
