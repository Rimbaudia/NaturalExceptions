package naturalexceptions.minecraft.biology;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import naturalexceptions.minecraft.metallurgy.TileEntityStarterSmelter;
import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * The one modelled airborne hazard: smoke from a Starter Smelter fire that is actually burning. The amount
 * scales with the fire's heat output and falls off with distance. Under open sky the smoke rises away and is not
 * inhaled, and a smelter whose shell is incomplete has a stalled fire that makes none. Nothing else (a lit
 * vanilla furnace, a torch) is treated as a smoke source; atmosphere simulation is out of scope.
 */
public final class SmelterSmoke {

    /** Smoke-laden air inhaled per second, per watt of fire output, next to an enclosed fire. */
    static final double GRAMS_PER_SECOND_PER_WATT = 1.0e-5;
    /** Open sky carries the smoke away. */
    static final double OPEN_AIR_FACTOR = 0.0;
    static final int SEARCH_RADIUS = 4;

    private SmelterSmoke() {}

    /** One second of inhaled smoke from a fire of {@code powerWatts} at {@code distance} blocks. */
    public static double gramsPerSecond(double powerWatts, double distance, boolean openAir) {
        if (!(powerWatts > 0.0) || !(distance >= 0.0) || distance > SEARCH_RADIUS + 1.0) return 0.0;
        double dilution = 1.0 / (1.0 + distance * distance);
        return powerWatts * GRAMS_PER_SECOND_PER_WATT * dilution * (openAir ? OPEN_AIR_FACTOR : 1.0);
    }

    public static double gramsPerSecondAt(EntityPlayer player) {
        if (player == null || player.worldObj == null || NEBlocks.starterSmelter == null) return 0.0;
        World world = player.worldObj;
        int px = MathHelper.floor_double(player.posX);
        int py = MathHelper.floor_double(player.posY + player.getEyeHeight());
        int pz = MathHelper.floor_double(player.posZ);
        boolean openAir = world.canBlockSeeTheSky(px, py, pz);
        if (openAir && OPEN_AIR_FACTOR <= 0.0) return 0.0;
        double total = 0.0;
        for (int x = px - SEARCH_RADIUS; x <= px + SEARCH_RADIUS; x++) {
            for (int y = py - 2; y <= py + 2; y++) {
                for (int z = pz - SEARCH_RADIUS; z <= pz + SEARCH_RADIUS; z++) {
                    if (world.getBlock(x, y, z) != NEBlocks.starterSmelter) continue;
                    TileEntity tile = world.getTileEntity(x, y, z);
                    if (!(tile instanceof TileEntityStarterSmelter)) continue;
                    TileEntityStarterSmelter smelter = (TileEntityStarterSmelter) tile;
                    // An incomplete shell pauses the smelter; its buffered fire is stalled, not burning.
                    if (!smelter.isStructureValid() || !smelter.vesselState()
                        .fuelState()
                        .isBurning()) continue;
                    double dx = x + 0.5 - player.posX;
                    double dy = y + 0.5 - (player.posY + player.getEyeHeight());
                    double dz = z + 0.5 - player.posZ;
                    total += gramsPerSecond(
                        smelter.vesselState()
                            .fuelState()
                            .powerWatts(),
                        Math.sqrt(dx * dx + dy * dy + dz * dz),
                        openAir);
                }
            }
        }
        return total;
    }
}
