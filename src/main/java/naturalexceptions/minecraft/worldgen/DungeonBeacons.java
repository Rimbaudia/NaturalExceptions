package naturalexceptions.minecraft.worldgen;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import naturalexceptions.core.worldgen.SkyDungeon;
import naturalexceptions.core.worldgen.SkyIslands;
import naturalexceptions.minecraft.item.ItemSkyKey;

/**
 * Where a sky dungeon's secrets are, worked out again from the world's seed exactly as the dungeon was laid out: the
 * gate the Sky Key opens and the chest the key lies in. The Sky Key uses it to point the way; and while a dungeon's
 * key is still in its chest, the chest chimes to a player close by, so it can be found by ear through the walls.
 */
public final class DungeonBeacons {

    /** A dungeon's key gate (its middle) and its key chest, in world coordinates. */
    public static final class Beacon {

        public final int[] gate, chest;
        /** The dungeon itself, and its name for keeping what a player has walked of it. */
        public SkyDungeon dungeon;
        public String name;

        Beacon(int[] gate, int[] chest) {
            this.gate = gate;
            this.chest = chest;
        }
    }

    /** How far out (horizontally) a dungeon is felt. */
    public static final int REACH = 320;
    /** How close a player must be to hear the key's chest. */
    static final int CHIME_RANGE = 16;
    static final int CHIME_TICKS = 60;

    private static SkyIslands field;
    private static final Map<String, Beacon> BEACONS = new HashMap<>();

    static synchronized SkyIslands field(World world) {
        long seed = NEOverworldGenerator.landmarkSeed(world);
        if (field == null || field.seed() != seed) {
            field = new SkyIslands(seed);
            BEACONS.clear();
        }
        return field;
    }

    /** The nearest sky dungeon within {@link #REACH} of (x, z) in the overworld, or null. */
    public static Beacon nearest(World world, double x, double z) {
        if (world == null || world.provider.dimensionId != 0) return null;
        SkyIslands islands = field(world);
        Beacon best = null;
        double bestDistance = Double.MAX_VALUE;
        for (SkyIslands.Island island : islands
            .near((int) x - REACH, (int) z - REACH, (int) x + REACH, (int) z + REACH)) {
            if (!island.hasDungeon()) continue;
            double dx = island.centreX() - x, dz = island.centreZ() - z;
            double distance = dx * dx + dz * dz;
            if (distance > (double) REACH * REACH || distance >= bestDistance) continue;
            Beacon beacon = beacon(islands, island);
            if (beacon == null) continue;
            best = beacon;
            bestDistance = distance;
        }
        return best;
    }

    private static synchronized Beacon beacon(SkyIslands islands, SkyIslands.Island island) {
        String key = island.cellX() + ":" + island.cellZ();
        if (BEACONS.containsKey(key)) return BEACONS.get(key);
        SkyDungeon dungeon = SkyDungeon.of(islands, island);
        Beacon beacon = dungeon == null ? null : locate(dungeon);
        if (beacon != null) {
            beacon.dungeon = dungeon;
            beacon.name = key;
        }
        BEACONS.put(key, beacon);
        return beacon;
    }

    /** The middle of the key gate and the key chest of a laid-out dungeon (null if it has either missing). */
    public static Beacon locate(SkyDungeon dungeon) {
        long gx = 0, gy = 0, gz = 0;
        int gates = 0;
        int[] chest = null;
        for (SkyDungeon.Piece p : dungeon.pieces()) {
            if (p.block == SkyDungeon.Block.GATE && p.meta == SkyDungeon.KEY_GATE) {
                gx += p.x;
                gy += p.y;
                gz += p.z;
                gates++;
            } else if (p.block == SkyDungeon.Block.CHEST && p.loot == SkyDungeon.Loot.KEY) {
                chest = new int[] { p.x, p.y, p.z };
            }
        }
        if (gates == 0 || chest == null) return null;
        return new Beacon(new int[] { (int) (gx / gates), (int) (gy / gates), (int) (gz / gates) }, chest);
    }

