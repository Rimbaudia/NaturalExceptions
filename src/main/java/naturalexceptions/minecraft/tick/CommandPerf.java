package naturalexceptions.minecraft.tick;

import java.util.Map;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import naturalexceptions.NaturalExceptions;
import naturalexceptions.core.id.ResourceId;
import naturalexceptions.core.profile.SubsystemMetrics;
import naturalexceptions.core.profile.SubsystemProfiler;
import naturalexceptions.core.worldgen.SkyDungeon;
import naturalexceptions.core.worldgen.SkyIslands;
import naturalexceptions.minecraft.worldgen.CommandLandmarks;
import naturalexceptions.minecraft.worldgen.NEOverworldGenerator;

/**
 * {@code /ne_perf [reset|gen]}: what NE's own work costs: the server's tick time, then for each timed operation
 * (chunk materialization, fluids, networks, star rain, the scheduler) how often it ran, its average and its worst
 * call. {@code gen} generates the land of the nearest citadel not yet generated and times it, so worldgen can be
 * measured from a server console with nobody flying there.
 */
public final class CommandPerf extends CommandBase {

    @Override
    public String getCommandName() {
        return "ne_perf";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/ne_perf [reset|gen] - NE's timed work since start or the last reset: calls, average and worst ms;"
            + " gen generates and times the nearest ungenerated citadel";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        SubsystemProfiler ticks = NaturalExceptions.tickHandler() != null ? NaturalExceptions.tickHandler()
            .profiler() : null;
        if (args.length > 0 && "reset".equalsIgnoreCase(args[0])) {
            NEOverworldGenerator.PROFILER.reset();
            if (ticks != null) ticks.reset();
            say(sender, "NE timings reset.");
            return;
        }
        if (args.length > 0 && "gen".equalsIgnoreCase(args[0])) {
            generateCitadel(sender);
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null) {
            double mspt = mean(server.tickTimeArray) / 1.0e6;
            say(sender, String.format("Server tick: %.1f ms (%.1f TPS)", mspt, Math.min(20.0, 1000.0 / mspt)));
        }
        report(sender, NEOverworldGenerator.PROFILER);
        if (ticks != null) report(sender, ticks);
    }

    private static void generateCitadel(ICommandSender sender) {
        MinecraftServer server = MinecraftServer.getServer();
        World world = server != null ? server.worldServerForDimension(0) : null;
        if (world == null) return;
        net.minecraft.util.ChunkCoordinates from = sender.getPlayerCoordinates();
        SkyIslands islands = new SkyIslands(CommandLandmarks.landmarkSeed(world));
        SkyIslands.Island target = null;
        for (SkyIslands.Island island : CommandLandmarks
            .nearestDungeons(islands, from.posX, from.posZ, SkyDungeon.Kind.CITADEL, 12000)) {
            if (CommandLandmarks.presence(world, islands, island) == CommandLandmarks.Presence.UNEXPLORED) {
                target = island;
                break;
            }
        }
        if (target == null) {
            say(sender, "No ungenerated citadel within 12000 blocks.");
            return;
        }
        NEOverworldGenerator.PROFILER.reset();
        long start = System.nanoTime();
        // Straight through the provider: from a console, the world hands out empty stand-in chunks instead.
        net.minecraft.world.gen.ChunkProviderServer provider = ((net.minecraft.world.WorldServer) world).theChunkProviderServer;
        int reach = ((int) Math.ceil(target.radius() * 1.3) >> 4) + 2, chunks = 0;
        int cx = target.centreX() >> 4, cz = target.centreZ() >> 4;
        for (int dx = -reach; dx <= reach; dx++)
            for (int dz = -reach; dz <= reach; dz++, chunks++) provider.loadChunk(cx + dx, cz + dz);
        double ms = (System.nanoTime() - start) / 1.0e6;
        say(
            sender,
            String.format(
                "Generated the citadel at %d, %d: %d chunks in %.0f ms (vanilla terrain, NE materialization, lighting)",
                target.centreX(),
                target.centreZ(),
                chunks,
                ms));
        report(sender, NEOverworldGenerator.PROFILER);
    }

    private static void report(ICommandSender sender, SubsystemProfiler profiler) {
        for (Map.Entry<ResourceId, SubsystemMetrics> entry : profiler.snapshotAll()
            .entrySet()) {
            SubsystemMetrics m = entry.getValue();
            for (Map.Entry<String, Long> op : m.invocations()
                .entrySet()) {
                long calls = op.getValue();
                if (calls == 0) continue;
                say(
                    sender,
                    String.format(
                        "%s %s: %d calls, avg %.2f ms, worst %.2f ms",
                        entry.getKey()
                            .path(),
                        op.getKey(),
                        calls,
                        m.accumulatedNanos(op.getKey()) / 1.0e6 / calls,
                        m.maxNanos(op.getKey()) / 1.0e6));
            }
        }
    }

    private static double mean(long[] values) {
        if (values == null || values.length == 0) return 0.0;
        long sum = 0;
        for (long v : values) sum += v;
        return (double) sum / values.length;
    }

    private static void say(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText(text));
    }
}
