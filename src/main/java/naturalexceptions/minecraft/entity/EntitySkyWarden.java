package naturalexceptions.minecraft.entity;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentHelper;
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
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import naturalexceptions.minecraft.block.BlockSealedAlcove;
import naturalexceptions.minecraft.gear.Treasure;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;
import naturalexceptions.minecraft.weapon.EntityStormglassBolt;
import naturalexceptions.minecraft.worldgen.SkyCacheLoot;

/**
 * The Sky Warden: a guardian of Skystone and Stormglass left to keep a sky dungeon's vault. Slow and heavy; its danger
 * is wind. Every eight seconds it raises its fists and, a second later, a Gale Slam throws everyone near it back
 * (towards the pits of its hall). Below half health it comes Unbound: faster, firing fans of Stormglass bolts at anyone
 * keeping their distance, and it breaks open the hall's alcoves. Its big attacks are always telegraphed.
 */
public final class EntitySkyWarden extends EntityMob implements IBossDisplayData {

    private static final int FLAGS = 16;
    static final int CHARGING_SLAM = 1;
    static final int CHARGING_FAN = 2;
    static final int UNBOUND = 4;

    private int slamTimer = WardenRules.SLAM_INTERVAL / 2;
    private int fanTimer;
    private int windup;
    private boolean alcovesOpened;

