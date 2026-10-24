package naturalexceptions.minecraft.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * A Skystone Sentinel: a guard of the sky dungeons, a heavy figure in plates of Skystone. Slow, and nothing moves
 * it; arrows glance off its plates. Close in, it lifts its fists (dust rising round it) and a second later brings them
 * down in a Ground Slam that hurts and throws back everyone within three blocks.
 */
public final class EntitySkystoneSentinel extends EntityMob implements CitadelMachine {

    public static final float MAX_HEALTH = 30.0F;
    public static final int ARMOUR = 8;
    static final double SLAM_TRIGGER = 2.5, SLAM_RADIUS = 3.0;
    static final float SLAM_DAMAGE = 5.0F;
    static final int SLAM_WINDUP = 20, SLAM_COOLDOWN = 80;
    /** Chance it carries a Sky Key. */
    public static final double KEY_CHANCE = 0.04;

    private static final int WINDING = 16;

    private int windup, cooldown = SLAM_COOLDOWN / 2;

    public EntitySkystoneSentinel(World world) {
        super(world);
        setSize(0.7F, 2.1F);
        experienceValue = 10;
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIAttackOnCollide(this, EntityPlayer.class, 1.0, false));
        tasks.addTask(6, new EntityAIWander(this, 0.7));
        tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 10.0F));
        tasks.addTask(8, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true));
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(WINDING, (byte) 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(MAX_HEALTH);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.2);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(6.0);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(20.0);
        getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(1.0);
    }

    @Override
    public boolean isAIEnabled() {
        return true;
    }

    @Override
    public int getTotalArmorValue() {
        return ARMOUR;
    }

    /** Whether it is lifting its fists for a Ground Slam (for the model and the dust). */
    public boolean windingUp() {
        return dataWatcher.getWatchableObjectByte(WINDING) != 0;
    }

    // --- The Ground Slam ------------------------------------------------------------------------------------

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (worldObj.isRemote) {
            if (windingUp()) for (int i = 0; i < 2; i++) worldObj.spawnParticle(
                dust(),
                posX + (rand.nextDouble() - 0.5) * 1.6,
                posY + 0.1,
                posZ + (rand.nextDouble() - 0.5) * 1.6,
                0.0,
                0.12,
                0.0);
            return;
        }
        if (cooldown > 0) cooldown--;
        EntityLivingBase target = getAttackTarget();
        if (windup > 0) {
            getNavigator().clearPathEntity();
            if (target != null) getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
            if (--windup == 0) {
                slam();
                dataWatcher.updateObject(WINDING, (byte) 0);
                cooldown = SLAM_COOLDOWN;
            }
            return;
        }
        if (target != null && cooldown == 0 && getDistanceToEntity(target) < SLAM_TRIGGER) {
            windup = SLAM_WINDUP;
            dataWatcher.updateObject(WINDING, (byte) 1);
            worldObj.playSoundAtEntity(this, "mob.irongolem.throw", 1.0F, 0.6F);
        }
    }

    private void slam() {
        worldObj.playSoundAtEntity(this, "random.explode", 0.6F, 1.6F);
        worldObj.playSoundAtEntity(this, "dig.stone", 1.0F, 0.6F);
        if (worldObj instanceof WorldServer) {
            WorldServer server = (WorldServer) worldObj;
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI * 2.0 / 24.0;
                server.func_147487_a(
                    dust(),
                    posX + Math.cos(a) * 1.2,
                    posY + 0.1,
                    posZ + Math.sin(a) * 1.2,
                    2,
                    0.1,
                    0.1,
                    0.1,
                    0.15);
            }
        }
        @SuppressWarnings("unchecked")
        List<EntityPlayer> players = worldObj
            .getEntitiesWithinAABB(EntityPlayer.class, boundingBox.expand(SLAM_RADIUS, 1.5, SLAM_RADIUS));
        for (EntityPlayer player : players) {
            double dx = player.posX - posX, dz = player.posZ - posZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > SLAM_RADIUS || player.capabilities.isCreativeMode) continue;
            if (distance < 1.0E-3) {
                dx = 1.0;
                distance = 1.0;
            }
            player.attackEntityFrom(DamageSource.causeMobDamage(this), SLAM_DAMAGE);
            player.motionX = dx / distance * 0.9;
            player.motionZ = dz / distance * 0.9;
            player.motionY = 0.35;
            player.velocityChanged = true;
        }
    }

    /** Chips of its own Skystone (as a broken block's particles). */
    private static String dust() {
        Block block = NEBlocks.skystoneBricks != null ? NEBlocks.skystoneBricks : net.minecraft.init.Blocks.stonebrick;
        return "blockcrack_" + Block.getIdFromBlock(block) + "_0";
    }

    // --- Hard to hurt ---------------------------------------------------------------------------------------

    /** Arrows and bolts glance off its plates: half damage. Every hit knocks chips off it. */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source.isProjectile()) amount *= 0.5F;
        boolean hurt = super.attackEntityFrom(source, amount);
        if (hurt && worldObj instanceof WorldServer)
            ((WorldServer) worldObj).func_147487_a(dust(), posX, posY + height * 0.6, posZ, 8, 0.3, 0.4, 0.3, 0.1);
        return hurt;
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        boolean hit = super.attackEntityAsMob(target);
        if (hit) {
            target.addVelocity(
                -Math.sin(Math.toRadians(rotationYaw)) * 0.6,
                0.2,
                Math.cos(Math.toRadians(rotationYaw)) * 0.6);
            worldObj.playSoundAtEntity(this, "mob.irongolem.hit", 1.0F, 0.8F);
        }
        return hit;
    }

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

    /** What a Sentinel leaves: some of its Skystone, some copper, and now and then a Sky Key. */
    public static List<ItemStack> drops(Random random, int looting) {
        List<ItemStack> drops = new ArrayList<>();
        int bricks = random.nextInt(3 + looting), copper = random.nextInt(3 + looting);
        if (bricks > 0 && NEBlocks.skystoneBricks != null)
            drops.add(new ItemStack(Item.getItemFromBlock(NEBlocks.skystoneBricks), bricks));
        if (copper > 0 && NEItems.copperIngot != null) drops.add(new ItemStack(NEItems.copperIngot, copper));
        if (NEItems.skyKey != null && random.nextDouble() < KEY_CHANCE) drops.add(new ItemStack(NEItems.skyKey));
        return drops;
    }

    @Override
    protected Item getDropItem() {
        return null;
    }

    // --- Sounds ---------------------------------------------------------------------------------------------

    @Override
    protected String getLivingSound() {
        return null;
    }

    @Override
    protected String getHurtSound() {
        return "mob.irongolem.hit";
    }

    @Override
    protected String getDeathSound() {
        return "mob.irongolem.death";
    }

    @Override
    protected void func_145780_a(int x, int y, int z, Block block) {
        playSound("mob.irongolem.walk", 0.6F, 1.1F);
    }

    @Override
    protected float getSoundPitch() {
        return 0.9F + rand.nextFloat() * 0.1F;
    }
}
