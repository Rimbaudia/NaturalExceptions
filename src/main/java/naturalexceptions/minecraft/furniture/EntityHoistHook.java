package naturalexceptions.minecraft.furniture;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;

/**
 * A quarry hoist's basket on its rope, with its rider in it, worked as a derrick works a load: boarded at the lip
 * under the boom beside the windlass, hoisted clear, run out along under the boom over the pit, and lowered to the
 * floor; or the same way back up. The rope hangs from the boom down to the basket all the way. The basket block at the
 * foot goes with it, and is back in its place when the ride is over.
 * <p>
 * The ride is worked out from its path and the ticks since it began, the same way on the server and on the client
 * (the path is sent once, as it spawns), so the client moves it itself and never waits on, or jumps to, the
 * server's word. It eases off and on at either end and takes the corner over the lip in a curve. Like a seat, it is
 * never saved: it lasts only for the ride.
 */
public final class EntityHoistHook extends Entity implements IEntityAdditionalSpawnData {

    /** Average speed, in blocks a tick. */
    static final double SPEED = 0.16;
    /** How much of each leg, at most, the curve at a turn takes. */
    private static final double CORNER = 1.0;
    /** How high the basket is hoisted off the lip before it is run out over the pit. */
    private static final double HOIST = 1.5;

    /** The rider's feet at each point of the ride, start to end: {x, y, z} each. */
    private double[][] path;
    /** The basket block at the foot, hidden while it rides; and the height its rope hangs from. */
    private int basketX, basketY, basketZ;
    double ropeTop;
    private int duration;

    public EntityHoistHook(World world) {
        super(world);
        setSize(0.0F, 0.0F);
        noClip = true;
        ignoreFrustumCheck = true; // the basket and its rope reach far from this point at the rider's feet
    }

    /**
     * A ride between a windlass and its basket, starting at the windlass's end when {@code fromTop}. The top of the
     * ride is on the lip beside the windlass, under the boom that runs out over the basket; the rope hangs from
     * {@code ropeTop}.
     */
    static EntityHoistHook between(World world, int[] windlass, int[] basket, boolean fromTop, int ropeTop) {
        int dx = basket[0] - windlass[0], dz = basket[2] - windlass[2];
        boolean alongX = Math.abs(dx) >= Math.abs(dz);
        // The lip under the boom: a step from the windlass towards the basket, in line with the boom.
        double[] lip = alongX ? new double[] { windlass[0] + Integer.signum(dx) + 0.5, windlass[1], basket[2] + 0.5 }
            : new double[] { basket[0] + 0.5, windlass[1], windlass[2] + Integer.signum(dz) + 0.5 };
        double[] hoisted = { lip[0], lip[1] + HOIST, lip[2] };
        double[] over = { basket[0] + 0.5, lip[1] + HOIST, basket[2] + 0.5 };
        double[] foot = { basket[0] + 0.5, basket[1] + 0.1, basket[2] + 0.5 };
        EntityHoistHook hook = new EntityHoistHook(world);
        hook.path = fromTop ? new double[][] { lip, hoisted, over, foot } : new double[][] { foot, over, hoisted, lip };
        hook.basketX = basket[0];
        hook.basketY = basket[1];
        hook.basketZ = basket[2];
        hook.ropeTop = ropeTop;
        hook.start();
        return hook;
    }

    private void start() {
        duration = (int) Math.ceil(length() / SPEED * 1.25); // easing in and out takes a quarter longer
        at(feetAt(0.0));
    }

    /** Puts the hook so that its rider's feet are at {@code feet}. */
    private void at(double[] feet) {
        setPosition(feet[0], feet[1] + 0.5, feet[2]);
    }

    /** The curve's reach either side of turn {@code i} (an inner point of the path). */
    private double radius(int i) {
        return Math.min(CORNER, Math.min(distance(path[i - 1], path[i]), distance(path[i], path[i + 1])) / 2.0);
    }

    /**
     * The ride as pieces: straight runs and the curves between them, {from, control, to} each (null control: straight).
     */
    private double[][][] pieces() {
        int n = path.length;
        double[][][] out = new double[2 * n - 3][][];
        double[] from = path[0];
        for (int i = 1; i < n - 1; i++) {
            double r = radius(i);
            double[] in = towards(path[i], path[i - 1], r), next = towards(path[i], path[i + 1], r);
            out[2 * i - 2] = new double[][] { from, null, in };
            out[2 * i - 1] = new double[][] { in, path[i], next };
            from = next;
        }
        out[2 * n - 4] = new double[][] { from, null, path[n - 1] };
        return out;
    }

    private static double pieceLength(double[][] piece) {
        if (piece[1] == null) return distance(piece[0], piece[2]);
        // A quarter-turn's arc, near enough, for the short curves at the turns.
        return (distance(piece[0], piece[1]) + distance(piece[1], piece[2])) * Math.PI / 4.0;
    }

