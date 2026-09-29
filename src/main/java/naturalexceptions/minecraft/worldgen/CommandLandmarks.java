package naturalexceptions.minecraft.worldgen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import naturalexceptions.core.persistence.WorldgenEpoch;
import naturalexceptions.core.worldgen.Landmarks;
import naturalexceptions.core.worldgen.SkyIslands;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;

/**
 * {@code /ne_landmarks [tp|tp dungeon]}: where the nearest sky islands, sky dungeons and giant landmarks are, and
 * optionally a teleport to the nearest island (or dungeon). Only new chunks get landmarks, so a listed one inside
 * explored land may be missing.
 */
public final class CommandLandmarks extends CommandBase {

    private static final int SEARCH = 1200;
    /** Dungeon islands are few (about one per 1536-block square, half the time), so look further for them. */
    private static final int DUNGEON_SEARCH = 4000;
    private static final int SHOWN = 4;

    @Override
    public String getCommandName() {
        return "ne_landmarks";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/ne_landmarks [tp|tp dungeon] - nearest sky islands, dungeons and landmarks; tp goes to the nearest island"
            + " (or dungeon)";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        World world = player.worldObj;
        if (world.provider.dimensionId != 0) {
            sender.addChatMessage(new ChatComponentText("Landmarks are only in the Overworld."));
            return;
        }
        long seed = landmarkSeed(world);
        int px = (int) Math.floor(player.posX), pz = (int) Math.floor(player.posZ);
        SkyIslands islands = new SkyIslands(seed);
        List<SkyIslands.Island> found = new ArrayList<>(
            islands.near(px - SEARCH, pz - SEARCH, px + SEARCH, pz + SEARCH));
        found.sort(
            (a, b) -> Double
                .compare(distance(a.centreX(), a.centreZ(), px, pz), distance(b.centreX(), b.centreZ(), px, pz)));
        sender.addChatMessage(new ChatComponentText("\u00a7eNearest sky islands:"));
        SkyIslands.Island target = null;
        for (int i = 0; i < Math.min(SHOWN, found.size()); i++) {
            SkyIslands.Island island = found.get(i);
            Presence presence = presence(world, islands, island);
            if (target == null && presence != Presence.MISSING) target = island;
            sender.addChatMessage(
                new ChatComponentText(
                    String.format(
                        "  %s%d, %d, %d  (%d blocks %s)%s - %s",
                        presence == Presence.MISSING ? "\u00a78" : "",
                        island.centreX(),
                        island.centreY(),
                        island.centreZ(),
                        (int) distance(island.centreX(), island.centreZ(), px, pz),
                        direction(island.centreX() - px, island.centreZ() - pz),
                        island.hasDungeon() ? ", sky dungeon" : island.hasCache() ? ", has a hidden cache" : "",
                        presence.words)));
        }
        if (target == null) for (SkyIslands.Island island : found) {
            if (presence(world, islands, island) != Presence.MISSING) {
                target = island;
                break;
            }
        }
        List<SkyIslands.Island> dungeons = new ArrayList<>();
        for (SkyIslands.Island island : islands
            .near(px - DUNGEON_SEARCH, pz - DUNGEON_SEARCH, px + DUNGEON_SEARCH, pz + DUNGEON_SEARCH)) {
            if (island.hasDungeon()) dungeons.add(island);
        }
        dungeons.sort(
            (a, b) -> Double
                .compare(distance(a.centreX(), a.centreZ(), px, pz), distance(b.centreX(), b.centreZ(), px, pz)));
        sender.addChatMessage(new ChatComponentText("\u00a7eNearest sky dungeons:"));
        if (dungeons.isEmpty()) sender.addChatMessage(new ChatComponentText("  none within " + DUNGEON_SEARCH));
        SkyIslands.Island dungeonTarget = null;
        for (SkyIslands.Island island : dungeons) {
            Presence presence = presence(world, islands, island);
            if (dungeonTarget == null && presence != Presence.MISSING) dungeonTarget = island;
        }
        for (int i = 0; i < Math.min(SHOWN - 1, dungeons.size()); i++) {
            SkyIslands.Island island = dungeons.get(i);
            Presence presence = presence(world, islands, island);
            naturalexceptions.core.worldgen.SkyDungeon.Kind kind = islands.dungeonKind(island);
            sender.addChatMessage(
                new ChatComponentText(
                    String.format(
                        "  %s%s at %d, %d, %d  (%d blocks %s) - %s",
                        presence == Presence.MISSING ? "\u00a78" : "",
                        kind == naturalexceptions.core.worldgen.SkyDungeon.Kind.CITADEL ? "citadel" : "sanctum",
                        island.centreX(),
                        island.centreY(),
                        island.centreZ(),
                        (int) distance(island.centreX(), island.centreZ(), px, pz),
                        direction(island.centreX() - px, island.centreZ() - pz),
                        presence.words)));
        }
        Landmarks landmarks = new Landmarks(seed);
        List<Landmarks.Landmark> giants = new ArrayList<>(
            landmarks.near(px - SEARCH, pz - SEARCH, px + SEARCH, pz + SEARCH));
        giants.sort((a, b) -> Double.compare(distance(a.x(), a.z(), px, pz), distance(b.x(), b.z(), px, pz)));
        sender.addChatMessage(new ChatComponentText("\u00a7eNearest landmarks:"));
        for (int i = 0; i < Math.min(SHOWN, giants.size()); i++) {
            Landmarks.Landmark landmark = giants.get(i);
            sender.addChatMessage(
                new ChatComponentText(
                    String.format(
                        "  %s at %d, %d  (%d blocks %s)",
                        landmark.kind()
                            .name()
                            .toLowerCase(java.util.Locale.ROOT),
                        landmark.x(),
                        landmark.z(),
                        (int) distance(landmark.x(), landmark.z(), px, pz),
                        direction(landmark.x() - px, landmark.z() - pz))));
        }
        sender
            .addChatMessage(new ChatComponentText("\u00a78Islands only form in land generated after they were added."));
        if (args.length > 0 && "tp".equalsIgnoreCase(args[0])) {
            boolean toDungeon = args.length > 1 && "dungeon".equalsIgnoreCase(args[1]);
            if (toDungeon) target = dungeonTarget;
            if (target == null) {
                sender.addChatMessage(new ChatComponentText("No island within reach can still appear here."));
                return;
            }
            prepare(world, target);
            int tx = target.centreX(), tz = target.centreZ();
            int y;
            if (toDungeon) {
                // Beside the entrance (in the sanctum's gateway, or outside the citadel's gate), not on a roof.
                int[] entrance = naturalexceptions.core.worldgen.SkyDungeon.of(islands, target)
                    .entrance();
                tx = entrance[0];
                tz = entrance[2] + 2;
                y = entrance[1];
            } else {
                int[] top = islands.column(target, tx, tz);
                y = (top != null ? top[1] : target.centreY()) + 2;
            }
            player.setPositionAndUpdate(tx + 0.5, y, tz + 0.5);
            sender.addChatMessage(
                new ChatComponentText(
                    "Went to the island at " + target.centreX()
                        + ", "
                        + target.centreY()
                        + ", "
                        + target.centreZ()
                        + " ("
                        + presence(world, islands, target).words
                        + ")."));
        }
    }

