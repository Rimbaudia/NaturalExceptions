package naturalexceptions.minecraft.weapon;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.item.Item;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import naturalexceptions.minecraft.registry.NEItems;

/**
 * A Stormglass shard bolt from the Shardcaster: flies straight and fast, passes through up to {@link #PIERCE}
 * creatures, and shatters into fragments where it stops. Leaves a pale violet ribbon, crackling.
 */
public final class EntityStormglassBolt extends EntityThrowable {

    public static final float DAMAGE = 4.0F;
    static final int PIERCE = 3;
    static final int LIFETIME_TICKS = 40;

    private final Set<Integer> struck = new HashSet<>();

    public EntityStormglassBolt(World world) {
        super(world);
    }

    public EntityStormglassBolt(World world, EntityLivingBase shooter) {
        super(world, shooter);
    }

    @Override
    protected float func_70182_d() {
        return 2.6F;
    }

    @Override
    protected float getGravityVelocity() {
        return 0.0F;
    }

    /** Where the bolt has been, newest first ({x, y, z} each): the client draws its ribbon trail from these. */
    public final double[][] trail = new double[TRAIL][];
    public static final int TRAIL = 7;

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (worldObj.isRemote) {
            System.arraycopy(trail, 0, trail, 1, TRAIL - 1);
            trail[0] = new double[] { posX, posY, posZ };
            // A few pale motes behind the ribbon: reddust takes its colour from the motion arguments.
            if (ticksExisted % 2 == 0) {
                float hue = 0.76F + 0.04F * (float) Math.sin(ticksExisted * 0.6);
                int rgb = java.awt.Color.HSBtoRGB(hue, 0.4F, 1.0F);
                worldObj.spawnParticle(
                    "reddust",
                    posX,
                    posY,
                    posZ,
                    Math.max(0.01, ((rgb >> 16) & 0xFF) / 255.0),
                    ((rgb >> 8) & 0xFF) / 255.0,
                    (rgb & 0xFF) / 255.0);
            }
            if (ticksExisted % 5 == 0) worldObj.spawnParticle("fireworksSpark", posX, posY, posZ, 0.0, 0.0, 0.0);
        } else if (ticksExisted > LIFETIME_TICKS) {
            setDead();
        }
    }

    @Override
    protected void onImpact(MovingObjectPosition hit) {
        if (worldObj.isRemote) return;
        if (hit.entityHit != null) {
            Entity target = hit.entityHit;
            if (target == getThrower() || !struck.add(target.getEntityId())) return;
            target.attackEntityFrom(
                new EntityDamageSourceIndirect("naturalexceptions.shard", this, getThrower()).setProjectile(),
                DAMAGE);
            sparkle(4);
            shatter(6, 0.1);
            if (struck.size() < PIERCE) return;
        }
        sparkle(12);
        shatter(16, hit.entityHit == null ? 0.2 : 0.12);
        worldObj.playSoundEffect(
            posX,
            posY,
            posZ,
            "naturalexceptions:weapon.shard_hit",
            0.8F,
            0.9F + rand.nextFloat() * 0.3F);
        setDead();
    }

    private void sparkle(int count) {
        if (worldObj instanceof WorldServer)
            ((WorldServer) worldObj).func_147487_a("fireworksSpark", posX, posY, posZ, count, 0.15, 0.15, 0.15, 0.08);
    }

    /** The crystal breaking into its own fragments, which fall and bounce, and a ring of sparks round it. */
    private void shatter(int count, double speed) {
        if (!(worldObj instanceof WorldServer)) return;
        WorldServer world = (WorldServer) worldObj;
        world.func_147487_a(
            "iconcrack_" + Item.getIdFromItem(NEItems.stormglassShard),
            posX,
            posY,
            posZ,
            count,
            0.1,
            0.1,
            0.1,
            speed);
        for (int i = 0; i < 10; i++) {
            double a = i * Math.PI / 5;
            world.func_147487_a(
                "fireworksSpark",
                posX + Math.cos(a) * 0.3,
                posY,
                posZ + Math.sin(a) * 0.3,
                1,
                0.0,
                0.0,
                0.0,
                0.02);
        }
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
