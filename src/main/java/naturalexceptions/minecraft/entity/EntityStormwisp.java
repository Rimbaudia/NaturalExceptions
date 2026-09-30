package naturalexceptions.minecraft.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import naturalexceptions.minecraft.registry.NEItems;
import naturalexceptions.minecraft.weapon.EntityStormglassBolt;

/**
 * A Stormwisp: three splinters of Stormglass circling a crackling core, drifting through the sky dungeons at head
 * height. It keeps its distance and circles; every few seconds it gathers light (brighter, crackling, a rising chime)
 * and looses a Stormglass bolt. Fragile, and light enough that a sword blow sends it flying.
 */
public final class EntityStormwisp extends EntityMob {

    public static final float MAX_HEALTH = 12.0F;
    /** How close and how far it likes to hover from its target. */
    static final double NEAR = 5.0, FAR = 9.0;
    static final int CHARGE_TICKS = 20;
    /** Chance it carries a Sky Key. */
    public static final double KEY_CHANCE = 0.02;

    private static final int CHARGING = 16;

    private int shotTimer = 40;
    private int charge;
    /** Which way it circles, and for how long before it turns about. */
    private int circle = 1, circleTicks;

    public EntityStormwisp(World world) {
        super(world);
        setSize(0.7F, 0.9F);
        isImmuneToFire = true;
        experienceValue = 6;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(CHARGING, (byte) 0);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(MAX_HEALTH);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(20.0);
        getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(2.0);
    }

    /** How far through gathering its light it is, 0 to 1 (for the renderer). */
    public float chargeLevel() {
        return (dataWatcher.getWatchableObjectByte(CHARGING) & 0xFF) / (float) CHARGE_TICKS;
    }

    // --- Flight ---------------------------------------------------------------------------------------------

    /** It flies: no gravity, a light drag. */
    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        moveEntity(motionX, motionY, motionZ);
        motionX *= 0.9;
        motionY *= 0.9;
        motionZ *= 0.9;
    }

    @Override
    public boolean isOnLadder() {
        return false;
    }

    @Override
    protected void fall(float distance) {}

    @Override
    protected void updateFallState(double dy, boolean onGround) {}

    @Override
    protected void updateEntityActionState() {
        despawnEntity();
        EntityPlayer target = entityToAttack instanceof EntityPlayer && entityToAttack.isEntityAlive()
            ? (EntityPlayer) entityToAttack
            : worldObj.getClosestVulnerablePlayerToEntity(this, 16.0);
        entityToAttack = target;
        if (target == null) {
            // Idle: a slow bob where it is.
            motionY += Math.sin(ticksExisted * 0.08) * 0.004;
            charge = 0;
            dataWatcher.updateObject(CHARGING, (byte) 0);
            return;
        }
        double dx = target.posX - posX, dz = target.posZ - posZ;
        double flat = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
        // Towards the ring between NEAR and FAR; round it, sideways; level with the target's head.
        double radial = flat < NEAR ? -1.0 : flat > FAR ? 1.0 : 0.0;
        if (++circleTicks > 60 + rand.nextInt(60)) {
            circle = -circle;
            circleTicks = 0;
        }
        double ax = (dx / flat) * radial * 0.03 + (-dz / flat) * circle * 0.015;
        double az = (dz / flat) * radial * 0.03 + (dx / flat) * circle * 0.015;
        double ay = (target.posY + target.getEyeHeight() + 0.3 - (posY + height / 2)) * 0.02;
        motionX += ax;
        motionZ += az;
        motionY += Math.max(-0.04, Math.min(0.04, ay));
        rotationYaw = renderYawOffset = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;

        if (charge > 0) {
            charge++;
            dataWatcher.updateObject(CHARGING, (byte) charge);
            if (charge >= CHARGE_TICKS) {
                fire(target);
                charge = 0;
                dataWatcher.updateObject(CHARGING, (byte) 0);
                shotTimer = 0;
            }
        } else if (++shotTimer >= 60 + rand.nextInt(20) && flat < 16.0 && canEntityBeSeen(target)) {
            charge = 1;
            worldObj.playSoundAtEntity(this, "note.harp", 0.6F, 1.0F);
        }
    }

    private void fire(EntityPlayer target) {
        EntityStormglassBolt bolt = new EntityStormglassBolt(worldObj, this);
        double y = posY + height / 2;
        bolt.setPosition(posX, y, posZ);
        bolt.setThrowableHeading(
            target.posX - posX,
            target.posY + target.getEyeHeight() * 0.7 - y,
            target.posZ - posZ,
            1.4F,
            2.0F);
        worldObj.spawnEntityInWorld(bolt);
        worldObj.playSoundAtEntity(this, "naturalexceptions:weapon.shard_fire", 0.8F, 1.3F);
    }

    /** Light: a blow sends it much further than a creature of flesh. */
    @Override
    public void knockBack(Entity attacker, float strength, double dx, double dz) {
        super.knockBack(attacker, strength * 2.5F, dx, dz);
    }

    /** It lives where it was made (a spawner, an alcove), lit or not. */
    @Override
    public boolean getCanSpawnHere() {
        return worldObj.checkNoEntityCollision(boundingBox) && worldObj.getCollidingBoundingBoxes(this, boundingBox)
            .isEmpty() && !worldObj.isAnyLiquid(boundingBox);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (worldObj.isRemote && rand.nextInt(chargeLevel() > 0 ? 1 : 4) == 0) worldObj.spawnParticle(
            "reddust",
            posX + (rand.nextDouble() - 0.5) * 0.6,
            posY + height / 2 + (rand.nextDouble() - 0.5) * 0.6,
            posZ + (rand.nextDouble() - 0.5) * 0.6,
            0.72,
            0.58,
            1.0);
    }

    // --- Death and drops ------------------------------------------------------------------------------------

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        if (worldObj instanceof WorldServer && NEItems.stormglassShard != null) {
            WorldServer world = (WorldServer) worldObj;
            world.func_147487_a(
                "iconcrack_" + Item.getIdFromItem(NEItems.stormglassShard),
                posX,
                posY + height / 2,
                posZ,
                16,
                0.2,
                0.2,
                0.2,
                0.15);
            world.func_147487_a("witchMagic", posX, posY + height / 2, posZ, 12, 0.3, 0.3, 0.3, 0.1);
        }
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        for (ItemStack stack : drops(rand, looting)) entityDropItem(stack, 0.3F);
    }

    /** What a Stormwisp leaves: one or two shards (more with Looting), and now and then a Sky Key. */
    public static List<ItemStack> drops(Random random, int looting) {
        List<ItemStack> drops = new ArrayList<>();
        if (NEItems.stormglassShard != null)
            drops.add(new ItemStack(NEItems.stormglassShard, 1 + random.nextInt(2) + random.nextInt(looting + 1)));
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
        return "fireworks.twinkle_far";
    }

    @Override
    protected String getHurtSound() {
        return "dig.glass";
    }

    @Override
    protected String getDeathSound() {
        return "random.glass";
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }

    @Override
    protected float getSoundPitch() {
        return 1.3F + rand.nextFloat() * 0.3F;
    }
}
