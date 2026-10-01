package naturalexceptions.minecraft.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import naturalexceptions.minecraft.registry.NEBlocks;

/**
 * A Gale Husk: one of the sky's dead, dried to a husk by the wind and wrapped in what is left of its clothes. From a
 * few blocks off it crouches (dust and air whirling at its feet) and springs at you on a gust; its blows throw you
 * back hard, which on an island's rim is the worst of it. It has no fear of falling and takes no harm from it.
 */
public final class EntityGaleHusk extends EntityMob {

    public static final float MAX_HEALTH = 22.0F;
    /** How near and how far off it leaps from. */
    public static final double LEAP_NEAR = 3.0, LEAP_FAR = 8.0;
    static final int CROUCH = 8, LEAP_COOLDOWN = 60;
    static final double LEAP_ACROSS = 0.9, LEAP_UP = 0.45, BLOW_ACROSS = 0.9, BLOW_UP = 0.35;

    private static final int CROUCHING = 16;
    private int crouch, cooldown = LEAP_COOLDOWN / 2;

    public EntityGaleHusk(World world) {
        super(world);
        setSize(0.6F, 1.8F);
        experienceValue = 6;
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIAttackOnCollide(this, EntityPlayer.class, 1.0, false));
        tasks.addTask(6, new EntityAIWander(this, 0.8));
        tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 10.0F));
        tasks.addTask(8, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true));
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(CROUCHING, (byte) 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(MAX_HEALTH);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.26);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(4.0);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(24.0);
    }

    @Override
    public boolean isAIEnabled() {
        return true;
    }

    @Override
    public int getTotalArmorValue() {
        return Math.max(2, super.getTotalArmorValue());
    }

    @Override
    public EnumCreatureAttribute getCreatureAttribute() {
        return EnumCreatureAttribute.UNDEAD;
    }

    /** Whether it is gathering itself to leap (for the model and the gust at its feet). */
    public boolean crouching() {
        return dataWatcher.getWatchableObjectByte(CROUCHING) != 0;
    }

    /** Whether a target {@code distance} away is in its leaping range. */
    public static boolean inLeap(double distance) {
        return distance >= LEAP_NEAR && distance <= LEAP_FAR;
    }

    // --- The leap -------------------------------------------------------------------------------------------

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (worldObj.isRemote) {
            if (crouching()) for (int i = 0; i < 2; i++) worldObj.spawnParticle(
                "cloud",
                posX + (rand.nextDouble() - 0.5) * 1.2,
                posY + 0.1,
                posZ + (rand.nextDouble() - 0.5) * 1.2,
                0.0,
                0.05,
                0.0);
            return;
        }
        if (cooldown > 0) cooldown--;
        EntityLivingBase target = getAttackTarget();
        if (crouch > 0) {
            getNavigator().clearPathEntity();
            if (target != null) getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
            if (--crouch == 0) {
                dataWatcher.updateObject(CROUCHING, (byte) 0);
                if (target != null) leapAt(target);
                cooldown = LEAP_COOLDOWN;
            }
            return;
        }
        if (target != null && cooldown == 0 && onGround && inLeap(getDistanceToEntity(target))) {
            crouch = CROUCH;
            dataWatcher.updateObject(CROUCHING, (byte) 1);
            worldObj.playSoundAtEntity(this, "mob.enderdragon.wings", 0.4F, 1.6F);
        }
    }

    private void leapAt(EntityLivingBase target) {
        double dx = target.posX - posX, dz = target.posZ - posZ, d = Math.sqrt(dx * dx + dz * dz);
        if (d < 1.0E-3) return;
        motionX = dx / d * LEAP_ACROSS;
        motionZ = dz / d * LEAP_ACROSS;
        motionY = LEAP_UP;
        isAirBorne = true;
        worldObj.playSoundAtEntity(this, "mob.ghast.fireball", 0.3F, 1.8F);
    }

    /** Its blows throw the struck far back, on top of the blow's own knockback. */
    @Override
    public boolean attackEntityAsMob(Entity target) {
        boolean hit = super.attackEntityAsMob(target);
        if (hit) {
            double yaw = Math.toRadians(rotationYaw);
            target.addVelocity(-Math.sin(yaw) * BLOW_ACROSS, BLOW_UP, Math.cos(yaw) * BLOW_ACROSS);
            target.velocityChanged = true;
        }
        return hit;
    }

    /** The wind has it: no fall hurts it. */
    @Override
    protected void fall(float distance) {}

    /** It stands where it was made (a spawner, an alcove), lit or not. */
    @Override
    public boolean getCanSpawnHere() {
        return worldObj.checkNoEntityCollision(boundingBox) && worldObj.getCollidingBoundingBoxes(this, boundingBox)
            .isEmpty() && !worldObj.isAnyLiquid(boundingBox);
    }

    // --- Drops ----------------------------------------------------------------------------------------------

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        for (ItemStack stack : drops(rand, looting)) entityDropItem(stack, 0.5F);
    }

    /** Rotten flesh and feathers; now and then a Windbloom caught in its wrappings. */
    public static List<ItemStack> drops(Random random, int looting) {
        List<ItemStack> drops = new ArrayList<>();
        int flesh = random.nextInt(3 + looting), feathers = random.nextInt(3 + looting);
        if (flesh > 0) drops.add(new ItemStack(Items.rotten_flesh, flesh));
        if (feathers > 0) drops.add(new ItemStack(Items.feather, feathers));
        if (NEBlocks.windbloom != null && random.nextDouble() < 0.05)
            drops.add(new ItemStack(Item.getItemFromBlock(NEBlocks.windbloom)));
        return drops;
    }

    @Override
    protected Item getDropItem() {
        return null;
    }

    // --- Sounds ---------------------------------------------------------------------------------------------

    @Override
    protected String getLivingSound() {
        return "mob.zombie.say";
    }

    @Override
    protected String getHurtSound() {
        return "mob.zombie.hurt";
    }

    @Override
    protected String getDeathSound() {
        return "mob.zombie.death";
    }

    @Override
    protected float getSoundPitch() {
        return 1.25F + rand.nextFloat() * 0.1F; // dry and thin
    }
}
