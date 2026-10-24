package naturalexceptions.minecraft.worldgen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.World;

import naturalexceptions.core.worldgen.SkyDungeon;
import naturalexceptions.core.worldgen.SkyIslands;

/**
 * The Lookout's Log, left in a Hermit's lookout on top of a spire: the last watchman's bearings to the sky-halls
 * nearest to it. A written book, worked out when the lookout is built.
 */
final class LookoutLog {

    /** How far the watchman could see. */
    static final int SIGHT = 3000;
    static final int MAX_BEARINGS = 3;

    private LookoutLog() {}

    static ItemStack write(World world, int x, int z) {
        List<String> pages = new ArrayList<>();
        pages.add(
            "The last watch.\n\nThe halls in the sky have gone dark, and no one answers the lamps. I set down their bearings for whoever climbs up after me.");
        List<String> bearings = bearings(DungeonBeacons.field(world), x, z);
        if (bearings.isEmpty())
            pages.add("No hall within sight of this post.\n\nWhatever kept us here, it was not them.");
        else {
            StringBuilder page = new StringBuilder("Bearings, from this post:\n");
            for (String bearing : bearings) page.append('\n')
                .append(bearing)
                .append('\n');
            pages.add(page.toString());
        }
        ItemStack book = new ItemStack(Items.written_book);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("title", "Lookout's Log");
        tag.setString("author", "The last watch");
        NBTTagList list = new NBTTagList();
        for (String page : pages) list.appendTag(new NBTTagString(page));
        tag.setTag("pages", list);
        book.setTagCompound(tag);
        return book;
    }

    /** "A Citadel: north-east, about 1250 blocks, high above." for the nearest halls, nearest first. */
    static List<String> bearings(SkyIslands islands, int x, int z) {
        List<SkyIslands.Island> halls = new ArrayList<>();
        for (SkyIslands.Island island : islands.near(x - SIGHT, z - SIGHT, x + SIGHT, z + SIGHT)) {
            if (!island.hasDungeon()) continue;
            double dx = island.centreX() - x, dz = island.centreZ() - z;
            if (dx * dx + dz * dz <= (double) SIGHT * SIGHT) halls.add(island);
        }
        halls.sort(
            (a, b) -> Double
                .compare(Math.hypot(a.centreX() - x, a.centreZ() - z), Math.hypot(b.centreX() - x, b.centreZ() - z)));
        List<String> bearings = new ArrayList<>();
        for (SkyIslands.Island island : halls) {
            if (bearings.size() == MAX_BEARINGS) break;
            double dx = island.centreX() - x, dz = island.centreZ() - z;
            long distance = Math.round(Math.hypot(dx, dz) / 50.0) * 50;
            String name = islands.dungeonKind(island) == SkyDungeon.Kind.CITADEL ? "A Citadel" : "A Sanctum";
            bearings.add(name + ": " + DungeonBeacons.compass(dx, dz) + ", about " + distance + " blocks, high above.");
        }
        return bearings;
    }
}
