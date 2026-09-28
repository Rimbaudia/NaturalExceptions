package naturalexceptions.minecraft.tick;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import naturalexceptions.core.event.EventInstanceFactory;
import naturalexceptions.core.event.EventPhase;
import naturalexceptions.core.event.EventRecord;
import naturalexceptions.core.event.EventRegion;
import naturalexceptions.core.event.StarRainImpact;
import naturalexceptions.core.event.StarRainNavigation;
import naturalexceptions.core.fluid.FluidCellKey;
import naturalexceptions.core.fluid.FluidParcel;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;

/** Operator diagnostics and explicit development-world time acceleration for P11 acceptance. */
public final class CommandP11 extends CommandBase {

    @Override
    public String getCommandName() {
        return "ne_p11";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/ne_p11 <status|impacts|locate|goto|impact-now [impact-id]|advance|rain> (developer tools)";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        World world = sender.getEntityWorld();
        if (world == null || world.isRemote || world.provider.dimensionId != 0) {
            sender.addChatMessage(new ChatComponentText("P11 tools require the server Overworld."));
            return;
        }
        NEWorldSavedData saved = NEWorldSavedData.get(world);
        long now = world.getTotalWorldTime();
        String action = args.length == 0 ? "status" : args[0].toLowerCase();
        if ("impacts".equals(action)) {
            EntityPlayer player = sender instanceof EntityPlayer ? (EntityPlayer) sender : null;
            if (player == null) {
                sender.addChatMessage(new ChatComponentText("Run /ne_p11 impacts in game to sort by player distance."));
                return;
            }
            double px = player.posX;
            double pz = player.posZ;
            List<StarRainNavigation.Target> impacts = StarRainNavigation.list(
                saved.p11()
                    .snapshot(),
                now,
                px,
                pz,
                remainingSiteMasses(saved));
            sender.addChatMessage(new ChatComponentText("P11 impacts: " + impacts.size()));
            sender.addChatMessage(
                new ChatComponentText(
                    "PENDING=future, no site; IN_FLIGHT=due, no visible descent/site; "
                        + "IMPACTED=site or awaiting chunk; RECOVERED=matter left site."));
            for (int i = 0; i < Math.min(impacts.size(), 12); i++) {
                StarRainNavigation.Target target = impacts.get(i);
                sender.addChatMessage(new ChatComponentText(formatTarget(target, now)));
            }
            return;
        }
        if ("locate".equals(action) || "goto".equals(action)) {
            if (!(sender instanceof EntityPlayer)) {
                sender.addChatMessage(new ChatComponentText("Run this developer navigation tool as a player."));
                return;
            }
            EntityPlayer player = (EntityPlayer) sender;
            double px = player.posX;
            double pz = player.posZ;
            StarRainNavigation.Target target = StarRainNavigation.select(
                saved.p11()
                    .snapshot(),
                now,
                px,
                pz,
                remainingSiteMasses(saved));
            if (target == null) {
                sender.addChatMessage(
                    new ChatComponentText("No unrecovered, in-flight, or pending Star Rain impacts exist."));
                return;
            }
            String label = target.state() == StarRainNavigation.State.IMPACTED ? "Fallen Star" : "Star Rain impact";
            sender.addChatMessage(
                new ChatComponentText(
                    ("locate".equals(action) ? "Nearest " : "Selected ") + label + ": " + formatTarget(target, now)));
            sender.addChatMessage(new ChatComponentText(stateHint(target.state())));
            sender.addChatMessage(
                new ChatComponentText(
                    "Direction: " + cardinalDirection(
                        target.impact()
                            .x() + 0.5D
                            - px,
                        target.impact()
                            .z() + 0.5D
                            - pz)
                        + "."));
            if ("goto".equals(action)) {
                int[] safe = findSafeSurface(
                    world,
                    target.impact()
                        .x(),
                    target.impact()
                        .z(),
                    8);
                if (safe == null) {
                    sender.addChatMessage(
                        new ChatComponentText(
                            "No safe surface in already-loaded chunks near that impact; no chunks were loaded."));
                    return;
                }
                player.setPositionAndUpdate(safe[0] + 0.5D, safe[1], safe[2] + 0.5D);
                sender.addChatMessage(
                    new ChatComponentText(
                        "Developer test teleport complete near X=" + safe[0]
                            + " Z="
                            + safe[2]
                            + "; Star Rain state and timing were not changed."));
            }
            return;
        }
        if ("impact-now".equals(action)) {
            if (!(sender instanceof EntityPlayer)) {
                sender.addChatMessage(
                    new ChatComponentText("Run this developer impact control as a player in the Overworld."));
                return;
            }
            if (args.length > 2) {
                sender.addChatMessage(new ChatComponentText("Use /ne_p11 impact-now [full impact-id]."));
                return;
            }
            EntityPlayer player = (EntityPlayer) sender;
            StarRainImpact impact;
            if (args.length == 2) {
                DeterministicId id;
                try {
                    id = DeterministicId.parse(args[1]);
                } catch (IllegalArgumentException invalid) {
                    sender.addChatMessage(
                        new ChatComponentText("Use the full 64-character impact ID from /ne_p11 impacts."));
                    return;
                }
                impact = saved.p11()
                    .get(id);
                if (impact == null) {
                    sender.addChatMessage(new ChatComponentText("No existing P11 impact has that ID."));
                    return;
                }
            } else {
                StarRainNavigation.Target selected = StarRainNavigation.selectUnresolved(
                    saved.p11()
                        .snapshot(),
                    now,
                    player.posX,
                    player.posZ);
                impact = selected == null ? null : selected.impact();
            }
            if (impact == null) {
                sender.addChatMessage(new ChatComponentText("No unresolved P11 impact is available."));
                return;
            }
            if (impact.isResolved()) {
                sender.addChatMessage(
                    new ChatComponentText("That impact already happened; its site and matter cannot be recreated."));
                return;
            }
            boolean alreadyDue = impact.effectiveDueTick() <= now;
            if (saved.p11()
                .expedite(impact.id(), now)) saved.markDirty();
            sender.addChatMessage(
                new ChatComponentText(
                    "Existing impact " + impact.id()
                        .hex()
                        + (alreadyDue ? " is already due" : " was made due now")
                        + ". The normal P11 tick will resolve up to two due impacts per tick."));
            boolean loaded = world.getChunkProvider()
                .chunkExists(impact.x() >> 4, impact.z() >> 4);
            sender.addChatMessage(
                new ChatComponentText(
                    loaded
                        ? "Target chunk is loaded at X=" + impact.x()
                            + " Z="
                            + impact.z()
                            + "; normal P11 processing will attempt physical site materialization."
                        : "Target chunk is unloaded; normal P11 processing will record the impact, then materialize "
                            + "its site when that chunk loads."));
            return;
        }
        if ("advance".equals(action)) {
            long target = saved.p11()
                .nextDueTick();
            if (target < 0L) {
                for (EventRecord event : saved.p8()
                    .events()
                    .snapshot()
                    .events()) {
                    if (!EventRecord.PROTECTED_STAR_RAIN.equals(event.type()) || event.phase() != EventPhase.SCHEDULED)
                        continue;
                    target = event.scheduledAtTick();
                    break;
                }
            }
            if (target <= now || target < 0L) {
                sender.addChatMessage(new ChatComponentText("No future Star Rain boundary needs acceleration."));
                return;
            }
            naturalexceptions.minecraft.tick.NEWorldTickHandler handler = naturalexceptions.NaturalExceptions
                .tickHandler();
            if (handler == null) {
                sender.addChatMessage(new ChatComponentText("P11 development scheduler is unavailable."));
                return;
            }
            // A server World does not expose a supported setter for totalWorldTime in 1.7.10. Run the same
            // domain tick at the requested persisted boundary without altering the world's day/night clock.
            handler.advanceForDevelopment(saved, world, target);
            sender.addChatMessage(
                new ChatComponentText(
                    "Advanced P11 simulation to Star Rain boundary " + target + ". The world clock was not changed."));
            return;
        }
        if ("rain".equals(action)) {
            long ordinal = Math.max(0L, now);
            EventRecord next = EventInstanceFactory.regular(
                world.getSeed(),
                saved.metadata()
                    .worldgenEpoch()
                    .value(),
                EventRecord.PROTECTED_STAR_RAIN,
                EventRegion.global(),
                ordinal,
                now + 20L,
                2400L,
                24000L,
                null,
                0);
            if (!saved.p8()
                .events()
                .schedule(next)) {
                sender.addChatMessage(
                    new ChatComponentText("P8 has another Star Rain pending or the event cooldown is active."));
                return;
            }
            saved.markDirty();
            sender.addChatMessage(
                new ChatComponentText(
                    "Scheduled a development Star Rain through P8 as " + next.id()
                        .hex()
                        .substring(0, 12) + "."));
            return;
        }
        sender.addChatMessage(
            new ChatComponentText(
                "P8 Star Rains: " + saved.p8()
                    .events()
                    .protectedStarRainCount()
                    + " protected opportunity; "
                    + saved.p8()
                        .events()
                        .eventCount()
                    + " total event records."));
        sender.addChatMessage(
            new ChatComponentText(
                "P11 impacts: " + saved.p11()
                    .impactCount()
                    + ", due work: "
                    + saved.p11()
                        .pendingCount()
                    + ", awaiting loaded chunks: "
                    + saved.p11()
                        .awaitingMaterializationCount()
                    + ", next impact tick: "
                    + saved.p11()
                        .nextDueTick()));
    }

