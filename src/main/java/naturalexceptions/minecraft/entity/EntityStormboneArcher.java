package naturalexceptions.minecraft.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIArrowAttack;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
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

import naturalexceptions.minecraft.registry.NEItems;

/**
 * A Stormbone Archer: a skeleton whose bones the Stormglass has grown through, its eyes lit violet. It shoots from far
 * off ({@link #RANGE} blocks), fast and straight, with Stormglass-tipped arrows that shock what they strike; it will
 * not be caught close, but backs away from anyone within {@link #KEEP_AWAY} blocks and shoots again. The sun does not
 * burn it.
 */
public final class EntityStormboneArcher extends EntityMob implements IRangedAttackMob {

    public static final float MAX_HEALTH = 18.0F;
    public static final float RANGE = 24.0F, KEEP_AWAY = 4.0F;
    /** Chance it drops Stormglass Shards. */
    public static final double SHARD_CHANCE = 0.1;

    public EntityStormboneArcher(World world) {
        super(world);
        setSize(0.6F, 1.99F);
        experienceValue = 6;
        tasks.addTask(1, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIAvoidEntity(this, EntityPlayer.class, KEEP_AWAY, 1.0, 1.25));
        tasks.addTask(4, new EntityAIArrowAttack(this, 1.0, 40, 60, RANGE));
        tasks.addTask(6, new EntityAIWander(this, 0.8));
        tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 12.0F));
        tasks.addTask(8, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true));
        setCurrentItemOrArmor(0, new ItemStack(Items.bow));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(MAX_HEALTH);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.27);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(RANGE + 4.0);
    }

    @Override
    public boolean isAIEnabled() {
        return true;
    }

    @Override
    public EnumCreatureAttribute getCreatureAttribute() {
        return EnumCreatureAttribute.UNDEAD;
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float power) {
        EntityStormArrow arrow = new EntityStormArrow(worldObj, this, target);
        arrow.setDamage(power * 2.0F + rand.nextGaussian() * 0.25 + worldObj.difficultySetting.getDifficultyId() * 0.11F);
        worldObj.playSoundAtEntity(this, "random.bow", 1.0F, 1.0F / (rand.nextFloat() * 0.4F + 0.8F));
        worldObj.playSoundAtEntity(this, "fireworks.twinkle_far", 0.4F, 1.6F);
        worldObj.spawnEntityInWorld(arrow);
    }

    /** It stands where it was made (a spawner, an alcove), lit or not. */
    @Override
    public boolean getCanSpawnHere() {
        return worldObj.checkNoEntityCollision(boundingBox) && worldObj.getCollidingBoundingBoxes(this, boundingBox)
            .isEmpty() && !worldObj.isAnyLiquid(boundingBox);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (worldObj.isRemote && rand.nextInt(10) == 0)
            worldObj.spawnParticle("witchMagic", posX, posY + height * 0.8, posZ, 0.0, 0.0, 0.0);
    }

    // --- Drops ----------------------------------------------------------------------------------------------

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        for (ItemStack stack : drops(rand, looting)) entityDropItem(stack, 0.5F);
    }

    /** Bones and arrows; now and then the Stormglass out of its bones. */
    public static List<ItemStack> drops(Random random, int looting) {
        List<ItemStack> drops = new ArrayList<>();
        int bones = random.nextInt(3 + looting), arrows = random.nextInt(3 + looting);
        if (bones > 0) drops.add(new ItemStack(Items.bone, bones));
        if (arrows > 0) drops.add(new ItemStack(Items.arrow, arrows));
        if (NEItems.stormglassShard != null && random.nextDouble() < SHARD_CHANCE)
            drops.add(new ItemStack(NEItems.stormglassShard, 1 + random.nextInt(2)));
        return drops;
    }

    @Override
    protected void dropEquipment(boolean recentlyHit, int looting) {} // its bow is crackling bone, not a bow to take

    @Override
    protected Item getDropItem() {
        return null;
    }

    // --- Sounds ---------------------------------------------------------------------------------------------

    @Override
    protected String getLivingSound() {
        return "mob.skeleton.say";
    }

    @Override
    protected String getHurtSound() {
        return "mob.skeleton.hurt";
    }

    @Override
    protected String getDeathSound() {
        return "mob.skeleton.death";
    }

    @Override
    protected void func_145780_a(int x, int y, int z, net.minecraft.block.Block block) {
        playSound("mob.skeleton.step", 0.15F, 1.0F);
    }
}
