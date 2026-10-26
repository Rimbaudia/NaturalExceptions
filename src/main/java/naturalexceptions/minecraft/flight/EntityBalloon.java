package naturalexceptions.minecraft.flight;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.core.flight.BalloonPhysics;
import naturalexceptions.core.power.Wind;
import naturalexceptions.minecraft.registry.NEItems;

/**
 * A hot-air balloon: a basket with a brazier in it, under an envelope on ropes. Its rider works it: hold jump to open
 * the burner, hold back to pull the vent, and let both be to keep the brazier on its pilot. It rises and sinks by its
 * physics ({@link BalloonPhysics}) and goes with the wind, which blows a different way at each height. Use it with
 * charcoal or coal to feed the brazier, empty-handed to climb in; strike it, empty, to fold it up.
 */
public final class EntityBalloon extends Entity {

    /** The most fuel the brazier holds, and how long a piece burns with the burner open (seconds). */
    public static final int FUEL_MAX = 32;
    public static final double BURN_SECONDS = 40.0;
    /** Landing faster than this (m/s) jars whoever rides it. */
    public static final double SAFE_LANDING = 4.0;
    /** The envelope's reach round the basket, and its height over it (blocks). */
    public static final double ENVELOPE_RADIUS = 2.4, ENVELOPE_BOTTOM = 2.5, ENVELOPE_TOP = 8.0;

    /** For the look: the envelope's temperature (tenths of a kelvin), and the brazier (0 out, 1 pilot, 2 open). */
    private static final int KELVIN = 16, FLAME = 17;
    public static final int OUT = 0, PILOT = 1, OPEN = 2;

    private double kelvin = BalloonPhysics.AMBIENT_K, climb, vx, vz, burnLeft;
    /** Pieces of fuel in the brazier: coal and charcoal (kept apart, to give back as they came). */
    private int coal, charcoal;

    @SideOnly(Side.CLIENT)
    private int lerpSteps;
    @SideOnly(Side.CLIENT)
    private double lerpX, lerpY, lerpZ;

    public EntityBalloon(World world) {
        super(world);
        setSize(1.6F, 1.2F);
        preventEntitySpawning = true;
    }