    /** Recoverable stellar mass per resolved impact (in-flight or still on site). */
    public static Map<naturalexceptions.core.id.DeterministicId, Long> remainingSiteMasses(NEWorldSavedData saved) {
        Map<DeterministicId, Long> result = new HashMap<>();
        if (saved == null || saved.p11() == null) return result;
        List<StarRainImpact> impacts = saved.p11()
            .snapshot();
        Map<FluidCellKey, FluidParcel> cells = saved.fluids()
            .snapshotCells();
        Map<DeterministicId, Long> materializedMass = new HashMap<>();
        for (StarRainImpact impact : impacts) {
            if (impact.isResolved() && impact.isMaterialized()) materializedMass.put(impact.id(), 0L);
        }
        for (Map.Entry<FluidCellKey, FluidParcel> entry : cells.entrySet()) {
            DeterministicId provenance = entry.getValue()
                .provenanceId();
            if (provenance == null || !materializedMass.containsKey(provenance)) continue;
            StarRainImpact impact = saved.p11()
                .get(provenance);
            FluidCellKey key = entry.getKey();
            if (impact != null && key.x() == impact.x() && key.z() == impact.z()) {
                materializedMass.put(
                    provenance,
                    Math.addExact(
                        materializedMass.get(provenance),
                        entry.getValue()
                            .mass()
                            .milligrams()));
            }
        }
        for (StarRainImpact impact : impacts) {
            if (!impact.isResolved()) continue;
            if (!impact.isMaterialized()) {
                result.put(
                    impact.id(),
                    impact.stellarMass()
                        .milligrams());
            } else result.put(impact.id(), materializedMass.get(impact.id()));
        }
        return result;
    }

