package naturalexceptions.minecraft.entity;

import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import naturalexceptions.minecraft.power.TileHeartSocket;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * A Citadel Automaton a player has put back on its feet: a new heart, new plates, and it takes them for its master.
 * It follows them, or holds a spot; it fights the monsters that come near and whatever hurts its master, with its
 * halberd, behind its shield. It runs on its heart's charge, a little standing, more fighting; flat, it kneels dormant
 * until given another heart. Iron mends it.
 */
public final class EntityRepairedAutomaton extends EntityCreature implements AutomatonPose {

    public static final float MAX_HEALTH = 34.0F;
    /** What it spends: standing about, and while it fights (watts). */
    public static final double IDLE_WATTS = 20.0, FIGHTING_WATTS = 200.0;
    public static final float IRON_MENDS = 6.0F;
    static final double FOLLOW = 6.0, TELEPORT = 24.0;

    private static final int DORMANT = 16, SHIELD = 17;
    private String owner = "";
    private boolean staying;
    private double charge;

    public EntityRepairedAutomaton(World world) {
        super(world);
        setSize(0.6F, 1.9F);
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIAttackOnCollide(this, 1.0, true));
        tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 0.8));
        tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        tasks.addTask(8, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        targetTasks.addTask(
            3,
            new EntityAINearestAttackableTarget(
                this,
                EntityMob.class,
                0,
                true,
                false,
                // Not creepers, nor what sleeps: a boss, or a Citadel Automaton dormant at its post, is left be
                // unless it comes at us.
                e -> !(e instanceof EntityCreeper) && !(e instanceof net.minecraft.entity.boss.IBossDisplayData)
                    && !(e instanceof EntityCitadelAutomaton && ((EntityCitadelAutomaton) e).dormant())));
    }

    /** Stands a repaired Automaton up for {@code master}, with {@code charge} joules in its new heart. */
    public void repair(EntityPlayer master, double charge) {
        owner = master.getUniqueID()
            .toString();
        this.charge = charge;
        setDormant(charge <= 0.0);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(DORMANT, (byte) 0);
        dataWatcher.addObject(SHIELD, (byte) 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(MAX_HEALTH);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.27);
        getAttributeMap().registerAttribute(SharedMonsterAttributes.attackDamage);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(7.0);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(20.0);
        getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(0.5);
    }

    @Override
    public boolean isAIEnabled() {
        return true;
    }

    @Override
    public int getTotalArmorValue() {
        return EntityCitadelAutomaton.ARMOUR;
    }

    @Override
    public boolean dormant() {
        return dataWatcher.getWatchableObjectByte(DORMANT) != 0;
    }

    private void setDormant(boolean value) {
        dataWatcher.updateObject(DORMANT, (byte) (value ? 1 : 0));
    }

    @Override
    public boolean shieldUp() {
        return dataWatcher.getWatchableObjectByte(SHIELD) != 0;
    }

    @Override
    public boolean owned() {
        return true;
    }

    public double charge() {
        return charge;
    }

    public EntityPlayer master() {
        if (owner.isEmpty()) return null;
        try {
            return worldObj.func_152378_a(UUID.fromString(owner));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // --- Its life -------------------------------------------------------------------------------------------

    @Override
    protected void updateAITasks() {
        if (dormant()) {
            motionX = motionZ = 0.0;
            return;
        }
        super.updateAITasks();
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (worldObj.isRemote) return;
        EntityLivingBase target = getAttackTarget();
        // It spends its heart a second at a time.
        if (!dormant() && ticksExisted % 20 == 0) {
            charge -= target != null ? FIGHTING_WATTS : IDLE_WATTS;
            if (charge <= 0.0) {
                charge = 0.0;
                setDormant(true);
                setAttackTarget(null);
                worldObj.playSoundAtEntity(this, "random.fizz", 0.6F, 0.5F);
                EntityPlayer master = master();
                if (master != null && master.getDistanceSqToEntity(this) < 64 * 64)
                    master.addChatMessage(new ChatComponentText("§7Your Automaton's heart has run down; it kneels."));
            }
        }
        boolean up = target != null && !dormant();
        if (up != shieldUp()) dataWatcher.updateObject(SHIELD, (byte) (up ? 1 : 0));
        if (dormant() || ticksExisted % 10 != 0) return;
        EntityPlayer master = master();
        if (master == null) return;
        // Whatever hurts its master, it goes for.
        EntityLivingBase foe = master.getAITarget();
        if (foe != null && foe != this && foe.isEntityAlive() && !(foe instanceof EntityRepairedAutomaton))
            setAttackTarget(foe);
        if (staying || target != null) return;
        double d = getDistanceSqToEntity(master);
        if (d > TELEPORT * TELEPORT && master.onGround)
            setLocationAndAngles(master.posX, master.posY, master.posZ, rotationYaw, 0.0F);
        else if (d > FOLLOW * FOLLOW) getNavigator().tryMoveToEntityLiving(master, 1.0);
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        float damage = (float) getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
        swingItem();
        boolean hit = target.attackEntityFrom(DamageSource.causeMobDamage(this), damage);
        if (hit) worldObj.playSoundAtEntity(this, "random.break", 0.4F, 0.6F);
        return hit;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        // Its master's blows do not land on it (it would only stand there).
        if (source.getEntity() instanceof EntityPlayer && owner.equals(
            ((EntityPlayer) source.getEntity()).getUniqueID()
                .toString()))
            return false;
        return super.attackEntityFrom(source, amount);
    }

    // --- Its master's hand ----------------------------------------------------------------------------------

    @Override
    protected boolean interact(EntityPlayer player) {
        if (worldObj.isRemote) return true;
        if (!owner.equals(
            player.getUniqueID()
                .toString()))
            return false;
        ItemStack held = player.getCurrentEquippedItem();
        if (held != null && held.getItem() == NEItems.stormglassHeart) {
            charge += TileHeartSocket.heartCharge(held);
            if (!player.capabilities.isCreativeMode && --held.stackSize <= 0)
                player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            setDormant(false);
            worldObj.playSoundAtEntity(this, "random.fizz", 0.8F, 1.4F);
            if (worldObj instanceof WorldServer)
                ((WorldServer) worldObj).func_147487_a("cloud", posX, posY + 1.2, posZ, 8, 0.2, 0.3, 0.2, 0.02);
            player.addChatMessage(
                new ChatComponentText(String.format("§7A new heart: its charge is %.0f kJ.", charge / 1000.0)));
            return true;
        }
        if (held != null && held.getItem() == Items.iron_ingot && getHealth() < getMaxHealth()) {
            heal(IRON_MENDS);
            if (!player.capabilities.isCreativeMode && --held.stackSize <= 0)
                player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            worldObj.playSoundAtEntity(this, "random.anvil_use", 0.5F, 1.4F);
            return true;
        }
        if (held != null && held.getItem() == NEItems.multimeter) {
            player.addChatMessage(
                new ChatComponentText(
                    String.format(
                        "§eYour Automaton: %.0f kJ in its heart (about %d minutes standing), %.0f / %.0f health, %s",
                        charge / 1000.0,
                        (int) (charge / IDLE_WATTS / 60.0),
                        getHealth(),
                        getMaxHealth(),
                        staying ? "holding its spot" : "following you")));
            return true;
        }
        if (held == null) {
            staying = !staying;
            if (staying) setHomeArea((int) Math.floor(posX), (int) Math.floor(posY), (int) Math.floor(posZ), 6);
            else detachHome();
            player.addChatMessage(new ChatComponentText(staying ? "§7It holds this spot." : "§7It follows you."));
            worldObj.playSoundAtEntity(this, "random.click", 0.5F, 0.8F);
            return true;
        }
        return false;
    }

    // --- Saving ---------------------------------------------------------------------------------------------

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setString("Owner", owner);
        tag.setBoolean("Staying", staying);
        tag.setDouble("Charge", charge);
        tag.setBoolean("Dormant", dormant());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        owner = tag.getString("Owner");
        staying = tag.getBoolean("Staying");
        charge = tag.getDouble("Charge");
        setDormant(tag.getBoolean("Dormant"));
        if (staying) setHomeArea((int) Math.floor(posX), (int) Math.floor(posY), (int) Math.floor(posZ), 6);
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        for (ItemStack stack : naturalexceptions.minecraft.block.BlockBrokenAutomaton.salvage(rand, 0))
            entityDropItem(stack, 0.5F);
    }

    @Override
    protected Item getDropItem() {
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
        return 1.3F;
    }

    @Override
    protected void func_145780_a(int x, int y, int z, net.minecraft.block.Block block) {
        playSound("mob.irongolem.walk", 0.3F, 1.5F);
    }
}
