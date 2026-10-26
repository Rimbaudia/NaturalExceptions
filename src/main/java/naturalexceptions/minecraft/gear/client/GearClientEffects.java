package naturalexceptions.minecraft.gear.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.Vec3;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import naturalexceptions.minecraft.gear.PlayerGear;
import naturalexceptions.minecraft.gear.Treasure;
import naturalexceptions.minecraft.item.ItemTreasureGear;
import naturalexceptions.minecraft.network.NENetwork;
import naturalexceptions.minecraft.network.PacketGearAction;
import naturalexceptions.minecraft.network.PacketGearSync;

/**
 * The owner's side of movement gear. Like vanilla jumping, the client moves its own player; it tells the server
 * so the fall is forgiven and everyone sees and hears the effect.
 */
@SideOnly(Side.CLIENT)
public final class GearClientEffects {

    static final double CLOUD_JUMP_VELOCITY = 0.52;
    static final double GLIDE_FALL_SPEED = -0.11;
    static final double GLIDE_DRIFT = 0.012;
    static final double GLIDE_MAX_SPEED = 0.42;
    static final double CLIMB_SPEED_ONE = 0.17;
    static final double CLIMB_SPEED_PAIR = 0.25;
    static final double CLAW_SLIDE_SPEED = -0.10;
    private static final int REPORT_EVERY = 5;

    /** The key for a worn relic's active power (the Galeheart Belt's dash). */
    public static net.minecraft.client.settings.KeyBinding keyGearAbility;

    private boolean jumpWasDown;
    private long lastDash = Long.MIN_VALUE;
    private int airJumpsLeft;
    private int ticksInAir;
    private int reportTimer;
    private boolean raisedStep;
    /** Cloud Treads: the fastest they let you fall, in blocks a tick. */
    static final double CLOUD_TREAD_FALL_SPEED = -0.4;

    public static void register() {
        keyGearAbility = new net.minecraft.client.settings.KeyBinding(
            "key.naturalexceptions.gear_ability",
            org.lwjgl.input.Keyboard.KEY_V,
            naturalexceptions.minecraft.knowledge.client.NEKnowledgeKeys.CATEGORY);
        cpw.mods.fml.client.registry.ClientRegistry.registerKeyBinding(keyGearAbility);
        FMLCommonHandler.instance()
            .bus()
            .register(new GearClientEffects());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new GearClientSight());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;
        net.minecraft.item.ItemStack[] pending = PacketGearSync.clientPending;
        if (pending != null) {
            PacketGearSync.clientPending = null;
            PlayerGear.get(player)
                .applySync(pending);
        }
        footwear(player);
        cloudClimb(player, mc);
        mourn(player);
        // Keys typed into chat or any other screen are text, not movement: space in chat is not a jump.
        if (mc.isGamePaused() || mc.currentScreen != null || player.capabilities.isFlying || player.isRiding()) {
            jumpWasDown = false;
            return;
        }

        if (keyGearAbility != null && keyGearAbility.isPressed()
            && ItemTreasureGear.worn(player, Treasure.GALEHEART_BELT) > 0) {
            long now = player.worldObj.getTotalWorldTime();
            if (naturalexceptions.minecraft.gear.RelicRules
                .ready(lastDash, now, naturalexceptions.minecraft.gear.RelicRules.DASH_COOLDOWN_TICKS)) {
                lastDash = now;
                Vec3 look = player.getLookVec();
                double[] v = naturalexceptions.minecraft.gear.RelicRules.dash(look.xCoord, look.yCoord, look.zCoord);
                player.motionX = v[0];
                player.motionY = v[1];
                player.motionZ = v[2];
                player.fallDistance = 0.0F;
                for (int i = 0; i < 12; i++) player.worldObj.spawnParticle(
                    "cloud",
                    player.posX - v[0] * i * 0.15,
                    player.posY - 1.0,
                    player.posZ - v[2] * i * 0.15,
                    0.0,
                    0.02,
                    0.0);
                report(PacketGearAction.Action.DASH, true);
                return;
            }
        }

        boolean jumpDown = mc.gameSettings.keyBindJump.getIsKeyPressed();
        boolean jumpPressed = jumpDown && !jumpWasDown;
        jumpWasDown = jumpDown;
        boolean inFluid = player.isInWater() || player.handleLavaMovement();

        if (player.onGround || inFluid) {
            airJumpsLeft = ItemTreasureGear.worn(player, Treasure.CLOUD_FLASK) > 0 ? 1 : 0;
            ticksInAir = 0;
        } else {
            ticksInAir++;
        }

        if (!player.onGround && !inFluid && jumpPressed && ticksInAir > 2 && airJumpsLeft > 0) {
            airJumpsLeft--;
            player.motionY = CLOUD_JUMP_VELOCITY;
            player.fallDistance = 0.0F;
            report(PacketGearAction.Action.DOUBLE_JUMP, true);
            return;
        }

        int claws = ItemTreasureGear.worn(player, Treasure.CLIMBING_CLAWS);
        if (claws > 0 && player.isCollidedHorizontally && !inFluid) {
            if (jumpDown || player.moveForward > 0.0F) player.motionY = claws >= 2 ? CLIMB_SPEED_PAIR : CLIMB_SPEED_ONE;
            else if (player.isSneaking()) player.motionY = 0.0;
            else player.motionY = Math.max(player.motionY, CLAW_SLIDE_SPEED);
            player.fallDistance = 0.0F;
            report(PacketGearAction.Action.CLIMB, false);
            return;
        }

