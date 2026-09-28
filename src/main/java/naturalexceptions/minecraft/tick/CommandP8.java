package naturalexceptions.minecraft.tick;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import naturalexceptions.core.anomaly.DestabilizationField;
import naturalexceptions.core.anomaly.RegionKey;
import naturalexceptions.core.calendar.NaturalCalendar;
import naturalexceptions.core.event.EventPhase;
import naturalexceptions.core.event.EventRecord;
import naturalexceptions.core.event.EventRegion;
import naturalexceptions.core.event.P8WorldState;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;

/** Operator-only P8 inspection/setup command; not a gameplay progression mechanic. */
public final class CommandP8 extends CommandBase {
    @Override public String getCommandName() { return "ne_p8"; }
    @Override public String getCommandUsage(ICommandSender sender) {
        return "/ne_p8 <status|events|trigger blood_night|destabilize [amount]>";
    }
    @Override public int getRequiredPermissionLevel() { return 2; }

    @Override public void processCommand(ICommandSender sender, String[] args) {
        World world = sender.getEntityWorld();
        if (world == null || world.isRemote || world.provider.dimensionId != 0) {
            sender.addChatMessage(new ChatComponentText("P8 diagnostics require the server Overworld."));
            return;
        }
        NEWorldSavedData saved = NEWorldSavedData.get(world);
        if (saved == null || saved.p8() == null) return;
        long now = world.getTotalWorldTime();
        if (args.length > 0 && "events".equalsIgnoreCase(args[0])) {
            listEvents(sender, saved, now);
            return;
        }
        if (args.length > 0 && "trigger".equalsIgnoreCase(args[0])) {
            triggerEvent(sender, saved, now, args);
            return;
        }
        if (args.length > 0 && "destabilize".equalsIgnoreCase(args[0])) {
            double amount = 0.65;
            try { if (args.length > 1) amount = Double.parseDouble(args[1]); }
            catch (NumberFormatException e) {
                sender.addChatMessage(new ChatComponentText("Amount must be between 0.01 and 1.")); return;
            }
            if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0 || amount > 1) {
                sender.addChatMessage(new ChatComponentText("Amount must be between 0.01 and 1.")); return;
            }
            int x = sender instanceof net.minecraft.entity.player.EntityPlayer
                ? (int) Math.floor(((net.minecraft.entity.player.EntityPlayer) sender).posX) : 0;
            int z = sender instanceof net.minecraft.entity.player.EntityPlayer
                ? (int) Math.floor(((net.minecraft.entity.player.EntityPlayer) sender).posZ) : 0;
            RegionKey region = RegionKey.fromBlock(x, z);
            DeterministicId cause = DeterministicId.derive("naturalexceptions:p8:dev-command",
                saved.metadata().worldId(), Long.toString(region.x()), Long.toString(region.z()), Long.toString(now));
            saved.p8().destabilization().increase(region, ResourceId.of("naturalexceptions", "dev_test"), cause,
                amount, now);
            saved.p8().advance(now);
            saved.markDirty();
        }
        NaturalCalendar.Position calendar = NaturalCalendar.at(now);
        sender.addChatMessage(new ChatComponentText("NE calendar: year " + calendar.year() + ", day "
            + (calendar.dayOfYear() + 1) + ", " + calendar.season().name().toLowerCase() + "; strange season: "
            + saved.p8().strangeSeasonAt(now).name().toLowerCase()));
        sender.addChatMessage(new ChatComponentText("P8 events: " + saved.p8().events().eventCount()
            + ", protected Star Rain: " + saved.p8().events().protectedStarRainCount()
            + ", indexed anomalies: " + saved.p8().anomalies().size()));
        sender.addChatMessage(new ChatComponentText("Regional destabilization: "
            + saved.p8().destabilization().value(RegionKey.fromBlock(
                sender instanceof net.minecraft.entity.player.EntityPlayer
                    ? (int) ((net.minecraft.entity.player.EntityPlayer) sender).posX : 0,
                sender instanceof net.minecraft.entity.player.EntityPlayer
                    ? (int) ((net.minecraft.entity.player.EntityPlayer) sender).posZ : 0))
            + "; derived known-region mean: " + saved.p8().destabilization().derivedGlobal()));
    }

    private static void listEvents(ICommandSender sender, NEWorldSavedData saved, long now) {
        List<EventRecord> events = new ArrayList<>(saved.p8().events().snapshot().events());
        Collections.sort(events, new Comparator<EventRecord>() {
            @Override public int compare(EventRecord left, EventRecord right) {
                int leftRank = phaseRank(left.phase());
                int rightRank = phaseRank(right.phase());
                if (leftRank != rightRank) return Integer.compare(leftRank, rightRank);
                long leftTime = left.phase() == EventPhase.COMPLETED
                    ? left.phaseChangedAtTick() : left.scheduledAtTick();
                long rightTime = right.phase() == EventPhase.COMPLETED
                    ? right.phaseChangedAtTick() : right.scheduledAtTick();
                int byTime = left.phase() == EventPhase.COMPLETED
                    ? Long.compare(rightTime, leftTime) : Long.compare(leftTime, rightTime);
                return byTime != 0 ? byTime : left.id().compareTo(right.id());
            }
        });
        if (events.isEmpty()) {
            sender.addChatMessage(new ChatComponentText("P8 has no recorded events."));
            return;
        }
        sender.addChatMessage(new ChatComponentText("P8 event history (showing up to 12 of " + events.size() + "):"));
        int count = Math.min(events.size(), 12);
        for (int i = 0; i < count; i++) {
            EventRecord event = events.get(i);
            String identity = event.id().hex();
            sender.addChatMessage(new ChatComponentText(event.type() + " [" + identity.substring(0, 12)
                + "] " + event.phase().name().toLowerCase() + " @ " + event.targetRegion()
                + " ticks " + event.scheduledAtTick() + "–" + event.endsAtTick()
                + (event.protectedOpportunity() ? " [protected first Star Rain opportunity]" : "")
                + (event.phase() == EventPhase.ACTIVE && now >= event.scheduledAtTick()
                    ? " (active now)" : "")));
        }
    }

    private static int phaseRank(EventPhase phase) {
        if (phase == EventPhase.ACTIVE) return 0;
        if (phase == EventPhase.SCHEDULED) return 1;
        return 2;
    }

    private static void triggerEvent(ICommandSender sender, NEWorldSavedData saved, long now, String[] args) {
        if (args.length != 2 || !"blood_night".equalsIgnoreCase(args[1])) {
            sender.addChatMessage(new ChatComponentText("Usage: /ne_p8 trigger blood_night"));
            return;
        }
        int x = sender instanceof net.minecraft.entity.player.EntityPlayer
            ? (int) Math.floor(((net.minecraft.entity.player.EntityPlayer) sender).posX) : 0;
        int z = sender instanceof net.minecraft.entity.player.EntityPlayer
            ? (int) Math.floor(((net.minecraft.entity.player.EntityPlayer) sender).posZ) : 0;
        naturalexceptions.core.anomaly.RegionKey region = RegionKey.fromBlock(x, z);
        boolean scheduled = saved.p8().scheduleBloodNight(EventRegion.of((int) region.x(), (int) region.z()),
            now, now, 3L * NaturalCalendar.TICKS_PER_DAY, 36L * NaturalCalendar.TICKS_PER_DAY);
        if (!scheduled) {
            sender.addChatMessage(new ChatComponentText("Blood Night was not scheduled: this region is cooling down or its retained history is full."));
            return;
        }
        saved.p8().advance(now);
        saved.markDirty();
        EventRecord event = saved.p8().events().snapshot().events().stream()
            .filter(candidate -> candidate.type().equals(P8WorldState.BLOOD_NIGHT)
                && candidate.targetRegion().equals(EventRegion.of((int) region.x(), (int) region.z()))
                && candidate.scheduledAtTick() == now)
            .findFirst().orElse(null);
        sender.addChatMessage(new ChatComponentText("Triggered local Blood Night"
            + (event == null ? "" : " [" + event.id().hex().substring(0, 12) + "]")
            + "; inspect it with /ne_p8 events. Operator test only."));
    }
}
