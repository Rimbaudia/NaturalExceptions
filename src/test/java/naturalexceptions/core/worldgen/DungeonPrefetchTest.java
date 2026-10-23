package naturalexceptions.core.worldgen;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * A grand island's dungeon worked out ahead on the layout thread: ready by itself, and the same dungeon whoever asks,
 * without being worked out twice.
 */
final class DungeonPrefetchTest {

    /** A grand island with a dungeon, from a seed no other test uses (so the cache starts without it). */
    private static Object[] island(long seed) {
        SkyIslands field = new SkyIslands(seed);
        for (long cx = 0; cx < 6; cx++) for (long cz = 0; cz < 6; cz++) {
            SkyIslands.Island island = field.atCell(SkyIslands.Tier.GRAND, cx, cz);
            if (island != null && field.dungeonKind(island) != null) return new Object[] { field, island };
        }
        throw new IllegalStateException("no dungeon island for seed " + seed);
    }

    @Test
    void aPrefetchedDungeonBecomesReadyByItself() throws InterruptedException {
        Object[] found = island(918273L);
        SkyIslands field = (SkyIslands) found[0];
        SkyIslands.Island island = (SkyIslands.Island) found[1];
        SkyDungeon.prefetch(field, island);
        SkyDungeon ready = null;
        for (int i = 0; i < 600 && ready == null; i++) {
            ready = SkyDungeon.ifReady(field, island);
            if (ready == null) Thread.sleep(50);
        }
        assertNotNull(ready, "the layout thread finished it within half a minute");
        assertSame(ready, SkyDungeon.of(field, island), "and from then on it is the same dungeon");
    }

    @Test
    void askingWhileItIsBeingWorkedOutWaitsForThatOne() {
        Object[] found = island(564738L);
        SkyIslands field = (SkyIslands) found[0];
        SkyIslands.Island island = (SkyIslands.Island) found[1];
        SkyDungeon.prefetch(field, island);
        SkyDungeon asked = SkyDungeon.of(field, island); // waits for the prefetch rather than building again
        assertNotNull(asked);
        assertSame(asked, SkyDungeon.ifReady(field, island));
        assertTrue(
            asked.pieces()
                .size() > 1000,
            "a whole dungeon");
    }
}
