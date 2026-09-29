package naturalexceptions.minecraft.weapon;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * A Lumenite shard bolt from the Shardcaster: flies straight and fast, passes through up to {@link #PIERCE}
 * creatures, and shatters on stone. Leaves a trail of prismatic sparks.
 */
public final class EntityLumeniteBolt extends EntityThrowable {

    public static final float DAMAGE = 4.0F;
    static final int PIERCE = 3;
    static final int LIFETIME_TICKS = 40;

    private final Set<Integer> struck = new HashSet<>();

    public EntityLumeniteBolt(World world) {
        super(world);
    }

    public EntityLumeniteBolt(World world, EntityLivingBase shooter) {
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

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (worldObj.isRemote) {
            // Prismatic trail: reddust takes its colour from the motion arguments.
            float hue = (ticksExisted * 0.09F) % 1.0F;
            int rgb = java.awt.Color.HSBtoRGB(hue, 0.55F, 1.0F);
            worldObj.spawnParticle(
                "reddust",
                posX,
                posY,
                posZ,
                Math.max(0.01, ((rgb >> 16) & 0xFF) / 255.0),
                ((rgb >> 8) & 0xFF) / 255.0,
                (rgb & 0xFF) / 255.0);
            if (ticksExisted % 2 == 0) worldObj.spawnParticle("fireworksSpark", posX, posY, posZ, 0.0, 0.0, 0.0);
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
            if (struck.size() < PIERCE) return;
        }
        sparkle(12);
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

    @Override
    public int getBrightnessForRender(float partialTicks) {
        return 0xF000F0;
    }

    @Override
    public float getBrightness(float partialTicks) {
        return 1.0F;
    }
}
