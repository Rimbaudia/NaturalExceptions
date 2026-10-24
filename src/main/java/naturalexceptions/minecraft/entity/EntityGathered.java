package naturalexceptions.minecraft.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import naturalexceptions.minecraft.furniture.BlockCandelabra;
import naturalexceptions.minecraft.gear.Treasure;
import naturalexceptions.minecraft.registry.NEBlocks;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * The Gathered: the dead of a fallen city, gathered in their holy place and grown together in the pale light of
 * Stormglass. A towering knot of shrouds and reaching arms, faces pressed in its chest, it drifts above the floor of
 * its
 * hall. Blows mostly pass through the dead; but candles stand round its hall, and while every one of them burns it
 * kneels, and can be struck as flesh. It reaches for whoever is near; it wails, and the wail hurts, weakens, and puts
 * candles out. Torn to half, faces tear loose from it and fly as shades. Laid to rest, it leaves the Mourner's Veil,
 * one
 * more of its relics, and the wind it held.
 */
public final class EntityGathered extends EntityMob implements IBossDisplayData {

    public static final int NONE = 0, GRASP = 1, WAIL = 2;
    private static final int KNEELING = 16, WINDING = 17;

    private final List<int[]> candles = new ArrayList<>();
    private boolean candlesFound;
    private int windup, graspCooldown, wailTimer = GatheredRules.WAIL_INTERVAL / 2, kneel, shadeTimer;
    private boolean torn;
    private final List<EntityGatheredShade> shades = new ArrayList<>();

    public EntityGathered(World world) {
        super(world);
        setSize(2.0F, 3.6F);
        isImmuneToFire = true;
        experienceValue = GatheredRules.XP;
        func_110163_bv(); // it keeps its hall
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(KNEELING, (byte) 0);
        dataWatcher.addObject(WINDING, (byte) NONE);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(GatheredRules.MAX_HEALTH);
        getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(GatheredRules.SPEED);
        getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(32.0);
        getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(1.0);
    }

    @Override
    public EnumCreatureAttribute getCreatureAttribute() {
        return EnumCreatureAttribute.UNDEAD;
    }

    public boolean kneeling() {
        return dataWatcher.getWatchableObjectByte(KNEELING) != 0;
    }

    /** What it is winding up ({@link #GRASP}, {@link #WAIL}), for the model. */
    public int winding() {
        return dataWatcher.getWatchableObjectByte(WINDING);
    }

    private void wind(int what) {
        dataWatcher.updateObject(WINDING, (byte) what);
    }

    // --- The candles ----------------------------------------------------------------------------------------

    /** The candelabra round its hall, found once. */
    private void findCandles() {
        candlesFound = true;
        if (NEBlocks.candelabra == null) return;
        net.minecraft.util.ChunkCoordinates home = getHomePosition();
        int cx = hasHome() && home != null ? home.posX : MathHelper.floor_double(posX);
        int cy = hasHome() && home != null ? home.posY : MathHelper.floor_double(posY);
        int cz = hasHome() && home != null ? home.posZ : MathHelper.floor_double(posZ);
        int r = GatheredRules.CANDLE_REACH;
        for (int x = cx - r; x <= cx + r; x++)
            for (int z = cz - r; z <= cz + r; z++) for (int y = cy - 3; y <= cy + 4; y++)
                if (worldObj.getBlock(x, y, z) == NEBlocks.candelabra) candles.add(new int[] { x, y, z });
    }

    private int litCandles() {
        int lit = 0;
        for (int[] c : candles) if (worldObj.getBlock(c[0], c[1], c[2]) == NEBlocks.candelabra
            && BlockCandelabra.lit(worldObj, c[0], c[1], c[2])) lit++;
        return lit;
    }

    private int standingCandles() {
        int n = 0;
        for (int[] c : candles) if (worldObj.getBlock(c[0], c[1], c[2]) == NEBlocks.candelabra) n++;
        return n;
    }

