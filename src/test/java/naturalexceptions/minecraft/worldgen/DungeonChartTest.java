package naturalexceptions.minecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import naturalexceptions.core.worldgen.SkyDungeon;
import naturalexceptions.core.worldgen.SkyIslands;
import naturalexceptions.core.worldgen.dungeon.RoomTemplate.Role;

/** The Surveyor's Chart: it knows where you have been, where you stand, and where the key was kept. */
final class DungeonChartTest {

    private static SkyDungeon dungeon() {
        SkyIslands field = new SkyIslands(7919L);
        for (long cx = 0; cx < 3; cx++) for (long cz = 0; cz < 3; cz++) {
            SkyIslands.Island island = field.atCell(SkyIslands.Tier.GRAND, cx, cz);
            if (island != null) return SkyDungeon.of(field, island);
        }
        throw new IllegalStateException("no dungeon");
    }

    private static int entrance(List<SkyDungeon.Room> rooms) {
        for (int i = 0; i < rooms.size(); i++)
            if (rooms.get(i).role == Role.ENTRANCE && rooms.get(i).level >= 0) return i;
        throw new IllegalStateException("no entrance");
    }

    @Test
    void everyRoomIsFoundWhereItStands() {
        SkyDungeon d = dungeon();
        List<SkyDungeon.Room> rooms = d.rooms();
        for (int i = 0; i < rooms.size(); i++) {
            SkyDungeon.Room r = rooms.get(i);
            if (r.level < 0) continue;
            assertEquals(
                i,
                DungeonChart.roomAt(d, r.x + 0.5, r.floorY + 1.0, r.z + 0.5),
                r.template + " at its middle");
        }
    }

    @Test
    void aFreshChartShowsOnlyTheSurveyorsKeyMark() {
        SkyDungeon d = dungeon();
        DungeonChart chart = DungeonChart.of(d, new BitSet(), 0, 0, 0, 0.0F);
        assertEquals(1, chart.rooms.size(), "only the key room");
        assertEquals(DungeonChart.KEY, chart.rooms.get(0).mark);
        assertTrue(chart.keyLevel >= 0 && chart.keyLevel < chart.levels);
        assertTrue(chart.links.isEmpty());
    }

    @Test
    void walkingARoomInksItAndSketchesItsNeighbours() {
        SkyDungeon d = dungeon();
        List<SkyDungeon.Room> rooms = d.rooms();
        int e = entrance(rooms);
        BitSet walked = new BitSet();
        walked.set(e);
        SkyDungeon.Room hall = rooms.get(e);
        DungeonChart chart = DungeonChart.of(d, walked, hall.x + 0.5, hall.floorY + 1, hall.z + 0.5, 90.0F);
        int inked = 0, sketched = 0;
        for (DungeonChart.Room r : chart.rooms) {
            if (r.visited) {
                inked++;
                assertEquals(DungeonChart.ENTRANCE, r.mark, "the entrance is marked");
            } else if (r.mark != DungeonChart.KEY) sketched++;
        }
        assertEquals(1, inked);
        assertTrue(sketched >= 1, "the rooms it opens onto are sketched in");
        assertFalse(chart.links.isEmpty(), "and the ways to them drawn");
        assertEquals(hall.level, chart.playerLevel, "you are on the entrance's level");
    }

    @Test
    void theChartCrossesTheWireWhole() {
        SkyDungeon d = dungeon();
        BitSet walked = new BitSet();
        for (int i = 0; i < d.rooms()
            .size(); i += 3) walked.set(i);
        DungeonChart chart = DungeonChart.of(d, walked, 10.5, 100, -20.5, 45.0F);
        ByteBuf buf = Unpooled.buffer();
        chart.write(buf);
        DungeonChart back = DungeonChart.read(buf);
        assertEquals(chart.levels, back.levels);
        assertEquals(chart.playerLevel, back.playerLevel);
        assertEquals(chart.playerX, back.playerX);
        assertEquals(chart.keyLevel, back.keyLevel);
        assertEquals(chart.rooms.size(), back.rooms.size());
        for (int i = 0; i < chart.rooms.size(); i++) {
            DungeonChart.Room a = chart.rooms.get(i), b = back.rooms.get(i);
            assertEquals(a.minX, b.minX);
            assertEquals(a.maxZ, b.maxZ);
            assertEquals(a.mark, b.mark);
            assertEquals(a.visited, b.visited);
            assertEquals(a.level, b.level);
        }
        assertEquals(chart.links.size(), back.links.size());
    }

    @Test
    void walkedRoomsAreKept() {
        NBTTagCompound charts = new NBTTagCompound();
        assertTrue(DungeonChart.visit(charts, "3:4", 5));
        assertFalse(DungeonChart.visit(charts, "3:4", 5), "already walked");
        assertTrue(DungeonChart.visit(charts, "3:4", 70));
        BitSet walked = DungeonChart.visited(charts, "3:4");
        assertTrue(walked.get(5) && walked.get(70) && !walked.get(6));
        assertTrue(
            DungeonChart.visited(charts, "9:9")
                .isEmpty(),
            "each dungeon its own chart");
    }
}
