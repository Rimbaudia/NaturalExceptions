package naturalexceptions.minecraft.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * A strand of Cloudsilk, spat by a Cloudweaver or shot from a Silkspinner: it flies nearly straight, trailing vapour,
 * and binds what it strikes (Slowness {@link #BIND_AMPLIFIER} + 1 for {@link #BIND_TICKS} ticks), doing it no harm
 * itself. It leaves nothing behind.
 */
public final class EntityCloudsilk extends EntityThrowable {

    public static final int BIND_TICKS = 30, BIND_AMPLIFIER = 3;

    public EntityCloudsilk(World world) {
        super(world);
    }

    public EntityCloudsilk(World world, EntityLivingBase thrower) {
        super(world, thrower);
    }

    /** Binds a creature as a strand does. */
    public static void bind(EntityLivingBase target) {
        target.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, BIND_TICKS, BIND_AMPLIFIER));
    }

    @Override
    protected void onImpact(MovingObjectPosition hit) {
        if (worldObj.isRemote) return;
        if (hit.entityHit instanceof EntityLivingBase && hit.entityHit != getThrower()) {
            bind((EntityLivingBase) hit.entityHit);
            worldObj.playSoundAtEntity(hit.entityHit, "step.cloth", 1.0F, 1.4F);
        }
        if (worldObj instanceof WorldServer)
            ((WorldServer) worldObj).func_147487_a("cloud", posX, posY, posZ, 10, 0.25, 0.25, 0.25, 0.02);
        setDead();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (worldObj.isRemote) worldObj.spawnParticle("cloud", posX, posY, posZ, 0.0, 0.0, 0.0);
    }

    /** Light as vapour: it hardly drops. */
    @Override
    protected float getGravityVelocity() {
        return 0.008F;
    }
}
