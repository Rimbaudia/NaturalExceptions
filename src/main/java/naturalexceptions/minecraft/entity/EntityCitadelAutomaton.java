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
 * A Citadel Automaton: one of the machines that still keep the watch for a city whose people are long dead. Skystone
 * plates on a brass frame, a Stormglass heart, a halberd in one hand and a tower shield on the other arm. Posted, it
 * stands dormant, head bowed, until someone comes near or strikes it; then it wakes with a hiss of steam. Once it has
 * seen you its shield is up between blows, and what comes at it from the front glances off (arrows wholly, blows
 * mostly): get round to its side. Its halberd reaches further than a sword. When it wakes it sounds an alarm, and every
 * machine within earshot wakes and comes. Losing you, it walks back to its post and goes dormant again.
 */
public final class EntityCitadelAutomaton extends EntityMob implements CitadelMachine {

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
    /** Its heart, now and then whole enough to take; and the rarer crest of a sentry. */
    public static final double HEART_CHANCE = 0.25, CREST_CHANCE = 0.03;
    /** How near someone must come to wake it, and how long after losing them it goes dormant again. */
    public static final double WAKE_RANGE = 6.0;
    public static final int SETTLE_TICKS = 200;

    private static final int SHIELD = 16, DORMANT = 17;
    private int cooldown, lastSwing = -100, idle;
    private int[] post;
    private float postYaw;

    public EntityCitadelAutomaton(World world) {
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
        dataWatcher.addObject(DORMANT, (byte) 0);
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

    /**
     * Sets it at its post, facing the way it faces now: it keeps within {@link #POST_RADIUS} of it, goes back to it,
     * and waits there dormant.
     */
    public void post(int x, int y, int z) {
        post = new int[] { x, y, z };
        postYaw = rotationYaw;
        setHomeArea(x, y, z, POST_RADIUS);
        func_110163_bv(); // it stays: the post is its
        setDormant(true);
    }

    /** Whether its shield is up (for the model). */
    public boolean shieldUp() {
        return dataWatcher.getWatchableObjectByte(SHIELD) != 0;
    }

    /** Whether it stands dormant at its post, head bowed (for the model, too). */
    public boolean dormant() {
        return dataWatcher.getWatchableObjectByte(DORMANT) != 0;
    }

    private void setDormant(boolean dormant) {
        dataWatcher.updateObject(DORMANT, (byte) (dormant ? 1 : 0));
    }

    /** It wakes: a hiss and a breath of steam, and it looks for who woke it. */
    public void wake() {
        if (!dormant()) return;
        setDormant(false);
        idle = 0;
        worldObj.playSoundAtEntity(this, "random.fizz", 0.8F, 0.6F);
        if (worldObj instanceof WorldServer) ((WorldServer) worldObj)
            .func_147487_a("cloud", posX, posY + height * 0.55, posZ, 12, 0.25, 0.3, 0.25, 0.02);
    }

    /** Dormant, nothing in it moves; it only listens. */
    @Override
    protected void updateAITasks() {
        if (!dormant()) {
            super.updateAITasks();
            return;
        }
        motionX = motionZ = 0.0;
        rotationYaw = renderYawOffset = rotationYawHead = postYaw;
        if (ticksExisted % 10 != 0) return;
        EntityPlayer near = worldObj.getClosestVulnerablePlayerToEntity(this, WAKE_RANGE);
        if (near != null && !CitadelMachine.passes(near, this) && canEntityBeSeen(near)) {
            wake();
            setAttackTarget(near);
        }
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
        if (!worldObj.isRemote && source.getEntity() != null) wake();
        Entity from = source.getSourceOfDamage();
        if (from != null && !source.isUnblockable()) {
            float through = through(
                renderYawOffset,
                posX,
                posZ,
                from.posX,
                from.posZ,
                source.isProjectile(),
                shieldUp());
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
        settle(target);
        if (target != null && cooldown == 0 && getDistanceToEntity(target) <= REACH && canEntityBeSeen(target))
            attackEntityAsMob(target);
    }

    /** Back at its post with no one to fight for a while, it goes dormant again. */
    private void settle(EntityLivingBase target) {
        if (post == null || dormant()) return;
        if (target != null) {
            idle = 0;
            return;
        }
        if (++idle < SETTLE_TICKS) return;
        double dx = posX - (post[0] + 0.5), dz = posZ - (post[2] + 0.5);
        if (dx * dx + dz * dz > 2.25) {
            if (getNavigator().noPath()) getNavigator().tryMoveToXYZ(post[0] + 0.5, post[1], post[2] + 0.5, 0.8);
            return;
        }
        setDormant(true);
        worldObj.playSoundAtEntity(this, "random.click", 0.5F, 0.5F);
    }

    /** A halberd's blow, from out of a sword's reach; it lowers its shield for a moment. */
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

    /**
     * On first seeing someone it sounds its alarm, a clang and a shriek of steam, and every machine within earshot
     * wakes and takes up the same fight. One wearing a Sentry's Crest it takes for one of its own, until struck.
     */
    @Override
    public void setAttackTarget(EntityLivingBase target) {
        if (target instanceof EntityPlayer && CitadelMachine.passes((EntityPlayer) target, this)) target = null;
        boolean first = target != null && getAttackTarget() == null;
        super.setAttackTarget(target);
        if (!first || worldObj.isRemote) return;
        wake();
        worldObj.playSoundAtEntity(this, "random.anvil_land", 1.2F, 1.6F);
        worldObj.playSoundAtEntity(this, "random.fizz", 1.5F, 1.4F);
        @SuppressWarnings("unchecked")
        List<EntityCitadelAutomaton> machines = worldObj
            .getEntitiesWithinAABB(EntityCitadelAutomaton.class, boundingBox.expand(RALLY, 8.0, RALLY));
        for (EntityCitadelAutomaton machine : machines)
            if (machine != this && machine.getAttackTarget() == null) machine.rally(target);
    }

    /** Takes up a fight another machine called it to (without calling the rest again). */
    void rally(EntityLivingBase target) {
        if (target instanceof EntityPlayer && CitadelMachine.passes((EntityPlayer) target, this)) return;
        wake();
        super.setAttackTarget(target);
    }

    /** It stands where it was posted, lit or not. */
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
        tag.setFloat("PostYaw", postYaw);
        tag.setBoolean("Dormant", dormant());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.hasKey("Post")) {
            int[] p = tag.getIntArray("Post");
            if (p.length == 3) post(p[0], p[1], p[2]);
        }
        if (tag.hasKey("PostYaw")) postYaw = tag.getFloat("PostYaw");
        setDormant(tag.getBoolean("Dormant"));
    }

