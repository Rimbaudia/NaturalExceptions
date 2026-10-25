package naturalexceptions.minecraft.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import naturalexceptions.minecraft.block.BlockMoltenMetal;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * The Foundry Colossus: the dead city's foundry engine, a furnace on legs with a hammer for one arm and a crucible
 * for the other. It sleeps cold in its foundry until someone comes in; then its furnace roars up. Close in it raises
 * its hammer and brings it down; now and then it tilts its crucible and pours molten metal across the floor towards
 * you; keep away and it lobs slag. Every blow is shown first. Hot, its iron shell turns most of what you strike it
 * with: douse its furnace-heart with a bucket of water (the foundry's troughs hold plenty) and it stops dead,
 * steaming, its heart open to your blows, for a few seconds. It drops the Colossus Crucible.
 */
public final class EntityFoundryColossus extends EntityMob implements IBossDisplayData {

    /** What it is doing, for the model: 0 asleep, 1 awake, 2 doused; and what it is winding up. */
    public static final int ASLEEP = 0, AWAKE = 1, QUENCHED = 2;
    public static final int NONE = 0, SLAM = 1, POUR = 2, THROW = 3;
    private static final int STATE = 16, WINDING = 17;

    private int windup, slamCooldown, pourCooldown = ColossusRules.POUR_COOLDOWN / 2, throwCooldown, quenched, idle;
    /** Rekindled, it burns too hot for water to douse it again for a while. */
    private int kindled;
    private double[] pourAt;
    private int[] post;

    public EntityFoundryColossus(World world) {
        super(world);
        setSize(2.0F, 2.9F);
        isImmuneToFire = true;
        experienceValue = ColossusRules.XP;
        func_110163_bv(); // it keeps its foundry: never despawns
        tasks.addTask(2, new EntityAIAttackOnCollide(this, EntityPlayer.class, 1.0, false));
        tasks.addTask(4, new EntityAIMoveTowardsRestriction(this, 1.0));
        tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 14.0F));
        tasks.addTask(8, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true));
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(STATE, (byte) ASLEEP);
        dataWatcher.addObject(WINDING, (byte) NONE);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(ColossusRules.MAX_HEALTH);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(ColossusRules.SPEED);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(ColossusRules.SLAM_DAMAGE);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(20.0);
        getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(1.0);
    }

    @Override
    public boolean isAIEnabled() {
        return true;
    }

    @Override
    public int getTotalArmorValue() {
        return ColossusRules.ARMOUR;
    }

    public int state() {
        return dataWatcher.getWatchableObjectByte(STATE);
    }

    /** What it is winding up ({@link #SLAM}, {@link #POUR}, {@link #THROW}), for the model. */
    public int winding() {
        return dataWatcher.getWatchableObjectByte(WINDING);
    }

    private void setState(int state) {
        dataWatcher.updateObject(STATE, (byte) state);
    }

    private void wind(int what) {
        dataWatcher.updateObject(WINDING, (byte) what);
    }

    /** Sets it asleep in its foundry: it keeps near (x, y, z). */
    public void post(int x, int y, int z) {
        post = new int[] { x, y, z };
        setHomeArea(x, y, z, 12);
        setState(ASLEEP);
    }

    // --- Waking, dousing ------------------------------------------------------------------------------------

    private void wake() {
        if (state() != ASLEEP) return;
        setState(AWAKE);
        worldObj.playSoundAtEntity(this, "mob.ghast.fireball", 2.0F, 0.5F);
        worldObj.playSoundAtEntity(this, "mob.irongolem.death", 2.0F, 0.4F);
        burst("flame", 30);
    }

    /** Doused: its fire hisses out and its heart opens, for a few seconds. */
    public void quench() {
        if (worldObj.isRemote || state() == QUENCHED) return;
        if (kindled > 0) {
            worldObj.playSoundAtEntity(this, "random.fizz", 1.5F, 1.4F);
            burst("cloud", 8);
            return;
        }
        wake();
        setState(QUENCHED);
        quenched = ColossusRules.QUENCH_TICKS;
        windup = 0;
        wind(NONE);
        getNavigator().clearPathEntity();
        worldObj.playSoundAtEntity(this, "random.fizz", 2.5F, 0.5F);
        burst("cloud", 40);
    }

    private void burst(String particle, int count) {
        if (worldObj instanceof WorldServer) ((WorldServer) worldObj).func_147487_a(
            particle,
            posX,
            posY + height * 0.55,
            posZ,
            count,
            width * 0.4,
            height * 0.3,
            width * 0.4,
            0.05);
    }

    /** A bucket of water thrown on it douses it (the bucket comes back empty). */
    @Override
    protected boolean interact(EntityPlayer player) {
        ItemStack held = player.getCurrentEquippedItem();
        if (held == null || held.getItem() != Items.water_bucket || state() == QUENCHED) return false;
        if (!worldObj.isRemote) {
            boolean took = kindled == 0;
            quench();
            if (took && !player.capabilities.isCreativeMode)
                player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(Items.bucket));
        }
        return true;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source.isFireDamage()) return false;
        if (!worldObj.isRemote && source.getEntity() != null) wake();
        boolean doused = state() == QUENCHED;
        if (!doused && !worldObj.isRemote && source.getEntity() != null) {
            worldObj.playSoundAtEntity(this, "random.anvil_land", 0.4F, 1.6F);
            burst("lava", 3);
        }
        return super.attackEntityFrom(source, amount * ColossusRules.through(doused));
    }

    // --- Its work -------------------------------------------------------------------------------------------

    @Override
    protected void updateAITasks() {
        if (state() == ASLEEP) {
            motionX = motionZ = 0.0;
            if (ticksExisted % 10 == 0) {
                EntityPlayer near = worldObj.getClosestVulnerablePlayerToEntity(this, ColossusRules.WAKE_RANGE);
                if (near != null && canEntityBeSeen(near)) {
                    wake();
                    setAttackTarget(near);
                }
            }
            return;
        }
        if (state() == QUENCHED || windup > 0) {
            getNavigator().clearPathEntity();
            motionX = motionZ = 0.0;
            if (getAttackTarget() != null) getLookHelper().setLookPositionWithEntity(getAttackTarget(), 10.0F, 10.0F);
            getLookHelper().onUpdateLook();
            return;
        }
        super.updateAITasks();
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (worldObj.isRemote) {
            if (state() == AWAKE && rand.nextInt(4) == 0) worldObj.spawnParticle(
                "largesmoke",
                posX + (rand.nextDouble() - 0.5) * 0.4,
                posY + height + 0.2,
                posZ + (rand.nextDouble() - 0.5) * 0.4,
                0.0,
                0.06,
                0.0);
            if (state() == QUENCHED) worldObj.spawnParticle(
                "cloud",
                posX + (rand.nextDouble() - 0.5) * width,
                posY + height * (0.4 + rand.nextDouble() * 0.5),
                posZ + (rand.nextDouble() - 0.5) * width,
                0.0,
                0.05,
                0.0);
            return;
        }
        if (kindled > 0) kindled--;
        if (isInWater() && kindled == 0) quench();
        if (state() == QUENCHED) {
            if (--quenched <= 0) {
                setState(AWAKE);
                kindled = ColossusRules.KINDLE_TICKS;
                worldObj.playSoundAtEntity(this, "mob.ghast.fireball", 1.5F, 0.6F);
                burst("flame", 20);
            }
            return;
        }
        if (state() != AWAKE) return;
        if (slamCooldown > 0) slamCooldown--;
        if (pourCooldown > 0) pourCooldown--;
        if (throwCooldown > 0) throwCooldown--;
        EntityLivingBase target = getAttackTarget();
        if (target == null && ticksExisted % 10 == 0) {
            // It does not lose you in its own foundry: whoever it can see, it goes for.
            EntityPlayer near = worldObj.getClosestVulnerablePlayerToEntity(this, ColossusRules.HUNT_RANGE);
            if (near != null && canEntityBeSeen(near)) setAttackTarget(target = near);
        }
        if (windup > 0) {
            if (--windup == 0) strike(target);
            return;
        }
        if (target == null) {
            settle();
            return;
        }
        idle = 0;
        double distance = getDistanceToEntity(target);
        if (pourCooldown == 0 && distance < ColossusRules.POUR_LENGTH + 1) begin(POUR, ColossusRules.POUR_WINDUP);
        else if (throwCooldown == 0 && distance >= ColossusRules.THROW_FROM && canEntityBeSeen(target))
            begin(THROW, ColossusRules.THROW_WINDUP);
        else if (slamCooldown == 0 && distance <= ColossusRules.SLAM_REACH) begin(SLAM, ColossusRules.SLAM_WINDUP);
        if (winding() == POUR) pourAt = new double[] { target.posX, target.posZ };
    }

    /** With no one to fight for a while, it goes back to its place and its fire sinks low. */
    private void settle() {
        if (post == null || ++idle < ColossusRules.SETTLE_TICKS) return;
        double dx = posX - (post[0] + 0.5), dz = posZ - (post[2] + 0.5);
        if (dx * dx + dz * dz > 2.25) {
            if (getNavigator().noPath()) getNavigator().tryMoveToXYZ(post[0] + 0.5, post[1], post[2] + 0.5, 0.8);
            return;
        }
        setState(ASLEEP);
        idle = 0;
        worldObj.playSoundAtEntity(this, "random.fizz", 0.6F, 0.4F);
    }

    private void begin(int what, int ticks) {
        wind(what);
        windup = ticks;
        worldObj.playSoundAtEntity(
            this,
            what == SLAM ? "random.anvil_use" : what == POUR ? "fire.fire" : "mob.blaze.breathe",
            1.5F,
            what == SLAM ? 0.5F : 0.7F);
    }

    /** The blow it wound up comes. */
    private void strike(EntityLivingBase target) {
        int what = winding();
        wind(NONE);
        switch (what) {
            case SLAM:
                slamCooldown = ColossusRules.SLAM_COOLDOWN;
                slam();
                break;
            case POUR:
                pourCooldown = ColossusRules.POUR_COOLDOWN;
                pour();
                break;
            case THROW:
                throwCooldown = ColossusRules.THROW_COOLDOWN;
                if (target != null) lob(target);
                break;
            default:
                break;
        }
    }

    /** The hammer comes down before it: everyone in reach is struck and thrown back. */
    private void slam() {
        worldObj.playSoundAtEntity(this, "random.explode", 1.2F, 0.6F);
        burst("largesmoke", 20);
        double fx = -Math.sin(Math.toRadians(renderYawOffset)), fz = Math.cos(Math.toRadians(renderYawOffset));
        @SuppressWarnings("unchecked")
        List<EntityLivingBase> near = worldObj.getEntitiesWithinAABB(
            EntityLivingBase.class,
            boundingBox.expand(ColossusRules.SLAM_REACH, 1.0, ColossusRules.SLAM_REACH));
        for (EntityLivingBase living : near) {
            if (living == this) continue;
            double dx = living.posX - posX, dz = living.posZ - posZ, d = Math.sqrt(dx * dx + dz * dz);
            if (d > ColossusRules.SLAM_REACH + width / 2 || d > 0.5 && (fx * dx + fz * dz) / d < 0.0) continue;
            living.attackEntityFrom(DamageSource.causeMobDamage(this), ColossusRules.SLAM_DAMAGE);
            living.addVelocity(
                dx / Math.max(d, 0.5) * ColossusRules.SLAM_THROW,
                0.35,
                dz / Math.max(d, 0.5) * ColossusRules.SLAM_THROW);
            living.velocityChanged = true;
        }
    }

    /** It tips its crucible: a line of molten metal runs out across the floor towards where you stood. */
    private void pour() {
        if (pourAt == null || NEBlocks.moltenMetal == null) return;
        worldObj.playSoundAtEntity(this, "liquid.lavapop", 2.0F, 0.6F);
        int y = (int) Math.floor(posY);
        for (int[] at : ColossusRules.pourLine(posX, posZ, pourAt[0], pourAt[1])) {
            for (int dy = 0; dy >= -1; dy--) {
                if (BlockMoltenMetal.canLie(worldObj, at[0], y + dy, at[1])) {
                    worldObj.setBlock(at[0], y + dy, at[1], NEBlocks.moltenMetal, 0, 3);
                    break;
                }
            }
        }
        pourAt = null;
    }

    /** A glob of slag, lobbed high to come down on you. */
    private void lob(EntityLivingBase target) {
        EntitySlagGlob glob = new EntitySlagGlob(worldObj, this);
        glob.setPosition(posX, posY + height * 0.75, posZ);
        double dx = target.posX - posX, dz = target.posZ - posZ, dy = target.posY + 0.5 - glob.posY;
        double d = Math.sqrt(dx * dx + dz * dz);
        glob.setThrowableHeading(dx, dy + d * 0.25, dz, 0.9F + (float) d * 0.03F, 2.0F);
        worldObj.spawnEntityInWorld(glob);
        worldObj.playSoundAtEntity(this, "mob.ghast.fireball", 1.0F, 1.2F);
    }

    /** It does not swing at you on contact: it winds up its hammer instead (see onLivingUpdate). */
    @Override
    public boolean attackEntityAsMob(Entity target) {
        return false;
    }

    /** Peaceful clears monsters away, but not this one: there is only one, and its vault opens with it. */
    @Override
    public void setDead() {
        if (!worldObj.isRemote && worldObj.difficultySetting == net.minecraft.world.EnumDifficulty.PEACEFUL
            && getHealth() > 0.0F) return;
        super.setDead();
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    // --- Saving ---------------------------------------------------------------------------------------------

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (post != null) tag.setIntArray("Post", post);
        tag.setByte("State", (byte) (state() == QUENCHED ? AWAKE : state()));
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.hasKey("Post")) {
            int[] p = tag.getIntArray("Post");
            if (p.length == 3) post(p[0], p[1], p[2]);
        }
        setState(tag.getByte("State"));
    }

    // --- Drops ----------------------------------------------------------------------------------------------

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        for (ItemStack stack : drops(rand, looting)) entityDropItem(stack, 1.0F);
    }

    /** Its crucible, always; iron from its frame, its slag, its coal. */
    public static List<ItemStack> drops(Random random, int looting) {
        List<ItemStack> drops = new ArrayList<>();
        if (NEBlocks.colossusCrucible != null) drops.add(new ItemStack(NEBlocks.colossusCrucible));
        drops.add(new ItemStack(Items.iron_ingot, 4 + random.nextInt(5 + looting)));
        if (NEItems.slagLump != null) drops.add(new ItemStack(NEItems.slagLump, 2 + random.nextInt(4)));
        drops.add(new ItemStack(Items.coal, 3 + random.nextInt(4)));
        return drops;
    }

    @Override
    protected Item getDropItem() {
        return null;
    }

    // --- Sounds ---------------------------------------------------------------------------------------------

    @Override
    protected String getLivingSound() {
        return state() == AWAKE ? "fire.fire" : null;
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
        return 0.5F;
    }

    @Override
    protected void func_145780_a(int x, int y, int z, net.minecraft.block.Block block) {
        playSound("mob.irongolem.walk", 1.0F, 0.6F);
    }
}
