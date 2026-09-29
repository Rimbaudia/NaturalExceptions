package naturalexceptions.minecraft.weapon;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import naturalexceptions.minecraft.event.GlowingStar;

/**
 * A small star called down by the Skyfall sword. It streaks from high above onto the struck target and bursts,
 * burning everything close by except its caller. Presentation and effect only: it is never saved.
 */
public final class EntitySkyfallStar extends Entity implements GlowingStar {

    static final int FLIGHT_TICKS = 14;
    static final double START_HEIGHT = 28.0;
    static final double START_OFFSET = 10.0;
    public static final float DAMAGE = 5.0F;
    public static final double RADIUS = 2.6;
    static final int BURN_SECONDS = 3;

    private double startX;
    private double startY;
    private double startZ;
    private double targetX;
    private double targetY;
    private double targetZ;
    private int flightAge;
    private EntityLivingBase caller;

    public EntitySkyfallStar(World world) {
        super(world);
        setSize(0.5F, 0.5F);
        noClip = true;
        ignoreFrustumCheck = true;
        renderDistanceWeight = 6.0;
        isImmuneToFire = true;
    }

    /** A star aimed at (x, y, z), coming in from above and slightly behind the caller. */
    public static EntitySkyfallStar call(World world, EntityLivingBase caller, double x, double y, double z) {
        EntitySkyfallStar star = new EntitySkyfallStar(world);
        star.caller = caller;
        double yaw = Math.toRadians(caller.rotationYaw);
        star.targetX = x;
        star.targetY = y;
        star.targetZ = z;
        star.startX = x + Math.sin(yaw) * START_OFFSET;
        star.startY = y + START_HEIGHT;
        star.startZ = z - Math.cos(yaw) * START_OFFSET;
        star.setLocationAndAngles(star.startX, star.startY, star.startZ, 0.0F, 0.0F);
        return star;
    }

    /** Fraction of the path covered after {@code age} ticks: accelerating, like something falling. */
    static double progress(int age) {
        double t = Math.max(0.0, Math.min(1.0, age / (double) FLIGHT_TICKS));
        return t * t;
    }

    @Override
    public float starSize() {
        return 0.45F;
    }

    @Override
    protected void entityInit() {}

    @Override
    public void onUpdate() {
        lastTickPosX = prevPosX = posX;
        lastTickPosY = prevPosY = posY;
        lastTickPosZ = prevPosZ = posZ;
        ticksExisted++;
        if (worldObj.isRemote) {
            for (int i = 0; i < 3; i++) worldObj.spawnParticle(
                i == 0 ? "fireworksSpark" : "flame",
                posX + (rand.nextDouble() - 0.5) * 0.4,
                posY + (rand.nextDouble() - 0.5) * 0.4,
                posZ + (rand.nextDouble() - 0.5) * 0.4,
                0.0,
                0.02,
                0.0);
            return;
        }
        if (ticksExisted > FLIGHT_TICKS * 3) {
            setDead();
            return;
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
        if (flightAge >= FLIGHT_TICKS) burst();
    }

    private void burst() {
        setDead();
        AxisAlignedBB area = AxisAlignedBB
            .getBoundingBox(posX - RADIUS, posY - RADIUS, posZ - RADIUS, posX + RADIUS, posY + RADIUS, posZ + RADIUS);
        @SuppressWarnings("unchecked")
        List<EntityLivingBase> hit = worldObj.getEntitiesWithinAABB(EntityLivingBase.class, area);
        for (EntityLivingBase living : hit) {
            if (living == caller || living.getDistanceSq(posX, posY, posZ) > RADIUS * RADIUS) continue;
            DamageSource source = new EntityDamageSourceIndirect("naturalexceptions.skyfall", this, caller)
                .setFireDamage();
            living.hurtResistantTime = 0;
            living.attackEntityFrom(source, DAMAGE);
            living.setFire(BURN_SECONDS);
        }
        worldObj.playSoundEffect(
            posX,
            posY,
            posZ,
            "naturalexceptions:weapon.star_impact",
            1.0F,
            0.9F + rand.nextFloat() * 0.2F);
        if (worldObj instanceof WorldServer) {
            WorldServer server = (WorldServer) worldObj;
            server.func_147487_a("largeexplode", posX, posY + 0.3, posZ, 1, 0.0, 0.0, 0.0, 0.0);
            server.func_147487_a("lava", posX, posY + 0.2, posZ, 10, 0.6, 0.2, 0.6, 0.0);
            server.func_147487_a("fireworksSpark", posX, posY + 0.3, posZ, 24, 0.4, 0.4, 0.4, 0.15);
        }
    }

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
    public int getBrightnessForRender(float partialTicks) {
        return 0xF000F0;
    }

    @Override
    public float getBrightness(float partialTicks) {
        return 1.0F;
    }
}