    /** Whether an island listed from the seed is really in this world. */
    enum Presence {

        /** Its blocks are there. */
        THERE("there"),
        /** Its land has not been generated yet; it appears when it is. */
        UNEXPLORED("not generated yet"),
        /** Generated but not yet finished (neighbouring land missing); it fills in as you approach. */
        PENDING("still forming"),
        /** Its land was generated before islands (or before their current layout); it will never have it. */
        MISSING("missing: this land is older than the islands");

        final String words;

        Presence(String words) {
            this.words = words;
        }
    }

    static Presence presence(World world, SkyIslands islands, SkyIslands.Island island) {
        int x = island.centreX(), z = island.centreZ();
        int cx = x >> 4, cz = z >> 4;
        if (!chunkOnRecord(world, cx, cz)) return Presence.UNEXPLORED;
        net.minecraft.world.chunk.Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
        int[] column = islands.column(island, x, z);
        if (column != null) for (int y = column[0]; y <= column[1]; y++) {
            if (!world.isAirBlock(x, y, z)) return Presence.THERE;
        }
        return chunk.isTerrainPopulated ? Presence.MISSING : Presence.PENDING;
    }

    /** Loaded, or saved on disk; never generates anything. */
    private static boolean chunkOnRecord(World world, int cx, int cz) {
        if (!(world.getChunkProvider() instanceof net.minecraft.world.gen.ChunkProviderServer)) return true;
        net.minecraft.world.gen.ChunkProviderServer provider = (net.minecraft.world.gen.ChunkProviderServer) world
            .getChunkProvider();
        if (provider.chunkExists(cx, cz)) return true;
        return provider.currentChunkLoader instanceof net.minecraft.world.chunk.storage.AnvilChunkLoader
            && ((net.minecraft.world.chunk.storage.AnvilChunkLoader) provider.currentChunkLoader)
                .chunkExists(world, cx, cz);
    }

    /**
     * Generates (and so populates) the land under and around an island before a teleport, so the island is
     * already there on arrival. One ring of extra chunks lets the edge chunks finish too.
     */
    private static void prepare(World world, SkyIslands.Island island) {
        int reach = ((int) Math.ceil(island.radius() * 1.3) >> 4) + 2;
        int cx = island.centreX() >> 4, cz = island.centreZ() >> 4;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) world.getChunkFromChunkCoords(cx + dx, cz + dz);
        }
    }

    static long landmarkSeed(World world) {
        NEWorldSavedData saved = NEWorldSavedData.get(world);
        WorldgenEpoch epoch = saved != null && saved.metadata() != null ? saved.metadata()
            .worldgenEpoch() : WorldgenEpoch.CURRENT;
        return world.getSeed() ^ ((long) epoch.value() << 40);
    }

    private static double distance(int x, int z, int px, int pz) {
        return Math.hypot(x - px, z - pz);
    }

    private static String direction(int dx, int dz) {
        String ns = dz < -Math.abs(dx) / 2 ? "north" : dz > Math.abs(dx) / 2 ? "south" : "";
        String ew = dx > Math.abs(dz) / 2 ? "east" : dx < -Math.abs(dz) / 2 ? "west" : "";
        String both = ns + (ns.isEmpty() || ew.isEmpty() ? "" : "-") + ew;
        return both.isEmpty() ? "here" : both;
    }
}
