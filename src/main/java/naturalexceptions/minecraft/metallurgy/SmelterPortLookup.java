package naturalexceptions.minecraft.metallurgy;

import net.minecraft.world.IBlockAccess;

import naturalexceptions.minecraft.multiblock.SmelterVesselGeometry;
import naturalexceptions.minecraft.multiblock.StarterSmelterDefinition;

/** Shared port-to-vessel resolution; ports attach to shell positions, not to the controller block. */
public final class SmelterPortLookup {

    private SmelterPortLookup() {}

    public static TileEntityStarterSmelter findTuyereController(IBlockAccess world, int x, int y, int z) {
        if (world == null) return null;
        for (int cx = x - 8; cx <= x + 8; cx++) for (int cz = z - 8; cz <= z + 8; cz++) {
            net.minecraft.tileentity.TileEntity tile = world.getTileEntity(cx, y, cz);
            if (!(tile instanceof TileEntityStarterSmelter)) continue;
            int facing = world.getBlockMetadata(cx, y, cz);
            SmelterVesselGeometry geometry = StarterSmelterDefinition.INSTANCE.discoverVessel(world, cx, y, cz, facing);
            if (geometry != null && isTuyerePort(world, geometry, x, y, z)) return (TileEntityStarterSmelter) tile;
        }
        return null;
    }

    public static boolean isTuyerePort(IBlockAccess world, SmelterVesselGeometry geometry, int x, int y, int z) {
        int half = (geometry.interiorWidth() + 2) / 2;
        int outerDepth = geometry.interiorDepth() + 2;
        int[][] walls = new int[2 * (2 * half + outerDepth)][2];
        int count = 0;
        for (int dx = -half + 1; dx < half; dx++) {
            walls[count++] = new int[] { dx, 0 };
            walls[count++] = new int[] { dx, outerDepth - 1 };
        }
        for (int dz = 1; dz < outerDepth - 1; dz++) {
            walls[count++] = new int[] { -half, dz };
            walls[count++] = new int[] { half, dz };
        }
        for (int i = 0; i < count; i++) {
            int dx = walls[i][0], dz = walls[i][1];
            if (dx == 0 && dz == 0) continue;
            int outX = dx == -half ? -1 : dx == half ? 1 : 0;
            int outZ = dz == 0 ? -1 : dz == outerDepth - 1 ? 1 : 0;
            int[] wall = geometry.world(dx, 0, dz);
            int[] outside = geometry.world(dx + outX, 0, dz + outZ);
            if (outside[0] != x || outside[1] != y || outside[2] != z) continue;
            int inward = directionFor(wall[0] - outside[0], wall[2] - outside[2]);
            return world.getBlock(x, y, z) instanceof BlockSmelterTuyere && world.getBlockMetadata(x, y, z) == inward;
        }
        return false;
    }

    public static int countAttachedTuyeres(IBlockAccess world, SmelterVesselGeometry geometry) {
        if (world == null || geometry == null) return 0;
        int half = (geometry.interiorWidth() + 2) / 2;
        int depth = geometry.interiorDepth() + 2;
        int count = 0;
        for (int dx = -half + 1; dx < half; dx++) {
            for (int dz : new int[] { 0, depth - 1 }) {
                if (dx == 0 && dz == 0) continue;
                int outZ = dz == 0 ? -1 : 1;
                int[] port = geometry.world(dx, 0, dz + outZ);
                if (isTuyerePort(world, geometry, port[0], port[1], port[2])) count++;
            }
        }
        for (int dz = 1; dz < depth - 1; dz++) {
            for (int dx : new int[] { -half, half }) {
                int[] port = geometry.world(dx + (dx < 0 ? -1 : 1), 0, dz);
                if (isTuyerePort(world, geometry, port[0], port[1], port[2])) count++;
            }
        }
        return count;
    }

