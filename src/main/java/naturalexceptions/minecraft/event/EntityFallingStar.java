package naturalexceptions.minecraft.event;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import naturalexceptions.core.event.StarRainImpact;
import naturalexceptions.core.id.DeterministicId;
import naturalexceptions.minecraft.persistence.NEWorldSavedData;

/**
 * A fallen star seen in flight: a glowing body that streaks down from high in the sky onto its impact point and
 * then hands over to {@link StarRainWorldMaterializer}. It is presentation and trigger only. It is never saved; if it
 * is lost, the impact stays resolved-but-awaiting and the ordinary retry path still makes its crater.
 */
public final class EntityFallingStar extends Entity implements GlowingStar {

    /** Flight time from first sight to impact. */
    public static final int FLIGHT_TICKS = 80;
    static final double START_HEIGHT = 120.0;
    static final double START_DISTANCE = 70.0;

    private static final int WATCH_SEVERITY = 20;
    /** Server-side: impacts with a star currently in the air, so the retry path does not race it. */
    private static final Set<DeterministicId> IN_FLIGHT = Collections.synchronizedSet(new HashSet<DeterministicId>());

    private DeterministicId impactId;
    private double startX;
    private double startY;
    private double startZ;
    private double targetX;
    private double targetY;
    private double targetZ;
    private int flightAge;

    public EntityFallingStar(World world) {
        super(world);
        setSize(2.0F, 2.0F);
        noClip = true;
        ignoreFrustumCheck = true;
        renderDistanceWeight = 10.0;
        isImmuneToFire = true;
    }

    /** Starts a star on its way to {@code impact}, landing at the given surface height. */
    public static EntityFallingStar launch(World world, StarRainImpact impact, double surfaceY) {
        EntityFallingStar star = new EntityFallingStar(world);
        star.impactId = impact.id();
        star.dataWatcher.updateObject(WATCH_SEVERITY, (byte) impact.severity());
        long seed = Long.parseUnsignedLong(
            impact.id()
                .hex()
                .substring(16, 32),
            16);
        double heading = (seed >>> 11) * 0x1.0p-53 * Math.PI * 2.0;
        star.targetX = impact.x() + 0.5;
        star.targetY = surfaceY;
        star.targetZ = impact.z() + 0.5;
        star.startX = star.targetX - Math.cos(heading) * START_DISTANCE;
        star.startY = star.targetY + START_HEIGHT;
        star.startZ = star.targetZ - Math.sin(heading) * START_DISTANCE;
        star.setLocationAndAngles(star.startX, star.startY, star.startZ, 0.0F, 0.0F);
        star.forceSpawn = true;
        return star;
    }

    public static boolean isInFlight(DeterministicId impact) {
        return impact != null && IN_FLIGHT.contains(impact);
    }

    /** Position along the flight path; gravity-like easing, slow at first, fast at the end. */
    static double progress(int age) {
        double t = Math.max(0.0, Math.min(1.0, age / (double) FLIGHT_TICKS));
        return Math.pow(t, 1.6);
    }

    @Override
    public float starSize() {
        return 0.9F + 0.6F * severity();
    }

    public int severity() {
        return Math.max(1, dataWatcher.getWatchableObjectByte(WATCH_SEVERITY));
    }

    @Override
    protected void entityInit() {
        dataWatcher.addObject(WATCH_SEVERITY, (byte) 1);
    }

    @Override
    public void onUpdate() {
        lastTickPosX = prevPosX = posX;
        lastTickPosY = prevPosY = posY;
        lastTickPosZ = prevPosZ = posZ;
        ticksExisted++;
        if (worldObj.isRemote) {
            spawnTrail();
            return;
        }
        if (impactId == null) {
            setDead();
            return;
        }
        if (flightAge == 0) {
            IN_FLIGHT.add(impactId);
            worldObj.playSoundEffect(startX, startY, startZ, "ambient.weather.thunder", 6.0F, 1.7F);
        }
        flightAge++;
        double p = progress(flightAge);
        double nx = startX + (targetX - startX) * p;
        double ny = startY + (targetY - startY) * p;
        double nz = startZ + (targetZ - startZ) * p;
        motionX = nx - posX;
        motionY = ny - posY;
        motionZ = nz - posZ;
        setPosition(nx, ny, nz);
        if (flightAge >= FLIGHT_TICKS) arrive();
    }

    private void arrive() {
        IN_FLIGHT.remove(impactId);
        setDead();
        NEWorldSavedData saved = NEWorldSavedData.get(worldObj);
        if (saved == null) return;
        StarRainImpact impact = saved.p11()
            .get(impactId);
        if (impact != null && impact.isResolved() && !impact.isMaterialized()) {
            new StarRainWorldMaterializer().materialize(worldObj, saved, impact);
        }
    }

    @Override
    public void setDead() {
        if (impactId != null && !worldObj.isRemote) IN_FLIGHT.remove(impactId);
        super.setDead();
    }

    private void spawnTrail() {
        int size = severity();
        for (int i = 0; i < 2 + size; i++) {
            double jitter = 0.6 * size;
            worldObj.spawnParticle(
                "flame",
                posX + (rand.nextDouble() - 0.5) * jitter,
                posY + (rand.nextDouble() - 0.5) * jitter,
                posZ + (rand.nextDouble() - 0.5) * jitter,
                0.0,
                0.0,
                0.0);
        }
        worldObj.spawnParticle(
            "largesmoke",
            posX + (rand.nextDouble() - 0.5),
            posY + 0.5,
            posZ + (rand.nextDouble() - 0.5),
            0.0,
            0.02,
            0.0);
        if (rand.nextInt(2) == 0) {
            worldObj.spawnParticle(
                "fireworksSpark",
                posX,
                posY,
                posZ,
                (rand.nextDouble() - 0.5) * 0.2,
                0.1,
                (rand.nextDouble() - 0.5) * 0.2);
        }
    }

    /** Never saved: after a restart the impact's crater comes from the awaiting-impact retry instead. */
    @Override
    public boolean writeToNBTOptional(NBTTagCompound tag) {
        return false;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {}

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {}

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isInRangeToRenderDist(double distanceSquared) {
        return distanceSquared < 512.0 * 512.0;
    }

    @Override
    public int getBrightnessForRender(float partialTicks) {
        return 0xF000F0;
    }

    @Override
    public float getBrightness(float partialTicks) {
        return 1.0F;
    }
}
