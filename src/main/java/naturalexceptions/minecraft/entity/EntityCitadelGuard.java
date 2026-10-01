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
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * A Citadel Guard: one of the watch at the guardhouse on the road to a citadel's gate, in a Skystone-plated helm and
 * tabard, a halberd in one hand and a tower shield on the other arm. Once he has seen you his shield is up between
 * blows, and what comes at him from the front glances off it (arrows wholly, blows mostly): get round to his side.
 * His halberd reaches further than a sword. When he first sees you he shouts, and every guard within earshot comes
 * running. Guards keep to their post.
 */
public final class EntityCitadelGuard extends EntityMob {

    public static final float MAX_HEALTH = 34.0F;
    public static final int ARMOUR = 10;
    /** His halberd's reach, and how long between its blows. */
    public static final double REACH = 3.2;
    static final int SWING_COOLDOWN = 25, GUARD_AFTER_SWING = 15;
    /** How far his shout carries, and how far he keeps from his post. */
    public static final double RALLY = 24.0;
    public static final int POST_RADIUS = 10;
    /** The shield covers this wide an arc ahead of him (each way, in degrees). */
    public static final double SHIELD_ARC = 60.0;
    /** What a blow from the front does through the shield; arrows from the front do nothing. */
    public static final float MELEE_THROUGH = 0.2F;
    public static final double HALBERD_CHANCE = 0.06;

    private static final int SHIELD = 16;
    private int cooldown, lastSwing = -100;
    private int[] post;