    public EntitySkyWarden(World world) {
        super(world);
        setSize(1.2F, 2.6F);
        isImmuneToFire = true;
        experienceValue = WardenRules.XP;
        func_110163_bv(); // it keeps its hall: never despawns
        getNavigator().setAvoidsWater(true);
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(2, new EntityAIAttackOnCollide(this, EntityPlayer.class, 1.0, false));
        tasks.addTask(4, new EntityAIMoveTowardsRestriction(this, 1.0));
        tasks.addTask(6, new EntityAIWander(this, 0.6));
        tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 12.0F));
        tasks.addTask(8, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true));
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(FLAGS, (byte) 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(WardenRules.MAX_HEALTH);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(WardenRules.SPEED_BOUND);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(WardenRules.MELEE_DAMAGE);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(24.0);
        getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(WardenRules.KNOCKBACK_RESISTANCE);
    }

    @Override
    public boolean isAIEnabled() {
        return true;
    }

    @Override
    public int getTotalArmorValue() {
        return WardenRules.ARMOUR;
    }

    // --- State shared with the client (for the model and particles) -------------------------------------------

    private boolean wardenFlag(int bit) {
        return (dataWatcher.getWatchableObjectByte(FLAGS) & bit) != 0;
    }

    private void setWardenFlag(int bit, boolean on) {
        byte flags = dataWatcher.getWatchableObjectByte(FLAGS);
        dataWatcher.updateObject(FLAGS, (byte) (on ? flags | bit : flags & ~bit));
    }

    public boolean chargingSlam() {
        return wardenFlag(CHARGING_SLAM);
    }

    public boolean chargingFan() {
        return wardenFlag(CHARGING_FAN);
    }

    public boolean unbound() {
        return wardenFlag(UNBOUND);
    }

    // --- Behaviour ------------------------------------------------------------------------------------------

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (worldObj.isRemote) {
            clientEffects();
            return;
        }
        if (!unbound() && WardenRules.phase(getHealth(), getMaxHealth()) == WardenRules.Phase.UNBOUND) unbind();
        EntityLivingBase target = getAttackTarget();
        if (windup > 0) {
            getNavigator().clearPathEntity();
            if (target != null) getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
            if (--windup == 0) {
                if (chargingSlam()) slam();
                else if (chargingFan() && target != null) fan(target);
                setWardenFlag(CHARGING_SLAM, false);
                setWardenFlag(CHARGING_FAN, false);
            }
            return;
        }
        slamTimer++;
        fanTimer++;
        if (target == null) return;
        double distance = getDistanceToEntity(target);
        if (slamTimer >= WardenRules.SLAM_INTERVAL && distance < WardenRules.SLAM_RADIUS + 2.0) {
            slamTimer = 0;
            windup = WardenRules.SLAM_WINDUP;
            setWardenFlag(CHARGING_SLAM, true);
            worldObj.playSoundAtEntity(this, "naturalexceptions:warden.charge", 1.5F, 1.0F);
        } else if (unbound() && fanTimer >= WardenRules.FAN_INTERVAL
            && distance > WardenRules.FAN_MIN_RANGE
            && canEntityBeSeen(target)) {
                fanTimer = 0;
                windup = WardenRules.FAN_WINDUP;
                setWardenFlag(CHARGING_FAN, true);
                worldObj.playSoundAtEntity(this, "naturalexceptions:weapon.shard_fire", 1.2F, 0.6F);
            }
    }

    /** The Gale Slam: everyone close is thrown back and up, harder the closer they stood. */
    private void slam() {
        worldObj.playSoundAtEntity(this, "naturalexceptions:warden.slam", 2.0F, 1.0F);
        if (worldObj instanceof WorldServer) {
            WorldServer server = (WorldServer) worldObj;
            server.func_147487_a("largeexplode", posX, posY + 0.5, posZ, 1, 0.0, 0.0, 0.0, 0.0);
            for (int i = 0; i < 32; i++) {
                double a = i * Math.PI * 2.0 / 32.0;
                server.func_147487_a(
                    "cloud",
                    posX + Math.cos(a) * 1.5,
                    posY + 0.3,
                    posZ + Math.sin(a) * 1.5,
                    0,
                    Math.cos(a),
                    0.05,
                    Math.sin(a),
                    0.6);
            }
        }
        @SuppressWarnings("unchecked")
        List<EntityPlayer> players = worldObj.getEntitiesWithinAABB(
            EntityPlayer.class,
            boundingBox.expand(WardenRules.SLAM_RADIUS, 2.0, WardenRules.SLAM_RADIUS));
        for (EntityPlayer player : players) {
            double dx = player.posX - posX, dz = player.posZ - posZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            double strength = WardenRules.slamStrength(distance);
            if (strength <= 0.0 || player.capabilities.isCreativeMode) continue;
            if (distance < 1.0E-3) {
                dx = 1.0;
                distance = 1.0;
            }
            player.attackEntityFrom(DamageSource.causeMobDamage(this), WardenRules.SLAM_DAMAGE);
            player.motionX = dx / distance * WardenRules.SLAM_PUSH * strength;
            player.motionZ = dz / distance * WardenRules.SLAM_PUSH * strength;
            player.motionY = WardenRules.SLAM_LIFT * strength;
            player.velocityChanged = true;
        }
    }

    /** A fan of Stormglass bolts at a target keeping its distance. */
    private void fan(EntityLivingBase target) {
        double dx = target.posX - posX, dy = target.posY + target.getEyeHeight() * 0.7 - (posY + height * 0.75),
            dz = target.posZ - posZ;
        double flat = Math.sqrt(dx * dx + dz * dz);
        for (int i = 0; i < WardenRules.FAN_BOLTS; i++) {
            double angle = Math.toRadians((i - (WardenRules.FAN_BOLTS - 1) / 2.0) * WardenRules.FAN_SPREAD_DEGREES);
            double rx = dx * Math.cos(angle) - dz * Math.sin(angle), rz = dx * Math.sin(angle) + dz * Math.cos(angle);
            EntityStormglassBolt bolt = new EntityStormglassBolt(worldObj, this);
            bolt.setPosition(posX, posY + height * 0.75, posZ);
            bolt.setThrowableHeading(rx, dy + flat * 0.02, rz, 1.8F, 1.0F);
            worldObj.spawnEntityInWorld(bolt);
        }
    }

    /** Half health: it comes Unbound. Faster, its cracks glow, and the alcoves of its hall break open. */
    private void unbind() {
        setWardenFlag(UNBOUND, true);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(WardenRules.SPEED_UNBOUND);
        worldObj.playSoundAtEntity(this, "naturalexceptions:warden.phase", 2.0F, 1.0F);
        if (alcovesOpened) return;
        alcovesOpened = true;
        int x = MathHelper.floor_double(posX), y = MathHelper.floor_double(posY), z = MathHelper.floor_double(posZ);
        for (int dx = -12; dx <= 12; dx++) for (int dy = -3; dy <= 6; dy++) for (int dz = -12; dz <= 12; dz++) {
            Block block = worldObj.getBlock(x + dx, y + dy, z + dz);
            if (block instanceof BlockSealedAlcove) BlockSealedAlcove.release(worldObj, x + dx, y + dy, z + dz);
        }
    }

    private void clientEffects() {
        if (chargingSlam()) {
            for (int i = 0; i < 3; i++) {
                double a = (ticksExisted * 0.4 + i * 2.1) % (Math.PI * 2.0);
                worldObj.spawnParticle(
                    "cloud",
                    posX + Math.cos(a) * 1.4,
                    posY + 0.4 + rand.nextDouble() * 2.0,
                    posZ + Math.sin(a) * 1.4,
                    -Math.sin(a) * 0.15,
                    0.05,
                    Math.cos(a) * 0.15);
            }
        }
        if (chargingFan() || unbound() && rand.nextInt(3) == 0) worldObj.spawnParticle(
            "reddust",
            posX + (rand.nextDouble() - 0.5) * width,
            posY + rand.nextDouble() * height,
            posZ + (rand.nextDouble() - 0.5) * width,
            0.45,
            0.85,
            1.0);
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        boolean hit = super.attackEntityAsMob(target);
        if (hit) worldObj.playSoundAtEntity(this, "mob.irongolem.throw", 1.0F, 0.8F);
        return hit;
    }

    /** When the Warden falls, the vault's seal breaks. */
    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        if (worldObj.isRemote) return;
        net.minecraft.util.ChunkCoordinates home = getHomePosition();
        boolean hasHome = hasHome() && home != null;
        int x = hasHome ? home.posX : MathHelper.floor_double(posX);
        int y = hasHome ? home.posY : MathHelper.floor_double(posY);
        int z = hasHome ? home.posZ : MathHelper.floor_double(posZ);
        int opened = naturalexceptions.minecraft.block.BlockVaultGate.breakWardenSeals(worldObj, x, y, z, SEAL_REACH);
        if (opened > 0) {
            @SuppressWarnings("unchecked")
            List<EntityPlayer> players = worldObj.playerEntities;
            for (EntityPlayer player : players) if (player.getDistanceSqToEntity(this) < 48.0 * 48.0) player
                .addChatMessage(new net.minecraft.util.ChatComponentText("\u00a75\u00a7lThe vault's seal breaks."));
        }
    }

    /** How far from its hall the Warden's fall reaches to break the vault's seal. */
    static final int SEAL_REACH = 32;

    @Override
    protected void fall(float distance) {}

    @Override
    protected boolean canDespawn() {
        return false;
    }

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
        return "naturalexceptions:warden.roar";
    }

    @Override
    protected void func_145780_a(int x, int y, int z, Block block) {
        playSound("mob.irongolem.walk", 1.0F, 0.8F);
    }

    @Override
    protected float getSoundPitch() {
        return 0.8F + rand.nextFloat() * 0.1F;
    }

    // --- Drops ----------------------------------------------------------------------------------------------

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        for (ItemStack stack : drops(rand)) entityDropItem(stack, 0.5F);
    }

    /** Everything a Warden leaves behind: plenty of materials, Gale Cores, one relic, maybe more. */
    public static List<ItemStack> drops(Random random) {
        List<ItemStack> drops = new java.util.ArrayList<>();
        int[] counts = WardenRules.guaranteed(random);
        add(drops, NEItems.stormglassShard, counts[0]);
        add(drops, Item.getItemFromBlock(NEBlocks.skystoneBricks), counts[1]);
        add(drops, Items.gold_ingot, counts[2]);
        add(drops, Items.diamond, counts[3]);
        add(drops, NEItems.galeCore, counts[4]);
        Treasure relic = WardenRules.relic(random);
        add(drops, relic == null ? NEItems.galeMaul : NEItems.TREASURES.get(relic), 1);
        if (random.nextDouble() < WardenRules.ENCHANTED_CHANCE) {
            Item[] bases = { Items.book, Items.iron_sword, Items.diamond_sword, Items.iron_pickaxe,
                Items.diamond_pickaxe };
            ItemStack stack = new ItemStack(bases[random.nextInt(bases.length)]);
            try {
                stack = EnchantmentHelper.addRandomEnchantment(random, stack, 20 + random.nextInt(11));
            } catch (RuntimeException ignored) {
                // Enchantments not set up (tests): the plain item is still a fine prize.
            }
            drops.add(stack);
        }
        if (random.nextDouble() < WardenRules.TREASURE_CHANCE) {
            List<Item> pool = SkyCacheLoot.treasures();
            if (!pool.isEmpty()) drops.add(new ItemStack(pool.get(random.nextInt(pool.size()))));
        }
        return drops;
    }

    private static void add(List<ItemStack> drops, Item item, int count) {
        if (item == null || count <= 0) return;
        while (count > 0) {
            int size = Math.min(count, item.getItemStackLimit());
            drops.add(new ItemStack(item, size));
            count -= size;
        }
    }

    // --- Saving ---------------------------------------------------------------------------------------------

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setBoolean("Unbound", unbound());
        tag.setBoolean("AlcovesOpened", alcovesOpened);
        tag.setInteger("SlamTimer", slamTimer);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        alcovesOpened = tag.getBoolean("AlcovesOpened");
        slamTimer = tag.getInteger("SlamTimer");
        if (tag.getBoolean("Unbound")) {
            setWardenFlag(UNBOUND, true);
            getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(WardenRules.SPEED_UNBOUND);
        }
    }
}
