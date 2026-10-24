package naturalexceptions.minecraft.power;

import java.util.List;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.IMob;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;

import naturalexceptions.core.power.Grid;

/**
 * A Tesla Coil: a tall winding that charges a capacitor from its network and, full enough, throws an arc at the
 * nearest hostile creature in reach. Each arc spends {@link #ARC_JOULES}; it never strikes players or tame things.
 */
public final class TileTeslaCoil extends TileSource {

    public static final double CAPACITY = 20_000.0, ARC_JOULES = 2_000.0, CHARGING_OHMS = 1.2;
    public static final double REACH = 8.0;
    public static final float ARC_DAMAGE = 9.0F;
    public static final int ARC_COOLDOWN = 20;

    private double stored;
    private int load = -1;
    private double watts;
    private long lastArc = -100;

    @Override
    public void stamp(Grid grid, int node) {
        load = stored < CAPACITY ? grid.load(node, CHARGING_OHMS) : -1;
    }

    @Override
    public void powered(Grid grid, int node, double seconds) {
        watts = load >= 0 ? grid.power(load) : 0.0;
        stored = Math.min(CAPACITY, stored + watts * seconds);
        markDirty();
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (worldObj == null || worldObj.isRemote) return;
        long now = worldObj.getTotalWorldTime();
        if (stored < ARC_JOULES || now - lastArc < ARC_COOLDOWN || now % 5 != 0) return;
        @SuppressWarnings("unchecked")
        List<EntityLiving> near = worldObj.getEntitiesWithinAABB(
            EntityLiving.class,
            AxisAlignedBB.getBoundingBox(xCoord, yCoord, zCoord, xCoord + 1, yCoord + 2, zCoord + 1)
                .expand(REACH, REACH, REACH));
        EntityLiving best = null;
        double bestD = REACH * REACH;
        for (EntityLiving e : near) {
            if (!(e instanceof IMob) || e.isDead) continue;
            double d = e.getDistanceSq(xCoord + 0.5, yCoord + 2.0, zCoord + 0.5);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        if (best != null) arc(best);
    }

    private void arc(EntityLiving target) {
        lastArc = worldObj.getTotalWorldTime();
        stored -= ARC_JOULES;
        target.attackEntityFrom(BlockCable.SHOCK, ARC_DAMAGE);
        target.setFire(2);
        double x0 = xCoord + 0.5, y0 = yCoord + 1.9, z0 = zCoord + 0.5;
        double x1 = target.posX, y1 = target.posY + target.height * 0.6, z1 = target.posZ;
        if (worldObj instanceof WorldServer) {
            int steps = (int) Math
                .ceil(Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0) + (z1 - z0) * (z1 - z0)) * 3);
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                double jitter = Math.sin(t * Math.PI) * 0.3;
                ((WorldServer) worldObj).func_147487_a(
                    "fireworksSpark",
                    x0 + (x1 - x0) * t + (worldObj.rand.nextDouble() - 0.5) * jitter,
                    y0 + (y1 - y0) * t + (worldObj.rand.nextDouble() - 0.5) * jitter,
                    z0 + (z1 - z0) * t + (worldObj.rand.nextDouble() - 0.5) * jitter,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0);
            }
        }
        worldObj.playSoundEffect(x0, y0, z0, "fireworks.blast", 1.2F, 1.6F);
        worldObj.playSoundEffect(x1, y1, z1, "random.fizz", 0.8F, 1.6F);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setDouble("Stored", stored);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        stored = tag.getDouble("Stored");
    }

    @Override
    public String readout() {
        return String.format(
            "Tesla Coil: %.0f%% charged (%.0f arcs), drawing %.0f W",
            stored / CAPACITY * 100.0,
            Math.floor(stored / ARC_JOULES),
            watts);
    }
}
