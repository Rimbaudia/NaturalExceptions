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

    public static void register() {
        keyGearAbility = new net.minecraft.client.settings.KeyBinding(
            "key.naturalexceptions.gear_ability",
            org.lwjgl.input.Keyboard.KEY_V,
            naturalexceptions.minecraft.knowledge.client.NEKnowledgeKeys.CATEGORY);
        cpw.mods.fml.client.registry.ClientRegistry.registerKeyBinding(keyGearAbility);
        FMLCommonHandler.instance()
            .bus()
            .register(new GearClientEffects());
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

    /** Tells the server; continuous actions only every few ticks. */
    private void report(PacketGearAction.Action action, boolean immediately) {
        if (!immediately && reportTimer-- > 0) return;
        reportTimer = REPORT_EVERY;
        NENetwork.CHANNEL.sendToServer(new PacketGearAction(action));
    }
}