    // --- Drops ----------------------------------------------------------------------------------------------

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        for (ItemStack stack : drops(rand, looting)) entityDropItem(stack, 0.5F);
    }

    /** Salvage: some iron from its frame, often its Stormglass heart, now and then its halberd or a sentry's crest. */
    public static List<ItemStack> drops(Random random, int looting) {
        List<ItemStack> drops = new ArrayList<>();
        int iron = random.nextInt(3 + looting);
        if (iron > 0) drops.add(new ItemStack(Items.iron_ingot, iron));
        if (NEItems.stormglassHeart != null && random.nextDouble() < HEART_CHANCE + looting * 0.05)
            drops.add(new ItemStack(NEItems.stormglassHeart));
        if (NEItems.citadelHalberd != null && random.nextDouble() < HALBERD_CHANCE + looting * 0.02)
            drops.add(new ItemStack(NEItems.citadelHalberd));
        Item crest = NEItems.TREASURES.get(naturalexceptions.minecraft.gear.Treasure.SENTRYS_CREST);
        if (crest != null && random.nextDouble() < CREST_CHANCE + looting * 0.01) drops.add(new ItemStack(crest));
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
    protected float getSoundPitch() {
        return 1.3F + (rand.nextFloat() - 0.5F) * 0.1F;
    }

    @Override
    protected void func_145780_a(int x, int y, int z, Block block) {
        playSound("mob.irongolem.walk", 0.35F, 1.5F);
    }
}
