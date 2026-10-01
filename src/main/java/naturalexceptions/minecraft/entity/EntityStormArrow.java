package naturalexceptions.minecraft.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.world.World;

/**
 * A Stormbone Archer's arrow, tipped with Stormglass: it flies fast and straight, trailing sparks, and what it strikes
 * is shocked, slowed for a moment ({@link SkyMobEvents}). It shatters on striking; there is nothing to pick up.
 */
public final class EntityStormArrow extends EntityArrow {

    /** How fast it leaves the bow, and how little it strays. */
    public static final float SPEED = 2.4F, SPREAD = 2.0F;
    /** The shock: Slowness II for this long. */
    public static final int SHOCK_TICKS = 40, SHOCK_AMPLIFIER = 1;

    public EntityStormArrow(World world) {
        super(world);
        canBePickedUp = 0;
    }

    public EntityStormArrow(World world, EntityLivingBase shooter, EntityLivingBase target) {
        super(world, shooter, target, SPEED, SPREAD);
        canBePickedUp = 0;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (worldObj.isRemote && !isDead && ticksExisted > 1 && motionX * motionX + motionZ * motionZ > 0.01)
            worldObj.spawnParticle("fireworksSpark", posX, posY, posZ, 0.0, 0.0, 0.0);
        if (!worldObj.isRemote && ticksExisted > 20 && motionX * motionX + motionY * motionY + motionZ * motionZ < 1.0E-4)
            setDead(); // stuck fast: it has shattered
    }
}