        if (!player.onGround && !inFluid
            && jumpDown
            && player.motionY < 0.0
            && ItemTreasureGear.worn(player, Treasure.GLIDER_CLOAK) > 0) {
            player.motionY = Math.max(player.motionY, GLIDE_FALL_SPEED);
            Vec3 look = player.getLookVec();
            double vx = player.motionX + look.xCoord * GLIDE_DRIFT;
            double vz = player.motionZ + look.zCoord * GLIDE_DRIFT;
            double speed = Math.sqrt(vx * vx + vz * vz);
            if (speed > GLIDE_MAX_SPEED) {
                vx *= GLIDE_MAX_SPEED / speed;
                vz *= GLIDE_MAX_SPEED / speed;
            }
            player.motionX = vx;
            player.motionZ = vz;
            player.fallDistance = 0.0F;
            report(PacketGearAction.Action.GLIDE, false);
        }
    }

    /** Mourner's Veil: the restless dead about you glow faintly. */
    private static void mourn(EntityPlayerSP player) {
        if (player.ticksExisted % 8 != 0 || ItemTreasureGear.worn(player, Treasure.MOURNERS_VEIL) == 0) return;
        for (Object o : player.worldObj.getEntitiesWithinAABB(
            net.minecraft.entity.EntityLivingBase.class,
            player.boundingBox.expand(24.0, 12.0, 24.0))) {
            net.minecraft.entity.EntityLivingBase living = (net.minecraft.entity.EntityLivingBase) o;
            if (living == player || living.getCreatureAttribute() != net.minecraft.entity.EnumCreatureAttribute.UNDEAD)
                continue;
            for (int i = 0; i < 3; i++) player.worldObj.spawnParticle(
                "instantSpell",
                living.posX + (player.getRNG()
                    .nextDouble() - 0.5) * living.width,
                living.boundingBox.minY + player.getRNG()
                    .nextDouble() * living.height,
                living.posZ + (player.getRNG()
                    .nextDouble() - 0.5) * living.width,
                0.0,
                0.0,
                0.0);
        }
    }

    /** In cloud, holding jump climbs back up out of it, as one swims up through water. */
    private static void cloudClimb(EntityPlayerSP player, Minecraft mc) {
        if (naturalexceptions.minecraft.registry.NEBlocks.cloud == null || mc.currentScreen != null
            || !player.movementInput.jump) return;
        int x = net.minecraft.util.MathHelper.floor_double(player.posX);
        int z = net.minecraft.util.MathHelper.floor_double(player.posZ);
        int feet = net.minecraft.util.MathHelper.floor_double(player.boundingBox.minY + 0.1);
        net.minecraft.block.Block cloud = naturalexceptions.minecraft.registry.NEBlocks.cloud;
        if (player.worldObj.getBlock(x, feet, z) == cloud || player.worldObj.getBlock(x, feet + 1, z) == cloud)
            player.motionY = CLOUD_CLIMB;
    }

    /** How fast holding jump climbs up through cloud, in blocks a tick. */
    static final double CLOUD_CLIMB = 0.12;

    /** Gale Striders' higher step; Cloud Treads' gentle fall and, with a pair, footing on water and on cloud. */
    private void footwear(EntityPlayerSP player) {
        if (ItemTreasureGear.worn(player, Treasure.GALE_STRIDERS) >= 2) {
            player.stepHeight = naturalexceptions.minecraft.gear.HatAndShoeEffects.PAIR_STEP_HEIGHT;
            raisedStep = true;
        } else if (raisedStep) {
            player.stepHeight = naturalexceptions.minecraft.gear.HatAndShoeEffects.NORMAL_STEP_HEIGHT;
            raisedStep = false;
        }
        if (player.capabilities.isFlying || player.isRiding()) return;
        int treads = ItemTreasureGear.worn(player, Treasure.CLOUD_TREADS);
        if (treads == 0) return;
        if (treads >= 2 && !player.isSneaking() && standOnWater(player)) return;
        if (!player.onGround && !player.isInWater() && player.motionY < CLOUD_TREAD_FALL_SPEED) {
            player.motionY = CLOUD_TREAD_FALL_SPEED;
            player.fallDistance = 0.0F;
        }
    }

    /** Holds the feet at the top of the water they are on or have just dropped into; sneaking sinks. */
    private static boolean standOnWater(EntityPlayerSP player) {
        if (player.motionY > 0.0) return false; // jumping off it
        double feet = player.boundingBox.minY;
        int x = net.minecraft.util.MathHelper.floor_double(player.posX);
        int z = net.minecraft.util.MathHelper.floor_double(player.posZ);
        int y = net.minecraft.util.MathHelper.floor_double(feet - 0.01);
        net.minecraft.world.World world = player.worldObj;
        if (world.getBlock(x, y, z)
            .getMaterial() != net.minecraft.block.material.Material.water
            || world.getBlock(x, y + 1, z)
                .getMaterial() != net.minecraft.block.material.Material.air)
            return false;
        double lift = y + 1.0 - feet;
        if (lift > 0.5) return false; // already under: swim up as usual
        if (lift > 0.0) {
            player.boundingBox.offset(0.0, lift, 0.0);
            player.posY += lift;
        }
        player.motionY = 0.0;
        player.onGround = true;
        player.fallDistance = 0.0F;
        return true;
    }

    /** Tells the server; continuous actions only every few ticks. */
    private void report(PacketGearAction.Action action, boolean immediately) {
        if (!immediately && reportTimer-- > 0) return;
        reportTimer = REPORT_EVERY;
        NENetwork.CHANNEL.sendToServer(new PacketGearAction(action));
    }
}