    private double length() {
        double total = 0;
        for (double[][] piece : pieces()) total += pieceLength(piece);
        return total;
    }

    /** The rider's feet {@code ticks} into the ride (fractions between ticks too). */
    double[] feetAt(double ticks) {
        double t = Math.max(0.0, Math.min(1.0, ticks / duration));
        double s = t * t * (3.0 - 2.0 * t) * length();
        double[][][] pieces = pieces();
        for (double[][] piece : pieces) {
            double len = pieceLength(piece);
            if (s > len) {
                s -= len;
                continue;
            }
            double u = len <= 0 ? 1.0 : s / len;
            if (piece[1] == null) return towards(piece[0], piece[2], s);
            double[] p = new double[3];
            for (int i = 0; i < 3; i++)
                p[i] = (1 - u) * (1 - u) * piece[0][i] + 2 * (1 - u) * u * piece[1][i] + u * u * piece[2][i];
            return p;
        }
        return path[path.length - 1].clone();
    }

    private static double distance(double[] a, double[] b) {
        double x = b[0] - a[0], y = b[1] - a[1], z = b[2] - a[2];
        return Math.sqrt(x * x + y * y + z * z);
    }

    /** The point {@code by} blocks from {@code from} towards {@code to}. */
    private static double[] towards(double[] from, double[] to, double by) {
        double d = distance(from, to);
        if (d <= 0) return from.clone();
        double f = Math.min(1.0, by / d);
        return new double[] { from[0] + (to[0] - from[0]) * f, from[1] + (to[1] - from[1]) * f,
            from[2] + (to[2] - from[2]) * f };
    }

    /** Drawn out to 64 blocks: by its size (none) it would never be near enough to draw at all. */
    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public boolean isInRangeToRenderDist(double distanceSq) {
        return distanceSq < 64.0 * 64.0;
    }

    @Override
    protected void entityInit() {}

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (path == null) {
            if (!worldObj.isRemote) setDead();
            return;
        }
        at(feetAt(ticksExisted));
        if (worldObj.isRemote) return;
        if (ticksExisted == 1) showBasket(false);
        if (riddenByEntity == null) {
            finish();
            return;
        }
        riddenByEntity.fallDistance = 0.0F;
        if (ticksExisted % 12 == 1)
            worldObj.playSoundAtEntity(this, "random.door_open", 0.25F, 0.5F + rand.nextFloat() * 0.1F);
        if (ticksExisted >= duration) arrive();
    }

    /** Sets the rider down at the end of the ride, on its feet. */
    private void arrive() {
        Entity rider = riddenByEntity;
        double[] end = path[path.length - 1];
        if (rider != null) {
            rider.mountEntity(null);
            if (rider instanceof EntityPlayerMP) ((EntityPlayerMP) rider).setPositionAndUpdate(end[0], end[1], end[2]);
            else rider.setPosition(end[0], end[1], end[2]);
            rider.fallDistance = 0.0F;
        }
        worldObj.playSoundAtEntity(this, "random.wood_click", 0.6F, 0.8F);
        finish();
    }

    private void finish() {
        showBasket(true);
        setDead();
    }

    /** The basket block at the foot: there (meta 0), or away on the rope (meta 1). */
    private void showBasket(boolean there) {
        Block block = worldObj.getBlock(basketX, basketY, basketZ);
        if (block instanceof BlockHoist && !((BlockHoist) block).top)
            worldObj.setBlockMetadataWithNotify(basketX, basketY, basketZ, there ? 0 : 1, 2);
    }

    /** The server's word on where the hook is: the client works that out itself, so it is not needed. */
    @Override
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int increments) {}

    @Override
    public void setVelocity(double x, double y, double z) {}

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeInt(path.length);
        for (double[] p : path) for (double v : p) buffer.writeDouble(v);
        buffer.writeInt(basketX);
        buffer.writeInt(basketY);
        buffer.writeInt(basketZ);
        buffer.writeDouble(ropeTop);
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        path = new double[buffer.readInt()][3];
        for (double[] p : path) for (int i = 0; i < 3; i++) p[i] = buffer.readDouble();
        basketX = buffer.readInt();
        basketY = buffer.readInt();
        basketZ = buffer.readInt();
        ropeTop = buffer.readDouble();
        start();
        prevPosX = lastTickPosX = posX;
        prevPosY = lastTickPosY = posY;
        prevPosZ = lastTickPosZ = posZ;
    }

    /** Where the rider's feet are now, between ticks: for drawing the basket under them. */
    double[] feetBetweenTicks(float partialTicks) {
        return path == null ? new double[] { posX, posY - 0.5, posZ } : feetAt(ticksExisted - 1 + partialTicks);
    }

    @Override
    public double getMountedYOffset() {
        return 0.0;
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {}

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {}

    /** Hooks are made again for each ride; none is saved with the world. */
    @Override
    public boolean writeToNBTOptional(NBTTagCompound tag) {
        return false;
    }
}