    public EntityBalloon(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y, z);
        prevPosX = x;
        prevPosY = y;
        prevPosZ = z;
    }

    @Override
    protected void entityInit() {
        dataWatcher.addObject(KELVIN, (int) Math.round(BalloonPhysics.AMBIENT_K * 10));
        dataWatcher.addObject(FLAME, (byte) OUT);
    }

    public double kelvin() {
        return dataWatcher.getWatchableObjectInt(KELVIN) / 10.0;
    }

    public int flame() {
        return dataWatcher.getWatchableObjectByte(FLAME);
    }

    // --- Being handled ------------------------------------------------------------------------------------------

    @Override
    public boolean canBeCollidedWith() {
        return !isDead;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return null;
    }

    @Override
    public double getMountedYOffset() {
        return 0.25;
    }

    @Override
    public boolean interactFirst(EntityPlayer player) {
        if (worldObj.isRemote) return true;
        ItemStack held = player.getCurrentEquippedItem();
        if (held != null && held.getItem() == Items.coal) {
            int room = FUEL_MAX - coal - charcoal;
            if (room <= 0) {
                player.addChatComponentMessage(new ChatComponentText("§7The brazier is full."));
                return true;
            }
            int put = player.isSneaking() ? Math.min(room, held.stackSize) : 1;
            if (held.getItemDamage() == 1) charcoal += put;
            else coal += put;
            if (!player.capabilities.isCreativeMode) {
                held.stackSize -= put;
                if (held.stackSize <= 0) player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            }
            worldObj.playSoundAtEntity(this, "dig.gravel", 0.5F, 1.4F);
            return true;
        }
        if (riddenByEntity == null && held == null) {
            player.mountEntity(this);
            player.addChatComponentMessage(
                new ChatComponentText(
                    "§7Hold jump to open the burner, back to pull the vent. Fuel: " + (coal + charcoal)
                        + (coal + charcoal == 1 ? " piece" : " pieces")
                        + "."));
            return true;
        }
        return false;
    }

    /** Struck empty, it folds up into its item, and gives back its fuel. */
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (worldObj.isRemote || isDead || riddenByEntity != null || !(source.getEntity() instanceof EntityPlayer))
            return false;
        if (NEItems.hotAirBalloon != null) entityDropItem(new ItemStack(NEItems.hotAirBalloon), 0.5F);
        if (coal > 0) entityDropItem(new ItemStack(Items.coal, coal, 0), 0.5F);
        if (charcoal > 0) entityDropItem(new ItemStack(Items.coal, charcoal, 1), 0.5F);
        coal = charcoal = 0;
        setDead();
        return true;
    }

    // --- Flying -------------------------------------------------------------------------------------------------

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (worldObj.isRemote) {
            clientTick();
            return;
        }
        double dt = 0.05;
        EntityLivingBase rider = riddenByEntity instanceof EntityLivingBase ? (EntityLivingBase) riddenByEntity : null;
        boolean wantBurner = rider != null && jumping(rider), vent = rider != null && rider.moveForward < -0.1F;
        int flame = coal + charcoal <= 0 && burnLeft <= 0 || rider == null ? OUT : wantBurner ? OPEN : PILOT;
        if (flame != OUT) {
            if (burnLeft <= 0) {
                if (coal > 0) coal--;
                else charcoal--;
                burnLeft += BURN_SECONDS;
            }
            burnLeft -= flame == OPEN ? dt : dt * BalloonPhysics.PILOT_W / BalloonPhysics.BURNER_W;
        }
        // The burner holds back at the cloth's limit.
        double watts = flame == OPEN ? BalloonPhysics.BURNER_W : flame == PILOT ? BalloonPhysics.PILOT_W : 0.0;
        kelvin = BalloonPhysics.warm(kelvin, watts, vent, dt);
        double load = rider == null ? 0.0 : naturalexceptions.core.hoisting.HoistMasses.RIDER;
        climb = BalloonPhysics.climb(climb, kelvin, load, posY, dt);
        double wind = Wind.at(
            (int) Math.floor(posX),
            (int) Math.floor(posY),
            (int) Math.floor(posZ),
            worldObj.getTotalWorldTime(),
            worldObj.isRaining(),
            worldObj.isThundering());
        double[] v = onGround ? new double[] { vx * 0.5, vz * 0.5 }
            : BalloonPhysics.drift(vx, vz, wind, BalloonPhysics.windBearing(posY, worldObj.getTotalWorldTime()), dt);
        vx = v[0];
        vz = v[1];
        if (onGround && climb < 0) climb = 0;

        motionX = vx * dt;
        motionY = climb * dt;
        motionZ = vz * dt;
        // The envelope over the basket meets the world too: it stops against what it would push into.
        AxisAlignedBB envelope = AxisAlignedBB.getBoundingBox(
            posX - ENVELOPE_RADIUS,
            boundingBox.minY + ENVELOPE_BOTTOM,
            posZ - ENVELOPE_RADIUS,
            posX + ENVELOPE_RADIUS,
            boundingBox.minY + ENVELOPE_TOP,
            posZ + ENVELOPE_RADIUS);
        if (motionY > 0 && blocked(envelope.getOffsetBoundingBox(0, motionY, 0))) motionY = climb = 0;
        if (blocked(envelope.getOffsetBoundingBox(motionX, 0, motionZ))) {
            motionX = motionZ = 0;
            vx = vz = 0;
        }
        double before = climb;
        moveEntity(motionX, motionY, motionZ);
        if (isCollidedVertically) {
            if (before < -SAFE_LANDING && rider != null)
                rider.attackEntityFrom(DamageSource.fall, (float) ((-before - SAFE_LANDING) * 2.0));
            climb = 0;
        }
        dataWatcher.updateObject(KELVIN, (int) Math.round(kelvin * 10));
        dataWatcher.updateObject(FLAME, (byte) flame);
        if (flame == OPEN && ticksExisted % 30 == 0)
            worldObj.playSoundAtEntity(this, "naturalexceptions:balloon.burner", 1.0F, 0.9F + rand.nextFloat() * 0.2F);
    }

    private boolean blocked(AxisAlignedBB box) {
        @SuppressWarnings("unchecked")
        List<AxisAlignedBB> boxes = worldObj.func_147461_a(box);
        return !boxes.isEmpty();
    }

    /** It never falls, as a stone does: its landings are judged by how fast it comes down (see onUpdate). */
    @Override
    protected void updateFallState(double distance, boolean onGround) {
        fallDistance = 0.0F;
    }

    private static Field isJumping;

    /** Whether the rider holds jump (sent up by their client as they ride). */
    private static boolean jumping(EntityLivingBase rider) {
        try {
            if (isJumping == null) {
                isJumping = ReflectionHelper.findField(EntityLivingBase.class, "field_70703_bu", "isJumping");
            }
            return isJumping.getBoolean(rider);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    @SideOnly(Side.CLIENT)
    private void clientTick() {
        if (lerpSteps > 0) {
            setPosition(
                posX + (lerpX - posX) / lerpSteps,
                posY + (lerpY - posY) / lerpSteps,
                posZ + (lerpZ - posZ) / lerpSteps);
            lerpSteps--;
        }
        int flame = flame();
        if (flame != OUT && rand.nextInt(flame == OPEN ? 1 : 4) == 0) {
            worldObj.spawnParticle(
                "flame",
                posX + (rand.nextDouble() - 0.5) * 0.3,
                posY + 2.3,
                posZ + (rand.nextDouble() - 0.5) * 0.3,
                0.0,
                flame == OPEN ? 0.12 : 0.03,
                0.0);
            if (flame == OPEN) worldObj.spawnParticle("smoke", posX, posY + 2.6, posZ, 0.0, 0.08, 0.0);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpSteps = steps + 2;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void setVelocity(double x, double y, double z) {}

    // --- Saving -------------------------------------------------------------------------------------------------

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        kelvin = tag.hasKey("Kelvin") ? tag.getDouble("Kelvin") : BalloonPhysics.AMBIENT_K;
        climb = tag.getDouble("Climb");
        vx = tag.getDouble("Vx");
        vz = tag.getDouble("Vz");
        coal = tag.getInteger("Coal");
        charcoal = tag.getInteger("Charcoal");
        burnLeft = tag.getDouble("BurnLeft");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setDouble("Kelvin", kelvin);
        tag.setDouble("Climb", climb);
        tag.setDouble("Vx", vx);
        tag.setDouble("Vz", vz);
        tag.setInteger("Coal", coal);
        tag.setInteger("Charcoal", charcoal);
        tag.setDouble("BurnLeft", burnLeft);
    }
}
