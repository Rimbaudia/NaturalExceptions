package naturalexceptions.core.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

final class DeepLayersTest {

    private static final long SEED = 42L;

    private static Map<DeepLayers.Part, Integer> census(DeepLayers field, DeepLayers.Pocket pocket) {
        Map<DeepLayers.Part, Integer> parts = new EnumMap<>(DeepLayers.Part.class);
        int rx = (int) (pocket.rx() * 1.6) + 1, rz = (int) (pocket.rz() * 1.6) + 1;
        for (int x = pocket.x() - rx; x <= pocket.x() + rx; x++)
            for (int z = pocket.z() - rz; z <= pocket.z() + rz; z++)
                for (int y = pocket.minY(); y <= pocket.maxY(); y++)
                    parts.merge(field.partAt(pocket, x, y, z), 1, Integer::sum);
        return parts;
    }

    @Test
    void grottosSitAtTheirDepthWithOreWallsAndGlowingFloors() {
        DeepLayers field = new DeepLayers(SEED);
        int grottos = 0;
        for (long cx = 0; cx < 12; cx++) for (long cz = 0; cz < 12; cz++) {
            DeepLayers.Pocket grotto = field.grottoAtCell(cx, cz);
            if (grotto == null) continue;
            grottos++;
            assertTrue(grotto.minY() >= 5 && grotto.maxY() <= 40, "stays in the grotto band");
            Map<DeepLayers.Part, Integer> parts = census(field, grotto);
            assertTrue(parts.getOrDefault(DeepLayers.Part.OPEN, 0) > 500, "a real cavern");
            assertTrue(parts.getOrDefault(DeepLayers.Part.ORE, 0) > 20, "Lumenite in the walls");
            assertTrue(parts.getOrDefault(DeepLayers.Part.CLUSTER, 0) > 3, "clusters on the floor");
            assertEquals(0, (int) parts.getOrDefault(DeepLayers.Part.LAVA, 0), "no lava in a grotto");
        }
        assertTrue(grottos >= 25 && grottos <= 80, "zoned, not everywhere: " + grottos);
    }

    @Test
    void magmaPocketsHoldLavaUnderAirInsideMagmaRock() {
        DeepLayers field = new DeepLayers(SEED);
        DeepLayers.Pocket pocket = null;
        for (long cx = 0; pocket == null && cx < 10; cx++)
            for (long cz = 0; pocket == null && cz < 10; cz++) pocket = field.magmaAtCell(cx, cz);
        assertTrue(pocket != null);
        assertEquals(DeepLayers.Part.LAVA, field.partAt(pocket, pocket.x(), pocket.y() - 1, pocket.z()));
        assertEquals(DeepLayers.Part.OPEN, field.partAt(pocket, pocket.x(), pocket.y() + 1, pocket.z()));
        Map<DeepLayers.Part, Integer> parts = census(field, pocket);
        assertTrue(parts.getOrDefault(DeepLayers.Part.MAGMA_ROCK, 0) > 50, "baked rock around it");
        assertEquals(DeepLayers.Part.NONE, field.partAt(pocket, pocket.x(), 4, pocket.z()), "never at bedrock");
    }

    @Test
    void chunksSeeEveryPocketThatReachesThem() {
        DeepLayers field = new DeepLayers(SEED);
        DeepLayers.Pocket grotto = null;
        for (long cx = 0; grotto == null && cx < 10; cx++)
            for (long cz = 0; grotto == null && cz < 10; cz++) grotto = field.grottoAtCell(cx, cz);
        int edgeX = grotto.x() + (int) grotto.rx();
        int cx = Math.floorDiv(edgeX, 16) * 16, cz = Math.floorDiv(grotto.z(), 16) * 16;
        boolean seen = false;
        for (DeepLayers.Pocket near : field.near(cx, cz, cx + 15, cz + 15))
            seen |= near.x() == grotto.x() && near.z() == grotto.z() && !near.isMagma();
        assertTrue(seen);
    }
}
