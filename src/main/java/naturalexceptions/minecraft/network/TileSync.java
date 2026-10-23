package naturalexceptions.minecraft.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

/**
 * Sends a tile entity's state to the players who can see it, for the updates a machine makes as it runs (a smelter's
 * melt warming, a workpiece cooling). {@code world.markBlockForUpdate} would do it too, but it also resends the block,
 * and every client then rebuilds that chunk section's geometry: needless for a tile entity whose moving parts its
 * special renderer draws, and costly every few ticks on a slow machine. Keep markBlockForUpdate for changes to the
 * block's own look.
 */
public final class TileSync {

    private TileSync() {}

    /** The tile entity's description packet, to every player watching its chunk. */
    public static void toWatchers(TileEntity tile) {
        if (tile == null || !(tile.getWorldObj() instanceof WorldServer)) return;
        Packet packet = tile.getDescriptionPacket();
        if (packet != null) send((WorldServer) tile.getWorldObj(), tile.xCoord >> 4, tile.zCoord >> 4, packet);
    }

    /**
     * As {@link #toWatchers}, but only if its saved state differs from {@code last} (the state sent last time, or
     * null): a machine sitting idle sends nothing. Returns the state now, to pass as {@code last} next time.
     */
    public static NBTTagCompound toWatchersIfChanged(TileEntity tile, NBTTagCompound last) {
        if (tile == null || !(tile.getWorldObj() instanceof WorldServer)) return last;
        NBTTagCompound now = new NBTTagCompound();
        tile.writeToNBT(now);
        if (now.equals(last)) return last;
        toWatchers(tile);
        return now;
    }

    private static void send(WorldServer world, int chunkX, int chunkZ, Packet packet) {
        for (Object o : world.playerEntities) {
            if (!(o instanceof EntityPlayerMP)) continue;
            EntityPlayerMP player = (EntityPlayerMP) o;
            if (world.getPlayerManager()
                .isPlayerWatchingChunk(player, chunkX, chunkZ)) player.playerNetServerHandler.sendPacket(packet);
        }
    }
}
