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
 * {@code /ne_landmarks [tp]}: where the nearest sky islands and giant landmarks are, and optionally a teleport to
 * the nearest island. Only new chunks get landmarks, so a listed one inside explored land may be missing.
 */
public final class CommandLandmarks extends CommandBase {

    private static final int SEARCH = 1200;
    private static final int SHOWN = 4;

    @Override
    public String getCommandName() {
        return "ne_landmarks";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/ne_landmarks [tp] - nearest sky islands and landmarks; tp goes to the nearest island";
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
        sender.addChatMessage(new ChatComponentText("§eNearest sky islands:"));
        for (int i = 0; i < Math.min(SHOWN, found.size()); i++) {
            SkyIslands.Island island = found.get(i);
            sender.addChatMessage(
                new ChatComponentText(
                    String.format(
                        "  %d, %d, %d  (%d blocks %s)%s",
                        island.centreX(),
                        island.centreY(),
                        island.centreZ(),
                        (int) distance(island.centreX(), island.centreZ(), px, pz),
                        direction(island.centreX() - px, island.centreZ() - pz),
                        island.hasCache() ? ", has a sky cache" : "")));
        }
        Landmarks landmarks = new Landmarks(seed);
        List<Landmarks.Landmark> giants = new ArrayList<>(
            landmarks.near(px - SEARCH, pz - SEARCH, px + SEARCH, pz + SEARCH));
        giants.sort((a, b) -> Double.compare(distance(a.x(), a.z(), px, pz), distance(b.x(), b.z(), px, pz)));
        sender.addChatMessage(new ChatComponentText("§eNearest landmarks:"));
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
        sender.addChatMessage(new ChatComponentText("§8Only land generated since they were added has them."));
        if (args.length > 0 && "tp".equalsIgnoreCase(args[0]) && !found.isEmpty()) {
            SkyIslands.Island island = found.get(0);
            player.setPositionAndUpdate(island.centreX() + 0.5, island.centreY() + 3, island.centreZ() + 0.5);
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