    static String formatTarget(StarRainNavigation.Target target, long now) {
        StarRainImpact impact = target.impact();
        StringBuilder line = new StringBuilder();
        line.append(
            impact.id()
                .hex())
            .append(" ")
            .append(target.state())
            .append(" — X=")
            .append(impact.x())
            .append(" Z=")
            .append(impact.z())
            .append(" — ")
            .append(Math.round(target.distance()))
            .append(" horizontal blocks away")
            .append(" — severity ")
            .append(impact.severity());
        if (target.state() == StarRainNavigation.State.IMPACTED) {
            line.append(" — ")
                .append(target.remainingMass())
                .append(" remaining");
        }
        line.append(" — parent ")
            .append(
                impact.parentEventId()
                    .hex());
        if (target.state() == StarRainNavigation.State.PENDING) {
            long ticks = Math.max(0L, impact.effectiveDueTick() - now);
            line.append(" — impact in ")
                .append(ticks)
                .append(" ticks (")
                .append(ticks / 20L)
                .append(" s)");
        }
        return line.toString();
    }

    static String stateHint(StarRainNavigation.State state) {
        switch (state) {
            case PENDING:
                return "PENDING: no Fallen Star exists at this site yet. Use /ne_p11 impact-now to make an existing impact due.";
            case IN_FLIGHT:
                return "IN_FLIGHT: due but unresolved; no visible descent or confirmed physical site yet.";
            case IMPACTED:
                return "IMPACTED: physical site exists or awaits materialization when its chunk loads.";
            default:
                return "RECOVERED: stellar matter has left the impact site.";
        }
    }

    private static String cardinalDirection(double dx, double dz) {
        if (Math.abs(dx) < 0.5D && Math.abs(dz) < 0.5D) return "here";
        double degrees = Math.toDegrees(Math.atan2(dx, -dz));
        String[] directions = { "north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest" };
        int index = (int) Math.round(degrees / 45.0D);
        return directions[(index + directions.length) % directions.length];
    }

    /** Looks only at loaded chunks and requires a solid, non-hazardous floor and two clear body blocks. */
    private static int[] findSafeSurface(World world, int targetX, int targetZ, int radius) {
        List<int[]> candidates = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                candidates.add(new int[] { targetX + dx, targetZ + dz, dx * dx + dz * dz });
            }
        }
        Collections.sort(candidates, new Comparator<int[]>() {

            @Override
            public int compare(int[] left, int[] right) {
                int distance = Integer.compare(left[2], right[2]);
                if (distance != 0) return distance;
                int x = Integer.compare(left[0], right[0]);
                return x != 0 ? x : Integer.compare(left[1], right[1]);
            }
        });
        for (int[] candidate : candidates) {
            int x = candidate[0], z = candidate[1];
            if (!world.getChunkProvider()
                .chunkExists(x >> 4, z >> 4)) continue;
            int y = world.getTopSolidOrLiquidBlock(x, z);
            if (y < 1 || y + 1 >= world.getHeight()) continue;
            net.minecraft.block.Block floor = world.getBlock(x, y - 1, z);
            if (floor == Blocks.fire || floor == Blocks.lava
                || floor == Blocks.flowing_lava
                || floor == Blocks.water
                || floor == Blocks.flowing_water
                || !floor.getMaterial()
                    .isSolid())
                continue;
            if (!world.isAirBlock(x, y, z) || !world.isAirBlock(x, y + 1, z)) continue;
            return new int[] { x, y, z };
        }
        return null;
    }
}