    /** Where a spot lies from a player: "north-east, 34 blocks, two levels down". */
    public static String describe(double fromX, double fromY, double fromZ, int[] to) {
        double dx = to[0] + 0.5 - fromX, dz = to[2] + 0.5 - fromZ, dy = to[1] - fromY;
        int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz + dy * dy));
        String way = compass(dx, dz);
        int levels = (int) Math.round(Math.abs(dy) / 12.0);
        String level = Math.abs(dy) < 4 ? "on this level"
            : (levels <= 1 ? "a level" : count(levels) + " levels") + (dy < 0 ? " down" : " up");
        if (Math.abs(dy) >= 4 && levels == 0) level = dy < 0 ? "a little below" : "a little above";
        return (distance < 3 ? "right here" : way + ", " + distance + " blocks") + ", " + level;
    }

    static String compass(double dx, double dz) {
        // Minecraft's north is -Z, east +X.
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        String[] names = { "north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west" };
        return names[(int) Math.floorMod(Math.round(angle / 45.0), 8)];
    }

    private static String count(int n) {
        String[] words = { "no", "one", "two", "three", "four", "five" };
        return n < words.length ? words[n] : String.valueOf(n);
    }

    // --- The key's chest, calling ------------------------------------------------------------------------------

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (event.phase != TickEvent.Phase.END || player.worldObj.isRemote) return;
        long tick = player.worldObj.getTotalWorldTime() + player.getEntityId();
        if (tick % 20 != 0) return;
        Beacon beacon = nearest(player.worldObj, player.posX, player.posZ);
        if (beacon == null) return;
        // The room the player stands in goes on their chart.
        int room = DungeonChart.roomAt(beacon.dungeon, player.posX, player.posY, player.posZ);
        if (room >= 0) DungeonChart.visit(charts(player), beacon.name, room);
        if (tick % CHIME_TICKS != 0) return;
        int[] c = beacon.chest;
        if (player.getDistanceSq(c[0] + 0.5, c[1] + 0.5, c[2] + 0.5) > CHIME_RANGE * CHIME_RANGE) return;
        if (!holdsKey(player.worldObj.getTileEntity(c[0], c[1], c[2]))) return;
        player.worldObj.playSoundEffect(c[0] + 0.5, c[1] + 0.5, c[2] + 0.5, "note.pling", 0.7F, 1.4F);
        if (player.worldObj instanceof WorldServer) ((WorldServer) player.worldObj)
            .func_147487_a("witchMagic", c[0] + 0.5, c[1] + 1.1, c[2] + 0.5, 8, 0.25, 0.3, 0.25, 0.02);
    }

    /** Where a player's charts are kept: with the data that outlives death. */
    static net.minecraft.nbt.NBTTagCompound charts(EntityPlayer player) {
        net.minecraft.nbt.NBTTagCompound data = player.getEntityData();
        net.minecraft.nbt.NBTTagCompound persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        data.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        net.minecraft.nbt.NBTTagCompound charts = persisted.getCompoundTag("naturalexceptions.charts");
        persisted.setTag("naturalexceptions.charts", charts);
        return charts;
    }

    /** Shows the player the chart of the dungeon they are in or near; false if none is near. */
    public static boolean showChart(net.minecraft.entity.player.EntityPlayerMP player) {
        Beacon beacon = nearest(player.worldObj, player.posX, player.posZ);
        if (beacon == null) return false;
        DungeonChart chart = DungeonChart.of(
            beacon.dungeon,
            DungeonChart.visited(charts(player), beacon.name),
            player.posX,
            player.posY,
            player.posZ,
            player.rotationYaw);
        naturalexceptions.minecraft.network.NENetwork.CHANNEL
            .sendTo(new naturalexceptions.minecraft.network.PacketDungeonChart(chart), player);
        return true;
    }

    private static boolean holdsKey(TileEntity tile) {
        if (!(tile instanceof IInventory)) return false;
        IInventory inventory = (IInventory) tile;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemSkyKey) return true;
        }
        return false;
    }
}
