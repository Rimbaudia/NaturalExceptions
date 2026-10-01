package naturalexceptions.minecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import naturalexceptions.core.worldgen.SkyIslands;

/** Nothing spawns of itself on a sky island; the ground below and the open sky are as they were. */
final class SkyIslandSpawnsTest {

    @Test
    void anIslandForbidsNaturalSpawnsAndTheGroundBelowDoesNot() {
        SkyIslands field = new SkyIslands(1234567L);
        SkyIslands.Island island = null;
        for (long cx = 0; cx < 4 && island == null; cx++)
            for (long cz = 0; cz < 4 && island == null; cz++) island = field.atCell(SkyIslands.Tier.GRAND, cx, cz);
        int x = island.centreX(), z = island.centreZ(), top = island.centreY() + 1;
        SkyIslandSpawns spawns = new SkyIslandSpawns();
        assertTrue(spawns.forbids(field, x, top + 1, z), "on its grass");
        assertFalse(spawns.forbids(field, x, 70, z), "on the ground far below");
        // Open sky away from any island: natural spawns go on there as ever.
        int ox = x + 3000;
        while (SkyIslandSpawns.underside(field, ox, z) != Integer.MAX_VALUE) ox += 37;
        assertFalse(spawns.forbids(field, ox, top + 1, z), "open sky elsewhere");
    }

    @Test
    void theDungeonsSpawnTheSkysOwnDeadNotVanillas() {
        assertEquals("naturalexceptions.gale_husk", LandmarkMaterializer.entityName("Zombie"));
        assertEquals("naturalexceptions.stormbone_archer", LandmarkMaterializer.entityName("Skeleton"));
        assertEquals("naturalexceptions.citadel_guard", LandmarkMaterializer.entityName("CitadelGuard"));
        assertEquals("naturalexceptions.cloudweaver", LandmarkMaterializer.entityName("Spider"));
        assertEquals("naturalexceptions.cloudweaver", LandmarkMaterializer.entityName("CaveSpider"));
    }
}
