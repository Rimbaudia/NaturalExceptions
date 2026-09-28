package naturalexceptions.minecraft.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import naturalexceptions.core.calendar.NaturalCalendar;
import naturalexceptions.core.event.P8WorldState;
import naturalexceptions.core.event.StarRainImpact;
import naturalexceptions.core.event.StarRainNavigation;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.minecraft.event.P8Presentation;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;

/**
 * A kept record of days and skies. Reading it gives the date and season, what the land is going through, and
 * the rough bearing of any fallen star that has not yet been recovered.
 */
public final class ItemAlmanac extends Item {

    /** Fallen stars farther than this are beyond what an almanac-keeper would have noted. */
    static final double SITE_NOTE_RANGE = 1024.0;

    public ItemAlmanac() {
        setUnlocalizedName("almanac");
        setTextureName("naturalexceptions:almanac");
        setMaxStackSize(1);
    }

    /** The lines shown when reading; pure so it can be checked without a world. */
    public static List<String> lines(P8WorldState p8, List<StarRainImpact> impacts,
        Map<DeterministicId, Long> remainingMassMg, long nowTick, int x, int z) {
        List<String> lines = new ArrayList<>();
        NaturalCalendar.Position position = NaturalCalendar.at(Math.max(0L, nowTick));
        lines.add("§6" + P8Presentation.dateLine(position));
        lines.add("§7" + P8Presentation.seasonProgressLine(position));
        if (p8 != null) {
            String strange = P8Presentation.strangeSeasonLine(p8.strangeSeasonAt(nowTick));
            if (strange != null) lines.add("§8" + strange);
            if (p8.bloodNightActiveAt(x, z, nowTick)) lines.add("§c" + P8Presentation.bloodNightAlmanacLine());
        }
        if (impacts != null && !impacts.isEmpty()) {
            StarRainNavigation.Target nearest = StarRainNavigation.select(impacts, nowTick, x, z, remainingMassMg);
            if (nearest != null && nearest.state() == StarRainNavigation.State.IMPACTED
                && nearest.distance() <= SITE_NOTE_RANGE) {
                lines.add(
                    "§b" + P8Presentation.nearestSiteLine(
                        nearest.impact()
                            .x() + 0.5 - x,
                        nearest.impact()
                            .z() + 0.5 - z));
            }
        }
        return lines;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote || player == null) return stack;
        NEWorldSavedData saved = world.provider.dimensionId == 0 ? NEWorldSavedData.get(world) : null;
        int x = MathHelper.floor_double(player.posX);
        int z = MathHelper.floor_double(player.posZ);
        List<String> lines;
        if (saved == null) {
            lines = new ArrayList<>();
            lines.add("§7The days here do not follow the almanac.");
        } else {
            lines = lines(
                saved.p8(),
                saved.p11()
                    .snapshot(),
                naturalexceptions.minecraft.tick.CommandP11.remainingSiteMasses(saved),
                world.getTotalWorldTime(),
                x,
                z);
        }
        for (String line : lines) player.addChatMessage(new ChatComponentText(line));
        return stack;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        tooltip.add("§7Right-click to read the day, the season,");
        tooltip.add("§7and what the skies have done lately.");
    }
}