    public EntityCitadelGuard(World world) {
        super(world);
        setSize(0.6F, 1.9F);
        experienceValue = 12;
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIAttackOnCollide(this, EntityPlayer.class, 1.0, false));
        tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 1.0));
        tasks.addTask(6, new EntityAIWander(this, 0.6));
        tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 12.0F));
        tasks.addTask(8, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true));
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(SHIELD, (byte) 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(MAX_HEALTH);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.25);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(7.0);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(RALLY);
        getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(0.5);
    }

    @Override
    public boolean isAIEnabled() {
        return true;
    }

    @Override
    public int getTotalArmorValue() {
        return ARMOUR;
    }

    /** Sets him at his post: he keeps within {@link #POST_RADIUS} of it, and goes back to it. */
    public void post(int x, int y, int z) {
        post = new int[] { x, y, z };
        setHomeArea(x, y, z, POST_RADIUS);
        func_110163_bv(); // he stays: the guardhouse is his
    }

    /** Whether his shield is up (for the model). */
    public boolean shieldUp() {
        return dataWatcher.getWatchableObjectByte(SHIELD) != 0;
    }

    // --- The shield -----------------------------------------------------------------------------------------

    /**
     * What gets through his shield of a hit from (fromX, fromZ) when he stands at (x, z) facing {@code yaw}: 1 from
     * the side or behind or with his shield down; from the front, nothing of an arrow and {@link #MELEE_THROUGH} of
     * a blow.
     */
    public static float through(float yaw, double x, double z, double fromX, double fromZ, boolean projectile,
        boolean shieldUp) {
        if (!shieldUp) return 1.0F;
        double fx = -Math.sin(Math.toRadians(yaw)), fz = Math.cos(Math.toRadians(yaw));
        double dx = fromX - x, dz = fromZ - z, d = Math.sqrt(dx * dx + dz * dz);
        if (d < 1.0E-3) return 1.0F;
        double cos = (fx * dx + fz * dz) / d;
        if (cos < Math.cos(Math.toRadians(SHIELD_ARC))) return 1.0F;
        return projectile ? 0.0F : MELEE_THROUGH;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        Entity from = source.getSourceOfDamage();
        if (from != null && !source.isUnblockable()) {
            float through = through(renderYawOffset, posX, posZ, from.posX, from.posZ, source.isProjectile(), shieldUp());
            if (through < 1.0F) {
                worldObj.playSoundAtEntity(this, "random.anvil_land", 0.3F, 1.8F);
                if (worldObj instanceof WorldServer) ((WorldServer) worldObj)
                    .func_147487_a(chips(), posX, posY + height * 0.6, posZ, 6, 0.3, 0.3, 0.3, 0.1);
                if (through <= 0.0F) return false; // it glances off (an arrow springs back)
                amount *= through;
            }
        }
        return super.attackEntityFrom(source, amount);
    }

    private static String chips() {
        Block block = NEBlocks.skystoneBricks != null ? NEBlocks.skystoneBricks : net.minecraft.init.Blocks.stonebrick;
        return "blockcrack_" + Block.getIdFromBlock(block) + "_0";
    }

    // --- The halberd, and the alarm -------------------------------------------------------------------------

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (worldObj.isRemote) return;
        if (cooldown > 0) cooldown--;
        EntityLivingBase target = getAttackTarget();
        boolean up = target != null && ticksExisted - lastSwing > GUARD_AFTER_SWING;
        if (up != shieldUp()) dataWatcher.updateObject(SHIELD, (byte) (up ? 1 : 0));
        if (target != null && cooldown == 0 && getDistanceToEntity(target) <= REACH && canEntityBeSeen(target))
            attackEntityAsMob(target);
    }

    /** A halberd's blow, from out of a sword's reach; it lowers his shield for a moment. */
    @Override
    public boolean attackEntityAsMob(Entity target) {
        if (cooldown > 0) return false;
        cooldown = SWING_COOLDOWN;
        lastSwing = ticksExisted;
        swingItem();
        boolean hit = super.attackEntityAsMob(target);
        if (hit) worldObj.playSoundAtEntity(this, "random.break", 0.4F, 0.6F);
        return hit;
    }

    /** On first seeing someone, he shouts, and every guard within earshot takes up the same fight. */
    @Override
    public void setAttackTarget(EntityLivingBase target) {
        boolean first = target != null && getAttackTarget() == null;
        super.setAttackTarget(target);
        if (!first || worldObj.isRemote) return;
        worldObj.playSoundAtEntity(this, "mob.villager.no", 1.5F, 0.6F);
        @SuppressWarnings("unchecked")
        List<EntityCitadelGuard> guards = worldObj
            .getEntitiesWithinAABB(EntityCitadelGuard.class, boundingBox.expand(RALLY, 8.0, RALLY));
        for (EntityCitadelGuard guard : guards) if (guard != this && guard.getAttackTarget() == null) guard.rally(target);
    }

    /** Takes up a fight another guard called him to (without calling the rest again). */
    void rally(EntityLivingBase target) {
        super.setAttackTarget(target);
    }

    /** He stands where he was posted, lit or not. */
    @Override
    public boolean getCanSpawnHere() {
        return worldObj.checkNoEntityCollision(boundingBox) && worldObj.getCollidingBoundingBoxes(this, boundingBox)
            .isEmpty() && !worldObj.isAnyLiquid(boundingBox);
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    // --- Keeping his post -----------------------------------------------------------------------------------

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (post != null) tag.setIntArray("Post", post);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.hasKey("Post")) {
            int[] p = tag.getIntArray("Post");
            if (p.length == 3) post(p[0], p[1], p[2]);
        }
    }

    // --- Drops ----------------------------------------------------------------------------------------------

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        for (ItemStack stack : drops(rand, looting)) entityDropItem(stack, 0.5F);
    }

    /** Some iron, his bread, and now and then his halberd. */
    public static List<ItemStack> drops(Random random, int looting) {
        List<ItemStack> drops = new ArrayList<>();
        int iron = random.nextInt(3 + looting), bread = random.nextInt(2);
        if (iron > 0) drops.add(new ItemStack(Items.iron_ingot, iron));
        if (bread > 0) drops.add(new ItemStack(Items.bread, bread));
        if (NEItems.citadelHalberd != null && random.nextDouble() < HALBERD_CHANCE + looting * 0.02)
            drops.add(new ItemStack(NEItems.citadelHalberd));
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
        return "game.player.hurt";
    }

    @Override
    protected String getDeathSound() {
        return "game.player.die";
    }

    @Override
    protected void func_145780_a(int x, int y, int z, Block block) {
        playSound("step.stone", 0.3F, 0.8F);
    }
}
