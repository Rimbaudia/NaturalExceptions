package naturalexceptions.core.worldgen;

import naturalexceptions.core.worldgen.SkyDungeon.Block;
import naturalexceptions.core.worldgen.SkyDungeon.Loot;
import naturalexceptions.core.worldgen.SkyDungeon.Room;
import naturalexceptions.core.worldgen.dungeon.RoomTemplate;

/**
 * Lays out a citadel: a walled fort on a grand island's top with corner towers, a courtyard well and a three-floor
 * keep; the vault lies in a basement carved under the keep, behind a sealed gate and a trapped corridor.
 */
final class CitadelLayout {

    static final int ROOM_HALF = 4;
    static final int ROOM_HEIGHT = 6;
    static final int TOWER_HALF = SkyDungeon.TOWER_HALF;
    static final int TOWER_HEIGHT = 12;
    static final int WALL_HEIGHT = 7;
    static final int KEEP_HALF = 7;
    static final int KEEP_FLOOR_HEIGHT = 6;
    static final int VAULT_DEPTH = 10;
    static final int CITADEL_HALF = SkyDungeon.CITADEL_HALF;
    static final int SOUTH = 3;
    static final int NORTH = 2;
    static final int LADDER_ON_NORTH_WALL = 3;
    static final int LADDER_ON_WEST_WALL = 5;
    static final int LADDER_ON_EAST_WALL = 4;

    private final SkyDungeon d;

    CitadelLayout(SkyDungeon dungeon) {
        this.d = dungeon;
    }

    void build() {
        int cx = d.island.centreX(), cz = d.island.centreZ();
        int b = d.island.centreY() + 1; // the citadel's floor level
        int h = CITADEL_HALF;
        int kx = cx, kz = cz - 3; // the keep stands back from the gate
        int vaultX = kx - 5, anteX = kx + 6, vf = b - VAULT_DEPTH;

        // Pass 1: shells and solid parts.
        for (int dx = -h; dx <= h; dx++) for (int dz = -h; dz <= h; dz++) d.stone(cx + dx, b, cz + dz);
        for (int dx = -h; dx <= h; dx++) for (int dz = -h; dz <= h; dz++) {
            if (Math.abs(dx) != h && Math.abs(dz) != h) continue;
            for (int y = b + 1; y <= b + WALL_HEIGHT; y++) d.stone(cx + dx, y, cz + dz);
            if ((dx + dz) % 2 == 0) d.stone(cx + dx, b + WALL_HEIGHT + 1, cz + dz);
        }
        int[][] towers = { { -h, -h }, { h, -h }, { -h, h }, { h, h } };
        for (int[] t : towers) {
            int tx = cx + t[0], tz = cz + t[1];
            d.shell(tx - TOWER_HALF, b, tz - TOWER_HALF, tx + TOWER_HALF, b + TOWER_HEIGHT, tz + TOWER_HALF, false);
            for (int dx = -TOWER_HALF; dx <= TOWER_HALF; dx++) for (int dz = -TOWER_HALF; dz <= TOWER_HALF; dz++) {
                boolean ring = Math.abs(dx) == TOWER_HALF || Math.abs(dz) == TOWER_HALF;
                if (ring && (dx + dz) % 2 == 0) d.stone(tx + dx, b + TOWER_HEIGHT + 1, tz + dz);
            }
        }
        int keepTop = b + 3 * KEEP_FLOOR_HEIGHT;
        d.shell(kx - KEEP_HALF, b, kz - KEEP_HALF, kx + KEEP_HALF, keepTop, kz + KEEP_HALF, false);
        for (int floor = 1; floor < 3; floor++) for (int dx = -KEEP_HALF; dx <= KEEP_HALF; dx++)
            for (int dz = -KEEP_HALF; dz <= KEEP_HALF; dz++) d.stone(kx + dx, b + floor * KEEP_FLOOR_HEIGHT, kz + dz);
        for (int dx = -KEEP_HALF; dx <= KEEP_HALF; dx++) for (int dz = -KEEP_HALF; dz <= KEEP_HALF; dz++) {
            boolean ring = Math.abs(dx) == KEEP_HALF || Math.abs(dz) == KEEP_HALF;
            if (ring && (dx + dz) % 2 == 0) d.stone(kx + dx, keepTop + 1, kz + dz);
        }
        // Basement: the antechamber, the vault beside it, the shaft down from the keep, the short corridor between.
        d.shell(anteX - ROOM_HALF, vf, kz - ROOM_HALF, anteX + ROOM_HALF, vf + ROOM_HEIGHT, kz + ROOM_HALF, false);
        shaftShell(anteX, kz - 3, vf + 1, b);
        for (int x = vaultX + ROOM_HALF; x <= anteX - ROOM_HALF; x++)
            for (int y = vf; y <= vf + 4; y++) for (int z = kz - 2; z <= kz + 2; z++) {
                boolean edge = y == vf || y == vf + 4 || Math.abs(z - kz) == 2;
                if (edge) d.stone(x, y, z);
            }
        d.shell(vaultX - ROOM_HALF, vf, kz - ROOM_HALF, vaultX + ROOM_HALF, vf + ROOM_HEIGHT, kz + ROOM_HALF, true);
        // The well in the courtyard.
        int wx = cx + 9, wz = cz + 10;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            for (int y = b - 3; y <= b; y++) d.stone(wx + dx, y, wz + dz);
            if (Math.abs(dx) == 2 || Math.abs(dz) == 2) d.stone(wx + dx, b + 1, wz + dz);
        }

