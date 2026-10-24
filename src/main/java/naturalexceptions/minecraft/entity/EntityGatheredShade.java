package naturalexceptions.minecraft.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/**
 * A shade torn loose from the Gathered: a pale face trailing a wisp of shroud, flying at whoever troubles its rest. It
 * is weak alone, and fades when the Gathered is laid to rest.
 */
public final class EntityGatheredShade extends EntityMob {

    public static final float MAX_HEALTH = 8.0F;
    public static final float TOUCH = 3.0F;
    private int touchCooldown;
    private int hostId = -1;

    public EntityGatheredShade(World world) {
        super(world);
        setSize(0.6F, 0.6F);
        isImmuneToFire = true;
        experienceValue = 3;
    }

    public EntityGatheredShade(World world, EntityGathered host) {
        this(world);
        hostId = host.getEntityId();
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(MAX_HEALTH);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(24.0);
    }

    @Override
    public EnumCreatureAttribute getCreatureAttribute() {
        return EnumCreatureAttribute.UNDEAD;
    }

    @Override
    protected void fall(float distance) {}

    @Override
    protected void updateFallState(double y, boolean onGround) {}

    /** It flies: no weight, only drift. */
    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        moveFlying(strafe, forward, 0.02F);
        moveEntity(motionX, motionY, motionZ);
        motionX *= 0.9;
        motionY *= 0.9;
        motionZ *= 0.9;
    }

    /** It steers itself (see onLivingUpdate), not by walking paths. */
    @Override
    protected void updateEntityActionState() {}

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (worldObj.isRemote) {
            if (rand.nextInt(2) == 0) worldObj.spawnParticle(
                "instantSpell",
                posX + (rand.nextDouble() - 0.5) * 0.4,
                posY + 0.2,
                posZ + (rand.nextDouble() - 0.5) * 0.4,
                0.0,
                0.0,
                0.0);
            return;
        }
        // It fades with the one it tore from.
        if (hostId >= 0) {
            Entity host = worldObj.getEntityByID(hostId);
            if (host == null || host.isDead) {
                setDead();
                return;
            }
        } else if (ticksExisted > 20) {
            setDead(); // loaded without its host: it does not linger
            return;
        }
        if (touchCooldown > 0) touchCooldown--;
        EntityPlayer target = worldObj.getClosestVulnerablePlayerToEntity(this, 24.0);
        if (target == null) return;
        double dx = target.posX - posX, dy = target.posY + 1.0 - posY, dz = target.posZ - posZ;
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d > 0.5) {
            motionX += dx / d * 0.03;
            motionY += dy / d * 0.04;
            motionZ += dz / d * 0.03;
        }
        faceEntity(target, 30.0F, 30.0F);
        if (d < 1.2 && touchCooldown == 0) {
            target.attackEntityFrom(DamageSource.causeMobDamage(this), TOUCH);
            touchCooldown = 25;
            worldObj.playSoundAtEntity(this, "mob.ghast.scream", 0.4F, 1.6F);
        }
    }

    @Override
    protected String getLivingSound() {
        return "mob.ghast.moan";
    }

    @Override
    protected float getSoundVolume() {
        return 0.3F;
    }

    @Override
    protected float getSoundPitch() {
        return 1.6F + rand.nextFloat() * 0.2F;
    }

    @Override
    protected String getHurtSound() {
        return "mob.ghast.scream";
    }

    @Override
    protected String getDeathSound() {
        return "mob.ghast.death";
    }

    @Override
    protected Item getDropItem() {
        return null;
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    protected boolean isValidLightLevel() {
        return true;
    }
}
