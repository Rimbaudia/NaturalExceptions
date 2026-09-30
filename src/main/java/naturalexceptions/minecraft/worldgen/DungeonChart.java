package naturalexceptions.minecraft.worldgen;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import io.netty.buffer.ByteBuf;
import naturalexceptions.core.worldgen.SkyDungeon;
import naturalexceptions.core.worldgen.dungeon.RoomTemplate.Role;

/**
 * What a Surveyor's Chart shows of one dungeon for one player: the rooms they have been in (inked), the rooms those
 * open onto (sketched), the ways between them, what the visited rooms are (a way down, a way up, the entrance, the
 * vault, the Warden's gate once its neighbour has been reached), where the player stands, and always the old
 * surveyor's mark on the room where the key was kept.
 */
public final class DungeonChart {

    /** What a room is marked as on the chart. */
    public static final int PLAIN = 0, DOWN = 1, UP = 2, ENTRANCE = 3, KEY = 4, WARDEN = 5, VAULT = 6;

    /** A room as charted: where it lies, on which level, whether it has been walked, and its mark. */
    public static final class Room {

        public final int minX, minZ, maxX, maxZ, level, mark;
        public final boolean visited;

        Room(int minX, int minZ, int maxX, int maxZ, int level, int mark, boolean visited) {
            this.minX = minX;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxZ = maxZ;
            this.level = level;
            this.mark = mark;
            this.visited = visited;
        }
    }

    public final int levels;
    /** The level the player is on (or nearest), and where they stand and face. */
    public final int playerLevel;
    public final int playerX, playerZ;
    public final float playerYaw;
    /** The level the key was kept on, or -1 if the dungeon has none. */
    public final int keyLevel;
    public final List<Room> rooms;
    /** Pairs of indices into {@link #rooms} for the ways walked between them. */
    public final List<int[]> links;

    DungeonChart(int levels, int playerLevel, int playerX, int playerZ, float playerYaw, int keyLevel, List<Room> rooms,
        List<int[]> links) {
        this.levels = levels;
        this.playerLevel = playerLevel;
        this.playerX = playerX;
        this.playerZ = playerZ;
        this.playerYaw = playerYaw;
        this.keyLevel = keyLevel;
        this.rooms = rooms;
        this.links = links;
    }

    /** The index of the dungeon room (below ground) that (x, y, z) stands in, or -1. */
    public static int roomAt(SkyDungeon dungeon, double x, double y, double z) {
        List<SkyDungeon.Room> all = dungeon.rooms();
        for (int i = 0; i < all.size(); i++) {
            SkyDungeon.Room r = all.get(i);
            if (r.level < 0) continue;
            if (x >= r.minX && x < r.maxX + 1
                && z >= r.minZ
                && z < r.maxZ + 1
                && y >= r.floorY
                && y < r.floorY + r.height + 1) return i;
        }
        return -1;
    }

    /** The chart of {@code dungeon} for a player who has walked the rooms in {@code visited}, standing at (x, y, z). */
    public static DungeonChart of(SkyDungeon dungeon, BitSet visited, double x, double y, double z, float yaw) {
        List<SkyDungeon.Room> all = dungeon.rooms();
        int levels = 0, keyLevel = -1;
        for (SkyDungeon.Room r : all) {
            levels = Math.max(levels, r.level + 1);
            if (r.role == Role.KEY) keyLevel = r.level;
        }
        // Rooms the visited ones open onto are sketched in; the Warden's hall once the room before its gate is walked.
        BitSet near = new BitSet();
        for (SkyDungeon.Connection c : dungeon.connections()) {
            int a = all.indexOf(c.a), b = all.indexOf(c.b);
            if (a < 0 || b < 0 || c.style == SkyDungeon.Style.SECRET) continue;
            if (visited.get(a)) near.set(b);
            if (visited.get(b)) near.set(a);
        }
        List<Room> rooms = new ArrayList<>();
        int[] index = new int[all.size()];
        java.util.Arrays.fill(index, -1);
        for (int i = 0; i < all.size(); i++) {
            SkyDungeon.Room r = all.get(i);
            if (r.level < 0) continue;
            boolean walked = visited.get(i), key = r.role == Role.KEY;
            if (!walked && !near.get(i) && !key) continue;
            if (r.role == Role.SECRET && !walked) continue;
            int mark = key ? KEY : walked || r.role == Role.WARDEN ? mark(r.role) : PLAIN;
            index[i] = rooms.size();
            rooms.add(new Room(r.minX, r.minZ, r.maxX, r.maxZ, r.level, mark, walked));
        }
        List<int[]> links = new ArrayList<>();
        for (SkyDungeon.Connection c : dungeon.connections()) {
            int a = all.indexOf(c.a), b = all.indexOf(c.b);
            if (a < 0 || b < 0 || index[a] < 0 || index[b] < 0 || !(visited.get(a) || visited.get(b))) continue;
            if (c.style == SkyDungeon.Style.SHAFT || c.a.level != c.b.level) continue;
            links.add(new int[] { index[a], index[b] });
        }
        int here = roomAt(dungeon, x, y, z);
        int level = here >= 0 ? all.get(here).level : nearestLevel(all, y);
        return new DungeonChart(levels, level, (int) Math.floor(x), (int) Math.floor(z), yaw, keyLevel, rooms, links);
    }

