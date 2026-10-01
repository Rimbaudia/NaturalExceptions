package naturalexceptions.minecraft.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * A Cloudweaver: a long-legged spider of the sky, grey-white as a thunderhead, its eyes violet. It climbs as any spider
 * does and pounces, but it hunts by day as well as by night; and from a distance ({@link #SPIT_NEAR} to
 * {@link #SPIT_FAR} blocks) it rears and spits a strand of Cloudsilk that binds you where you stand for its pounce.
 * Its silk is worth the having.
 */
public final class EntityCloudweaver extends EntitySpider {

    public static final float MAX_HEALTH = 20.0F;
    public static final double SPIT_NEAR = 4.0, SPIT_FAR = 14.0;
    public static final int SPIT_COOLDOWN = 80;
    public static final double CLOUD_CHANCE = 0.1, SPINNER_CHANCE = 0.02;

    private int spit = SPIT_COOLDOWN / 2;

    public EntityCloudweaver(World world) {
        super(world);
        experienceValue = 8;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(MAX_HEALTH);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.32);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(3.0);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(24.0);
    }

    /** Whether a target {@code distance} off is in reach of its spit. */
    public static boolean inSpit(double distance) {
        return distance >= SPIT_NEAR && distance <= SPIT_FAR;
    }

    /** Day or night, it hunts. */
    @Override
    protected Entity findPlayerToAttack() {
        return worldObj.getClosestVulnerablePlayerToEntity(this, 16.0);
    }

    /** A pounce from a few blocks off, a bite close in; the light does not put it off. */
    @Override
    protected void attackEntity(Entity target, float distance) {
        if (distance > 2.0F && distance < 6.0F && rand.nextInt(10) == 0) {
            if (onGround) {
                double dx = target.posX - posX, dz = target.posZ - posZ;
                float d = MathHelper.sqrt_double(dx * dx + dz * dz);
                motionX = dx / d * 0.5 * 0.8 + motionX * 0.2;
                motionZ = dz / d * 0.5 * 0.8 + motionZ * 0.2;
                motionY = 0.4;
            }
        } else if (attackTime <= 0 && distance < 2.0F
            && target.boundingBox.maxY > boundingBox.minY
            && target.boundingBox.minY < boundingBox.maxY) {
                attackTime = 20;
                attackEntityAsMob(target);
            }
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (worldObj.isRemote) return;
        if (spit > 0) spit--;
        if (spit > 0 || !(entityToAttack instanceof EntityLivingBase)) return;
        EntityLivingBase target = (EntityLivingBase) entityToAttack;
        if (!inSpit(getDistanceToEntity(target)) || !canEntityBeSeen(target)) return;
        EntityCloudsilk strand = new EntityCloudsilk(worldObj, this);
        double dx = target.posX - posX, dz = target.posZ - posZ;
        double dy = target.posY + target.getEyeHeight() * 0.6 - strand.posY;
        strand.setThrowableHeading(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.05, dz, 1.4F, 2.0F);
        worldObj.spawnEntityInWorld(strand);
        worldObj.playSoundAtEntity(this, "mob.spider.say", 1.0F, 1.6F);
        spit = SPIT_COOLDOWN;
    }

    /** No jockeys and no strange humours: it is born of the sky, not of a dark forest. */
    @Override
    public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
        return data;
    }

    /** It stands where it was made (a spawner, an alcove), lit or not. */
    @Override
    public boolean getCanSpawnHere() {
        return worldObj.checkNoEntityCollision(boundingBox) && worldObj.getCollidingBoundingBoxes(this, boundingBox)
            .isEmpty() && !worldObj.isAnyLiquid(boundingBox);
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        for (ItemStack stack : drops(rand, looting)) entityDropItem(stack, 0.5F);
    }

    /** Its Cloudsilk, always; a little string; now and then a block of Cloud; once in a long while, its spinneret. */
    public static List<ItemStack> drops(Random random, int looting) {
        List<ItemStack> drops = new ArrayList<>();
        if (NEItems.cloudsilk != null) drops.add(new ItemStack(NEItems.cloudsilk, 1 + random.nextInt(3 + looting)));
        if (random.nextBoolean()) drops.add(new ItemStack(Items.string));
        if (NEBlocks.cloud != null && random.nextDouble() < CLOUD_CHANCE)
            drops.add(new ItemStack(Item.getItemFromBlock(NEBlocks.cloud)));
        if (NEItems.silkspinner != null && random.nextDouble() < SPINNER_CHANCE + looting * 0.01)
            drops.add(new ItemStack(NEItems.silkspinner));
        return drops;
    }

    @Override
    protected float getSoundPitch() {
        return 1.3F + rand.nextFloat() * 0.1F;
    }
}