        // Pass 2: insides.
        clearCourtyard(cx, cz, b, kx, kz, towers);
        for (int[] t : towers) {
            int tx = cx + t[0], tz = cz + t[1];
            d.clear(
                tx - TOWER_HALF + 1,
                b + 1,
                tz - TOWER_HALF + 1,
                tx + TOWER_HALF - 1,
                b + TOWER_HEIGHT - 1,
                tz + TOWER_HALF - 1);
            // The door faces the courtyard, two blocks in from the outer wall so it does not breach it.
            int doorX = tx - Integer.signum(t[0]) * TOWER_HALF, doorZ = tz - Integer.signum(t[1]) * 2;
            for (int y = b + 1; y <= b + 2; y++) d.put(doorX, y, doorZ, Block.AIR);
        }
        for (int floor = 0; floor < 3; floor++) {
            int f = b + floor * KEEP_FLOOR_HEIGHT;
            d.clear(
                kx - KEEP_HALF + 1,
                f + 1,
                kz - KEEP_HALF + 1,
                kx + KEEP_HALF - 1,
                f + KEEP_FLOOR_HEIGHT - 1,
                kz + KEEP_HALF - 1);
        }
        for (int dx = -1; dx <= 1; dx++) for (int y = b + 1; y <= b + 3; y++) d.put(cx + dx, y, cz + h, Block.AIR); // gate
        for (int y = b + 1; y <= b + 2; y++) d.put(kx, y, kz + KEEP_HALF, Block.AIR); // keep door
        d.clear(
            anteX - ROOM_HALF + 1,
            vf + 1,
            kz - ROOM_HALF + 1,
            anteX + ROOM_HALF - 1,
            vf + ROOM_HEIGHT - 1,
            kz + ROOM_HALF - 1);
        d.clear(
            vaultX - ROOM_HALF + 1,
            vf + 1,
            kz - ROOM_HALF + 1,
            vaultX + ROOM_HALF - 1,
            vf + ROOM_HEIGHT - 1,
            kz + ROOM_HALF - 1);
        d.clear(vaultX + ROOM_HALF, vf + 1, kz - 1, anteX - ROOM_HALF, vf + 3, kz + 1);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            d.put(wx + dx, b, wz + dz, Block.WATER);
            d.put(wx + dx, b - 1, wz + dz, Block.WATER);
            d.put(wx + dx, b - 2, wz + dz, Block.WATER);
        }

        // Pass 3: ladders, gates, lamps, chests, spawners, traps, windows.
        for (int[] t : towers) {
            int tx = cx + t[0], tz = cz + t[1];
            int lx = tx + Integer.signum(t[0]) * (TOWER_HALF - 1);
            ladder(lx, tz, b + 1, b + TOWER_HEIGHT, t[0] > 0 ? LADDER_ON_EAST_WALL : LADDER_ON_WEST_WALL);
            d.put(tx, b + TOWER_HEIGHT - 1, tz, Block.LAMP);
        }
        // Key on the north-east tower's roof; a storeroom chest in the north-west tower; a guard in the south-west.
        d.put(cx + h - 1, b + TOWER_HEIGHT + 1, cz - h + 1, Block.CHEST, 0, Loot.KEY, null);
        d.put(cx - h + 1, b + 1, cz - h + 1, Block.CHEST, 0, Loot.STORE, null);
        d.put(cx - h, b + 1, cz + h, Block.SPAWNER, 0, null, mob(cx - h, cz + h));
        int ladderX = kx - KEEP_HALF + 1;
        ladder(ladderX, kz, b + 1, keepTop, LADDER_ON_WEST_WALL);
        for (int floor = 0; floor < 3; floor++) {
            int f = b + floor * KEEP_FLOOR_HEIGHT;
            d.put(kx, f + KEEP_FLOOR_HEIGHT, kz, Block.LAMP);
            for (int[] w : new int[][] { { KEEP_HALF, 0 }, { -KEEP_HALF, 0 }, { 0, -KEEP_HALF } })
                d.put(kx + w[0], f + 3, kz + w[1], Block.GLASS_PANE);
        }
        d.put(kx + 2, b + 1, kz, Block.SPAWNER, 0, null, mob(kx, kz));
        d.put(kx - 3, b + KEEP_FLOOR_HEIGHT + 1, kz + 3, Block.CHEST, 0, Loot.STORE, null);
        d.put(kx + 3, b + KEEP_FLOOR_HEIGHT + 1, kz + 3, Block.CHEST, 0, Loot.STORE, null);
        d.put(kx + 2, b + 2 * KEEP_FLOOR_HEIGHT + 1, kz, Block.SPAWNER, 0, null, mob(kx + 1, kz));
        // Down to the basement: a hole in the keep floor over the shaft, a guarded antechamber, a trapped corridor.
        ladder(anteX, kz - 3, vf + 1, b, LADDER_ON_NORTH_WALL);
        furnish(anteX, vf, kz, RoomTemplate.Role.GUARD);
        int midX = (vaultX + ROOM_HALF + anteX - ROOM_HALF) / 2;
        for (int dz = -1; dz <= 1; dz++) d.put(midX, vf, kz + dz, Block.TRIGGER); // the whole width: no safe lane
        d.put(midX, vf + 1, kz - 2, Block.DISPENSER, SOUTH, null, null);
        d.put(midX, vf + 1, kz + 2, Block.DISPENSER, NORTH, null, null);
        furnish(vaultX, vf, kz, RoomTemplate.Role.VAULT);
        for (int z = kz - 1; z <= kz + 1; z++)
            for (int y = vf + 1; y <= vf + 3; y++) d.put(vaultX + ROOM_HALF, y, z, Block.GATE);

        d.entrance = new int[] { cx, b + 1, cz + h + 1 };
        d.rooms.add(new Room(cx, b, cz + h, RoomTemplate.Role.ENTRANCE, 1, 3));
        d.rooms.add(new Room(kx, b, kz, RoomTemplate.Role.GUARD, KEEP_HALF - 1, KEEP_FLOOR_HEIGHT - 1));
        d.rooms.add(
            new Room(kx, b + KEEP_FLOOR_HEIGHT, kz, RoomTemplate.Role.STORE, KEEP_HALF - 1, KEEP_FLOOR_HEIGHT - 1));
        d.rooms.add(new Room(cx + h, b, cz - h, RoomTemplate.Role.KEY, TOWER_HALF - 1, TOWER_HEIGHT - 1));
        d.rooms.add(new Room(anteX, vf, kz, RoomTemplate.Role.GUARD, ROOM_HALF - 1, ROOM_HEIGHT - 1));
        d.rooms.add(new Room(vaultX, vf, kz, RoomTemplate.Role.VAULT, ROOM_HALF - 1, ROOM_HEIGHT - 1));
        d.surface = new int[] { cx - h - TOWER_HALF - 1, cz - h - TOWER_HALF - 1, cx + h + TOWER_HALF + 1,
            cz + h + TOWER_HALF + 1 };
    }

    /** Clears the courtyard: everything inside the walls up to well above them, except the keep and towers. */
    private void clearCourtyard(int cx, int cz, int b, int kx, int kz, int[][] towers) {
        int h = CITADEL_HALF;
        for (int dx = -h + 1; dx <= h - 1; dx++) for (int dz = -h + 1; dz <= h - 1; dz++) {
            int x = cx + dx, z = cz + dz;
            if (Math.abs(x - kx) <= KEEP_HALF && Math.abs(z - kz) <= KEEP_HALF) continue;
            boolean inTower = false;
            for (int[] t : towers)
                inTower |= Math.abs(x - (cx + t[0])) <= TOWER_HALF && Math.abs(z - (cz + t[1])) <= TOWER_HALF;
            if (inTower) continue;
            if (Math.abs(x - (cx + 9)) <= 2 && Math.abs(z - (cz + 10)) <= 2) continue; // the well
            for (int y = b + 1; y <= b + 20; y++) d.put(x, y, z, Block.AIR);
        }
    }

    private void shaftShell(int x, int z, int y0, int y1) {
        d.shell(x - 1, y0 - 1, z - 1, x + 1, y1, z + 1, false);
    }

    private void ladder(int x, int z, int y0, int y1, int meta) {
        for (int y = y0; y <= y1; y++) d.put(x, y, z, Block.LADDER, meta, null, null);
    }

    private void furnish(int x, int f, int z, RoomTemplate.Role role) {
        d.put(x, f + ROOM_HEIGHT, z, Block.LAMP);
        switch (role) {
            case GUARD:
                d.put(x, f + 1, z, Block.SPAWNER, 0, null, mob(x, z));
                break;
            case STORE:
                d.put(x - 2, f + 1, z + 2, Block.CHEST, 0, Loot.STORE, null);
                if (d.noise.unit(x, z, 11) < 0.5) d.put(x + 2, f + 1, z + 2, Block.CHEST, 0, Loot.STORE, null);
                break;
            case KEY:
                d.put(x, f + 1, z + 2, Block.CHEST, 0, Loot.KEY, null);
                break;
            case VAULT:
                d.put(x, f + ROOM_HEIGHT, z, Block.WARDED); // no lamp in the vault's warded ceiling: it stays dark
                d.put(x, f + 1, z, Block.CHEST, 0, Loot.VAULT_TREASURE, null);
                d.put(x + 2, f + 1, z + 2, Block.CHEST, 0, Loot.VAULT_RICH, null);
                d.put(x - 2, f + 1, z + 2, Block.LAMP);
                break;
            default:
                break;
        }
    }

    private String mob(int x, int z) {
        return SkyDungeon.DEFAULT_MOBS[(int) (d.noise.unit(x, z, 7) * SkyDungeon.DEFAULT_MOBS.length)];
    }
}