    private static int mark(Role role) {
        switch (role) {
            case SHAFT_TOP:
                return DOWN;
            case SHAFT_FOOT:
                return UP;
            case ENTRANCE:
                return ENTRANCE;
            case WARDEN:
                return WARDEN;
            case VAULT:
                return VAULT;
            default:
                return PLAIN;
        }
    }

    private static int nearestLevel(List<SkyDungeon.Room> all, double y) {
        int best = 0;
        double bestDistance = Double.MAX_VALUE;
        for (SkyDungeon.Room r : all) {
            if (r.level < 0) continue;
            double distance = Math.abs(y - r.floorY);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = r.level;
            }
        }
        return best;
    }

    // --- The wire ------------------------------------------------------------------------------------------

    public void write(ByteBuf buf) {
        buf.writeByte(levels);
        buf.writeByte(playerLevel);
        buf.writeInt(playerX);
        buf.writeInt(playerZ);
        buf.writeFloat(playerYaw);
        buf.writeByte(keyLevel);
        buf.writeShort(rooms.size());
        for (Room r : rooms) {
            buf.writeInt(r.minX);
            buf.writeInt(r.minZ);
            buf.writeShort(r.maxX - r.minX);
            buf.writeShort(r.maxZ - r.minZ);
            buf.writeByte(r.level);
            buf.writeByte(r.mark | (r.visited ? 0x80 : 0));
        }
        buf.writeShort(links.size());
        for (int[] link : links) {
            buf.writeShort(link[0]);
            buf.writeShort(link[1]);
        }
    }

    public static DungeonChart read(ByteBuf buf) {
        int levels = buf.readByte(), playerLevel = buf.readByte(), px = buf.readInt(), pz = buf.readInt();
        float yaw = buf.readFloat();
        int keyLevel = buf.readByte();
        int count = buf.readShort();
        List<Room> rooms = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int minX = buf.readInt(), minZ = buf.readInt(), w = buf.readShort(), d = buf.readShort();
            int level = buf.readByte(), flags = buf.readUnsignedByte();
            rooms.add(new Room(minX, minZ, minX + w, minZ + d, level, flags & 0x7F, (flags & 0x80) != 0));
        }
        int linkCount = buf.readShort();
        List<int[]> links = new ArrayList<>(linkCount);
        for (int i = 0; i < linkCount; i++) links.add(new int[] { buf.readShort(), buf.readShort() });
        return new DungeonChart(levels, playerLevel, px, pz, yaw, keyLevel, rooms, links);
    }

    // --- What a player has walked, kept with them ------------------------------------------------------------

    /** The NBT key a player's walked rooms of one dungeon are kept under. */
    static String visitKey(String dungeon) {
        return "chart:" + dungeon;
    }

    public static BitSet visited(net.minecraft.nbt.NBTTagCompound charts, String dungeon) {
        return BitSet.valueOf(charts.getByteArray(visitKey(dungeon)));
    }

    /** Marks room {@code room} walked; returns whether it was new. */
    public static boolean visit(net.minecraft.nbt.NBTTagCompound charts, String dungeon, int room) {
        BitSet bits = visited(charts, dungeon);
        if (bits.get(room)) return false;
        bits.set(room);
        charts.setByteArray(visitKey(dungeon), bits.toByteArray());
        return true;
    }
}