    public static TileEntityStarterSmelter findOutletController(IBlockAccess world, int x, int y, int z, int facing) {
        if (world == null) return null;
        for (int cx = x - 8; cx <= x + 8; cx++) for (int cz = z - 8; cz <= z + 8; cz++) {
            net.minecraft.tileentity.TileEntity tile = world.getTileEntity(cx, y, cz);
            if (!(tile instanceof TileEntityStarterSmelter)) continue;
            SmelterVesselGeometry geometry = StarterSmelterDefinition.INSTANCE
                .discoverVessel(world, cx, y, cz, world.getBlockMetadata(cx, y, cz));
            if (geometry != null && isOutletPort(geometry, x, y, z, facing)) return (TileEntityStarterSmelter) tile;
        }
        return null;
    }

    public static boolean isOutletPort(SmelterVesselGeometry geometry, int x, int y, int z, int facing) {
        if (geometry == null) return false;
        int half = (geometry.interiorWidth() + 2) / 2;
        int depth = geometry.interiorDepth() + 2;
        for (int dx = -half + 1; dx < half; dx++) {
            for (int dz : new int[] { 0, depth - 1 }) {
                if (dx == 0 && dz == 0) continue;
                int outZ = dz == 0 ? -1 : 1;
                int[] port = geometry.world(dx, 0, dz + outZ);
                int[] wall = geometry.world(dx, 0, dz);
                if (port[0] == x && port[1] == y
                    && port[2] == z
                    && facing == directionFor(port[0] - wall[0], port[2] - wall[2])) return true;
            }
        }
        for (int dz = 1; dz < depth - 1; dz++) {
            for (int dx : new int[] { -half, half }) {
                int outX = dx < 0 ? -1 : 1;
                int[] port = geometry.world(dx + outX, 0, dz);
                int[] wall = geometry.world(dx, 0, dz);
                if (port[0] == x && port[1] == y
                    && port[2] == z
                    && facing == directionFor(port[0] - wall[0], port[2] - wall[2])) return true;
            }
        }
        return false;
    }

    public static TileEntitySmelterOutlet findOutlet(IBlockAccess world, SmelterVesselGeometry geometry) {
        if (world == null || geometry == null) return null;
        int half = (geometry.interiorWidth() + 2) / 2;
        int depth = geometry.interiorDepth() + 2;
        for (int dx = -half + 1; dx < half; dx++) for (int dz : new int[] { 0, depth - 1 }) {
            if (dx == 0 && dz == 0) continue;
            int[] wall = geometry.world(dx, 0, dz);
            int[] port = geometry.world(dx, 0, dz + (dz == 0 ? -1 : 1));
            int facing = directionFor(port[0] - wall[0], port[2] - wall[2]);
            if (world.getBlock(port[0], port[1], port[2]) instanceof BlockSmelterOutlet
                && world.getBlockMetadata(port[0], port[1], port[2]) == facing) {
                net.minecraft.tileentity.TileEntity te = world.getTileEntity(port[0], port[1], port[2]);
                if (te instanceof TileEntitySmelterOutlet) return (TileEntitySmelterOutlet) te;
            }
        }
        for (int dz = 1; dz < depth - 1; dz++) for (int dx : new int[] { -half, half }) {
            int[] wall = geometry.world(dx, 0, dz);
            int[] port = geometry.world(dx + (dx < 0 ? -1 : 1), 0, dz);
            int facing = directionFor(port[0] - wall[0], port[2] - wall[2]);
            if (world.getBlock(port[0], port[1], port[2]) instanceof BlockSmelterOutlet
                && world.getBlockMetadata(port[0], port[1], port[2]) == facing) {
                net.minecraft.tileentity.TileEntity te = world.getTileEntity(port[0], port[1], port[2]);
                if (te instanceof TileEntitySmelterOutlet) return (TileEntitySmelterOutlet) te;
            }
        }
        return null;
    }

    public static int directionFor(int dx, int dz) {
        if (dx < 0) return 4;
        if (dx > 0) return 5;
        if (dz < 0) return 2;
        return 3;
    }

}
