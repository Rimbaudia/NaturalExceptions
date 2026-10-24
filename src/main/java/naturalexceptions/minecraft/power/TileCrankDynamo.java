package naturalexceptions.minecraft.power;

import java.lang.ref.WeakReference;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

import naturalexceptions.core.power.Grid;

/**
 * A crank dynamo: turn its handle (hold use on it) and its armature spins up, its EMF rising with its speed, up to
 * {@link #EMF} volts; let go and it spins down. What it delivers is work out of whoever turns it: the harder its
 * network draws, the hungrier they get.
 */
public final class TileCrankDynamo extends TileEntity implements PowerDevice, PowerReadout {

    public static final double EMF = 24.0, INTERNAL_OHMS = 0.8;
    /** Hunger (exhaustion) per joule of work turned out of a player. */
    public static final double EXHAUSTION_PER_JOULE = 0.0012;
    static final int CRANK_GAP = 6;

    private double speed;
    private long lastCrank = -100;
    private WeakReference<EntityPlayer> cranker = new WeakReference<>(null);
    private int source = -1;
    private double volts, amps;
    private boolean known;

    /** Turned once more by {@code player}. */
    public void crank(EntityPlayer player) {
        lastCrank = worldObj.getTotalWorldTime();
        cranker = new WeakReference<>(player);
    }

    public double speed() {
        return speed;
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote) return;
        if (!known) {
            known = true;
            PowerNetworks.dirty(worldObj, xCoord, yCoord, zCoord);
        }
        boolean turning = worldObj.getTotalWorldTime() - lastCrank <= CRANK_GAP;
        speed = turning ? Math.min(1.0, speed + 0.06) : Math.max(0.0, speed - 0.04);
        if (turning && worldObj.getTotalWorldTime() % 5 == 0) {
            worldObj.playSoundEffect(
                xCoord + 0.5,
                yCoord + 0.5,
                zCoord + 0.5,
                "random.click",
                0.25F,
                0.6F + (float) speed * 0.8F);
            if (worldObj instanceof WorldServer && amps > 1.0) ((WorldServer) worldObj)
                .func_147487_a("crit", xCoord + 0.5, yCoord + 0.7, zCoord + 0.5, 2, 0.2, 0.1, 0.2, 0.05);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (worldObj != null) PowerNetworks.dirty(worldObj, xCoord, yCoord, zCoord);
    }

    @Override
    public void stamp(Grid grid, int node) {
        source = speed > 0.01 ? grid.source(node, EMF * speed, INTERNAL_OHMS) : -1;
    }

    @Override
    public void powered(Grid grid, int node, double seconds) {
        volts = grid.voltage(node);
        amps = source >= 0 ? grid.sourceCurrent(source) : 0.0;
        // The work it takes to turn: its EMF times the current it drives (its own losses included).
        double work = source >= 0 ? Math.max(0.0, EMF * speed * amps) * seconds : 0.0;
        EntityPlayer player = cranker.get();
        if (work > 0.0 && player != null && !player.capabilities.isCreativeMode)
            player.addExhaustion((float) (work * EXHAUSTION_PER_JOULE));
    }

    @Override
    public String readout() {
        return String.format(
            "Crank Dynamo: %.0f%% speed, %.1f V at its terminal, driving %.1f A (%.0f W)",
            speed * 100.0,
            volts,
            amps,
            volts * amps);
    }
}
