package naturalexceptions.minecraft.persistence;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import naturalexceptions.core.event.StarRainHistory;
import naturalexceptions.core.event.StarRainImpact;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.metallurgy.MaterialQuantity;

/** Bounded semantic P11 history codec. Derived event indexes and rendering state are not serialized. */
public final class StarRainHistoryNbtCodec {

    private static final int VERSION = 2;

    private StarRainHistoryNbtCodec() {}

    public static NBTTagCompound write(StarRainHistory history) {
        if (history == null) throw new IllegalArgumentException("P11 history is required");
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("Version", VERSION);
        NBTTagList list = new NBTTagList();
        for (StarRainImpact impact : history.snapshot()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString(
                "Parent",
                impact.parentEventId()
                    .hex());
            tag.setString(
                "Id",
                impact.id()
                    .hex());
            tag.setInteger("Ordinal", impact.ordinal());
            tag.setLong("Due", impact.dueTick());
            if (impact.expeditedDueTick() >= 0L) tag.setLong("ExpeditedDue", impact.expeditedDueTick());
            tag.setInteger("X", impact.x());
            tag.setInteger("Z", impact.z());
            tag.setInteger("Severity", impact.severity());
            tag.setLong(
                "MassMg",
                impact.stellarMass()
                    .milligrams());
            tag.setBoolean("Resolved", impact.isResolved());
            tag.setBoolean("Materialized", impact.isMaterialized());
            list.appendTag(tag);
        }
        root.setTag("Impacts", list);
        return root;
    }

    public static StarRainHistory read(NBTTagCompound root) {
        if (root == null || !root.hasKey("Version", 99)
            || root.getInteger("Version") <= 0
            || root.getInteger("Version") > VERSION
            || !root.hasKey("Impacts", 9)) return new StarRainHistory();
        NBTTagList list = root.getTagList("Impacts", 10);
        List<StarRainImpact> impacts = new ArrayList<>();
        for (int i = 0; i < Math.min(list.tagCount(), StarRainHistory.MAX_IMPACTS); i++) {
            try {
                NBTTagCompound tag = list.getCompoundTagAt(i);
                long mass = tag.getLong("MassMg");
                impacts.add(
                    new StarRainImpact(
                        DeterministicId.parse(tag.getString("Parent")),
                        DeterministicId.parse(tag.getString("Id")),
                        tag.getInteger("Ordinal"),
                        tag.getLong("Due"),
                        tag.getInteger("X"),
                        tag.getInteger("Z"),
                        tag.getInteger("Severity"),
                        MaterialQuantity.ofMilligrams(mass),
                        tag.getBoolean("Resolved"),
                        tag.getBoolean("Materialized"),
                        tag.hasKey("ExpeditedDue", 99) ? tag.getLong("ExpeditedDue") : -1L));
            } catch (RuntimeException ignored) {
                // Keep valid independent impact history when one malformed record is encountered.
            }
        }
        try {
            return new StarRainHistory(impacts);
        } catch (RuntimeException invalid) {
            return new StarRainHistory();
        }
    }
}
