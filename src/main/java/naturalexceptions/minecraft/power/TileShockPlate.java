package naturalexceptions.minecraft.power;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;

import naturalexceptions.core.power.Grid;

/**
 * A Shock Plate: copper mesh set in the floor. Whatever steps on it closes the circuit through itself and is jolted,
 * the harder the more power the plate can draw; it holds them a moment. (Featherstep Slippers pass over it unfelt.)
 */
public final class TileShockPlate extends TileSource {

    /** The resistance of whatever stands on it, as the circuit sees it. */
    public static final double BODY_OHMS = 3.0;
    public static final int JOLT_TICKS = 10;
    private long touched = -100;
    private int load = -1;
    private double watts;

    /** Something is standing on it. */
    public void touch() {
        long now = worldObj.getTotalWorldTime();
        boolean was = now - touched <= JOLT_TICKS;
        touched = now;
        if (!was) PowerNetworks.dirty(worldObj, xCoord, yCoord, zCoord);
    }

    private boolean occupied() {
        return worldObj.getTotalWorldTime() - touched <= JOLT_TICKS;
    }

    /** How hard a jolt of {@code watts} bites. */
    public static float jolt(double watts) {
        return (float) Math.min(12.0, watts / 25.0);
    }

    @Override
    public void stamp(Grid grid, int node) {
        load = occupied() ? grid.load(node, BODY_OHMS) : -1;
    }

    @Override
    public void powered(Grid grid, int node, double seconds) {
        watts = load >= 0 ? grid.power(load) : 0.0;
        if (watts < 20.0 || worldObj.getTotalWorldTime() % JOLT_TICKS >= PowerNetworks.TICKS) return;
        @SuppressWarnings("unchecked")
        List<EntityLivingBase> on = worldObj.getEntitiesWithinAABB(
            EntityLivingBase.class,
            AxisAlignedBB.getBoundingBox(xCoord, yCoord, zCoord, xCoord + 1, yCoord + 0.5, zCoord + 1));
        for (EntityLivingBase living : on) {
            if (living.doesEntityNotTriggerPressurePlate()) continue;
            living.attackEntityFrom(BlockCable.SHOCK, jolt(watts));
            living.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 20, 3));
        }
        if (!on.isEmpty()) {
            worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.2, zCoord + 0.5, "random.fizz", 0.7F, 1.8F);
            if (worldObj instanceof WorldServer) ((WorldServer) worldObj)
                .func_147487_a("fireworksSpark", xCoord + 0.5, yCoord + 0.3, zCoord + 0.5, 8, 0.3, 0.1, 0.3, 0.08);
        }
    }

    @Override
    public String readout() {
        return occupied() ? String.format("Shock Plate: jolting at %.0f W", watts)
            : "Shock Plate: armed, nothing on it";
    }
}