    /** Puts out up to {@code count} lit candles, the nearest to whoever it is angry with first. */
    private void snuff(int count) {
        for (int[] c : candles) {
            if (count <= 0) return;
            if (worldObj.getBlock(c[0], c[1], c[2]) != NEBlocks.candelabra
                || !BlockCandelabra.lit(worldObj, c[0], c[1], c[2])) continue;
            BlockCandelabra.light(worldObj, c[0], c[1], c[2], false);
            count--;
        }
    }

    // --- Being struck ---------------------------------------------------------------------------------------

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source.isFireDamage() || source == DamageSource.inWall || source == DamageSource.drown) return false;
        boolean flesh = kneeling();
        if (!flesh && !worldObj.isRemote && source.getEntity() != null) {
            worldObj.playSoundAtEntity(this, "mob.ghast.moan", 0.5F, 1.8F);
            if (worldObj instanceof WorldServer) ((WorldServer) worldObj)
                .func_147487_a("instantSpell", posX, posY + height * 0.5, posZ, 6, 0.4, 0.6, 0.4, 0.02);
        }
        return super.attackEntityFrom(source, amount * GatheredRules.through(flesh));
    }

    // --- Its life -------------------------------------------------------------------------------------------

    @Override
    protected void updateEntityActionState() {
        // It drifts at whoever troubles it (see onLivingUpdate); no walking paths.
        entityToAttack = worldObj.getClosestVulnerablePlayerToEntity(this, 32.0);
        if (entityToAttack != null) faceEntity(entityToAttack, 10.0F, 20.0F);
    }

    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        moveEntity(motionX, motionY, motionZ);
        motionX *= 0.85;
        motionY *= 0.85;
        motionZ *= 0.85;
    }

    @Override
    protected void fall(float distance) {}

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (worldObj.isRemote) {
            if (rand.nextInt(2) == 0) worldObj.spawnParticle(
                kneeling() ? "flame" : "instantSpell",
                posX + (rand.nextDouble() - 0.5) * width,
                posY + rand.nextDouble() * height,
                posZ + (rand.nextDouble() - 0.5) * width,
                0.0,
                0.02,
                0.0);
            return;
        }
        if (!candlesFound) findCandles();
        hover();
        if (kneeling()) {
            if (--kneel <= 0) rise();
            return;
        }
        if (GatheredRules.kneels(standingCandles(), litCandles())) {
            kneelDown();
            return;
        }
        if (!torn && GatheredRules.torn(getHealth())) tear();
        if (torn && --shadeTimer <= 0) {
            shadeTimer = GatheredRules.SHADE_INTERVAL;
            loose(GatheredRules.SHADES_LATER);
        }
        if (graspCooldown > 0) graspCooldown--;
        EntityPlayer target = worldObj.getClosestVulnerablePlayerToEntity(this, 32.0);
        if (windup > 0) {
            if (--windup == 0) strike(target);
            return;
        }
        if (--wailTimer <= 0) {
            begin(WAIL, GatheredRules.WAIL_WINDUP);
            wailTimer = torn ? GatheredRules.WAIL_INTERVAL_TORN : GatheredRules.WAIL_INTERVAL;
            return;
        }
        if (target == null) return;
        double d = getDistanceToEntity(target);
        if (graspCooldown == 0 && d <= GatheredRules.GRASP_REACH) begin(GRASP, GatheredRules.GRASP_WINDUP);
        else if (d > 2.5) drift(target.posX, target.posZ, GatheredRules.SPEED);
    }

    /** It hangs a little above the floor beneath it, bobbing. */
    private void hover() {
        int x = MathHelper.floor_double(posX), z = MathHelper.floor_double(posZ);
        int y = MathHelper.floor_double(posY);
        int floor = y;
        while (floor > y - 6 && worldObj.isAirBlock(x, floor - 1, z)) floor--;
        double want = floor + GatheredRules.HOVER + (kneeling() ? -0.5 : MathHelper.sin(ticksExisted * 0.05F) * 0.2);
        motionY += (want - posY) * 0.05;
    }

    private void drift(double tx, double tz, double speed) {
        double dx = tx - posX, dz = tz - posZ, d = Math.sqrt(dx * dx + dz * dz);
        if (d < 1.0E-3) return;
        motionX += dx / d * speed;
        motionZ += dz / d * speed;
    }

    private void begin(int what, int ticks) {
        wind(what);
        windup = ticks;
        worldObj.playSoundAtEntity(this, what == WAIL ? "mob.ghast.moan" : "mob.ghast.affectionate_scream", 2.0F, 0.5F);
    }

    private void strike(EntityPlayer target) {
        int what = winding();
        wind(NONE);
        if (what == GRASP) {
            graspCooldown = GatheredRules.GRASP_COOLDOWN;
            grasp();
        } else if (what == WAIL) wail();
    }

    /** Its arms close on everyone near it: they are struck and held back. */
    private void grasp() {
        worldObj.playSoundAtEntity(this, "mob.ghast.scream", 1.5F, 0.4F);
        for (EntityLivingBase living : near(GatheredRules.GRASP_REACH)) {
            living.attackEntityFrom(DamageSource.causeMobDamage(this), GatheredRules.GRASP_DAMAGE);
            living.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 60, 1));
        }
    }

    /** It wails: everyone in its hall is hurt and weakened, and candles go out. */
    private void wail() {
        worldObj.playSoundAtEntity(this, "mob.ghast.death", 3.0F, 0.3F);
        if (worldObj instanceof WorldServer) ((WorldServer) worldObj)
            .func_147487_a("mobSpellAmbient", posX, posY + height * 0.6, posZ, 60, 3.0, 1.5, 3.0, 0.1);
        for (EntityLivingBase living : near(GatheredRules.WAIL_REACH)) {
            living.attackEntityFrom(
                DamageSource.causeMobDamage(this)
                    .setDamageBypassesArmor(),
                GatheredRules.WAIL_DAMAGE);
            living.addPotionEffect(new PotionEffect(Potion.weakness.id, 100, 0));
        }
        snuff(GatheredRules.snuffs(torn));
    }

    private List<EntityLivingBase> near(double reach) {
        List<EntityLivingBase> out = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<EntityLivingBase> all = worldObj
            .getEntitiesWithinAABB(EntityLivingBase.class, boundingBox.expand(reach, reach / 2, reach));
        for (EntityLivingBase living : all) if (living != this && !(living instanceof EntityGatheredShade)
            && getDistanceToEntity(living) <= reach + width / 2) out.add(living);
        return out;
    }

    /** Every candle burns: it sinks to its knees, and for a while it is flesh. */
    private void kneelDown() {
        dataWatcher.updateObject(KNEELING, (byte) 1);
        kneel = GatheredRules.KNEEL_TICKS;
        windup = 0;
        wind(NONE);
        motionX = motionZ = 0.0;
        worldObj.playSoundAtEntity(this, "mob.ghast.moan", 2.5F, 0.3F);
        for (EntityPlayer player : players(40.0))
            player.addChatMessage(new ChatComponentText("§eThe Gathered kneel before the candles."));
    }

    /** It rises, and its breath puts every candle out. */
    private void rise() {
        dataWatcher.updateObject(KNEELING, (byte) 0);
        snuff(candles.size());
        wailTimer = GatheredRules.WAIL_INTERVAL;
        worldObj.playSoundAtEntity(this, "mob.ghast.death", 3.0F, 0.5F);
    }

    /** Torn to half: faces tear loose from it and fly. */
    private void tear() {
        torn = true;
        shadeTimer = GatheredRules.SHADE_INTERVAL;
        worldObj.playSoundAtEntity(this, "mob.ghast.scream", 3.0F, 0.6F);
        loose(GatheredRules.SHADES_AT_TEAR);
    }

    private void loose(int count) {
        shades.removeIf(s -> s.isDead);
        for (int i = 0; i < count && shades.size() < GatheredRules.MAX_SHADES; i++) {
            EntityGatheredShade shade = new EntityGatheredShade(worldObj, this);
            shade.setLocationAndAngles(
                posX + (rand.nextDouble() - 0.5) * 2.0,
                posY + height * 0.6,
                posZ + (rand.nextDouble() - 0.5) * 2.0,
                rand.nextFloat() * 360.0F,
                0.0F);
            worldObj.spawnEntityInWorld(shade);
            shades.add(shade);
        }
    }

    private List<EntityPlayer> players(double reach) {
        List<EntityPlayer> out = new ArrayList<>();
        for (Object o : worldObj.playerEntities)
            if (((EntityPlayer) o).getDistanceSqToEntity(this) < reach * reach) out.add((EntityPlayer) o);
        return out;
    }

    // --- Laid to rest ---------------------------------------------------------------------------------------

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        if (worldObj.isRemote) return;
        for (int[] c : candles) if (worldObj.getBlock(c[0], c[1], c[2]) == NEBlocks.candelabra)
            BlockCandelabra.light(worldObj, c[0], c[1], c[2], true);
        net.minecraft.util.ChunkCoordinates home = getHomePosition();
        boolean hasHome = hasHome() && home != null;
        int x = hasHome ? home.posX : MathHelper.floor_double(posX);
        int y = hasHome ? home.posY : MathHelper.floor_double(posY);
        int z = hasHome ? home.posZ : MathHelper.floor_double(posZ);
        naturalexceptions.minecraft.block.BlockVaultGate.breakWardenSeals(worldObj, x, y, z, 40);
        for (EntityPlayer player : players(48.0))
            player.addChatMessage(new ChatComponentText("§e§oThe Gathered are at rest. The vault stands open."));
    }

    @Override
    protected void dropFewItems(boolean recentlyHit, int looting) {
        for (ItemStack stack : drops(rand)) entityDropItem(stack, 1.0F);
    }

    /** The Mourner's Veil always; one more of its relics; and the wind it held, with what the dead were buried with. */
    public static List<ItemStack> drops(Random random) {
        List<ItemStack> drops = new ArrayList<>();
        Item veil = NEItems.TREASURES.get(Treasure.MOURNERS_VEIL);
        if (veil != null) drops.add(new ItemStack(veil));
        Item[] more = { NEItems.bellOfRest, NEItems.reliquaryLantern, NEItems.TREASURES.get(Treasure.GATHERED_HEART) };
        Item pick = more[random.nextInt(more.length)];
        if (pick != null) drops.add(new ItemStack(pick));
        int[] counts = WardenRules.guaranteed(random);
        if (NEItems.stormglassShard != null) drops.add(new ItemStack(NEItems.stormglassShard, counts[0]));
        drops.add(new ItemStack(Items.gold_ingot, counts[2]));
        if (counts[3] > 0) drops.add(new ItemStack(Items.diamond, counts[3]));
        if (NEItems.galeCore != null && counts[4] > 0) drops.add(new ItemStack(NEItems.galeCore, counts[4]));
        return drops;
    }

    @Override
    protected Item getDropItem() {
        return null;
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    protected boolean isValidLightLevel() {
        return true;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setBoolean("Torn", torn);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        torn = tag.getBoolean("Torn");
    }

    // --- Sounds ---------------------------------------------------------------------------------------------

    @Override
    protected String getLivingSound() {
        return "mob.ghast.moan";
    }

    @Override
    protected float getSoundPitch() {
        return 0.4F + rand.nextFloat() * 0.1F;
    }

    @Override
    protected String getHurtSound() {
        return "mob.ghast.scream";
    }

    @Override
    protected String getDeathSound() {
        return "mob.ghast.death";
    }
}
