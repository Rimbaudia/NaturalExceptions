package naturalexceptions.minecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import net.minecraft.entity.EntityList;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import naturalexceptions.content.BuiltinContent;
import naturalexceptions.core.worldgen.RoomWriterAccess;
import naturalexceptions.core.worldgen.dungeon.RoomTemplate;
import naturalexceptions.core.worldgen.dungeon.TemplateLibrary;
import naturalexceptions.minecraft.block.BlockSealedAlcove;
import naturalexceptions.minecraft.entity.EntitySkystoneSentinel;
import naturalexceptions.minecraft.entity.EntityStormwisp;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/** The sky dungeons' creatures: every name a room lists is a real creature, and what they drop. */
final class DungeonMobsTest {

    @BeforeAll
    static void init() {
        BuiltinContent.register();
        NEItems.register();
        NEBlocks.register();
    }

    @Test
    void everyListedCreatureExists() {
        int ours = 0;
        for (RoomTemplate t : TemplateLibrary.all()) for (String mob : t.mobs) {
            String name = mob.endsWith("+") ? mob.substring(0, mob.length() - 1) : mob;
            String entity = LandmarkMaterializer.entityName(name);
            if (entity.startsWith(naturalexceptions.NaturalExceptions.MOD_ID + ".")) ours++;
            else assertTrue(
                EntityList.stringToClassMapping.containsKey(entity),
                t.name + ": " + mob + " is a creature");
        }
        assertTrue(ours >= 10, "the sky's own creatures live in its rooms: " + ours);
    }

    @Test
    void alcovesHoldTheNewCreatures() {
        assertEquals(BlockSealedAlcove.STORMWISP, BlockSealedAlcove.kind(RoomWriterAccess.alcoveMeta("Stormwisp")));
        assertEquals(BlockSealedAlcove.SENTINEL, BlockSealedAlcove.kind(RoomWriterAccess.alcoveMeta("Sentinel")));
        assertEquals(BlockSealedAlcove.SKELETON, BlockSealedAlcove.kind(RoomWriterAccess.alcoveMeta("Skeleton+")));
        assertTrue((RoomWriterAccess.alcoveMeta("Skeleton+") & BlockSealedAlcove.ARMED) != 0, "armed stays armed");
        assertEquals(BlockSealedAlcove.CAVE_SPIDER, BlockSealedAlcove.kind(RoomWriterAccess.alcoveMeta("CaveSpider")));
    }

    @Test
    void theyDropWhatTheyAreMadeOfAndSometimesAKey() {
        Random random = new Random(5);
        int wispKeys = 0, sentinelKeys = 0, shards = 0, n = 20000;
        for (int i = 0; i < n; i++) {
            for (ItemStack s : EntityStormwisp.drops(random, 0)) {
                if (s.getItem() == NEItems.skyKey) wispKeys++;
                if (s.getItem() == NEItems.stormglassShard) shards += s.stackSize;
            }
            for (ItemStack s : EntitySkystoneSentinel.drops(random, 0))
                if (s.getItem() == NEItems.skyKey) sentinelKeys++;
        }
        assertTrue(shards >= n && shards <= 2 * n, "one or two shards a wisp");
        assertTrue(Math.abs(wispKeys / (double) n - EntityStormwisp.KEY_CHANCE) < 0.006, "wisp keys: " + wispKeys);
        assertTrue(
            Math.abs(sentinelKeys / (double) n - EntitySkystoneSentinel.KEY_CHANCE) < 0.008,
            "sentinel keys: " + sentinelKeys);
    }
}
