package naturalexceptions.minecraft.power;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * An arc: a high-tension line broken while it carries current (a cable cut, a switch or a rack's lever thrown under
 * load) does not just stop. The current jumps the gap in a white flash that burns whoever made it and can set
 * what is near alight.
 */
public final class Arc {

    /** Below this a broken line only sparks. */
    public static final double VOLTS = 60.0;
    /** Below this there is too little current to keep an arc going. */
    public static final double AMPS = 5.0;
    /** What it does to whoever drew it, at most. */
    public static final float MAX_DAMAGE = 8.0F;

    private Arc() {}

    /** Whether a line breaking at {@code volts} while carrying {@code amps} arcs. */
    public static boolean strikes(double volts, double amps) {
        return Math.abs(volts) >= VOLTS && Math.abs(amps) >= AMPS;
    }

    /** How badly the arc burns its maker: worse with more power behind it. */
    public static float damage(double volts, double amps) {
        double kilowatts = Math.abs(volts * amps) / 1000.0;
        return (float) Math.min(MAX_DAMAGE, 2.0 + kilowatts);
    }

    /** It arcs at (x, y, z), if it would; {@code maker} (who broke the line) takes the burn. Whether it did. */
    public static boolean at(World world, int x, int y, int z, double volts, double amps, EntityLivingBase maker) {
        if (world == null || world.isRemote || !strikes(volts, amps)) return false;
        world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, "naturalexceptions:power.arc", 1.5F, 0.9F + world.rand.nextFloat() * 0.2F);
        if (world instanceof WorldServer) {
            WorldServer server = (WorldServer) world;
            server.func_147487_a("fireworksSpark", x + 0.5, y + 0.5, z + 0.5, 30, 0.2, 0.2, 0.2, 0.15);
            server.func_147487_a("lava", x + 0.5, y + 0.5, z + 0.5, 6, 0.2, 0.2, 0.2, 0.0);
        }
        if (maker != null && maker.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) < 16.0) {
            maker.attackEntityFrom(BlockCable.SHOCK, damage(volts, amps));
            maker.setFire(2);
        }
        // Something burnable beside it may catch.
        for (int[] f : new int[][] { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } }) {
            int fx = x + f[0], fy = y + f[1], fz = z + f[2];
            if (!world.isAirBlock(fx, fy, fz) || world.rand.nextInt(3) != 0) continue;
            Block below = world.getBlock(fx, fy - 1, fz);
            if (below.isFlammable(world, fx, fy - 1, fz, net.minecraftforge.common.util.ForgeDirection.UP))
                world.setBlock(fx, fy, fz, Blocks.fire);
        }
        return true;
    }
}
